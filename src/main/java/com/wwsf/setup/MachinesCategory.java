package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class MachinesCategory {

    public static ItemGroup MACHINES;

    public static void register(WWSFPlugin plugin) {
        MACHINES = new ItemGroup(
            new NamespacedKey(plugin, "great_war_industries"),
            new CustomItemStack(
                Material.PISTON,
                "&8&lGreat War Industries",
                "&7Oil drills, refineries & war supply"
            ),
            6
        );
        MACHINES.register(plugin);
    }

    private MachinesCategory() {
    }
}
