package com.wwsf.multiblock;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import com.wwsf.artillery.ArtillerySpec;

public interface CannonDefinition {

    @Nonnull
    String getId();

    @Nonnull
    String getName();

    boolean isValid(@Nonnull Block trigger);

    @Nonnull
    default String getMissingBlocksMessage(@Nonnull Block trigger) {
        return "&cStructure incomplete or invalid.";
    }

    int getLastRotation();

    @Nonnull
    Location getBarrelTip(@Nonnull Location origin, int rotation);

    @Nonnull
    Vector getBarrelDirection(int rotation);

    @Nonnull
    ArtillerySpec getWeaponSpec();

    int triggerOffsetX();

    int triggerOffsetY();

    int triggerOffsetZ();

    @Nonnull
    Material getTriggerMaterial();

    int getSizeX();

    int getSizeY();

    int getSizeZ();

    @Nonnull
    Material getMaterialAt(int x, int y, int z);

    default int[] rotateOffset(int cx, int cz, int rotation) {
        return switch (rotation) {
            case 0 -> new int[]{cx, cz};
            case 1 -> new int[]{cz, -cx};
            case 2 -> new int[]{-cx, -cz};
            case 3 -> new int[]{-cz, cx};
            default -> new int[]{cx, cz};
        };
    }
}
