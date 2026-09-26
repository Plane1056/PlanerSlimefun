package com.wwsf.gas;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;

/**
 * One detonation path for the Gas Bomb.
 *
 * <p>HowitzerArtillery owns the gas simulation. When it is installed the bomb
 * seeds a real concentration-cell cloud of {@code gas.bomb.cells} cells, so it
 * flows, decays, corrodes terrain, respects gas masks and can be vacuumed just
 * like an artillery gas shell. The legacy particle cloud is retained only as a
 * fallback for servers running WWSF2 without HowitzerArtillery.</p>
 */
public final class GasEffectService {

    private GasEffectService() {
    }

    public static boolean detonate(
        WWSFPlugin plugin,
        Location location,
        ItemStack sourceItem,
        Player sourcePlayer
    ) {
        if (!GasConfig.isEnabled()) {
            return false;
        }

        GasDetonationEvent event = new GasDetonationEvent(location, sourceItem, sourcePlayer);
        plugin.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        int cells = GasConfig.bombCells();
        if (plugin.getHowitzerBridge().releaseGas(location, cells)) {
            burstSignature(location);
            return true;
        }

        // No HowitzerArtillery: keep the weapon usable with the local cloud.
        return GasCloud.spawn(plugin, location) != null;
    }

    private static void burstSignature(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        world.playSound(location, Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 0.5f);
        world.spawnParticle(Particle.FLASH, location, 1, 0, 0, 0, 0);
    }
}
