package com.wwsf.items;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Handles pontoon bridge deployment when bridge kit snowball lands on water.
 */
public class BridgeDeployListener implements Listener {

    private final WWSFPlugin plugin;
    private final Map<Location, DeployTask> activeDeployments = new HashMap<>();

    public BridgeDeployListener(WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        ItemStack item = snowball.getItem();
        if (item == null) {
            return;
        }
        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        if (sfItem == null || !(sfItem instanceof PontoonBridgeKit)) {
            return;
        }

        event.setCancelled(true);
        Location impact = snowball.getLocation();
        snowball.remove();

        Block block = impact.getBlock();
        if (block.getType() != Material.WATER && block.getType() != Material.SEAGRASS) {
            return;
        }

        Player player = (Player) snowball.getShooter();
        if (player == null) return;

        // Get player facing direction
        BlockFace direction = getHorizontalFacing(player);

        // Cancel any existing deployment at this location
        DeployTask existing = activeDeployments.get(impact);
        if (existing != null) {
            existing.cancel();
        }

        // Start deployment
        DeployTask task = new DeployTask(plugin, impact, direction, player);
        activeDeployments.put(impact, task);
        task.runTaskTimer(plugin, 0L, 10L); // Deploy one section every 0.5s
    }

    private BlockFace getHorizontalFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        // Normalize yaw to 0-360
        yaw = (yaw + 360) % 360;

        if (yaw >= 315 || yaw < 45) {
            return BlockFace.SOUTH;
        } else if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

    private static class DeployTask extends BukkitRunnable {
        private final WWSFPlugin plugin;
        private final Location start;
        private final BlockFace direction;
        private final Player player;
        private int section = 0;
        private final int totalSections = 5;

        DeployTask(WWSFPlugin plugin, Location start, BlockFace direction, Player player) {
            this.plugin = plugin;
            this.start = start.clone();
            this.direction = direction;
            this.player = player;
        }

        @Override
        public void run() {
            if (section >= totalSections) {
                // Deployment complete
                Location impact = start.clone();
                impact.getWorld().playSound(impact, Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f);
                impact.getWorld().spawnParticle(org.bukkit.Particle.FLASH, impact, 10, 3, 0, 3, 0.1);
                cancel();
                return;
            }

            // Calculate position for this section
            Location pontoonLoc = start.clone().add(
                direction.getModX() * section,
                0,
                direction.getModZ() * section
            );

            // Place pontoon block
            Block block = pontoonLoc.getBlock();
            if (block.getType() == Material.WATER || block.getType() == Material.SEAGRASS) {
                block.setType(Material.OAK_PLANKS);
                
                // Play placement sound
                pontoonLoc.getWorld().playSound(pontoonLoc, Sound.BLOCK_WOOD_PLACE, 0.8f, 1.0f);
                pontoonLoc.getWorld().spawnParticle(org.bukkit.Particle.HEART, pontoonLoc.add(0.5, 0.5, 0.5), 3, 0.3, 0.3, 0.3, 0.05);
            }

            section++;
        }
    }
}