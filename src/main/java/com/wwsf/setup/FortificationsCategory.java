package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class FortificationsCategory {

    public static ItemGroup FORTIFICATIONS;

    public static void register(WWSFPlugin plugin) {
        FORTIFICATIONS = new ItemGroup(
            new NamespacedKey(plugin, "wwsf_fortifications"),
            new CustomItemStack(
                Material.IRON_BARS,
                "&8&lField Fortifications",
                "&7Trench lines, wire, and cover",
                "",
                "&8Barbed wire, steel, and more"
            ),
            2
        );
        FORTIFICATIONS.register(plugin);
    }

    private FortificationsCategory() {
    }
}
