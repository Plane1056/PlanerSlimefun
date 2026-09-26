package com.wwsf.artillery;

import java.util.EnumSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.ammo.AmmoItem;
import com.wwsf.ammo.AmmoRegistry;
import com.wwsf.ammo.MachineGunAmmoItem;
import com.wwsf.artillery.AmmoFeeder;
import com.wwsf.artillery.ArtillerySpec;
import com.wwsf.artillery.ShellProfile;
import com.wwsf.artillery.ShellTemplates;
import com.wwsf.artillery.ShellType;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.util.Messages;
import com.wwsf.vehicle.VehicleQuery;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

public class ArtilleryCombatListener implements Listener {

    private final WWSFPlugin plugin;

    public ArtilleryCombatListener(WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Player player = event.getPlayer();
        if (handleFire(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onArmSwing(PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        if (handleFire(player)) {
            event.setCancelled(true);
        }
    }

    private boolean handleFire(Player player) {
        if (plugin.getAimingManager().isAiming(player)) {
            fireArtillery(player);
            return true;
        }

        return false;
    }

    private void fireArtillery(Player player) {
        AimingManager.ArtilleryAim aim = plugin.getAimingManager().getAim(player);
        if (aim == null) {
            return;
        }

        if (!aim.canFire()) {
            player.sendActionBar(Messages.color("&cWeapon not ready yet — wait a moment."));
            return;
        }

        if (!aim.isValid(player)) {
            plugin.getAimingManager().stopAiming(player);
            player.sendMessage(Messages.get("messages.too-far", "&cYou moved too far from the weapon. Aiming cancelled."));
            return;
        }

        if (!plugin.getAimingManager().isCannonValid(player)) {
            plugin.getAimingManager().stopAiming(player);
            player.sendMessage(Messages.get("messages.cannon-destroyed",
                "&cThe weapon was destroyed. Aiming cancelled."));
            return;
        }

        ArtillerySpec spec = aim.spec();

        java.util.UUID vesselId = aim.vesselId();
        if (vesselId != null && VehicleQuery.isAvailable()) {
            java.util.Optional<Double> speedOpt = VehicleQuery.getSpeed(vesselId);
            if (speedOpt.isPresent() && speedOpt.get() > 3.0) {
                player.sendMessage(Messages.color("&eThe ship's motion is too violent to fire safely."));
                return;
            }
        }

        ShellType shellType = null;
        if (spec.hasCustomAmmo()) {
            if (MachineGunHeatManager.isOverheated(aim.cannonLocation())) {
                player.sendMessage(Messages.color("&c&lMAXIM OVERHEATED — cooling down..."));
                player.sendActionBar(Messages.color("&cOVERHEATED"));
                return;
            }
            if (!AmmoFeeder.hasCustomAmmo(player, aim.cannonLocation(), spec.ammoItemId())) {
                player.sendMessage(Messages.get("messages.no-ammo",
                    "&cNo compatible ammunition. Load a chest beside the weapon or carry shells."));
                return;
            }
            if (!AmmoFeeder.consumeCustomAmmo(player, aim.cannonLocation(), spec.ammoItemId())) {
                return;
            }
        } else {
            shellType = AmmoFeeder.resolveAvailableType(
                player, aim.cannonLocation(), allowedShellTypes(spec));
            if (shellType == null) {
                player.sendMessage(Messages.get("messages.no-ammo",
                    "&cNo compatible ammunition. Load a chest beside the weapon or carry shells."));
                return;
            }
            if (!AmmoFeeder.consume(plugin, player, aim.cannonLocation(), shellType)) {
                return;
            }
        }

        ShellProfile shell;
        if (spec.hasCustomAmmo()) {
            shell = ShellTemplates.forType(ShellType.STANDARD, spec.trajectory());
        } else {
            shell = ShellTemplates.forType(shellType, spec.trajectory());
        }

        Location origin = aim.cannonLocation();
        Location barrelEnd = aim.barrelEnd();
        Vector barrelDir = aim.barrelDir();

        org.bukkit.Location target;
        if (barrelEnd != null && barrelDir != null) {
            origin = barrelEnd.clone();
            double coneHalfWidth = ItemConfigHelper.getDouble(
                ItemConfigHelper.configId(spec.id()), "aim.cone-blocks", 3.0);
            target = AimTargetResolver.resolveFromBarrel(
                player, barrelEnd, barrelDir, spec.maxRange(), spec.minRange(), coneHalfWidth);
        } else {
            double minDistance = plugin.getConfig().getDouble("weapons.min-fire-distance", 15.0);
            target = AimTargetResolver.resolve(player, origin, spec.maxRange(), minDistance);
        }

        double effectiveMin = getMinDistance();
        if (origin.distance(target) < effectiveMin) {
            player.sendMessage(Messages.get("messages.target-too-close",
                "&cTarget too close. Minimum distance: {distance} blocks.",
                "{distance}", String.format("%.1f", effectiveMin)));
            plugin.getAimingManager().stopAiming(player);
            return;
        }

        Vector vesselVelocity = null;
        if (vesselId != null && VehicleQuery.isAvailable()) {
            vesselVelocity = com.wwsf.vehicle.VehicleQuery.getLastTranslation(vesselId).orElse(null);
        }

        ProjectileLauncher.fire(plugin, player, origin, target, spec, shell,
            spec.minArcHeight(), vesselVelocity, spec.minRange());

        if (!spec.hasCustomAmmo() && shellType != null) {
            SpentCasingBlock.place(casingOrigin(aim), barrelDir, shellType);
        }

        if (vesselId != null && com.wwsf.vehicle.MovecraftCombatBridge.isAvailable()) {
            com.wwsf.vehicle.MovecraftCombatBridge.fireWeaponEvent(
                vesselId, player, spec.id().toUpperCase(java.util.Locale.ROOT));
        }

        if (spec.hasCustomAmmo()) {
            MachineGunHeatManager.addHeat(aim.cannonLocation(), MachineGunHeatManager.getHeatPerShot());
        }

        long reloadMs;
        if (spec.hasCustomAmmo()) {
            reloadMs = ItemConfigHelper.getLong(
                ItemConfigHelper.configId(spec.id()), "aim.fire-delay-ms", 100L);
        } else {
            reloadMs = ItemConfigHelper.getLong(
                ItemConfigHelper.configId(spec.id()), "aim.reload-ms", 3000L);
        }
        plugin.getAimingManager().applyReload(player, reloadMs);
        player.sendMessage(Messages.get("messages.fired", "&cFiring!"));
    }

    private static Location casingOrigin(AimingManager.ArtilleryAim aim) {
        if (aim.markerId() == null) {
            return aim.cannonLocation();
        }

        com.wwsf.multiblock.MarkerManager.MarkerData marker =
            com.wwsf.multiblock.MarkerManager.getMarkerData(aim.markerId());
        if (marker == null) {
            return aim.cannonLocation();
        }

        com.wwsf.multiblock.CannonDefinition definition = marker.definition();
        int[] triggerOffset = definition.rotateOffset(
            definition.triggerOffsetX(),
            definition.triggerOffsetZ(),
            marker.rotation()
        );
        return marker.core().clone().add(
            triggerOffset[0], definition.triggerOffsetY(), triggerOffset[1]);
    }

    private Set<ShellType> allowedShellTypes(ArtillerySpec spec) {
        java.util.List<String> configured = plugin.getConfig().getStringList(
            "weapons." + spec.id() + ".allowed-shells");
        EnumSet<ShellType> allowed = EnumSet.noneOf(ShellType.class);
        for (String name : configured) {
            try {
                allowed.add(ShellType.valueOf(name.toUpperCase(java.util.Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignored invalid shell type '" + name
                    + "' for " + spec.id());
            }
        }
        return allowed.isEmpty() ? EnumSet.allOf(ShellType.class) : allowed;
    }

    private double getMinDistance() {
        return plugin.getConfig().getDouble("weapons.min-fire-distance", 15.0);
    }
}
