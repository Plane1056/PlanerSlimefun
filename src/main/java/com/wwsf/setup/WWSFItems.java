package com.wwsf.setup;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtilleryRegistry;
import com.wwsf.items.GunPart;

public final class WWSFItems {

    private WWSFItems() {
    }

    public static void registerAll(WWSFPlugin plugin) {
        ArtilleryRegistry.registerAll(plugin);
        registerGunPart(plugin);
    }

    public static void registerGunPart(WWSFPlugin plugin) {
        new GunPart(GunsCategory.GUNS).register(plugin);
    }
}
