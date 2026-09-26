package com.wwsf.artillery;

import javax.annotation.Nullable;

import org.bukkit.Material;

import com.wwsf.multiblock.MultiblockPattern;

public record ArtillerySpec(
    String id,
    Material blockMaterial,
    String displayName,
    String[] extraLore,
    @Nullable MultiblockPattern multiblock,
    WeaponRole role,
    TrajectoryMode trajectory,
    double maxRange,
    double minRange,
    double shellSpeed,
    double minArcHeight,
    double arcHeightFactor,
    double power,
    @Nullable String ammoItemId
) {
    public boolean usesArc() {
        return trajectory == TrajectoryMode.ARC;
    }

    public boolean isTurret() {
        return role == WeaponRole.TURRET;
    }

    public boolean isMortar() {
        return role == WeaponRole.MORTAR;
    }

    public boolean hasCustomAmmo() {
        return ammoItemId != null && !ammoItemId.isBlank();
    }
}
