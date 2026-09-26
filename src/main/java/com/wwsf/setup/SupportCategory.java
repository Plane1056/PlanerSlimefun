package com.wwsf.setup;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import com.wwsf.WWSFPlugin;
import com.wwsf.items.BridgeDeployListener;
import com.wwsf.items.PontoonBridgeKit;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;

public final class SupportCategory {

    public static ItemGroup SUPPORT;

    public static void register(WWSFPlugin plugin) {
        SUPPORT = new ItemGroup(
            new NamespacedKey(plugin, "wwsf_support"),
            new CustomItemStack(
                Material.RED_DYE,
                "&c&lField Support",
                "&7Medical and signalling equipment",
                "",
                "&8Bandages, flares, and aid"
            ),
            3
        );
        SUPPORT.register(plugin);

        // Register bridge kit
        new PontoonBridgeKit(SUPPORT).register(plugin);

        // Register bridge deployment listener
        plugin.getServer().getPluginManager().registerEvents(new BridgeDeployListener(plugin), plugin);
    }

    private SupportCategory() {
    }
}
