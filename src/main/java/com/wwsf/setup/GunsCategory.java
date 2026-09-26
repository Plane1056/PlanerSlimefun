package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class GunsCategory {

    public static ItemGroup GUNS;

    public static void register(WWSFPlugin plugin) {
        GUNS = new ItemGroup(
            new NamespacedKey(plugin, "wwsf_guns"),
            new CustomItemStack(
                Material.CROSSBOW,
                "&8Guns",
                "&7Firearms, parts, and ammunition"
            ),
            5
        );
        GUNS.register(plugin);
    }

    private GunsCategory() {
    }
}