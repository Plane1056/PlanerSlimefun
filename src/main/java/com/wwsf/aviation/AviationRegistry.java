package com.wwsf.aviation;

import com.wwsf.WWSFPlugin;

public final class AviationRegistry {

    private AviationRegistry() {
    }

    public static void register(WWSFPlugin plugin) {
        Parachute.registerStatic(plugin);
    }
}
