package com.wwsf.artillery;

/**
 * Per-shell explosion, damage falloff and terrain-modification tuning.
 *
 * <p>Most fields are read from {@code shells.<TYPE>.<key>} in {@code config.yml} via
 * {@link ShellTemplates} and {@link ShellConfig}, with sane hardcoded defaults so the plugin works
 * even before any tuning is added.
 */
public record BlastProfile(
    float explosionPower,
    float entityDamage,
    double damageRadius,
    boolean breakBlocks,
    boolean setFire,
    double blockBreakRadius,
    double falloffExponent,
    double suppressRadius,
    int suppressTicks
) {
    /**
     * Returns a copy of this profile with explosion power, entity damage, the damage radius,
     * the block-break radius and the suppression radius multiplied by {@code power} (used for the
     * per-cannon blast multiplier). The falloff exponent and boolean flags are left unchanged.
     */
    public BlastProfile scaled(double power) {
        if (power == 1.0) {
            return this;
        }
        return new BlastProfile(
            (float) (explosionPower * power),
            (float) (entityDamage * power),
            damageRadius * power,
            breakBlocks,
            setFire,
            blockBreakRadius * power,
            falloffExponent,
            suppressRadius * power,
            suppressTicks
        );
    }
}
