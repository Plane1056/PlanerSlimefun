package com.wwsf.setup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class CustomModelDataRegistryTest {

    @Test
    void containsEveryItemThatUsesCustomModelData() {
        Map<String, Integer> expected = Map.ofEntries(
            Map.entry("GUN_PART", 1),
            Map.entry("WWSF_GAS_BOMB", 10006),
            Map.entry("GAS_GRENADE", 10006),
            Map.entry("WWSF_MUSTARD_GAS_BOMB", 10009),
            Map.entry("WWSF_BANDAGE", 10007),
            Map.entry("WWSF_SIGNAL_FLARE", 10008),
            Map.entry("WWSF_MORPHINE_SYRINGE", 10011),
            Map.entry("WWSF_GAS_ALARM_BELL", 10010),
            Map.entry("WWSF_BARBED_WIRE", 1010),
            Map.entry("WWSF_ENTRENCHING_TOOL", 10012),
            Map.entry("WWSF_PONTOON_BRIDGE_KIT", 2001),
            Map.entry("WWSF_MORTAR_SHELL", 10001),
            Map.entry("WWSF_SHRAPNEL_SHELL", 10002),
            Map.entry("WWSF_INCENDIARY_SHELL", 10003),
            Map.entry("WWSF_AP_SHELL", 10004),
            Map.entry("WWSF_CLUSTER_SHELL", 10005),
            Map.entry("WWSF_SMOKE_SHELL", 10006),
            Map.entry("WWSF_MACHINE_GUN_AMMO", 10007),
            Map.entry("WWSF_SHELL_CASING", 10020)
        );

        assertEquals(expected, CustomModelDataRegistry.registeredModelData());
    }
}
