package com.wwsf.multiblock;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.wwsf.WWSFPlugin;

public final class MaximMachineGunValidator {

    private MaximMachineGunValidator() {}

    public static final class BlockMismatch {
        private final int worldX;
        private final int worldY;
        private final int worldZ;
        private final Material expected;
        private final Material found;

        public BlockMismatch(int worldX, int worldY, int worldZ, Material expected, Material found) {
            this.worldX = worldX;
            this.worldY = worldY;
            this.worldZ = worldZ;
            this.expected = expected;
            this.found = found;
        }

        public int getWorldX() { return worldX; }
        public int getWorldY() { return worldY; }
        public int getWorldZ() { return worldZ; }
        public Material getExpected() { return expected; }
        public Material getFound() { return found; }

        @Nonnull
        public String describe() {
            return "&7[&f" + worldX + " " + worldY + " " + worldZ + "&7] expected &f"
                + formatMaterial(expected) + "&7 but found &f" + formatMaterial(found);
        }

        private static String formatMaterial(Material mat) {
            return mat.name().replace("_", " ").toLowerCase();
        }
    }

    public static final class Result {
        private final boolean valid;
        private final int rotation;
        private final Location origin;
        private final List<BlockMismatch> mismatches;

        private Result(boolean valid, int rotation, Location origin, List<BlockMismatch> mismatches) {
            this.valid = valid;
            this.rotation = rotation;
            this.origin = origin;
            this.mismatches = mismatches;
        }

        public boolean isValid() { return valid; }
        public int getRotation() { return rotation; }
        @Nullable
        public Location getOrigin() { return origin; }
        @Nonnull
        public List<BlockMismatch> getMismatches() { return mismatches; }

        static Result success(int rotation, Location origin) {
            return new Result(true, rotation, origin, List.of());
        }

        static Result failure(int bestRotation, Location bestOrigin, List<BlockMismatch> mismatches) {
            return new Result(false, bestRotation, bestOrigin, List.copyOf(mismatches));
        }
    }

    @Nonnull
    public static Result validate(@Nonnull MaximMachineGunDefinition definition, @Nonnull Block anchor) {
        World world = anchor.getWorld();
        int ax = anchor.getX();
        int ay = anchor.getY();
        int az = anchor.getZ();

        int caX = definition.getTriggerX();
        int caY = definition.getTriggerY();
        int caZ = definition.getTriggerZ();

        int bestRotation = 0;
        Location bestOrigin = null;
        List<BlockMismatch> bestMismatches = null;
        int bestMismatchCount = Integer.MAX_VALUE;

        for (int rot = 0; rot < 4; rot++) {
            int[] rotatedAnchor = rotate(caX, caZ, rot);
            int originX = ax - rotatedAnchor[0];
            int originY = ay - caY;
            int originZ = az - rotatedAnchor[1];

            List<BlockMismatch> mismatches = new ArrayList<>();
            if (checkStructure(definition, world, originX, originY, originZ, rot, mismatches)) {
                return Result.success(rot, new Location(world, originX, originY, originZ));
            }

            if (mismatches.size() < bestMismatchCount) {
                bestMismatchCount = mismatches.size();
                bestRotation = rot;
                bestOrigin = new Location(world, originX, originY, originZ);
                bestMismatches = mismatches;
            }
        }

        List<BlockMismatch> report;
        if (bestMismatches != null && bestMismatches.size() > 16) {
            report = bestMismatches.subList(0, 16);
        } else {
            report = bestMismatches != null ? bestMismatches : List.of();
        }

        WWSFPlugin.getInstance().getLogger().info("[Maxim] validation failed bestRot=" + bestRotation
            + " origin=" + bestOrigin
            + " mismatches=" + report.size() + "/" + (bestMismatches != null ? bestMismatches.size() : 0));
        for (BlockMismatch m : report) {
            WWSFPlugin.getInstance().getLogger().info("[Maxim] " + m.describe());
        }

        return Result.failure(bestRotation, bestOrigin, report);
    }

    private static boolean checkStructure(
        MaximMachineGunDefinition def,
        World world,
        int ox, int oy, int oz,
        int rotation,
        List<BlockMismatch> mismatches
    ) {
        boolean allMatch = true;
        int sizeX = def.getSizeX();
        int sizeY = def.getSizeY();
        int sizeZ = def.getSizeZ();

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    Material expected = def.getMaterialAt(x, y, z);
                    if (x == def.getTriggerX() && y == def.getTriggerY() && z == def.getTriggerZ()) {
                        continue;
                    }
                    if (expected == Material.AIR) {
                        continue;
                    }

                    int[] wp = toWorld(x, z, rotation);
                    int wx = ox + wp[0];
                    int wy = oy + y;
                    int wz = oz + wp[1];

                    Material found = world.getBlockAt(wx, wy, wz).getType();
                    if (found != expected) {
                        allMatch = false;
                        mismatches.add(new BlockMismatch(wx, wy, wz, expected, found));
                        if (mismatches.size() >= 24) {
                            return false;
                        }
                    }
                }
            }
        }

        return allMatch;
    }

    static int[] rotate(int cx, int cz, int rotation) {
        return switch (rotation) {
            case 0 -> new int[]{cx, cz};
            case 1 -> new int[]{cz, -cx};
            case 2 -> new int[]{-cx, -cz};
            case 3 -> new int[]{-cz, cx};
            default -> new int[]{cx, cz};
        };
    }

    private static int[] toWorld(int cx, int cz, int rotation) {
        return rotate(cx, cz, rotation);
    }
}
