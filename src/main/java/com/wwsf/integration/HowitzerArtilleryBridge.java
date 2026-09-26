package com.wwsf.integration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.wwsf.WWSFPlugin;

/**
 * Dependency-free bridge to the standalone HowitzerArtillery plugin.
 *
 * <p>Howitzer remains the authority for its item identities and gas simulation.
 * Reflection keeps WWSF2 loadable when the optional plugin is absent while the
 * soft-dependency guarantees the correct load order when it is installed.</p>
 */
public final class HowitzerArtilleryBridge {

    private static final String PLUGIN_NAME = "HowitzerArtillery";
    private static final String ITEM_FACTORY_CLASS =
        "dev.howitzer.artillery.items.ArtilleryItemFactory";
    private static final String ITEM_TYPE_CLASS =
        "dev.howitzer.artillery.items.ArtilleryItemType";
    private static final String BARBED_WIRE_ENUM = "BARBED_WIRE_COIL";
    private static final String GAS_DRUM_ENUM = "GAS_DRUM";

    private final WWSFPlugin plugin;
    private boolean itemFailureLogged;
    private boolean gasFailureLogged;
    private boolean releaseFailureLogged;
    private boolean recipeFailureLogged;

    public HowitzerArtilleryBridge(@Nonnull WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Creates the real Howitzer barbed-wire coil, including its authoritative
     * {@code arty:item_id} PDC tag and custom model data.
     */
    @Nonnull
    public Optional<ItemStack> createBarbedWireCoil() {
        return createItem(BARBED_WIRE_ENUM, "barbed-wire coil");
    }

    /**
     * Creates the real Howitzer gas release drum with Howitzer's item identity,
     * custom model data, and default gas capacity behavior.
     */
    @Nonnull
    public Optional<ItemStack> createGasDrum() {
        return createItem(GAS_DRUM_ENUM, "gas release drum");
    }

    /**
     * Creates any Howitzer native item by its {@code ArtilleryItemType} name,
     * including its authoritative {@code arty:item_id} tag, custom model data
     * and lore. Empty when HowitzerArtillery is absent or the name is unknown.
     */
    @Nonnull
    public Optional<ItemStack> createItem(@Nonnull String artilleryItemTypeName) {
        return createItem(artilleryItemTypeName, artilleryItemTypeName);
    }

    /**
     * Seeds one real Howitzer harmful-gas cloud of {@code cells} unique cells.
     * Returns false when HowitzerArtillery is absent, gas is unavailable, or
     * nothing could be seeded — callers then use their own fallback.
     */
    public boolean releaseGas(@Nonnull Location location, int cells) {
        Plugin howitzer = enabledPlugin();
        if (howitzer == null || cells <= 0) {
            return false;
        }
        try {
            Method release = howitzer.getClass()
                .getMethod("releaseHarmfulGas", Location.class, int.class);
            Object result = release.invoke(howitzer, location, cells);
            return result instanceof Boolean seeded && seeded;
        } catch (ReflectiveOperationException | LinkageError error) {
            logOnce(
                "Could not seed a Howitzer gas cloud; using the WWSF fallback cloud.",
                error,
                () -> releaseFailureLogged,
                () -> releaseFailureLogged = true);
            return false;
        }
    }

    /**
     * Retires HowitzerArtillery's plugin-native vanilla firing-cord recipe once
     * WWSF2 has published the same item as a Slimefun recipe.
     */
    public boolean retireVanillaRecipes() {
        Plugin howitzer = enabledPlugin();
        if (howitzer == null) {
            return false;
        }
        try {
            howitzer.getClass().getMethod("unregisterVanillaRecipes").invoke(howitzer);
            return true;
        } catch (ReflectiveOperationException | LinkageError error) {
            logOnce(
                "Could not retire Howitzer's vanilla firing-cord recipe; both remain craftable.",
                error,
                () -> recipeFailureLogged,
                () -> recipeFailureLogged = true);
            return false;
        }
    }

    private void logOnce(
        String message,
        Throwable error,
        java.util.function.BooleanSupplier alreadyLogged,
        Runnable markLogged
    ) {
        if (alreadyLogged.getAsBoolean()) {
            return;
        }
        markLogged.run();
        plugin.getLogger().log(java.util.logging.Level.WARNING, message, error);
    }

    @Nonnull
    private Optional<ItemStack> createItem(String enumName, String itemName) {
        Plugin howitzer = enabledPlugin();
        if (howitzer == null) {
            return Optional.empty();
        }

        try {
            ClassLoader loader = howitzer.getClass().getClassLoader();
            Class<?> itemTypeClass = Class.forName(ITEM_TYPE_CLASS, true, loader);
            Class<?> factoryClass = Class.forName(ITEM_FACTORY_CLASS, true, loader);
            Object itemType = enumConstant(itemTypeClass, enumName);
            Object factory = factoryClass.getDeclaredConstructor().newInstance();
            Object result = factoryClass.getMethod("create", itemTypeClass)
                .invoke(factory, itemType);

            if (result instanceof ItemStack stack) {
                return Optional.of(stack.clone());
            }
            logItemFailure("Howitzer item factory returned an unexpected value.", null);
        } catch (ReflectiveOperationException | LinkageError error) {
            logItemFailure("Could not create Howitzer's " + itemName + ".", error);
        }
        return Optional.empty();
    }

    /**
     * Returns exact harmful gas cells that are within {@code radius} of at least
     * one supplied detector. Smoke is excluded by Howitzer's own concentration API.
     */
    @Nonnull
    public List<Location> harmfulGasCellsNear(
        @Nonnull World world,
        @Nonnull Collection<Location> detectors,
        double radius
    ) {
        if (detectors.isEmpty() || !Double.isFinite(radius) || radius < 0.0) {
            return List.of();
        }

        Plugin howitzer = enabledPlugin();
        if (howitzer == null) {
            return List.of();
        }

        try {
            Object gasEffect = howitzer.getClass().getMethod("gasEffect").invoke(howitzer);
            Method debugCells = gasEffect.getClass().getMethod("debugCellPositions", UUID.class);
            Method harmfulConcentration =
                gasEffect.getClass().getMethod("harmfulConcentrationAt", Location.class);
            Object result = debugCells.invoke(gasEffect, world.getUID());
            if (!(result instanceof Set<?> positions) || positions.isEmpty()) {
                return List.of();
            }

            Object sample = positions.iterator().next();
            Method xAccessor = sample.getClass().getMethod("x");
            Method yAccessor = sample.getClass().getMethod("y");
            Method zAccessor = sample.getClass().getMethod("z");
            double radiusSquared = radius * radius;
            List<Location> harmful = new ArrayList<>();

            for (Object position : positions) {
                int x = ((Number) xAccessor.invoke(position)).intValue();
                int y = ((Number) yAccessor.invoke(position)).intValue();
                int z = ((Number) zAccessor.invoke(position)).intValue();
                Location cell = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                if (!isNearAny(cell, detectors, radiusSquared)) {
                    continue;
                }

                Object concentration = harmfulConcentration.invoke(gasEffect, cell);
                if (concentration instanceof Number number && number.doubleValue() > 0.0) {
                    harmful.add(cell);
                }
            }

            return List.copyOf(harmful);
        } catch (ReflectiveOperationException | LinkageError error) {
            logGasFailure("Could not inspect Howitzer's live gas cells.", error);
            return List.of();
        }
    }

    public boolean isAvailable() {
        return enabledPlugin() != null;
    }

    private Plugin enabledPlugin() {
        Plugin howitzer = plugin.getServer().getPluginManager().getPlugin(PLUGIN_NAME);
        return howitzer != null && howitzer.isEnabled() ? howitzer : null;
    }

    private static boolean isNearAny(
        Location cell,
        Collection<Location> detectors,
        double radiusSquared
    ) {
        for (Location detector : detectors) {
            if (detector.getWorld() == cell.getWorld()
                && detector.distanceSquared(cell) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(Class<?> enumClass, String name) {
        return Enum.valueOf((Class<? extends Enum>) enumClass.asSubclass(Enum.class), name);
    }

    private void logItemFailure(String message, Throwable error) {
        if (itemFailureLogged) {
            return;
        }
        itemFailureLogged = true;
        if (error == null) {
            plugin.getLogger().warning(message);
        } else {
            plugin.getLogger().log(java.util.logging.Level.WARNING, message, error);
        }
    }

    private void logGasFailure(String message, Throwable error) {
        if (gasFailureLogged) {
            return;
        }
        gasFailureLogged = true;
        plugin.getLogger().log(java.util.logging.Level.WARNING, message, error);
    }
}
