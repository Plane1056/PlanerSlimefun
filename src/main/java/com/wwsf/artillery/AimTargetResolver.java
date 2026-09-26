package com.wwsf.artillery;

import javax.annotation.Nonnull;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class AimTargetResolver {

    /** Distance of the player-facing aim square; shared with its particle renderer. */
    public static final double AIM_BOX_DISTANCE = 3.0;

    private AimTargetResolver() {
    }

    @Nonnull
    public static Location resolve(@Nonnull Player player, @Nonnull Location origin, double maxRange, double minDistance) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();

        RayTraceResult trace = player.getWorld().rayTraceBlocks(
            eye,
            direction,
            maxRange,
            FluidCollisionMode.NEVER,
            true
        );

        Location hit;
        if (trace != null && trace.getHitPosition() != null) {
            hit = trace.getHitPosition().toLocation(player.getWorld());
        } else {
            hit = eye.clone().add(direction.clone().multiply(maxRange));
        }

        double distFromCannon = origin.distance(hit);
        if (distFromCannon > maxRange) {
            Vector fromCannon = hit.toVector().subtract(origin.toVector()).normalize().multiply(maxRange);
            hit = origin.clone().add(fromCannon);
            distFromCannon = maxRange;
        }

        if (distFromCannon < minDistance) {
            Vector fromCannon = hit.toVector().subtract(origin.toVector()).normalize().multiply(minDistance);
            hit = origin.clone().add(fromCannon);
        }

        return hit;
    }

    @Nonnull
    public static Location resolveFreeAim(
        @Nonnull Player player,
        @Nonnull Location origin,
        double maxRange,
        double minDistance
    ) {
        Vector direction = player.getEyeLocation().getDirection().normalize();

        if (direction.getY() < 0) {
            direction.setY(0);
            direction.normalize();
        }

        World world = origin.getWorld();
        if (world == null) {
            world = player.getWorld();
        }

        RayTraceResult trace = world.rayTraceBlocks(
            origin, direction, maxRange, FluidCollisionMode.NEVER, true
        );

        double dist = maxRange;
        if (trace != null && trace.getHitPosition() != null) {
            Location hit = trace.getHitPosition().toLocation(world);
            dist = origin.distance(hit);
        }

        if (dist < minDistance) {
            dist = minDistance;
        }

        return origin.clone().add(direction.clone().multiply(dist));
    }

    @Nonnull
    public static Location preview(@Nonnull Player player, double maxRange, double minDistance) {
        return resolve(player, player.getLocation(), maxRange, minDistance);
    }

    /**
     * Resolves the aim point for a fixed, rotation-aware barrel emplacement.
     *
     * <p>The player's look ray is traced to find where they are aiming. The resulting point is
     * then clamped into the weapon's aiming view: its lateral offset from the barrel's forward
     * centerline is capped at {@code coneHalfWidth} blocks (horizontal), and its vertical offset
     * from the barrel center is capped at the same half-height. The forward distance is preserved.
     * This keeps every shot inside the ±N block view box but never blocks the shot.
     */
    @Nonnull
    public static Location resolveFromBarrel(
        @Nonnull Player player,
        @Nonnull Location barrelTip,
        @Nonnull Vector barrelDir,
        double maxRange,
        double minDistance,
        double coneHalfWidth
    ) {
        Location centeredTip = barrelTip.clone().add(0.5, 0.5, 0.5);
        Vector flatDir = new Vector(barrelDir.getX(), 0, barrelDir.getZ()).normalize();
        Vector perp = new Vector(-flatDir.getZ(), 0, flatDir.getX());
        // Distance in front of the muzzle where the aiming box is drawn. This MUST match the
        // visual box distance (AimingManager.VIEW_DISTANCE) so that "crosshair inside the box"
        // corresponds exactly to the allowed field of fire. The angular clamp (below) keeps a
        // forward component, so aiming within the box follows the crosshair instead of forcing up.
        double boxDist = AIM_BOX_DISTANCE;
        double maxRatio = coneHalfWidth / boxDist;

        Location eye = player.getEyeLocation();
        Vector look = eye.getDirection().normalize();

        // How far the player is actually looking (wall hit, or max range).
        RayTraceResult trace = player.getWorld().rayTraceBlocks(
            eye, look, maxRange, FluidCollisionMode.NEVER, true
        );
        Location hit;
        if (trace != null && trace.getHitPosition() != null) {
            hit = trace.getHitPosition().toLocation(player.getWorld());
        } else {
            hit = eye.clone().add(look.clone().multiply(maxRange));
        }
        double aimDistance = centeredTip.distance(hit);
        if (aimDistance > maxRange) {
            aimDistance = maxRange;
        }
        if (aimDistance < minDistance) {
            aimDistance = minDistance;
        }

        // Decompose the look direction in the barrel frame and clamp its off-axis ratio. Within
        // the box the shot follows the crosshair exactly; outside it is clamped to the cone edge.
        // Because the forward (barrel) component always stays dominant, shots never go straight up.
        double fComp = look.getX() * flatDir.getX() + look.getZ() * flatDir.getZ();
        if (fComp < 0.01) {
            fComp = 0.01;
        }
        double lComp = look.getX() * perp.getX() + look.getZ() * perp.getZ();
        double vComp = look.getY();

        double hLen = Math.sqrt(fComp * fComp + lComp * lComp);
        double latRatio = (fComp != 0) ? lComp / fComp : Math.signum(lComp);
        double vertRatio = (hLen > 0.001) ? vComp / hLen : Math.signum(vComp);

        boolean outside = false;
        if (latRatio > maxRatio) { latRatio = maxRatio; outside = true; }
        else if (latRatio < -maxRatio) { latRatio = -maxRatio; outside = true; }
        if (vertRatio > maxRatio) { vertRatio = maxRatio; outside = true; }
        else if (vertRatio < -maxRatio) { vertRatio = -maxRatio; outside = true; }

        Vector dir;
        if (!outside) {
            dir = look.clone();
        } else {
            dir = flatDir.clone()
                .add(perp.clone().multiply(latRatio))
                .add(new Vector(0, vertRatio, 0))
                .normalize();
        }

        return centeredTip.clone().add(dir.multiply(aimDistance));
    }
}
