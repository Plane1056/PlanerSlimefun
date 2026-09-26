package com.oildrills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

class OilDrillTierTest {

    @Test
    void usesFixedDefaultExtractionTimes() {
        YamlConfiguration config = new YamlConfiguration();

        assertEquals(60, OilDrillTier.MARK_I.getSeconds(config));
        assertEquals(18, OilDrillTier.MARK_II.getSeconds(config));
        assertEquals(9, OilDrillTier.MARK_III.getSeconds(config));
    }

    @Test
    void usesHalfCoalDefaultsRoundedToWholeItems() {
        YamlConfiguration config = new YamlConfiguration();

        assertEquals(10, OilDrillTier.MARK_I.getCoalPerBarrel(config));
        assertEquals(8, OilDrillTier.MARK_II.getCoalPerBarrel(config));
        assertEquals(5, OilDrillTier.MARK_III.getCoalPerBarrel(config));
    }

    @Test
    void honorsExplicitServerOverrides() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("drills.mark-2.seconds", 7);
        config.set("drills.mark-2.coal-per-barrel", 3);

        assertEquals(7, OilDrillTier.MARK_II.getSeconds(config));
        assertEquals(3, OilDrillTier.MARK_II.getCoalPerBarrel(config));
    }

    @Test
    void clampsInvalidExtractionTimeToOneSecond() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("drills.mark-3.seconds", 0);

        assertEquals(1, OilDrillTier.MARK_III.getSeconds(config));
    }

    @Test
    void markThreeRigUsesSteelBlocks() {
        assertEquals(Material.RED_NETHER_BRICKS, DrillStructure.tierMaterial(OilDrillTier.MARK_III));
    }

    @Test
    void tiersRestoreTheirOriginalCoreMaterials() {
        assertEquals(Material.PISTON, OilDrillTier.MARK_I.getCoreMaterial());
        assertEquals(Material.STICKY_PISTON, OilDrillTier.MARK_II.getCoreMaterial());
        assertEquals(Material.OBSERVER, OilDrillTier.MARK_III.getCoreMaterial());
        assertEquals(OilDrillTier.MARK_I, OilDrillTier.fromCoreMaterial(Material.PISTON));
        assertEquals(OilDrillTier.MARK_II, OilDrillTier.fromCoreMaterial(Material.STICKY_PISTON));
        assertEquals(OilDrillTier.MARK_III, OilDrillTier.fromCoreMaterial(Material.OBSERVER));
    }

    @Test
    void onlyTheKnownMarkTwoStickyPistonDamageIsAutoRepaired() {
        assertTrue(DrillStructure.isLegacyStickyPistonPullDamage(
            OilDrillTier.MARK_II,
            Material.DEEPSLATE_BRICKS,
            Material.AIR
        ));
        assertFalse(DrillStructure.isLegacyStickyPistonPullDamage(
            OilDrillTier.MARK_I,
            Material.STONE_BRICKS,
            Material.AIR
        ));
        assertFalse(DrillStructure.isLegacyStickyPistonPullDamage(
            OilDrillTier.MARK_II,
            Material.DEEPSLATE_BRICKS,
            Material.DEEPSLATE_BRICKS
        ));
    }
}
