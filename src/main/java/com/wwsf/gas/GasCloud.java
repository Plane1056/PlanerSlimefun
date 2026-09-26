package com.wwsf.gas;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.wwsf.WWSFPlugin;

/**
 * Organic gas cloud with strict lifetime and configurable lingering effects.
 */
public final class GasCloud {

    private static final Random RANDOM = new Random();

    private GasCloud() {
    }

    public static BukkitTask spawn(WWSFPlugin plugin, Location center) {
        return spawn(plugin, center, null);
    }

    /**
     * Checks if the player is immune to gas effects.
     * <p>
     * Immunity is granted if:
     * <ul>
     *   <li>The player has a registered immunity item on their head (helmet slot),
     *       matching any item stored via {@code /wwsf gas immunity}.</li>
     *   <li>OR the player has full armor (all 4 pieces equipped).</li>
     * </ul>
     */
    private static boolean isImmuneToGas(Player player) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) return false;

        ItemStack helmet = eq.getHelmet();
        if (helmet != null && helmet.getType() != Material.AIR) {
            if (GasImmunityConfig.isImmunityItem(helmet)) {
                return true;
            }
        }

        return eq.getChestplate() != null && eq.getChestplate().getType() != Material.AIR
            && eq.getLeggings() != null && eq.getLeggings().getType() != Material.AIR
            && eq.getBoots() != null && eq.getBoots().getType() != Material.AIR;
    }

    public static BukkitTask spawn(WWSFPlugin plugin, Location center, ItemStack sourceItem) {
        if (!GasConfig.isEnabled()) {
            return null;
        }

        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        GasConfig.GasProfile profile = GasConfig.profile();

        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_HURT, 0.8f, 0.5f);
        world.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);

        final Particle.DustOptions dustOptions =
            new Particle.DustOptions(org.bukkit.Color.fromRGB(166, 255, 77), 3.5f);

        Set<UUID> recentlyDamaged = new HashSet<>();

        return new BukkitRunnable() {
            int elapsed = 0;
            double radius = 1.0;

            @Override
            public void run() {
                if (elapsed >= profile.durationTicks()) {
                    cancel();
                    return;
                }

                radius = Math.min(profile.maxRadius(), radius + profile.spreadSpeed());
                spawnOrganicParticles(world, center, radius, dustOptions);
                applyEffects(world, center, radius, recentlyDamaged, profile);

                elapsed += profile.tickInterval();
            }
        }.runTaskTimer(plugin, 0L, profile.tickInterval());
    }

    private static void spawnOrganicParticles(World world, Location center, double radius, Particle.DustOptions dust) {
        int points = Math.max(8, (int) (radius * 4));

        for (int i = 0; i < points; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double horizontal = RANDOM.nextDouble() * radius;
            double vertical = RANDOM.nextDouble() * Math.min(3.0, radius * 0.35);
            Location point = center.clone().add(
                Math.cos(angle) * horizontal,
                vertical + 1.0,
                Math.sin(angle) * horizontal
            );
            world.spawnParticle(Particle.DUST, point, 2, 0.15, 0.1, 0.15, dust);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, point, 1, 0.1, 0.05, 0.1, 0.005);
        }
        Location bottom = center.clone().add(0, -1.0, 0);
        world.spawnParticle(Particle.DUST, bottom, (int) (radius * 3), 0.3, 0, 0.3, dust);
        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, bottom, (int) (radius), 0.3, 0, 0.3, 0.005);
    }

    private static void applyEffects(
        World world,
        Location center,
        double radius,
        Set<UUID> recentlyDamaged,
        GasConfig.GasProfile profile
    ) {
        double radiusSq = radius * radius;
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof Player player)) {
                continue;
            }

            Location playerLoc = player.getLocation();
            double dx = playerLoc.getX() - center.getX();
            double dy = playerLoc.getY() - center.getY();
            double dz = playerLoc.getZ() - center.getZ();
            if (dx * dx + dy * dy + dz * dz > radiusSq) {
                continue;
            }

            double density = 1.0 - (Math.sqrt(dx * dx + dz * dz) / Math.max(1.0, radius));
            if (RANDOM.nextDouble() > density) {
                continue;
            }

            if (isImmuneToGas(player)) {
                player.removePotionEffect(PotionEffectType.WITHER);
                player.removePotionEffect(PotionEffectType.NAUSEA);
                continue;
            }

            player.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER,
                profile.witherDuration(),
                profile.witherAmplifier(),
                false,
                true
            ));
            player.addPotionEffect(new PotionEffect(
                PotionEffectType.NAUSEA,
                profile.nauseaDuration(),
                profile.nauseaAmplifier(),
                false,
                true
            ));

            UUID id = player.getUniqueId();
            if (!recentlyDamaged.contains(id) && profile.contactDamage() > 0) {
                player.damage(profile.contactDamage());
                recentlyDamaged.add(id);
            }

            if (profile.tickDamage() > 0) {
                player.damage(profile.tickDamage());
            }
        }
    }
}
