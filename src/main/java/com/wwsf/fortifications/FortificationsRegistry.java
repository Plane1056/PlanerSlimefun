package com.wwsf.fortifications;

import com.wwsf.WWSFPlugin;

public final class FortificationsRegistry {

    private FortificationsRegistry() {
    }

    public static void register(WWSFPlugin plugin) {
        BarbedWireBlock.registerStatic(plugin);
        SteelBlock.registerStatic(plugin);
        SteelSlab.registerStatic(plugin);
        SteelWall.registerStatic(plugin);
        SteelStairs.registerStatic(plugin);
        SteelRecipeOverride.install();
        GasAlarmBell.registerStatic(plugin);
    }
}
