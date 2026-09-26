package com.wwsf.aviation;

/**
 * Unit conversion for the parachute's fixed descent speed.
 */
final class ParachutePhysics {

    static final double DEFAULT_DESCENT_BLOCKS_PER_SECOND = 5.0;
    private static final double TICKS_PER_SECOND = 20.0;

    private ParachutePhysics() {
    }

    static double downwardVelocityPerTick(double blocksPerSecond) {
        double safeSpeed = Double.isFinite(blocksPerSecond) && blocksPerSecond > 0.0
            ? blocksPerSecond
            : DEFAULT_DESCENT_BLOCKS_PER_SECOND;
        return -safeSpeed / TICKS_PER_SECOND;
    }
}
