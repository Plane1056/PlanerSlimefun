package com.wwsf.artillery;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.util.Messages;

public final class MachineGunHeatManager {

    private static final Map<Location, HeatData> heatByLocation = new ConcurrentHashMap<>();
    private static final Map<Location, BukkitTask> cooldownTasks = new ConcurrentHashMap<>();

    private static final double DEFAULT_MAX_HEAT = 100.0;
    private static final double DEFAULT_HEAT_PER_SHOT = 12.0;
    private static final double DEFAULT_DISSIPATION = 0.5;
    private static final long DEFAULT_COOLDOWN_TICKS = 200L;

    private MachineGunHeatManager() {
    }

    public static void addHeat(Location loc, double amount) {
        if (loc == null) {
            return;
        }
        heatByLocation.compute(loc, (k, data) -> {
            if (data == null) {
                double maxHeat = getMaxHeat(loc);
                if (amount >= maxHeat) {
                    triggerOverheat(loc);
                    return new HeatData(maxHeat, true);
                }
                return new HeatData(amount, false);
            }
            if (data.overheated) {
                return data;
            }
            double newHeat = data.heat + amount;
            double maxHeat = getMaxHeat(loc);
            if (newHeat >= maxHeat) {
                triggerOverheat(loc);
                return new HeatData(maxHeat, true);
            }
            return new HeatData(newHeat, false);
        });
    }

    public static boolean isOverheated(Location loc) {
        if (loc == null) {
            return false;
        }
        HeatData data = heatByLocation.get(loc);
        return data != null && data.overheated;
    }

    public static double getHeatPercent(Location loc) {
        if (loc == null) {
            return 0.0;
        }
        HeatData data = heatByLocation.get(loc);
        if (data == null) {
            return 0.0;
        }
        if (data.overheated) {
            return 1.0;
        }
        return Math.min(1.0, data.heat / getMaxHeat(loc));
    }

    public static void triggerOverheat(Location loc) {
        if (loc == null) {
            return;
        }
        heatByLocation.compute(loc, (k, data) -> new HeatData(getMaxHeat(loc), true));

        BukkitTask existing = cooldownTasks.remove(loc);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                heatByLocation.remove(loc);
                cooldownTasks.remove(loc);
            }
        }.runTaskLater(WWSFPlugin.getInstance(), getCooldownTicks(loc));
        cooldownTasks.put(loc, task);
    }

    public static void tick() {
        for (Map.Entry<Location, HeatData> entry : heatByLocation.entrySet()) {
            HeatData data = entry.getValue();
            if (!data.overheated && data.heat > 0) {
                double newHeat = Math.max(0, data.heat - getDissipationPerTick(entry.getKey()));
                if (newHeat <= 0) {
                    heatByLocation.remove(entry.getKey());
                } else {
                    entry.setValue(new HeatData(newHeat, false));
                }
            }
        }
    }

    private static double getMaxHeat(Location loc) {
        return ItemConfigHelper.getDouble("WWSF_MAXIM_MACHINE_GUN_TRIGGER", "machine-gun.max-heat", DEFAULT_MAX_HEAT);
    }

    public static double getHeatPerShot() {
        return ItemConfigHelper.getDouble("WWSF_MAXIM_MACHINE_GUN_TRIGGER", "machine-gun.heat-per-shot", DEFAULT_HEAT_PER_SHOT);
    }

    private static double getDissipationPerTick(Location loc) {
        return ItemConfigHelper.getDouble("WWSF_MAXIM_MACHINE_GUN_TRIGGER", "machine-gun.heat-dissipation", DEFAULT_DISSIPATION);
    }

    private static long getCooldownTicks(Location loc) {
        return (long) ItemConfigHelper.getDouble("WWSF_MAXIM_MACHINE_GUN_TRIGGER", "machine-gun.overheat-cooldown-ticks", DEFAULT_COOLDOWN_TICKS);
    }

    private record HeatData(double heat, boolean overheated) {
    }
}
