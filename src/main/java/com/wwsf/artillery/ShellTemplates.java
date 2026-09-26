package com.wwsf.artillery;

import org.bukkit.Material;

import com.wwsf.artillery.ShellType;
import com.wwsf.artillery.TrajectoryMode;

public final class ShellTemplates {

    private ShellTemplates() {
    }

    public static ShellProfile forType(ShellType type, TrajectoryMode trajectory) {
        return switch (type) {
            case STANDARD -> new ShellProfile(type, blast(type, false), WeaponVfx.forWeapon(trajectory, type), behavior(type));
            case SHRAPNEL -> new ShellProfile(type, blast(type, false), WeaponVfx.forWeapon(trajectory, type), behavior(type));
            case INCENDIARY -> new ShellProfile(type, blast(type, true), WeaponVfx.forWeapon(trajectory, type), behavior(type));
            case ARMOR_PIERCING -> new ShellProfile(type, blast(type, false), WeaponVfx.forWeapon(trajectory, type), behavior(type));
            case CLUSTER -> new ShellProfile(type, blast(type, false), WeaponVfx.forWeapon(trajectory, type), behavior(type));
            case SMOKE -> new ShellProfile(type, smokeBlast(type), WeaponVfx.forWeapon(trajectory, type), behavior(type));
        };
    }

    private static BlastProfile blast(ShellType type, boolean setFire) {
        return new BlastProfile(
            (float) ShellConfig.getDouble(type, "explosion-power", 0.5),
            (float) ShellConfig.getDouble(type, "damage", 5.0),
            ShellConfig.getDouble(type, "radius", 2.0),
            true,
            setFire,
            ShellConfig.getDouble(type, "block-break-radius", 1.25),
            ShellConfig.getDouble(type, "falloff", 1.0),
            ShellConfig.getDouble(type, "suppress-radius", 0.0),
            ShellConfig.getInt(type, "suppress-ticks", 0)
        );
    }

    // Smoke shell: no damage, no blast — radius is the smoke-sphere radius (scaled by cannon power).
    private static BlastProfile smokeBlast(ShellType type) {
        return new BlastProfile(
            0f, 0f,
            ShellConfig.getDouble(type, "radius", 1.75),
            false, false, 0.0, 1.0, 0.0, 0
        );
    }

    private static ShellBehavior behavior(ShellType type) {
        return new ShellBehavior(
            ShellConfig.getBoolean(type, "airburst", false),
            ShellConfig.getDouble(type, "airburst-height", 3.0),
            ShellConfig.getDouble(type, "speed", 1.0),
            ShellConfig.getDouble(type, "gravity", 1.0),
            ShellConfig.getDouble(type, "range", 1.0)
        );
    }
}
