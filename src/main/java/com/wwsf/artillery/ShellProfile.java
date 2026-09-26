package com.wwsf.artillery;

public record ShellProfile(
    ShellType shellType,
    BlastProfile baseBlast,
    WeaponVfx vfx,
    ShellBehavior behavior
) {
    public BlastProfile blastWithPower(double power) {
        return baseBlast.scaled(power);
    }
}
