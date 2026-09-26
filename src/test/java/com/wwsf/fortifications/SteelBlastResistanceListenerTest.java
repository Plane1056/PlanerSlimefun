package com.wwsf.fortifications;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class SteelBlastResistanceListenerTest {

    @Test
    void recognizesAllFourSteelCarrierShapes() {
        assertTrue(SteelBlock.isSteelMaterial(Material.RED_NETHER_BRICKS));
        assertTrue(SteelBlock.isSteelMaterial(Material.RED_NETHER_BRICK_SLAB));
        assertTrue(SteelBlock.isSteelMaterial(Material.RED_NETHER_BRICK_WALL));
        assertTrue(SteelBlock.isSteelMaterial(Material.RED_NETHER_BRICK_STAIRS));

        assertFalse(SteelBlock.isSteelMaterial(Material.END_STONE));
    }

    @Test
    void increasesCarrierSurvivalTowardEndStoneResistance() {
        assertEquals(
            1.0 / 3.0,
            SteelBlastResistanceListener.protectionChance(Material.RED_NETHER_BRICKS),
            0.000_001
        );
        assertEquals(0.0, SteelBlastResistanceListener.protectionChance(Material.STONE));
    }

    @Test
    void protectsSteelOnlyWhenTheRollIsInsideTheCalculatedShare() {
        double chance = SteelBlastResistanceListener.protectionChance(Material.RED_NETHER_BRICK_WALL);

        assertTrue(SteelBlastResistanceListener.shouldProtect(
            Material.RED_NETHER_BRICK_WALL,
            Math.max(0.0, chance - 0.000_001)
        ));
        assertFalse(SteelBlastResistanceListener.shouldProtect(
            Material.RED_NETHER_BRICK_WALL,
            chance
        ));
        assertFalse(SteelBlastResistanceListener.shouldProtect(Material.COBBLESTONE, 0.0));
    }
}
