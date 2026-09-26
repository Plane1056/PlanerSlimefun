package com.wwsf.ammo;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ShellType;
import com.wwsf.items.ShellCasing;
import com.wwsf.setup.ElectricityCategory;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

public final class AmmoRegistry {

    private AmmoRegistry() {
    }

    private static final Map<ShellType, AmmoItem> AMMO_ITEMS = new HashMap<>();

    public static void register(WWSFPlugin plugin) {
        ItemGroup category = ElectricityCategory.AMMUNITION;

        ShellCasing.registerStatic(plugin);

        ItemStack gunpowder = new ItemStack(Material.GUNPOWDER);

        registerAmmo(plugin, new AmmoItem(category, "WWSF_MORTAR_SHELL", Material.FIREWORK_STAR, "&6Mortar Shell", ShellType.STANDARD, 0,
            new ItemStack[]{
                shellCasing(), new ItemStack(Material.TNT), new ItemStack(Material.IRON_NUGGET),
                null, null, null,
                null, null, null
            }
        ));
        registerAmmo(plugin, new AmmoItem(category, "WWSF_SHRAPNEL_SHELL", Material.ARROW, "&7Shrapnel Shell", ShellType.SHRAPNEL, 0,
            new ItemStack[]{
                shellCasing(), new ItemStack(Material.TNT), new ItemStack(Material.IRON_NUGGET),
                new ItemStack(Material.IRON_NUGGET), new ItemStack(Material.IRON_NUGGET), new ItemStack(Material.IRON_NUGGET),
                null, null, null
            }
        ));
        registerAmmo(plugin, new AmmoItem(category, "WWSF_INCENDIARY_SHELL", Material.BLAZE_POWDER, "&6Incendiary Shell", ShellType.INCENDIARY, 0,
            new ItemStack[]{
                shellCasing(), new ItemStack(Material.TNT), gunpowder,
                gunpowder, new ItemStack(Material.COAL), null,
                null, null, null
            }
        ));
        registerAmmo(plugin, new AmmoItem(category, "WWSF_AP_SHELL", Material.FIREWORK_STAR, "&8Armor-Piercing Shell", ShellType.ARMOR_PIERCING, 0,
            new ItemStack[]{
                shellCasing(), new ItemStack(Material.TNT), SlimefunItems.STEEL_INGOT,
                null, null, null,
                null, null, null
            }
        ));
        registerAmmo(plugin, new AmmoItem(category, "WWSF_CLUSTER_SHELL", Material.FIREWORK_ROCKET, "&dCluster Shell", ShellType.CLUSTER, 0,
            new ItemStack[]{
                shellCasing(), mortarShell(), mortarShell(),
                mortarShell(), null, null,
                null, null, null
            }
        ));
        registerAmmo(plugin, new AmmoItem(category, "WWSF_SMOKE_SHELL", Material.DRAGON_BREATH, "&7Smoke Shell", ShellType.SMOKE, 0,
            new ItemStack[]{
                shellCasing(), gunpowder, new ItemStack(Material.WHITE_DYE),
                new ItemStack(Material.COAL), null, null,
                null, null, null
            }
        ));
        // The Gas Bomb is the only chemical munition; there is no gas shell.
    }

    private static void registerAmmo(WWSFPlugin plugin, @Nonnull AmmoItem item) {
        item.register(plugin);
        AMMO_ITEMS.put(item.getShellType(), item);
    }

    @Nonnull
    public static AmmoItem forShellType(@Nonnull ShellType type) {
        AmmoItem item = AMMO_ITEMS.get(type);
        return item != null ? item : AMMO_ITEMS.get(ShellType.STANDARD);
    }

    public static boolean hasMatchingAmmo(@Nonnull ItemStack item, @Nonnull ShellType type) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        SlimefunItem sfItem = SlimefunItem.getByItem(item);
        return sfItem instanceof AmmoItem ammo && ammo.getShellType() == type;
    }

    private static ItemStack shellCasing() {
        return new SlimefunItemStack(ShellCasing.ID, Material.BLAZE_ROD, "&fShell Casing");
    }

    private static ItemStack mortarShell() {
        return new SlimefunItemStack("WWSF_MORTAR_SHELL", Material.FIREWORK_STAR, "&6Mortar Shell");
    }
}
