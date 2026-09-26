package com.wwsf.gas;

import org.bukkit.configuration.file.FileConfiguration;

import com.wwsf.WWSFPlugin;

/**
 * Gas-effect configuration for the merged Gas Bomb.
 *
 * <p>{@link #bombCells()} sizes the real HowitzerArtillery cloud. The
 * {@link GasProfile} below only drives the standalone fallback cloud used when
 * HowitzerArtillery is absent, so a server without it still gets a usable
 * weapon.</p>
 */
public final class GasConfig {

    /** Fallback cell count when nothing is configured. */
    public static final int DEFAULT_BOMB_CELLS = 600;

    private GasConfig() {
    }

    public static boolean isEnabled() {
        return config().getBoolean("gas.enabled", true);
    }

    /** Unique gas cells one Gas Bomb seeds in the Howitzer simulation. */
    public static int bombCells() {
        return Math.max(1, config().getInt("gas.bomb.cells", DEFAULT_BOMB_CELLS));
    }

    /** Effects for the standalone fallback cloud only. */
    public static GasProfile profile() {
        String path = "gas.bomb.";
        return new GasProfile(
            positiveInt(path + "duration-ticks", 200),
            positiveInt(path + "tick-interval", 4),
            positiveDouble(path + "max-radius", 8.0),
            nonNegativeDouble(path + "spread-speed", 0.25),
            positiveInt(path + "wither-duration", 140),
            nonNegativeInt(path + "wither-amplifier", 2),
            positiveInt(path + "nausea-duration", 100),
            nonNegativeInt(path + "nausea-amplifier", 0),
            nonNegativeDouble(path + "contact-damage", 2.0),
            nonNegativeDouble(path + "tick-damage", 0.0)
        );
    }

    private static FileConfiguration config() {
        return WWSFPlugin.getInstance().getConfig();
    }

    private static int positiveInt(String path, int fallback) {
        return Math.max(1, config().getInt(path, fallback));
    }

    private static int nonNegativeInt(String path, int fallback) {
        return Math.max(0, config().getInt(path, fallback));
    }

    private static double positiveDouble(String path, double fallback) {
        return Math.max(0.01, config().getDouble(path, fallback));
    }

    private static double nonNegativeDouble(String path, double fallback) {
        return Math.max(0.0, config().getDouble(path, fallback));
    }

    public record GasProfile(
        int durationTicks,
        int tickInterval,
        double maxRadius,
        double spreadSpeed,
        int witherDuration,
        int witherAmplifier,
        int nauseaDuration,
        int nauseaAmplifier,
        double contactDamage,
        double tickDamage
    ) {
    }
}
