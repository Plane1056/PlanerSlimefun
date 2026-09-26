package com.wwsf.support;

import com.wwsf.WWSFPlugin;

public final class SupportRegistry {

    private SupportRegistry() {
    }

    public static void register(WWSFPlugin plugin) {
        SignalFlare.registerStatic(plugin);
        Bandage.registerStatic(plugin);
        MorphineSyringe.registerStatic(plugin);
    }
}
