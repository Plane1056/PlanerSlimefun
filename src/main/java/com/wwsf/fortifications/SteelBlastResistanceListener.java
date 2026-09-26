package com.wwsf.fortifications;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

/**
 * Raises the effective explosion survival rate of the red-nether-brick steel carriers from their
 * vanilla resistance to approximately End Stone resistance.
 *
 * <p>Bukkit exposes a mutable explosion block list but cannot change a material's registry-level
 * blast resistance. Keeping the proportional share of affected blocks is therefore the closest
 * server-side equivalent without replacing the blocks with a different vanilla family.</p>
 */
public final class SteelBlastResistanceListener implements Listener {

    // Vanilla 1.21.x values. Keeping these as constants also makes the calculation safe before a
    // Paper server has initialized its registry access (for example, during unit tests).
    private static final double RED_NETHER_BRICK_RESISTANCE = 6.0;
    private static final double END_STONE_RESISTANCE = 9.0;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        applyEndStoneResistance(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        applyEndStoneResistance(event.blockList());
    }

    private static void applyEndStoneResistance(List<Block> affectedBlocks) {
        affectedBlocks.removeIf(block ->
            shouldProtect(block.getType(), ThreadLocalRandom.current().nextDouble())
        );
    }

    static boolean shouldProtect(Material material, double roll) {
        return SteelBlock.isSteelMaterial(material) && roll < protectionChance(material);
    }

    static double protectionChance(Material material) {
        if (!SteelBlock.isSteelMaterial(material)) {
            return 0.0;
        }

        return 1.0 - RED_NETHER_BRICK_RESISTANCE / END_STONE_RESISTANCE;
    }
}
