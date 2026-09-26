package com.wwsf.artillery;

/**
 * Per-shell flight and detonation behaviour, read from {@code shells.<TYPE>.<key>} in config.
 *
 * <ul>
 *   <li>{@code airburst} / {@code airburstHeight} — detonate above the target instead of on block
 *       collision (used by the Shrapnel shell's anti-personnel airburst).</li>
 *   <li>{@code speedMultiplier} / {@code gravityMultiplier} — shell-specific flight tuning so, for
 *       example, the Armor-Piercing shell flies flatter and faster than a lobbed mortar.</li>
 *   <li>{@code rangeMultiplier} — optional per-shell range override relative to the cannon.</li>
 * </ul>
 */
public record ShellBehavior(
    boolean airburst,
    double airburstHeight,
    double speedMultiplier,
    double gravityMultiplier,
    double rangeMultiplier
) {
}
