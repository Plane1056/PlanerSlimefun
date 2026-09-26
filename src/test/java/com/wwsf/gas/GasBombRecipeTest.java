package com.wwsf.gas;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Slot-layout regressions. WWSF2 has no MockBukkit, so these assert the exact
 * shape {@link GasBomb#starRecipe} builds rather than live ItemStacks.
 */
class GasBombRecipeTest {

    @Test
    void gasBombIsOneTntSurroundedByFourGasBottlesInAStar() {
        assertArrayEquals(new int[] {1, 3, 5, 7}, GasBomb.STAR_ARM_SLOTS,
            "the four gas bottles sit north, west, east and south");
        assertEquals(4, GasBomb.STAR_CENTRE_SLOT, "TNT sits in the centre");
        assertArrayEquals(new int[] {0, 2, 6, 8}, GasBomb.STAR_EMPTY_SLOTS,
            "the corners stay empty so the shape reads as a star");
        for (int corner : GasBomb.STAR_EMPTY_SLOTS) {
            assertFalse(Arrays.stream(GasBomb.STAR_ARM_SLOTS).anyMatch(arm -> arm == corner));
            assertFalse(corner == GasBomb.STAR_CENTRE_SLOT);
        }
    }

    @Test
    void theTwoRetiredIdentitiesAreStillRegisteredIds() {
        assertEquals("GAS_GRENADE", GasBomb.LEGACY_CHLORINE_ID);
        assertEquals("WWSF_MUSTARD_GAS_BOMB", GasBomb.LEGACY_MUSTARD_ID);
        assertEquals("WWSF_GAS_BOMB", GasBomb.ID);
    }
}
