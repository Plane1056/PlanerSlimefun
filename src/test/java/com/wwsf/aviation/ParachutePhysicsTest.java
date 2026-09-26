package com.wwsf.aviation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ParachutePhysicsTest {

    @Test
    void convertsFiveBlocksPerSecondToQuarterBlockPerTick() {
        assertEquals(-0.25, ParachutePhysics.downwardVelocityPerTick(5.0), 1.0E-9);
    }

    @Test
    void convertsOtherConfiguredSpeedsUsingTwentyTicksPerSecond() {
        assertEquals(-0.5, ParachutePhysics.downwardVelocityPerTick(10.0), 1.0E-9);
    }

    @Test
    void rejectsInvalidSpeedsToTheSafeFiveBlockDefault() {
        assertEquals(-0.25, ParachutePhysics.downwardVelocityPerTick(0.0), 1.0E-9);
        assertEquals(-0.25, ParachutePhysics.downwardVelocityPerTick(-2.0), 1.0E-9);
        assertEquals(
            -0.25,
            ParachutePhysics.downwardVelocityPerTick(Double.NaN),
            1.0E-9
        );
    }
}
