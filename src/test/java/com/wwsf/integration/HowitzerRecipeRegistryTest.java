package com.wwsf.integration;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Slot-layout regressions. WWSF2 has no MockBukkit, so these assert the exact
 * shapes the builders use rather than constructing live ItemStacks.
 */
class HowitzerRecipeRegistryTest {

    @Test
    void largeCasingLeavesTheCentreColumnEmpty() {
        assertArrayEquals(new int[] {1, 4}, HowitzerRecipeRegistry.CAULDRON_EMPTY_SLOTS,
            "seven brass ingots wrap an empty top-centre and centre");
    }

    @Test
    void equipmentUsesFourSteelIngotsAroundTheCentreIngredient() {
        assertArrayEquals(new int[] {1, 3, 5, 7}, HowitzerRecipeRegistry.PLUS_ARM_SLOTS);
        assertArrayEquals(new int[] {0, 2, 6, 8}, HowitzerRecipeRegistry.PLUS_EMPTY_SLOTS);
        assertEquals(4, HowitzerRecipeRegistry.PLUS_CENTRE_SLOT);
        for (int corner : HowitzerRecipeRegistry.PLUS_EMPTY_SLOTS) {
            assertFalse(Arrays.stream(HowitzerRecipeRegistry.PLUS_ARM_SLOTS)
                    .anyMatch(arm -> arm == corner),
                "an empty corner can never also be a steel arm");
        }
    }

    @Test
    void gasDrumConsumesABarrelInTheCentreToLeaveAnOutputSlot() {
        assertEquals(4, HowitzerRecipeRegistry.GAS_DRUM_BARREL_SLOT);
        assertArrayEquals(
            new int[] {0, 1, 2, 3, 5, 6, 7, 8},
            HowitzerRecipeRegistry.GAS_DRUM_BOTTLE_SLOTS
        );
        assertFalse(Arrays.stream(HowitzerRecipeRegistry.GAS_DRUM_BOTTLE_SLOTS)
            .anyMatch(slot -> slot == HowitzerRecipeRegistry.GAS_DRUM_BARREL_SLOT));
    }
}
