package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class WWSFCategory {

    public static ItemGroup WWSF;

    public static void register(WWSFPlugin plugin) {
        WWSF = new ItemGroup(
            new NamespacedKey(plugin, "wwsf"),
            new CustomItemStack(
                Material.IRON_BLOCK,
                "&4&lWWSF",
                "&7World War weapons & ordnance",
                "",
                "&8Field artillery and more"
            ),
            4
        );
        WWSF.register(plugin);
    }

    private WWSFCategory() {
    }
}
