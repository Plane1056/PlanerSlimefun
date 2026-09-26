package com.wwsf.artillery;

import java.util.Random;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import static org.bukkit.FluidCollisionMode.NEVER;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ShellProfile;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.util.Messages;
import com.wwsf.vehicle.VehicleQuery;

/**
 * Spawns and ticks inbound projectiles (direct tracers vs arcing lobs).
 *
 * <p>All firing originates at the multiblock's muzzle (barrel tip). Horizontal aim is constrained
 * to a narrow cone around the barrel's forward axis (see {@link AimTargetResolver#resolveFromBarrel}).
 */
public final class ProjectileLauncher {

    private static final Random RANDOM = new Random();

    // Base spread as a fraction of range - higher = less accurate.
    // Kept near-zero so shots inside the aiming view land almost exactly on target.
    // Per-weapon override via items.WWSF_<id>.projectile.spread.
    private static final double DEFAULT_SPREAD_FACTOR = 0.005;

    private static final double DEFAULT_DIRECT_GRAVITY = 0.02;
    private static final double DEFAULT_ARC_GRAVITY = 0.085;

    private ProjectileLauncher() {
    }

    private static double getMinFireDistance(WWSFPlugin plugin) {
        return plugin.getConfig().getDouble("weapons.min-fire-distance", 15.0);
    }

    private static double getSpreadFactor(ArtillerySpec spec) {
        return ItemConfigHelper.getDouble(
            ItemConfigHelper.configId(spec.id()), "projectile.spread", DEFAULT_SPREAD_FACTOR);
    }

    /**
     * Applies random horizontal spread to the target point, making the weapon less accurate.
     * The spread scales with distance so longer shots are less accurate.
     */
    private static Location applySpread(Location origin, Location target, ArtillerySpec spec) {
        double distance = origin.distance(target);
        double spread = distance * getSpreadFactor(spec);

        Location perturbed = target.clone();
        // Random horizontal offset in both X and Z
        perturbed.add(
            (RANDOM.nextDouble() - 0.5) * spread * 2,
            0,
            (RANDOM.nextDouble() - 0.5) * spread * 2
        );
        return perturbed;
    }

    public static void fire(WWSFPlugin plugin, Player shooter, Location origin, Location target, ArtillerySpec spec, ShellProfile shell) {
        fire(plugin, shooter, origin, target, spec, shell, spec.minArcHeight(), null, spec.minRange());
    }

    public static void fire(
        WWSFPlugin plugin,
        Player shooter,
        Location origin,
        Location target,
        ArtillerySpec spec,
        ShellProfile shell,
        double arcHeightOverride,
        Vector vesselVelocity
    ) {
        fire(plugin, shooter, origin, target, spec, shell, arcHeightOverride, vesselVelocity, spec.minRange());
    }

    public static void fire(
        WWSFPlugin plugin,
        Player shooter,
        Location origin,
        Location target,
        ArtillerySpec spec,
        ShellProfile shell,
        double arcHeightOverride,
        Vector vesselVelocity,
        double minDistance
    ) {
        Location muzzle = muzzleLocation(origin, target);
        playMuzzleEffect(muzzle, shell);

        if (spec.usesArc()) {
            launchArc(plugin, shooter, muzzle, target, spec, shell, arcHeightOverride, vesselVelocity, minDistance);
        } else {
            launchDirect(plugin, shooter, muzzle, target, spec, shell, vesselVelocity, minDistance);
        }
    }

    private static Location muzzleLocation(Location origin, Location target) {
        Location muzzle = origin.clone().add(0.5, 1.25, 0.5);
        Vector flat = target.toVector().subtract(muzzle.toVector());
        flat.setY(0);
        if (flat.lengthSquared() > 0.01) {
            flat.normalize().multiply(0.55);
            muzzle.add(flat);
        }
        return muzzle;
    }

    private static void playMuzzleEffect(Location muzzle, ShellProfile shell) {
        World world = muzzle.getWorld();
        if (world == null) {
            return;
        }

        WeaponVfx vfx = shell.vfx();
        world.playSound(muzzle, vfx.fireSound(), vfx.fireVolume(), vfx.firePitch());
        world.spawnParticle(vfx.muzzle(), muzzle, 6, 0.15, 0.1, 0.15, 0.06);
        world.spawnParticle(Particle.CLOUD, muzzle, 2, 0.1, 0.05, 0.1, 0.03);

        if (shell.shellType() == ShellType.SHRAPNEL) {
            world.spawnParticle(Particle.FLAME, muzzle, 4, 0.2, 0.1, 0.2, 0.09);
        } else {
            world.spawnParticle(Particle.CRIT, muzzle, 4, 0.1, 0.1, 0.1, 0.15);
        }
    }

    /**
     * Flat, fast direct-fire with realistic bullet drop. The shell is launched along the aim line
     * from the muzzle and gravity is applied each tick, producing a low arc rather than a hitscan.
     */
    private static void launchDirect(
        WWSFPlugin plugin,
        Player shooter,
        Location start,
        Location target,
        ArtillerySpec spec,
        ShellProfile shell,
        Vector vesselVelocity,
        double minDistance
    ) {
        World world = start.getWorld();
        if (world == null) {
            return;
        }

        ShellBehavior behavior = shell.behavior();
        double maxRange = ItemConfigHelper.getDouble(
            ItemConfigHelper.configId(spec.id()), "range.max", spec.maxRange()) * behavior.rangeMultiplier();
        double shellSpeed = spec.shellSpeed() * behavior.speedMultiplier();
        double gravity = ItemConfigHelper.getDouble(
            ItemConfigHelper.configId(spec.id()), "projectile.gravity", DEFAULT_DIRECT_GRAVITY) * behavior.gravityMultiplier();

        Vector toTarget = target.toVector().subtract(start.toVector());
        double dist = toTarget.length();
        if (dist < 0.001) {
            return;
        }
        if (dist > maxRange) {
            toTarget.multiply(maxRange / dist);
            dist = maxRange;
        }

        double minFireDistance = minDistance;
        if (minFireDistance <= 0) {
            minFireDistance = getMinFireDistance(plugin);
        }
        if (dist < minFireDistance) {
            shooter.sendMessage(Messages.get("messages.target-too-close",
                "&cTarget too close. Minimum distance: {distance} blocks.",
                "{distance}", String.format("%.1f", minFireDistance)));
            return;
        }

        Vector velocity = toTarget.clone().normalize().multiply(shellSpeed);
        if (vesselVelocity != null) {
            velocity.add(vesselVelocity);
        }
        WeaponVfx vfx = shell.vfx();
        boolean airburst = behavior.airburst();

        new BukkitRunnable() {
            final Location pos = start.clone();
            final Vector vel = velocity.clone();
            double travelled = 0;

            @Override
            public void run() {
                if (airburst) {
                    double horiz = Math.hypot(pos.getX() - target.getX(), pos.getZ() - target.getZ());
                    if (horiz <= 1.0 && pos.getY() > target.getY()) {
                        Location burst = new Location(world, target.getX(), target.getY() + behavior.airburstHeight(), target.getZ());
                        ShellDetonation.detonate(burst, shooter, shell, spec.power(), spec.usesArc());
                        cancel();
                        return;
                    }
                }

                if (travelled >= maxRange) {
                    ShellDetonation.detonate(pos, shooter, shell, spec.power(), spec.usesArc());
                    cancel();
                    return;
                }

                vel.setY(vel.getY() - gravity);
                Location prev = pos.clone();
                pos.add(vel);
                travelled += shellSpeed;
                spawnTrail(world, pos, shell, false);

                RayTraceResult hit = raycastObstruction(prev, pos);
                if (hit != null) {
                    Location detPos = snapToSurface(world, hit.getHitPosition().toLocation(world));
                    ShellDetonation.detonate(detPos, shooter, shell, spec.power(), spec.usesArc());
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Mortar / high-arc lob. Solves a ballistic trajectory that lands exactly on the target while
     * guaranteeing a minimum apex height ({@code arcHeightOverride}), so the shell clears cover. The
     * flight time is lengthened as needed to raise the arc without sacrificing accuracy.
     */
    private static void launchArc(
        WWSFPlugin plugin,
        Player shooter,
        Location start,
        Location end,
        ArtillerySpec spec,
        ShellProfile shell,
        double arcHeightOverride,
        Vector vesselVelocity,
        double minDistance
    ) {
        World world = start.getWorld();
        if (world == null) {
            return;
        }

        String cfgId = ItemConfigHelper.configId(spec.id());
        double gravity = ItemConfigHelper.getDouble(cfgId, "projectile.gravity", DEFAULT_ARC_GRAVITY) * shell.behavior().gravityMultiplier();
        double maxRange = ItemConfigHelper.getDouble(cfgId, "range.max", spec.maxRange()) * shell.behavior().rangeMultiplier();
        double mortarSpeed = ItemConfigHelper.getDouble(cfgId, "projectile.mortar-speed", 1.0) * shell.behavior().speedMultiplier();

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();

        double hDist = Math.sqrt(dx * dx + dz * dz);
        if (hDist > maxRange) {
            double scale = maxRange / hDist;
            dx *= scale;
            dz *= scale;
            hDist = maxRange;
        }

        double minFireDistance = minDistance;
        if (minFireDistance <= 0) {
            minFireDistance = getMinFireDistance(plugin);
        }
        double flightDistance = Math.sqrt(dx * dx + dz * dz);
        if (flightDistance < minFireDistance) {
            shooter.sendMessage(Messages.get("messages.target-too-close",
                "&cTarget too close. Minimum distance: {distance} blocks.",
                "{distance}", String.format("%.1f", minFireDistance)));
            return;
        }

        // Base flight time from the configured horizontal muzzle speed.
        double flightTicks = Math.max(30, flightDistance / Math.max(0.05, mortarSpeed));

        // If the natural arc is too flat, lengthen the flight so the apex reaches arcHeightOverride
        // while still landing exactly on the target.
        if (arcHeightOverride > 0) {
            double baseVy = (dy + 0.5 * gravity * flightTicks * flightTicks) / flightTicks;
            double apex = (baseVy * baseVy) / (2.0 * gravity);
            if (apex < arcHeightOverride) {
                double a = 0.5 * gravity;
                double b = -Math.sqrt(2.0 * gravity * arcHeightOverride);
                double c = dy;
                double disc = b * b - 4.0 * a * c;
                if (disc >= 0) {
                    double t1 = (-b - Math.sqrt(disc)) / (2.0 * a);
                    double t2 = (-b + Math.sqrt(disc)) / (2.0 * a);
                    double raised = Math.max(t1, t2);
                    if (raised > flightTicks) {
                        flightTicks = Math.max(raised, 30);
                    }
                }
            }
        }

        final double vx = dx / flightTicks + (vesselVelocity != null ? vesselVelocity.getX() : 0);
        final double vz = dz / flightTicks + (vesselVelocity != null ? vesselVelocity.getZ() : 0);
        final double vy = (dy + 0.5 * gravity * flightTicks * flightTicks) / flightTicks
            + (vesselVelocity != null ? vesselVelocity.getY() : 0);

        WeaponVfx vfx = shell.vfx();
        final double totalTicks = flightTicks;
        boolean airburst = shell.behavior().airburst();

        new BukkitRunnable() {
            double t = 0;
            int tickCount = 0;
            Location prevPos = start.clone();

            @Override
            public void run() {
                Location pos = new Location(
                    world,
                    start.getX() + vx * t,
                    start.getY() + vy * t - 0.5 * gravity * t * t,
                    start.getZ() + vz * t
                );

                if (airburst) {
                    double horiz = Math.hypot(pos.getX() - end.getX(), pos.getZ() - end.getZ());
                    if (horiz <= 1.0) {
                        Location burst = new Location(world, end.getX(), end.getY() + shell.behavior().airburstHeight(), end.getZ());
                        ShellDetonation.detonate(burst, shooter, shell, spec.power(), true);
                        cancel();
                        return;
                    }
                }

                if (t >= totalTicks) {
                    // Snap detonation to just above the highest solid block so the explosion is never muffled inside terrain.
                    Location safeEnd = snapToSurface(world, end);
                    ShellDetonation.detonate(safeEnd, shooter, shell, spec.power(), true);
                    cancel();
                    return;
                }

                spawnTrail(world, pos, shell, true);

                if (tickCount++ % 6 == 0 && vfx.flySound() != null) {
                    world.playSound(pos, vfx.flySound(), vfx.flyVolume() * 0.7f, 0.5f);
                }

                RayTraceResult hit = raycastObstruction(prevPos, pos);
                if (hit != null) {
                    Location detPos = snapToSurface(world, hit.getHitPosition().toLocation(world));
                    ShellDetonation.detonate(detPos, shooter, shell, spec.power(), true);
                    cancel();
                    return;
                }

                // Safety: if the shell drops below the world floor, detonate immediately.
                if (pos.getY() < world.getMinHeight()) {
                    ShellDetonation.detonate(snapToSurface(world, end), shooter, shell, spec.power(), true);
                    cancel();
                    return;
                }

                prevPos = pos.clone();
                t += 1.0;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void spawnTrail(World world, Location pos, ShellProfile shell, boolean arc) {
        WeaponVfx vfx = shell.vfx();
        world.spawnParticle(vfx.trail(), pos, arc ? 2 : 1, 0.03, 0.03, 0.03, arc ? 0.04 : 0.06);
        world.spawnParticle(vfx.trailSecondary(), pos, 1, 0.02, 0.02, 0.02, 0.04);
    }

    /**
     * Snaps a location to just above the surface so the explosion is never muffled inside terrain.
     * The climb rises straight up from the impact column (not the global column height), so a
     * ground hit near a tall hillside no longer teleports the detonation to that distant terrain.
     */
    private static Location snapToSurface(World world, Location loc) {
        if (world == null) return loc;
        if (!loc.getBlock().getType().isSolid()) {
            // Already in air — detonate here.
            return loc;
        }
        // Rise straight up from the impact block until clear of the solid column.
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        int y = loc.getBlockY();
        int maxY = world.getMaxHeight();
        while (y < maxY && world.getBlockAt(x, y, z).getType().isSolid()) {
            y++;
        }
        return new Location(world, loc.getX(), y + 0.5, loc.getZ());
    }

    private static boolean isObstruction(Block block) {
        Material type = block.getType();
        return type.isSolid() && type != Material.BARRIER;
    }

    @Nullable
    private static RayTraceResult raycastObstruction(Location from, Location to) {
        World world = from.getWorld();
        if (world == null) {
            return null;
        }
        Vector direction = to.toVector().subtract(from.toVector());
        double distance = direction.length();
        if (distance < 0.001) {
            return null;
        }
        direction.normalize();
        org.bukkit.util.RayTraceResult result = world.rayTraceBlocks(from, direction, distance,
                NEVER, true);
        if (result != null && result.getHitBlock() != null) {
            Block hit = result.getHitBlock();
            Material type = hit.getType();
            if (type.isSolid() && type != Material.BARRIER) {
                return result;
            }
        }
        return null;
    }
}
