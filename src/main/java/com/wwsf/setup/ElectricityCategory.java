package com.wwsf.setup;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class ElectricityCategory {

    public static ItemGroup AMMUNITION;

    public static void register(WWSFPlugin plugin) {
        AMMUNITION = new ItemGroup(
            new NamespacedKey(plugin, "wwsf_ammunition"),
            new CustomItemStack(
                Material.GUNPOWDER,
                "&6WWSF Ammunition",
                "&7Artillery ammunition and components"
            ),
            5
        );
        AMMUNITION.register(plugin);
    }

    private ElectricityCategory() {
    }
}