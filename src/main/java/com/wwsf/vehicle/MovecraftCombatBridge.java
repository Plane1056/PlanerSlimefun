package com.wwsf.vehicle;

import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.wwsf.WWSFPlugin;

public final class MovecraftCombatBridge {

    private static final String MOVECRAFT_COMBAT_PLUGIN_NAME = "Movecraft-Combat";
    private static final String CRAFT_FIRE_WEAPON_EVENT_CLASS = "net.countercraft.movecraft.combat.features.tracking.events.CraftFireWeaponEvent";
    private static final String TYPE_CLASS = "net.countercraft.movecraft.combat.features.tracking.types.Type";
    private static final String CRAFT_CLASS = "net.countercraft.movecraft.craft.Craft";

    private static volatile boolean available = false;
    private static volatile Class<?> craftFireWeaponEventClass;
    private static volatile Class<?> typeClass;
    private static volatile Class<?> craftClass;
    private static final Object INIT_LOCK = new Object();

    private MovecraftCombatBridge() {}

    public static boolean isAvailable() {
        lazyInit();
        return available;
    }

    public static void fireWeaponEvent(UUID vesselId, Player shooter, String weaponType) {
        if (!isAvailable() || vesselId == null || shooter == null) {
            return;
        }

        try {
            Object craft = VehicleQuery.getCraft(vesselId);
            if (craft == null) {
                return;
            }

            Object type = Proxy.newProxyInstance(
                typeClass.getClassLoader(),
                new Class<?>[] { typeClass },
                (proxy, method, args) -> {
                    if ("toString".equals(method.getName())) {
                        return weaponType;
                    }
                    return null;
                }
            );

            Object event = craftFireWeaponEventClass
                .getConstructor(craftClass, typeClass)
                .newInstance(craft, type);

            WWSFPlugin.getInstance().getServer().getPluginManager().callEvent(
                (org.bukkit.event.Event) event
            );
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[MovecraftCombatBridge] fireWeaponEvent failed: " + t.getMessage());
        }
    }

    private static synchronized void lazyInit() {
        if (available) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (available) {
                return;
            }

            JavaPlugin plugin = WWSFPlugin.getInstance();
            if (plugin == null) {
                return;
            }

            if (plugin.getServer().getPluginManager().getPlugin(MOVECRAFT_COMBAT_PLUGIN_NAME) == null) {
                return;
            }

            try {
                craftFireWeaponEventClass = Class.forName(CRAFT_FIRE_WEAPON_EVENT_CLASS);
                typeClass = Class.forName(TYPE_CLASS);
                craftClass = Class.forName(CRAFT_CLASS);

                if (craftFireWeaponEventClass != null && typeClass != null && craftClass != null) {
                    available = true;
                    plugin.getLogger().info("[MovecraftCombatBridge] Movecraft-Combat bridge initialized.");
                }
            } catch (Throwable t) {
                WWSFPlugin.getInstance().getLogger().fine("[MovecraftCombatBridge] Movecraft-Combat bridge unavailable: " + t.getMessage());
            }
        }
    }
}
