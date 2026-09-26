package com.oildrills;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class DrillRecoveryPolicyTest {

    @Test
    void emptyAndMovingPistonCoreStatesCanBeRepairedFromAnAuthoritativeRigRecord() {
        assertTrue(DrillManager.isRecoverableCoreMaterial(Material.AIR));
        assertTrue(DrillManager.isRecoverableCoreMaterial(Material.CAVE_AIR));
        assertTrue(DrillManager.isRecoverableCoreMaterial(Material.VOID_AIR));
        assertTrue(DrillManager.isRecoverableCoreMaterial(Material.MOVING_PISTON));
    }

    @Test
    void realReplacementBlocksAreNeverOverwrittenDuringRecovery() {
        assertFalse(DrillManager.isRecoverableCoreMaterial(Material.PISTON));
        assertFalse(DrillManager.isRecoverableCoreMaterial(Material.STICKY_PISTON));
        assertFalse(DrillManager.isRecoverableCoreMaterial(Material.OBSERVER));
        assertFalse(DrillManager.isRecoverableCoreMaterial(Material.STONE));
        assertFalse(DrillManager.isRecoverableCoreMaterial(Material.CHEST));
    }

    @Test
    void overpaidFuelIsRetainedAsRestartSafeCredit() {
        ActiveDrillData data = new ActiveDrillData(OilDrillTier.MARK_I, 0L, true, null, 8);

        assertTrue(data.isEnabled());
        assertEquals(5, data.spendFuelCredit(5));
        assertEquals(3, data.fuelCredit());
        data.addFuelCredit(12);
        assertEquals(15, data.fuelCredit());
    }
}
