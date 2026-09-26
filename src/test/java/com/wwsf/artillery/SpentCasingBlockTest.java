package com.wwsf.artillery;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.bukkit.util.Vector;

import java.util.List;

class SpentCasingBlockTest {

    @Test
    void explosiveRoundsUseTheHeCasing() {
        assertEquals(SpentCasingBlock.HE_NOTE, SpentCasingBlock.noteFor(ShellType.STANDARD));
        assertEquals(SpentCasingBlock.HE_NOTE, SpentCasingBlock.noteFor(ShellType.SHRAPNEL));
        assertEquals(SpentCasingBlock.HE_NOTE, SpentCasingBlock.noteFor(ShellType.INCENDIARY));
        assertEquals(SpentCasingBlock.HE_NOTE, SpentCasingBlock.noteFor(ShellType.ARMOR_PIERCING));
        assertEquals(SpentCasingBlock.HE_NOTE, SpentCasingBlock.noteFor(ShellType.CLUSTER));
    }

    @Test
    void smokeRoundsUseTheSmokeCasing() {
        assertEquals(SpentCasingBlock.SMOKE_NOTE, SpentCasingBlock.noteFor(ShellType.SMOKE));
    }

    @Test
    void gasRoundsUseTheGasCasing() {
    }

    @Test
    void firstPlacementCandidateIsBesideTheBreech() {
        List<int[]> offsets = SpentCasingBlock.candidateOffsets(new Vector(1, 0, 0));

        assertEquals(0, offsets.get(0)[0]);
        assertEquals(2, offsets.get(0)[1]);
    }
}
