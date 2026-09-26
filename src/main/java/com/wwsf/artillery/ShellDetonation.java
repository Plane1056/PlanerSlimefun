package com.wwsf.artillery;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.fortifications.SteelBlock;

public final class ShellDetonation {

    private ShellDetonation() {
    }

    // Soft, non-structural terrain + foliage that a mortar crater may chew through. Hard blocks
    // (stone, metal, obsidian, player builds) are intentionally excluded so shells can't grief.
    private static final Set<Material> SOFT_BLOCKS = buildSoftBlocks();

    private static final Set<Material> DEFAULT_AP_ARMOR = defaultApArmor();

    public static void detonate(Location impact, Player shooter, ShellProfile shell, double power, boolean usesArc) {
        World world = impact.getWorld();
        if (world == null) {
            return;
        }

        BlastProfile blast = shell.blastWithPower(power);

        switch (shell.shellType()) {
            case SHRAPNEL -> detonateShrapnel(world, impact, shooter, blast, usesArc);
            case INCENDIARY -> detonateIncendiary(world, impact, shooter, blast, usesArc);
            case ARMOR_PIERCING -> detonateAP(world, impact, shooter, blast, usesArc);
            case CLUSTER -> detonateCluster(world, impact, shooter, shell, power, usesArc);
            case SMOKE -> detonateSmoke(world, impact, shooter, blast, usesArc);
            default -> detonateImpact(world, impact, shooter, blast, ShellType.STANDARD, usesArc);
        }
    }

    // ----- Shared ground-impact explosion (Mortar / Cluster submunition base) -------------------

    private static void detonateImpact(World world, Location center, Player shooter, BlastProfile blast, ShellType type, boolean usesArc) {
        createBlast(world, center, shooter, blast);
        damageEntities(world, center, shooter, blast);
        if (blast.breakBlocks() && blast.blockBreakRadius() > 0) {
            if (blast.blockBreakRadius() < 2.0) {
                breakBlocksExcept(world, center, blast.blockBreakRadius(), java.util.Set.of(Material.OBSIDIAN, Material.BEDROCK));
            } else {
                breakSoftBlocks(world, center, blast.blockBreakRadius());
            }
        }
        if (blast.suppressRadius() > 0 && blast.suppressTicks() > 0) {
            applySuppression(world, center, blast.suppressRadius(), blast.suppressTicks(), shooter);
        }
        signature(world, center, type, usesArc);
        playShellSound(world, center, type);
    }

    // ----- 0. Smoke Shell (concealment cloud, no damage / no blast) --------------------------

    private static void detonateSmoke(World world, Location center, Player shooter, BlastProfile blast, boolean usesArc) {
        double radius = blast.damageRadius(); // 3.5 base, already scaled by cannon power
        int duration = ShellConfig.getInt(ShellType.SMOKE, "smoke-duration", 200);
        spawnSmokeSphere(world, center, radius);
        startSmokeZone(world, center, radius, duration);
        signature(world, center, ShellType.SMOKE, usesArc);
        playShellSound(world, center, ShellType.SMOKE);
    }

    private static void spawnSmokeSphere(World world, Location center, double radius) {
        if (radius <= 0) {
            return;
        }
        int r = (int) Math.ceil(radius);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int particles = (int) (radius * radius * 4);
        for (int i = 0; i < particles; i++) {
            double x = rnd.nextDouble(-radius, radius);
            double y = rnd.nextDouble(-radius, radius);
            double z = rnd.nextDouble(-radius, radius);
            if (x * x + y * y + z * z > radius * radius) {
                continue;
            }
            Location p = center.clone().add(x, y, z);
            world.spawnParticle(Particle.LARGE_SMOKE, p, 1, 0.15, 0.15, 0.15, 0.08);
            if (rnd.nextDouble() < 0.5) {
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, p, 1, 0.15, 0.15, 0.15, 0.08);
            }
        }
    }

    private static void startSmokeZone(World world, Location center, double radius, int durationTicks) {
        if (radius <= 0 || durationTicks <= 0) {
            return;
        }
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks) {
                    cancel();
                    return;
                }
                elapsed += 10;
                spawnSmokeSphere(world, center, radius);
            }
        }.runTaskTimer(WWSFPlugin.getInstance(), 0L, 10L);
    }

    // ----- 1. Mortar Shell (STANDARD) -----------------------------------------------------------
    // Handled by detonateImpact above: single HE blast, linear falloff, soft-block crater, suppression.

    // ----- 2. Shrapnel Shell (airburst anti-personnel) -----------------------------------------

    private static void detonateShrapnel(World world, Location center, Player shooter, BlastProfile blast, boolean usesArc) {
        // Shrapnel is still an explosive shell.  Previously it only spawned pellets, so a
        // cannon loaded with shrapnel appeared to pass through its impact without detonating.
        createBlast(world, center, shooter, blast);
        damageEntities(world, center, shooter, blast);

        int pellets = ShellConfig.getInt(ShellType.SHRAPNEL, "pellet-count", 28);
        double pelletDamage = ShellConfig.getDouble(ShellType.SHRAPNEL, "pellet-damage", 1.6);
        double pelletRange = ShellConfig.getDouble(ShellType.SHRAPNEL, "pellet-range", blast.damageRadius());
        int slowTicks = ShellConfig.getInt(ShellType.SHRAPNEL, "slow-ticks", 40);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < pellets; i++) {
            double theta = rnd.nextDouble() * Math.PI * 2;
            double phi = Math.acos(2 * rnd.nextDouble() - 1);
            Vector dir = new Vector(
                Math.sin(phi) * Math.cos(theta),
                Math.abs(Math.cos(phi)) * 0.6 + 0.25,
                Math.sin(phi) * Math.sin(theta)
            ).normalize();
            sprayPellet(world, center, dir, pelletRange, pelletDamage, shooter, slowTicks);
        }

        signature(world, center, ShellType.SHRAPNEL, usesArc);
        playShellSound(world, center, ShellType.SHRAPNEL);
    }

    // A single shrapnel fragment: travels in a straight ray, is stopped by solid cover, and
    // damages the first living thing it touches (sharp distance falloff along the ray).
    private static void sprayPellet(World world, Location from, Vector dir, double range, double damage, Player shooter, int slowTicks) {
        double step = 0.5;
        Location p = from.clone();
        for (double d = 0; d <= range; d += step) {
            p.add(dir.clone().multiply(step));
            Block b = p.getBlock();
            if (b.getType().isSolid() && b.getType() != Material.BARRIER) {
                world.spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0.15);
                return;
            }
            for (Entity e : world.getNearbyEntities(p, 0.6, 0.6, 0.6)) {
                if (e instanceof LivingEntity living && !e.equals(shooter)) {
                    double factor = Math.max(0.15, 1.0 - d / range);
                    living.damage((float) (damage * factor), shooter);
                    if (slowTicks > 0) {
                        living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowTicks, 0, false, true));
                    }
                    world.spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0.15);
                    return;
                }
            }
            if (d % 1.0 < step) {
                world.spawnParticle(Particle.CRIT, p, 1, 0, 0, 0, 0.15);
            }
        }
    }

    // ----- 3. Incendiary Shell (burn-and-linger area denial) -----------------------------------

    private static void detonateIncendiary(World world, Location center, Player shooter, BlastProfile blast, boolean usesArc) {
        createBlast(world, center, shooter, blast);
        damageEntities(world, center, shooter, blast);
        if (blast.blockBreakRadius() > 0) {
            breakSoftBlocks(world, center, blast.blockBreakRadius());
        }

        double burnRadius = ShellConfig.getDouble(ShellType.INCENDIARY, "burn-radius", blast.damageRadius());
        int fireTicks = ShellConfig.getInt(ShellType.INCENDIARY, "fire-ticks", 100);
        int burnDuration = ShellConfig.getInt(ShellType.INCENDIARY, "burn-duration", 200);
        double burnTickDamage = ShellConfig.getDouble(ShellType.INCENDIARY, "burn-tick-damage", 1.0);

        for (Entity e : world.getNearbyEntities(center, blast.damageRadius(), blast.damageRadius(), blast.damageRadius())) {
            if (e instanceof LivingEntity living && !e.equals(shooter) && e.getLocation().distance(center) <= blast.damageRadius()) {
                living.setFireTicks(fireTicks);
            }
        }

        igniteArea(world, center, Math.max(blast.blockBreakRadius(), burnRadius));
        startBurnZone(world, center, shooter, burnRadius, burnDuration, burnTickDamage);

        signature(world, center, ShellType.INCENDIARY, usesArc);
        playShellSound(world, center, ShellType.INCENDIARY);
    }

    // Reapplies burning damage to anything standing in the zone and keeps the fire alive.
    private static void startBurnZone(World world, Location center, Player shooter, double radius, int durationTicks, double tickDamage) {
        if (radius <= 0 || durationTicks <= 0) {
            return;
        }
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= durationTicks) {
                    igniteArea(world, center, radius);
                    cancel();
                    return;
                }
                elapsed += 20;
                for (Entity e : world.getNearbyEntities(center, radius, radius, radius)) {
                    if (e instanceof LivingEntity living && !e.equals(shooter) && e.getLocation().distance(center) <= radius) {
                        living.damage((float) tickDamage, shooter);
                    }
                }
                if (elapsed % 40 == 0) {
                    igniteArea(world, center, radius);
                }
            }
        }.runTaskTimer(WWSFPlugin.getInstance(), 20L, 20L);
    }

    // ----- 4. Armor-Piercing Shell (anti-fortification) --------------------------------------

    private static void detonateAP(World world, Location center, Player shooter, BlastProfile blast, boolean usesArc) {
        createBlast(world, center, shooter, blast);
        damageEntities(world, center, shooter, blast);
        if (blast.blockBreakRadius() > 0) {
            breakSoftBlocks(world, center, blast.blockBreakRadius());
        }

        List<String> raw = ShellConfig.getStringList(ShellType.ARMOR_PIERCING, "penetration-blocks");
        Set<Material> armor = raw.isEmpty()
            ? DEFAULT_AP_ARMOR
            : raw.stream().map(Material::matchMaterial).filter(Objects::nonNull).collect(java.util.stream.Collectors.toSet());
        if (!armor.isEmpty()) {
            breakArmorBlocks(world, center, Math.max(blast.blockBreakRadius(), 1.5), armor);
        }

        signature(world, center, ShellType.ARMOR_PIERCING, usesArc);
        playShellSound(world, center, ShellType.ARMOR_PIERCING);
    }

    // ----- 5. Cluster Shell (delayed area saturation) ------------------------------------------

    private static void detonateCluster(World world, Location center, Player shooter, ShellProfile shell, double power, boolean usesArc) {
        // A cluster shell's opening charge must also detonate; the submunitions are in
        // addition to the impact blast, not a replacement for it.
        createBlast(world, center, shooter, shell.blastWithPower(power));
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);
        world.spawnParticle(Particle.CRIT, center, 20, 2, 1.5, 2, 0.4);

        int count = ShellConfig.getInt(ShellType.CLUSTER, "submunition-count", 6);
        double scatter = ShellConfig.getDouble(ShellType.CLUSTER, "scatter-radius", 5.0);
        int stagger = Math.max(1, ShellConfig.getInt(ShellType.CLUSTER, "stagger-ticks", 6));

        BlastProfile subBlast = new BlastProfile(
            (float) ShellConfig.getDouble(ShellType.CLUSTER, "sub-explosion-power", 0.7),
            (float) ShellConfig.getDouble(ShellType.CLUSTER, "sub-damage", 5.0),
            ShellConfig.getDouble(ShellType.CLUSTER, "sub-radius", 2.5),
            true, false,
            ShellConfig.getDouble(ShellType.CLUSTER, "sub-block-break-radius", 1.5),
            ShellConfig.getDouble(ShellType.CLUSTER, "sub-falloff", 1.0),
            0, 0
        ).scaled(power);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        for (int i = 0; i < count; i++) {
            double ox = rnd.nextDouble(-scatter, scatter);
            double oz = rnd.nextDouble(-scatter, scatter);
            Location sub = new Location(world, center.getX() + ox, center.getY(), center.getZ() + oz);
            int delay = i * stagger;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> detonateSub(sub, shooter, subBlast, usesArc), delay);
        }
    }

    private static void detonateSub(Location center, Player shooter, BlastProfile blast, boolean usesArc) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        createBlast(world, center, shooter, blast);
        damageEntities(world, center, shooter, blast);
        if (blast.blockBreakRadius() > 0) {
            breakSoftBlocks(world, center, blast.blockBreakRadius());
        }
        signature(world, center, ShellType.CLUSTER, usesArc);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.6f, 1.3f);
    }

    // ----- Shared helpers ---------------------------------------------------------------

    /** Produces the impact flash and applies normal vanilla explosion block damage. */
    private static void createBlast(World world, Location center, Player shooter, BlastProfile blast) {
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1, 0, 0, 0, 0);
        world.createExplosion(center, blast.explosionPower(), blast.setFire(), blast.breakBlocks(), shooter);
    }

    private static void damageEntities(World world, Location center, Player shooter, BlastProfile blast) {
        double radius = blast.damageRadius();
        if (radius <= 0) {
            return;
        }
        double falloff = Math.max(0.1, blast.falloffExponent());
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(shooter)) {
                continue;
            }
            double distance = entity.getLocation().distance(center);
            if (distance > radius) {
                continue;
            }
            double factor = Math.pow(1.0 - distance / radius, falloff);
            float damage = (float) (blast.entityDamage() * factor);
            if (damage >= 0.5f) {
                living.damage(damage, shooter);
            }
        }
    }

    private static void breakSoftBlocks(World world, Location center, double radius) {
        if (radius <= 0) {
            return;
        }
        radius = Math.min(radius, 6.0);
        int r = (int) Math.ceil(radius);
        Location m = new Location(world, 0, 0, 0);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    m.setX(center.getX() + x);
                    m.setY(center.getY() + y);
                    m.setZ(center.getZ() + z);
                    if (m.distanceSquared(center) > radius * radius) {
                        continue;
                    }
                    Block block = m.getBlock();
                    if (SOFT_BLOCKS.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private static void breakBlocksExcept(World world, Location center, double radius, java.util.Set<Material> preserve) {
        if (radius <= 0) {
            return;
        }
        radius = Math.min(radius, 6.0);
        int r = (int) Math.ceil(radius);
        Location m = new Location(world, 0, 0, 0);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    m.setX(center.getX() + x);
                    m.setY(center.getY() + y);
                    m.setZ(center.getZ() + z);
                    if (m.distanceSquared(center) > radius * radius) {
                        continue;
                    }
                    Block block = m.getBlock();
                    Material type = block.getType();
                    if (type == Material.AIR
                        || type == Material.BARRIER
                        || preserve.contains(type)
                        || SteelBlock.isSteelMaterial(type)) {
                        continue;
                    }
                    block.setType(Material.AIR);
                }
            }
        }
    }

    private static void breakArmorBlocks(World world, Location center, double radius, Set<Material> armor) {
        int r = (int) Math.ceil(radius);
        Location m = new Location(world, 0, 0, 0);
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    m.setX(center.getX() + x);
                    m.setY(center.getY() + y);
                    m.setZ(center.getZ() + z);
                    if (m.distanceSquared(center) > radius * radius) {
                        continue;
                    }
                    Block block = m.getBlock();
                    if (armor.contains(block.getType())) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private static void applySuppression(World world, Location center, double radius, int ticks, Player shooter) {
        if (radius <= 0 || ticks <= 0) {
            return;
        }
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !entity.equals(shooter)) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, 1, false, true));
                living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, ticks, 0, false, true));
            }
        }
    }

    private static void igniteArea(World world, Location center, double radius) {
        int r = (int) Math.ceil(radius);
        Location m = new Location(world, 0, 0, 0);
        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    m.setX(center.getX() + x);
                    m.setY(center.getY() + y);
                    m.setZ(center.getZ() + z);
                    if (m.distanceSquared(center) > radius * radius) {
                        continue;
                    }
                    Block block = m.getBlock();
                    Block above = block.getRelative(0, 1, 0);
                    if (block.getType().isSolid() && above.getType().isAir()) {
                        above.setType(Material.FIRE);
                    }
                }
            }
        }
    }

    private static void signature(World world, Location center, ShellType type, boolean usesArc) {
        switch (type) {
            case SHRAPNEL -> {
                world.spawnParticle(Particle.CRIT, center, 40, 3, 2, 3, 0.4);
            }
            case INCENDIARY -> {
                world.spawnParticle(Particle.FLAME, center, 24, 1.5, 1, 1.5, 0.12);
                world.spawnParticle(Particle.LAVA, center, 10, 1.2, 0.6, 1.2, 0.09);
            }
            case ARMOR_PIERCING -> {
                world.spawnParticle(Particle.FALLING_DUST, center, 14, 1, 1, 1, 0.3, Material.IRON_BLOCK.createBlockData());
                world.spawnParticle(Particle.CRIT, center, 10, 1, 0.5, 1, 0.25);
            }
            case CLUSTER -> world.spawnParticle(Particle.SMOKE, center, 8, 0.8, 0.6, 0.8, 0.06);
            case SMOKE -> {
                world.spawnParticle(Particle.LARGE_SMOKE, center, 30, 2, 1.5, 2, 0.12);
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 20, 2, 1.5, 2, 0.1);
            }
            default -> {
                world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 6, 1, 0.6, 1, 0.06);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 2, 0.3, 0.3, 0.3, 0);
            }
        }
        if (usesArc) {
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, center, 5, 1, 0.5, 1, 0.08);
        }
    }

    private static void playShellSound(World world, Location center, ShellType type) {
        switch (type) {
            case SHRAPNEL -> {
                world.playSound(center, Sound.ENTITY_ARROW_HIT, 1.4f, 0.6f);
                world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.7f, 1.4f);
            }
            case INCENDIARY -> {
                world.playSound(center, Sound.BLOCK_FIRE_AMBIENT, 1.2f, 0.7f);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.9f, 0.7f);
            }
            case ARMOR_PIERCING -> {
                world.playSound(center, Sound.ENTITY_IRON_GOLEM_ATTACK, 1.1f, 0.5f);
                world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.6f);
            }
            case CLUSTER -> world.playSound(center, Sound.ENTITY_TNT_PRIMED, 0.8f, 1.2f);
            case SMOKE -> world.playSound(center, Sound.ENTITY_CREEPER_HURT, 0.6f, 0.9f);
            default -> world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        }
    }

    private static Set<Material> buildSoftBlocks() {
        Set<Material> set = new HashSet<>();
        for (String name : List.of(
            "DIRT", "GRASS_BLOCK", "SAND", "RED_SAND", "GRAVEL", "SNOW", "SNOW_BLOCK", "CLAY",
            "MYCELIUM", "PODZOL", "COARSE_DIRT", "ROOTED_DIRT", "MOSS_BLOCK", "MOSS_CARPET",
            "DIRT_PATH", "FARMLAND", "SHORT_GRASS", "TALL_GRASS", "FERN", "DEAD_BUSH", "VINE",
            "AZALEA", "AZALEA_LEAVES", "OAK_LEAVES", "SPRUCE_LEAVES", "BIRCH_LEAVES",
            "JUNGLE_LEAVES", "ACACIA_LEAVES", "DARK_OAK_LEAVES", "MANGROVE_LEAVES",
            "CHERRY_LEAVES", "PALE_OAK_LEAVES", "NETHER_SPROUTS", "WARPED_ROOTS", "CRIMSON_ROOTS"
        )) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                set.add(m);
            }
        }
        return set;
    }

    private static Set<Material> defaultApArmor() {
        Set<Material> set = new HashSet<>();
        for (String name : List.of(
            "REINFORCED_PLATE", "DAMASCUS_STEEL_INGOT", "HARDENED_METAL_INGOT",
            "REINFORCED_ALLOY_INGOT", "IRON_BLOCK", "OBSIDIAN", "NETHERITE_BLOCK"
        )) {
            Material m = Material.matchMaterial(name);
            if (m != null) {
                set.add(m);
            }
        }
        return set;
    }

}
