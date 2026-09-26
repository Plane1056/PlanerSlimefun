package com.wwsf.artillery;

/**
 * Determines player interaction flow for emplaced weapons.
 */
public enum WeaponRole {
    /** Direct-fire: aim with look direction, left-click to fire. */
    TURRET,
    /** Indirect-fire: charge powder while aiming, left-click to lob. */
    MORTAR,
    /** General artillery: crosshair aim with arcing trajectory. */
    CANNON
}
