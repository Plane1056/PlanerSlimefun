package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class ResourcesCategory {

    public static ItemGroup RESOURCES;

    public static void register(WWSFPlugin plugin) {
        RESOURCES = new ItemGroup(
            new NamespacedKey(plugin, "wwsf_resources"),
            new CustomItemStack(
                Material.IRON_INGOT,
                "&7Resources",
                "&7Metal dusts and raw materials"
            ),
            4
        );
        RESOURCES.register(plugin);
    }

    private ResourcesCategory() {
    }
}
