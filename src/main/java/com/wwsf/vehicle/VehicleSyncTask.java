package com.wwsf.vehicle;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import com.wwsf.WWSFPlugin;
import com.wwsf.items.AbstractArtilleryBlock;
import com.wwsf.items.TriggerBlock;

public final class VehicleSyncTask {

    private static final String CRAFT_CLASS = "net.countercraft.movecraft.craft.Craft";
    private static final String HITBOX_CLASS = "net.countercraft.movecraft.util.hitboxes.HitBox";
    private static final String MOVE_LOCATION_CLASS = "net.countercraft.movecraft.MovecraftLocation";
    private static final String BASE_CRAFT_CLASS = "net.countercraft.movecraft.craft.BaseCraft";
    private static final String PLAYER_CRAFT_CLASS = "net.countercraft.movecraft.craft.PlayerCraft";

    private static final Map<java.util.UUID, CacheEntry> vesselCache = new ConcurrentHashMap<>();

    private VehicleSyncTask() {
    }

    public static void start(WWSFPlugin plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!VehicleQuery.isAvailable()) {
                    return;
                }
                try {
                    tick(plugin);
                } catch (Throwable t) {
                    plugin.getLogger().fine("[VehicleSyncTask] tick failed: " + t.getMessage());
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private static void tick(WWSFPlugin plugin) {
        if (!VehicleQuery.isAvailable()) {
            return;
        }

        try {
            Class<?> craftClass = Class.forName(CRAFT_CLASS);
            Method getCrafts = craftClass.getMethod("getCrafts");
            Object crafts = getCrafts.invoke(null);
            if (!(crafts instanceof java.util.Collection<?> collection)) {
                return;
            }

            Set<java.util.UUID> seen = new HashSet<>();
            for (Object craft : collection) {
                Object uuidObj = craftClass.getMethod("getUUID").invoke(craft);
                if (!(uuidObj instanceof java.util.UUID vesselId)) {
                    continue;
                }
                seen.add(vesselId);

                Object worldObj = craftClass.getMethod("getWorld").invoke(craft);
                if (!(worldObj instanceof org.bukkit.World world)) {
                    continue;
                }

                CacheEntry prev = vesselCache.get(vesselId);
                if (prev != null && world.equals(prev.world()) && !hasMoved(craft, prev)) {
                    continue;
                }

                Object hitBox = craftClass.getMethod("getHitBox").invoke(craft);
                if (hitBox == null) {
                    continue;
                }

                java.util.UUID finalVesselId = vesselId;
                java.util.Set<org.bukkit.Location> oldCards = VehicleArtilleryRegistry.getForVessel(finalVesselId);
                java.util.Set<org.bukkit.Location> found = new HashSet<>();
                Class<?> moveLocClass = Class.forName(MOVE_LOCATION_CLASS);

                for (Object locObj : (Iterable<Object>) hitBox) {
                    int x = (int) moveLocClass.getMethod("getX").invoke(locObj);
                    int y = (int) moveLocClass.getMethod("getY").invoke(locObj);
                    int z = (int) moveLocClass.getMethod("getZ").invoke(locObj);
                    Block block = new org.bukkit.Location(world, x, y, z).getBlock();
                    if (AbstractArtilleryBlock.getArtilleryAt(block) != null || com.wwsf.items.TriggerBlock.getTriggerAt(block) != null) {
                        found.add(block.getLocation());
                    }
                }

                for (org.bukkit.Location loc : oldCards) {
                    if (!found.contains(loc)) {
                        VehicleArtilleryRegistry.unregister(loc);
                    }
                }
                for (org.bukkit.Location loc : found) {
                    if (!oldCards.contains(loc)) {
                        VehicleArtilleryRegistry.register(finalVesselId, loc);
                    }
                }

                Snapshot snap = snapshot(craft);
                if (prev != null) {
                    int deltaX = snap.dx - prev.snapshot().dx;
                    int deltaY = snap.dy - prev.snapshot().dy;
                    int deltaZ = snap.dz - prev.snapshot().dz;
                    if (deltaX != 0 || deltaY != 0 || deltaZ != 0) {
                        com.wwsf.multiblock.MarkerManager.moveMarkersForVessel(vesselId, new org.bukkit.util.Vector(deltaX, deltaY, deltaZ));
                    }
                }

                vesselCache.put(vesselId, new CacheEntry(world, snap));
            }

            Iterator<java.util.UUID> it = vesselCache.keySet().iterator();
            while (it.hasNext()) {
                java.util.UUID removed = it.next();
                if (!seen.contains(removed)) {
                    java.util.Set<org.bukkit.Location> remaining = VehicleArtilleryRegistry.getForVessel(removed);
                    for (org.bukkit.Location loc : remaining) {
                        VehicleArtilleryRegistry.unregister(loc);
                        plugin.getAimingManager().cancelAimAt(loc);
                    }
                    com.wwsf.multiblock.MarkerManager.clearAllForVessel(removed);
                    it.remove();
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("[VehicleSyncTask] tick failed: " + t.getMessage());
        }
    }

    private static boolean hasMoved(Object craft, CacheEntry prev) {
        try {
            Class<?> baseCraftClass = Class.forName(BASE_CRAFT_CLASS);
            Class<?> moveLocClass = Class.forName(MOVE_LOCATION_CLASS);

            Method getLastTranslation = baseCraftClass.getMethod("getLastTranslation");
            Object trans = getLastTranslation.invoke(craft);
            if (trans == null) {
                return false;
            }
            int dx = (int) moveLocClass.getMethod("getX").invoke(trans);
            int dy = (int) moveLocClass.getMethod("getY").invoke(trans);
            int dz = (int) moveLocClass.getMethod("getZ").invoke(trans);

            return prev.snapshot().dx != dx || prev.snapshot().dy != dy || prev.snapshot().dz != dz;
        } catch (Throwable t) {
            return true;
        }
    }

    private record Snapshot(int dx, int dy, int dz) {}

    private static Snapshot snapshot(Object craft) {
        try {
            Class<?> baseCraftClass = Class.forName(BASE_CRAFT_CLASS);
            Class<?> moveLocClass = Class.forName(MOVE_LOCATION_CLASS);

            Method getLastTranslation = baseCraftClass.getMethod("getLastTranslation");
            Object trans = getLastTranslation.invoke(craft);
            if (trans == null) {
                return new Snapshot(0, 0, 0);
            }
            int dx = (int) moveLocClass.getMethod("getX").invoke(trans);
            int dy = (int) moveLocClass.getMethod("getY").invoke(trans);
            int dz = (int) moveLocClass.getMethod("getZ").invoke(trans);
            return new Snapshot(dx, dy, dz);
        } catch (Throwable t) {
            return new Snapshot(0, 0, 0);
        }
    }

    private static final class CacheEntry {
        private final org.bukkit.World world;
        private final Snapshot snapshot;

        CacheEntry(org.bukkit.World world, Snapshot snapshot) {
            this.world = world;
            this.snapshot = snapshot;
        }

        org.bukkit.World world() { return world; }
        Snapshot snapshot() { return snapshot; }
    }
}
