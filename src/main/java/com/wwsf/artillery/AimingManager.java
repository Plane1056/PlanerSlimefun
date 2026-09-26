package com.wwsf.artillery;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.items.AbstractArtilleryBlock;
import com.wwsf.items.TriggerBlock;
import com.wwsf.multiblock.CannonDefinition;
import com.wwsf.util.Messages;
import com.wwsf.vehicle.VehicleQuery;

public class AimingManager {

    private final WWSFPlugin plugin;
    private final Map<UUID, ArtilleryAim> activeAims = new HashMap<>();
    private final Map<UUID, BukkitTask> previewTasks = new HashMap<>();
    private final Map<UUID, BossBar> reloadBars = new HashMap<>();
    private final Map<UUID, BossBar> heatBars = new HashMap<>();
    private final Map<UUID, Long> lastHeatBarUpdate = new HashMap<>();
    private final java.util.Map<Location, UUID> weaponInUse = new java.util.HashMap<>();

    public AimingManager(WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrates the weapon-in-use lock from an old cannon location to a new one.
     * Called when a Movecraft vessel moves so the lock follows the structure.
     */
    public void migrateWeaponLock(@Nonnull Location oldLoc, @Nonnull Location newLoc) {
        if (oldLoc == null || newLoc == null) {
            return;
        }
        UUID owner = weaponInUse.remove(oldLoc);
        if (owner != null) {
            weaponInUse.put(newLoc, owner);
        }
    }

    public void startAiming(Player player, Location cannonLocation, ArtillerySpec spec) {
        startAiming(player, cannonLocation, spec, null, null);
    }

    public void startAiming(Player player, Location cannonLocation, ArtillerySpec spec, Location barrelEnd, Vector barrelDir) {
        startAiming(player, cannonLocation, spec, barrelEnd, barrelDir, null);
    }

    public void startAiming(Player player, Location cannonLocation, ArtillerySpec spec, Location barrelEnd, Vector barrelDir, java.util.UUID vesselId) {
        startAiming(player, cannonLocation, spec, barrelEnd, barrelDir, vesselId, null);
    }

    public void startAiming(Player player, Location cannonLocation, ArtillerySpec spec, Location barrelEnd, Vector barrelDir, java.util.UUID vesselId, java.util.UUID markerId) {
        Location lockKey = cannonLocation != null ? cannonLocation : barrelEnd;
        UUID currentUser = lockKey != null ? weaponInUse.get(lockKey) : null;
        if (currentUser != null && !currentUser.equals(player.getUniqueId())) {
            Player other = plugin.getServer().getPlayer(currentUser);
            String name = other != null ? other.getName() : "Someone";
            player.sendMessage(Messages.color("&cThis weapon is currently in use by &f" + name + "&c."));
            return;
        }

        ArtilleryAim existing = activeAims.get(player.getUniqueId());
        if (existing != null && existing.cannonLocation() != null && existing.cannonLocation().equals(cannonLocation)) {
            if (existing.reloadTotalMs() > 0 && existing.isReloading()) {
                return;
            }
        }

        stopAiming(player);

        if (lockKey != null) {
            weaponInUse.put(lockKey, player.getUniqueId());
        }

        AbstractArtilleryBlock artillery = AbstractArtilleryBlock.getArtilleryAt(cannonLocation.getBlock());
        long fireDelayMs = artillery != null
            ? ItemConfigHelper.getLong(artillery, "aim.fire-delay-ms", 350L)
            : ItemConfigHelper.getLong(ItemConfigHelper.configId(spec.id()), "aim.fire-delay-ms", 350L);
        double aimDistance = artillery != null
            ? ItemConfigHelper.getDouble(artillery, "aim.max-distance", 10.0)
            : ItemConfigHelper.getDouble(ItemConfigHelper.configId(spec.id()), "aim.max-distance", 10.0);

        long readyAt = System.currentTimeMillis() + fireDelayMs;
        activeAims.put(player.getUniqueId(), new ArtilleryAim(cannonLocation.clone(), spec, readyAt, aimDistance, barrelEnd, barrelDir, 0L, vesselId, markerId));

        String trajectory = spec.isTurret() ? "direct" : spec.usesArc() ? "lobbed" : "direct";
        player.sendMessage(Messages.get(
            "messages.aiming",
            "&eAiming &7{weapon}&e — look and &fleft-click &7({trajectory} fire). &7Shift-right-click to cancel.",
            "{weapon}", Messages.color(spec.displayName()),
            "{trajectory}", trajectory
        ));

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (!isAiming(player)) {
                return;
            }
            ArtilleryAim aim = getAim(player);
            if (aim == null) {
                return;
            }
            if (!isCannonStillValid(aim)) {
                stopAiming(player);
                player.sendMessage(Messages.get("messages.cannon-destroyed",
                    "&cThe weapon was destroyed. Aiming cancelled."));
                return;
            }
            if (aim.cannonLocation().distance(player.getLocation()) > aim.aimDistance()) {
                stopAiming(player);
                player.sendMessage(Messages.get("messages.too-far", "&cYou moved too far from the weapon. Aiming cancelled."));
                return;
            }
            showPreview(player, aim);
        }, 0L, 4L);

        previewTasks.put(player.getUniqueId(), task);
    }

    private void showPreview(Player player, ArtilleryAim aim) {
        ArtillerySpec spec = aim.spec();
        double coneHalfWidth = ItemConfigHelper.getDouble(
            ItemConfigHelper.configId(spec.id()), "aim.cone-blocks", 3.0);

        ArtilleryAim refreshed = refreshAimForVessel(aim);
        if (refreshed == null) {
            stopAiming(player);
            player.sendMessage(Messages.get("messages.cannon-destroyed",
                "&cThe weapon was destroyed. Aiming cancelled."));
            return;
        }

        if (refreshed.cannonLocation() != null && !refreshed.cannonLocation().equals(aim.cannonLocation())) {
            activeAims.put(player.getUniqueId(), refreshed);
            migrateWeaponLock(aim.cannonLocation(), refreshed.cannonLocation());
        }

        Location preview;
        Location aimOrigin;
        if (refreshed.barrelEnd() != null && refreshed.barrelDir() != null) {
            preview = AimTargetResolver.resolveFromBarrel(
                player, refreshed.barrelEnd(), refreshed.barrelDir(), spec.maxRange(), spec.minRange(), coneHalfWidth);
            aimOrigin = refreshed.barrelEnd();
        } else {
            preview = AimTargetResolver.preview(player, spec.maxRange(), 0.0);
            aimOrigin = refreshed.cannonLocation();
        }

        double dist = aimOrigin.distance(preview);
        boolean valid = dist >= spec.minRange() && dist <= spec.maxRange();

        if (refreshed.barrelDir() != null) {
            drawBarrelViewSquare(player, refreshed.barrelDir(), coneHalfWidth, valid);
        }

        if (valid) {
            if (spec.usesArc()) {
                player.spawnParticle(Particle.SOUL_FIRE_FLAME, preview, 2, 0.2, 0.2, 0.2, 0);
                player.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, preview, 1, 0.15, 0.3, 0.15, 0.01);
            } else {
                Location muzzle = aimOrigin.clone().add(0.5, 1.25, 0.5);
                Vector flat = preview.toVector().subtract(muzzle.toVector());
                flat.setY(0);
                if (flat.lengthSquared() > 0.01) {
                    flat.normalize().multiply(0.55);
                    muzzle.add(flat);
                }
                drawDirectLine(player, muzzle, preview);
                player.spawnParticle(Particle.ELECTRIC_SPARK, preview, 10, 0.02, 0.02, 0.02, 0.05);
            }
        } else {
            player.spawnParticle(Particle.DAMAGE_INDICATOR, preview, 1, 0.2, 0.3, 0.2, 0);
        }

        updateReloadBar(player, refreshed);

        if (spec.hasCustomAmmo()) {
            updateHeatBar(player, refreshed);
        }
    }

    private ArtilleryAim refreshAimForVessel(ArtilleryAim aim) {
        java.util.UUID vesselId = aim.vesselId();
        if (vesselId == null) {
            return aim;
        }

        if (!VehicleQuery.isAvailable()) {
            return aim;
        }

        java.util.Optional<java.util.UUID> current = VehicleQuery.vesselIdAt(aim.cannonLocation());
        if (current.isPresent() && current.get().equals(vesselId)) {
            return aim;
        }

        if (aim.markerId() != null) {
            com.wwsf.multiblock.MarkerManager.MarkerData marker = com.wwsf.multiblock.MarkerManager.getMarkerData(aim.markerId());
            if (marker == null) {
                marker = com.wwsf.multiblock.MarkerManager.findMarkerAtLocation(aim.cannonLocation());
            }
            if (marker != null && vesselId.equals(marker.vesselId())) {
                Location origin = marker.core();
                Location barrelTip = marker.definition().getBarrelTip(origin, marker.rotation());
                Vector barrelDir = marker.definition().getBarrelDirection(marker.rotation());
                return new ArtilleryAim(origin, aim.spec(), aim.readyAtMs(), aim.aimDistance(), barrelTip, barrelDir, aim.reloadTotalMs(), vesselId, aim.markerId());
            }
            return null;
        }

        Object craft = VehicleQuery.getCraft(vesselId);
        if (craft == null) {
            return null;
        }

        try {
            Class<?> craftClass = Class.forName("net.countercraft.movecraft.craft.Craft");
            Class<?> hitboxClass = Class.forName("net.countercraft.movecraft.util.hitboxes.HitBox");
            Class<?> moveLocClass = Class.forName("net.countercraft.movecraft.MovecraftLocation");

            Object uuidObj = craftClass.getMethod("getUUID").invoke(craft);
            if (!(uuidObj instanceof java.util.UUID foundVesselId)) {
                return null;
            }
            if (!vesselId.equals(foundVesselId)) {
                return null;
            }

            Object hitBox = craftClass.getMethod("getHitBox").invoke(craft);

            for (Object oldLocObj : (Iterable<Object>) hitBox) {
                int x = (int) moveLocClass.getMethod("getX").invoke(oldLocObj);
                int y = (int) moveLocClass.getMethod("getY").invoke(oldLocObj);
                int z = (int) moveLocClass.getMethod("getZ").invoke(oldLocObj);
                org.bukkit.World world = (org.bukkit.World) craftClass.getMethod("getWorld").invoke(craft);
                if (world == null) continue;
                org.bukkit.Location loc = new org.bukkit.Location(world, x, y, z);
                TriggerBlock trigger = TriggerBlock.getTriggerAt(loc.getBlock());
                if (trigger != null) {
                    CannonDefinition def = trigger.findMatchingDefinition(loc.getBlock());
                    if (def != null) {
                        int[] rotated = def.rotateOffset(def.triggerOffsetX(), def.triggerOffsetZ(), def.getLastRotation());
                        Location origin = loc.clone().subtract(rotated[0], def.triggerOffsetY(), rotated[1]);
                        Location barrelTip = def.getBarrelTip(origin, def.getLastRotation());
                        Vector barrelDir = def.getBarrelDirection(def.getLastRotation());
                        org.bukkit.Location cannonLoc = loc.clone();
                        return new ArtilleryAim(cannonLoc, aim.spec(), aim.readyAtMs(), aim.aimDistance(), barrelTip, barrelDir, aim.reloadTotalMs(), vesselId, null);
                    }
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().fine("[AimingManager] vessel refresh failed: " + t.getMessage());
        }
        return null;
    }

    private void drawBarrelViewSquare(Player player, Vector barrelDir, double halfWidth, boolean valid) {
        Particle.DustOptions color = new Particle.DustOptions(valid ? org.bukkit.Color.LIME : org.bukkit.Color.RED, 1.0f);
        Vector flat = new Vector(barrelDir.getX(), 0, barrelDir.getZ()).normalize();
        Vector perp = new Vector(-flat.getZ(), 0, flat.getX());
        Vector up = new Vector(0, 1, 0);

        // Player-local square: the resolver uses this exact plane for its angular clamp.
        Location eye = player.getEyeLocation();
        Location center = eye.clone().add(flat.clone().multiply(AimTargetResolver.AIM_BOX_DISTANCE));

        Location tl = center.clone().add(perp.clone().multiply(-halfWidth)).add(up.clone().multiply(halfWidth));
        Location tr = center.clone().add(perp.clone().multiply(halfWidth)).add(up.clone().multiply(halfWidth));
        Location bl = center.clone().add(perp.clone().multiply(-halfWidth)).add(up.clone().multiply(-halfWidth));
        Location br = center.clone().add(perp.clone().multiply(halfWidth)).add(up.clone().multiply(-halfWidth));

        drawParticleLine(player, tl, tr, color);
        drawParticleLine(player, tr, br, color);
        drawParticleLine(player, br, bl, color);
        drawParticleLine(player, bl, tl, color);
    }

    private void drawParticleLine(Player player, Location from, Location to, Particle.DustOptions color) {
        Vector dir = to.toVector().subtract(from.toVector());
        double len = dir.length();
        if (len < 0.01) return;
        dir.normalize();
        for (double d = 0; d <= len; d += 0.3) {
            player.spawnParticle(Particle.DUST, from.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, color);
        }
    }

    private void drawDirectLine(Player player, Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        double len = dir.length();
        if (len < 1) {
            return;
        }
        dir.normalize();
        int points = (int) Math.min(20, len);
        Location point = from.clone();
        for (int i = 0; i < points; i++) {
            point.add(dir.clone().multiply(len / points));
            player.spawnParticle(Particle.ELECTRIC_SPARK, point, 5, 0.02, 0.02, 0.02, 0.05);
        }
    }

    public ArtilleryAim getAim(Player player) {
        return activeAims.get(player.getUniqueId());
    }

    public boolean isAiming(Player player) {
        return activeAims.containsKey(player.getUniqueId());
    }

    public UUID getWeaponUser(Location location) {
        if (location == null) {
            return null;
        }
        return weaponInUse.get(location);
    }

    public void stopAiming(Player player) {
        stopAiming(player.getUniqueId());
    }

    public void stopAiming(UUID uuid) {
        ArtilleryAim aim = activeAims.remove(uuid);
        if (aim != null && aim.cannonLocation() != null) {
            releaseWeaponLock(aim.cannonLocation(), uuid);
        }
        BukkitTask task = previewTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        removeReloadBar(uuid);
        removeHeatBar(uuid);
    }

    private void releaseWeaponLock(Location location, UUID expectedPlayer) {
        if (location == null) {
            return;
        }
        UUID owner = weaponInUse.get(location);
        if (owner != null && owner.equals(expectedPlayer)) {
            weaponInUse.remove(location);
        }
    }

    /**
     * Cancels the aim of any player currently aiming at the given weapon location. Used when a
     * multiblock is dismantled/destroyed so players are kicked out of aiming and shooting.
     */
    public void cancelAimAt(Location location) {
        if (location == null) {
            return;
        }
        for (UUID uuid : new HashSet<>(activeAims.keySet())) {
            ArtilleryAim aim = activeAims.get(uuid);
            if (aim != null && location.equals(aim.cannonLocation())) {
                Player player = plugin.getServer().getPlayer(uuid);
                if (player != null) {
                    stopAiming(uuid);
                    player.sendMessage(Messages.get("messages.cannon-destroyed",
                        "&cThe weapon was destroyed. Aiming cancelled."));
                } else {
                    stopAiming(uuid);
                }
            }
        }
        weaponInUse.remove(location);
    }

    /**
     * Returns true if the player is aiming and the weapon structure they are aiming at is still
     * intact (block present and the cannon multiblock still validates).
     */
    public boolean isCannonValid(Player player) {
        ArtilleryAim aim = getAim(player);
        return aim != null && isCannonStillValid(aim);
    }

    private boolean isCannonStillValid(ArtilleryAim aim) {
        if (aim == null) {
            return false;
        }

        if (aim.markerId() != null) {
            com.wwsf.multiblock.MarkerManager.MarkerData marker = com.wwsf.multiblock.MarkerManager.getMarkerData(aim.markerId());
            if (marker == null) {
                // The marker may have been re-summoned (new UUID) or briefly absent from the
                // in-memory map. Re-locate it by its core location before giving up.
                marker = com.wwsf.multiblock.MarkerManager.findMarkerAtLocation(aim.cannonLocation());
            }
            if (marker != null) {
                return com.wwsf.multiblock.MarkerManager.isCoreValid(marker);
            }
            return false;
        }

        Location loc = aim.cannonLocation();
        if (loc == null) {
            return false;
        }
        Block block = loc.getBlock();
        AbstractArtilleryBlock artillery = AbstractArtilleryBlock.getArtilleryAt(block);
        if (artillery == null) {
            return false;
        }
        if (artillery instanceof TriggerBlock trigger) {
            return trigger.findMatchingDefinition(block) != null;
        }
        return true;
    }

    public void clearAll() {
        activeAims.clear();
        previewTasks.forEach((id, task) -> task.cancel());
        previewTasks.clear();
        reloadBars.forEach((id, bar) -> bar.removeAll());
        reloadBars.clear();
        heatBars.forEach((id, bar) -> bar.removeAll());
        heatBars.clear();
        lastHeatBarUpdate.clear();
        weaponInUse.clear();
    }

    /**
     * Puts the weapon into its post-shot reload. The player stays in aiming mode; firing is
     * blocked by {@link ArtilleryAim#canFire()} until the reload elapses, and a boss bar shows
     * progress.
     */
    public void applyReload(Player player, long reloadMs) {
        ArtilleryAim aim = getAim(player);
        if (aim == null) {
            return;
        }
        long readyAt = System.currentTimeMillis() + reloadMs;
        ArtilleryAim updated = new ArtilleryAim(
            aim.cannonLocation(), aim.spec(), readyAt, aim.aimDistance(),
            aim.barrelEnd(), aim.barrelDir(), reloadMs, aim.vesselId(), aim.markerId());
        activeAims.put(player.getUniqueId(), updated);
        updateReloadBar(player, updated);
    }

    private void updateReloadBar(Player player, ArtilleryAim aim) {
        if (aim.reloadTotalMs() < 300) {
            removeReloadBar(player.getUniqueId());
            return;
        }
        BossBar bar = reloadBars.get(player.getUniqueId());
        if (aim.isReloading()) {
            if (bar == null) {
                bar = Bukkit.createBossBar(
                    Messages.color("&eReloading " + aim.spec().displayName()),
                    BarColor.YELLOW, BarStyle.SEGMENTED_10);
                bar.addPlayer(player);
                reloadBars.put(player.getUniqueId(), bar);
            }
            bar.setProgress((float) Math.min(1.0, Math.max(0.0, aim.reloadProgress())));
        } else {
            removeReloadBar(player.getUniqueId());
        }
    }

    private void removeReloadBar(UUID uuid) {
        BossBar bar = reloadBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
    }

    private void updateHeatBar(Player player, ArtilleryAim aim) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastUpdate = lastHeatBarUpdate.get(uuid);
        if (lastUpdate != null && now - lastUpdate < 500) {
            return;
        }
        lastHeatBarUpdate.put(uuid, now);

        double heat = MachineGunHeatManager.getHeatPercent(aim.cannonLocation());
        BossBar bar = heatBars.get(uuid);
        if (bar == null) {
            bar = Bukkit.createBossBar(
                Messages.color("&cHeat: 0%"),
                BarColor.YELLOW, BarStyle.SEGMENTED_10);
            bar.addPlayer(player);
            heatBars.put(uuid, bar);
        }

        int heatPercent = (int) (heat * 100);
        bar.setTitle(Messages.color("&cHeat: " + heatPercent + "%"));
        bar.setProgress((float) Math.max(0.0, 1.0 - heat));

        if (MachineGunHeatManager.isOverheated(aim.cannonLocation())) {
            bar.setColor(BarColor.RED);
            bar.setTitle(Messages.color("&cOVERHEATED"));
        } else if (heat > 0.75) {
            bar.setColor(BarColor.RED);
        } else if (heat > 0.5) {
            bar.setColor(BarColor.YELLOW);
        } else if (heat > 0.25) {
            bar.setColor(BarColor.YELLOW);
        } else {
            bar.setColor(BarColor.GREEN);
        }
    }

    private void removeHeatBar(UUID uuid) {
        BossBar bar = heatBars.remove(uuid);
        if (bar != null) {
            bar.removeAll();
        }
        lastHeatBarUpdate.remove(uuid);
    }

    public record ArtilleryAim(Location cannonLocation, ArtillerySpec spec, long readyAtMs, double aimDistance, Location barrelEnd, Vector barrelDir, long reloadTotalMs, java.util.UUID vesselId, java.util.UUID markerId) {
        public boolean canFire() {
            return System.currentTimeMillis() >= readyAtMs;
        }

        public boolean isReloading() {
            return reloadTotalMs > 0 && System.currentTimeMillis() < readyAtMs;
        }

        public double reloadProgress() {
            if (reloadTotalMs <= 0) {
                return 1.0;
            }
            long remaining = readyAtMs - System.currentTimeMillis();
            if (remaining <= 0) {
                return 1.0;
            }
            return 1.0 - (double) remaining / reloadTotalMs;
        }

        public boolean isValid(Player player) {
            if (player == null || cannonLocation == null) {
                return false;
            }
            return cannonLocation.distance(player.getLocation()) <= aimDistance;
        }
    }
}
