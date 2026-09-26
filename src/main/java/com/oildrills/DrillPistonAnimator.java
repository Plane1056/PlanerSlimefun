package com.oildrills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.PistonHead;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Physics-free visual pump stroke.
 *
 * <p>The drill core must never be changed to an extended piston state. A real
 * sticky-piston transition can pull the Mark II tower into the animation gap
 * and make the completed rig fail its own structure validation.</p>
 */
final class DrillPistonAnimator {

    private static final Map<Location, UUID> ACTIVE_DISPLAYS = new HashMap<>();

    private DrillPistonAnimator() {
    }

    static void setRunning(Block drill, boolean running) {
        removeLegacyHead(drill.getRelative(BlockFace.UP));
        if (running) {
            showDisplay(drill);
        } else {
            removeDisplay(drill);
        }
    }

    static void retract(Block drill) {
        removeDisplay(drill);
        removeLegacyHead(drill.getRelative(BlockFace.UP));
    }

    /** Clears cosmetic state left in the world by this or an older build. */
    static void normalizeAfterLoad(Block drill) {
        removeDisplay(drill);
        removeLegacyHead(drill.getRelative(BlockFace.UP));
    }

    private static void showDisplay(Block drill) {
        Location key = drill.getLocation();
        UUID existingId = ACTIVE_DISPLAYS.get(key);
        if (existingId != null) {
            Entity existing = Bukkit.getEntity(existingId);
            if (existing != null && existing.isValid()) {
                return;
            }
            ACTIVE_DISPLAYS.remove(key);
        }

        Block above = drill.getRelative(BlockFace.UP);
        if (!above.getType().isAir()) {
            return;
        }

        BlockData head = Material.PISTON_HEAD.createBlockData();
        if (head instanceof PistonHead pistonHead) {
            pistonHead.setFacing(BlockFace.UP);
            pistonHead.setShort(false);
        } else if (head instanceof Directional directional) {
            directional.setFacing(BlockFace.UP);
        }

        BlockDisplay display = drill.getWorld().spawn(above.getLocation(), BlockDisplay.class, spawned -> {
            spawned.setBlock(head);
            spawned.setGravity(false);
            spawned.setInvulnerable(true);
            spawned.setPersistent(false);
            spawned.setSilent(true);
        });
        ACTIVE_DISPLAYS.put(key, display.getUniqueId());
    }

    private static void removeDisplay(Block drill) {
        UUID displayId = ACTIVE_DISPLAYS.remove(drill.getLocation());
        if (displayId == null) {
            return;
        }
        Entity display = Bukkit.getEntity(displayId);
        if (display != null) {
            display.remove();
        }
    }

    /** Removes physical piston heads left by pre-2.2.7 animation. */
    private static void removeLegacyHead(Block above) {
        if (above.getType() == Material.PISTON_HEAD) {
            BlockStorage.clearBlockInfo(above);
            above.setType(Material.AIR, false);
        }
    }
}
