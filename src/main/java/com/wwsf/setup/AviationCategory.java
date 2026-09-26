package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class AviationCategory {

    public static ItemGroup AVIATION;

    public static void register(WWSFPlugin plugin) {
        AVIATION = new ItemGroup(
            new NamespacedKey(plugin, "wwsf_aviation"),
            new CustomItemStack(
                Material.ELYTRA,
                "&f&lEarly Aviation",
                "&7Airborne equipment of the Great War",
                "",
                "&8Parachutes and aerial support"
            ),
            3
        );
        AVIATION.register(plugin);
    }

    private AviationCategory() {
    }
}
