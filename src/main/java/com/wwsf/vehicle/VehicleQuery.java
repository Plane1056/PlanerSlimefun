package com.wwsf.vehicle;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;

public final class VehicleQuery {

    private static final String MOVECRAFT_PLUGIN_NAME = "Movecraft";
    private static final String CRAFT_MANAGER_CLASS = "net.countercraft.movecraft.craft.CraftManager";
    private static final String CRAFT_CLASS = "net.countercraft.movecraft.craft.Craft";
    private static final String MOVE_LOCATION_CLASS = "net.countercraft.movecraft.MovecraftLocation";
    private static final String HITBOX_CLASS = "net.countercraft.movecraft.util.hitboxes.HitBox";

    private static boolean movecraftAvailable;
    private static Class<?> craftManagerClass;
    private static Class<?> craftClass;
    private static Class<?> moveLocationClass;
    private static Class<?> hitboxClass;
    private static Object craftManagerInstance;
    private static final Object INIT_LOCK = new Object();

    private VehicleQuery() {
    }

    public static boolean isAvailable() {
        lazyInit();
        return movecraftAvailable && craftManagerClass != null && craftManagerInstance != null;
    }

    public static Optional<UUID> vesselIdAt(Location location) {
        if (location == null || location.getWorld() == null) {
            return Optional.empty();
        }
        if (!isAvailable()) {
            return Optional.empty();
        }

        try {
            Class<?> craftClass = Class.forName(CRAFT_CLASS);
            Method getCrafts = craftClass.getMethod("getCrafts");
            Object crafts = getCrafts.invoke(null);
            if (!(crafts instanceof java.util.Collection<?> collection)) {
                return Optional.empty();
            }

            for (Object craft : collection) {
                Object worldObj = craftClass.getMethod("getWorld").invoke(craft);
                if (!(worldObj instanceof World world) || !world.equals(location.getWorld())) {
                    continue;
                }

                Object uuidObj = craftClass.getMethod("getUUID").invoke(craft);
                if (!(uuidObj instanceof UUID craftUuid)) {
                    continue;
                }
                Object hitBox = craftClass.getMethod("getHitBox").invoke(craft);
                Object moveLoc = moveLocationClass.getConstructor(int.class, int.class, int.class)
                        .newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
                Object containsResult = hitboxClass.getMethod("contains", moveLocationClass).invoke(hitBox, moveLoc);
                if (containsResult instanceof Boolean bool && bool) {
                    return Optional.of(craftUuid);
                }
            }
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[VehicleQuery] vesselIdAt failed: " + t.getMessage());
        }
        return Optional.empty();
    }

    public static <T> T getCraft(UUID vesselId) {
        if (!isAvailable() || vesselId == null) {
            return null;
        }
        try {
            Object result = craftClass.getMethod("getCraftByUUID", UUID.class).invoke(null, vesselId);
            if (result == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            T casted = (T) result;
            return casted;
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[VehicleQuery] getCraft failed: " + t.getMessage());
        }
        return null;
    }

    public static Optional<Vector> getLastTranslation(UUID vesselId) {
        Object craft = getCraft(vesselId);
        if (craft == null) {
            return Optional.empty();
        }
        try {
            Object loc = craftClass.getMethod("getLastTranslation").invoke(craft);
            if (loc == null) {
                return Optional.empty();
            }
            int dx = (int) loc.getClass().getMethod("getX").invoke(loc);
            int dy = (int) loc.getClass().getMethod("getY").invoke(loc);
            int dz = (int) loc.getClass().getMethod("getZ").invoke(loc);
            return Optional.of(new Vector(dx, dy, dz));
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[VehicleQuery] getLastTranslation failed: " + t.getMessage());
        }
        return Optional.empty();
    }

    public static Optional<Double> getSpeed(UUID vesselId) {
        Object craft = getCraft(vesselId);
        if (craft == null) {
            return Optional.empty();
        }
        try {
            return Optional.of((Double) craftClass.getMethod("getSpeed").invoke(craft));
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[VehicleQuery] getSpeed failed: " + t.getMessage());
        }
        return Optional.empty();
    }

    public static Optional<String> getCraftName(UUID vesselId) {
        Object craft = getCraft(vesselId);
        if (craft == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable((String) craftClass.getMethod("getName").invoke(craft));
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[VehicleQuery] getCraftName failed: " + t.getMessage());
        }
        return Optional.empty();
    }

    public static boolean contains(UUID vesselId, Location location) {
        Object craft = getCraft(vesselId);
        if (craft == null || location == null || location.getWorld() == null) {
            return false;
        }
        try {
            Object hitBox = craftClass.getMethod("getHitBox").invoke(craft);
            Object moveLoc = moveLocationClass.getConstructor(int.class, int.class, int.class)
                    .newInstance(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            Object result = hitboxClass.getMethod("contains", moveLocationClass).invoke(hitBox, moveLoc);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[VehicleQuery] contains failed: " + t.getMessage());
        }
        return false;
    }

    private static synchronized void lazyInit() {
        if (movecraftAvailable) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (movecraftAvailable) {
                return;
            }

            JavaPlugin plugin = WWSFPlugin.getInstance();
            if (plugin == null) {
                return;
            }

            if (plugin.getServer().getPluginManager().getPlugin(MOVECRAFT_PLUGIN_NAME) == null) {
                return;
            }

            try {
                craftManagerClass = Class.forName(CRAFT_MANAGER_CLASS);
                craftClass = Class.forName(CRAFT_CLASS);
                moveLocationClass = Class.forName(MOVE_LOCATION_CLASS);
                hitboxClass = Class.forName(HITBOX_CLASS);

                Method getInstance = craftManagerClass.getMethod("getInstance");
                craftManagerInstance = getInstance.invoke(null);

                if (craftManagerInstance != null) {
                    movecraftAvailable = true;
                    plugin.getLogger().info("[VehicleQuery] Movecraft bridge initialized.");
                }
            } catch (Throwable t) {
                plugin.getLogger().fine("[VehicleQuery] Movecraft bridge unavailable: " + t.getMessage());
            }
        }
    }
}
