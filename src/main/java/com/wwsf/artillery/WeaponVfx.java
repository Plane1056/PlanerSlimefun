package com.wwsf.artillery;

import org.bukkit.Particle;
import org.bukkit.Sound;

public record WeaponVfx(
    Particle trail,
    Particle trailSecondary,
    Particle muzzle,
    Particle impactBurst,
    Sound fireSound,
    Sound flySound,
    float fireVolume,
    float firePitch,
    float flyVolume
) {

    public static WeaponVfx forWeapon(TrajectoryMode trajectory, ShellType shell) {
        if (trajectory == TrajectoryMode.ARC) {
            return switch (shell) {
                case CLUSTER -> new WeaponVfx(
                    Particle.FLAME, Particle.FLAME, Particle.LAVA,
                    Particle.TOTEM_OF_UNDYING, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                    Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 0.7f, 0.5f
                );
                case INCENDIARY -> new WeaponVfx(
                    Particle.FLAME, Particle.LAVA, Particle.CAMPFIRE_SIGNAL_SMOKE,
                    Particle.FLAME, Sound.ENTITY_GHAST_SHOOT,
                    Sound.BLOCK_FIRE_AMBIENT, 0.8f, 0.6f, 0.4f
                );
                case SMOKE -> new WeaponVfx(
                    Particle.LARGE_SMOKE, Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.CAMPFIRE_SIGNAL_SMOKE,
                    Particle.CAMPFIRE_SIGNAL_SMOKE, Sound.BLOCK_FIRE_AMBIENT,
                    Sound.ENTITY_CREEPER_HURT, 0.5f, 0.5f, 0.3f
                );
                default -> new WeaponVfx(
                    Particle.CAMPFIRE_COSY_SMOKE, Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.CLOUD,
                    Particle.TOTEM_OF_UNDYING, Sound.ENTITY_GENERIC_EXPLODE,
                    Sound.ENTITY_TNT_PRIMED, 1.0f, 0.5f, 0.3f
                );
            };
        }

        return switch (shell) {
            case SHRAPNEL -> new WeaponVfx(
                Particle.CRIT, Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.CLOUD,
                Particle.ENCHANTED_HIT, Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                Sound.ENTITY_ARROW_SHOOT, 0.8f, 1.4f, 0.3f
            );
            case ARMOR_PIERCING -> new WeaponVfx(
                Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.CRIT, Particle.FLASH,
                Particle.TOTEM_OF_UNDYING, Sound.ENTITY_IRON_GOLEM_ATTACK,
                Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.7f, 0.2f
            );
            case INCENDIARY -> new WeaponVfx(
                Particle.FLAME, Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.LAVA,
                Particle.FLAME, Sound.ENTITY_BLAZE_SHOOT,
                Sound.BLOCK_FIRE_AMBIENT, 0.8f, 1.0f, 0.25f
            );
            case CLUSTER -> new WeaponVfx(
                Particle.CRIT, Particle.FLAME, Particle.CLOUD,
                Particle.TOTEM_OF_UNDYING, Sound.ENTITY_FIREWORK_ROCKET_BLAST,
                Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.9f, 1.2f, 0.3f
            );
            case SMOKE -> new WeaponVfx(
                Particle.LARGE_SMOKE, Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.CAMPFIRE_SIGNAL_SMOKE,
                Particle.CAMPFIRE_SIGNAL_SMOKE, Sound.BLOCK_FIRE_AMBIENT,
                Sound.ENTITY_CREEPER_HURT, 0.5f, 0.5f, 0.3f
            );
            default -> new WeaponVfx(
                Particle.CRIT, Particle.CAMPFIRE_SIGNAL_SMOKE, Particle.CLOUD,
                Particle.TOTEM_OF_UNDYING, Sound.ENTITY_GENERIC_EXPLODE,
                Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.25f, 0.25f
            );
        };
    }
}
