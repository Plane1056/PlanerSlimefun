package com.wwsf.multiblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.block.Block;

public final class MultiblockPattern {

    private final Map<BlockOffset, Material> pattern;
    private final String frameName;

    public MultiblockPattern(@Nonnull String frameName, @Nonnull Map<BlockOffset, Material> pattern) {
        this.frameName = frameName;
        this.pattern = Map.copyOf(pattern);
    }

    @Nonnull
    public String getFrameName() {
        return frameName;
    }

    public boolean isValid(@Nonnull Block core) {
        for (var entry : pattern.entrySet()) {
            BlockOffset offset = entry.getKey();
            Block relative = core.getRelative(offset.x(), offset.y(), offset.z());
            if (relative.getType() != entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public void placeMultiblock(@Nonnull Block core) {
        for (var entry : pattern.entrySet()) {
            BlockOffset offset = entry.getKey();
            Block relative = core.getRelative(offset.x(), offset.y(), offset.z());
            relative.setType(entry.getValue());
        }
    }

    public void breakMultiblock(@Nonnull Block core) {
        for (var entry : pattern.entrySet()) {
            BlockOffset offset = entry.getKey();
            Block relative = core.getRelative(offset.x(), offset.y(), offset.z());
            relative.setType(Material.AIR);
        }
    }

    @Nonnull
    public static List<BlockOffset> getAdjacentOffsets() {
        List<BlockOffset> offsets = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        offsets.add(new BlockOffset(x, y, z));
                    }
                }
            }
        }
        return offsets;
    }

    @Nonnull
    public static MultiblockPattern capAndRing(
        @Nonnull String name,
        @Nonnull Material cap,
        @Nonnull Material side,
        @Nonnull Material corner
    ) {
        Map<BlockOffset, Material> map = new HashMap<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                map.put(new BlockOffset(x, 1, z), cap);
            }
        }

        map.put(new BlockOffset(0, 0, -1), side);
        map.put(new BlockOffset(0, 0, 1), side);
        map.put(new BlockOffset(-1, 0, 0), side);
        map.put(new BlockOffset(1, 0, 0), side);
        map.put(new BlockOffset(-1, 0, -1), corner);
        map.put(new BlockOffset(1, 0, -1), corner);
        map.put(new BlockOffset(-1, 0, 1), corner);
        map.put(new BlockOffset(1, 0, 1), corner);

        return new MultiblockPattern(name, map);
    }

    @Nonnull
    public static MultiblockPattern flakNest() {
        return capAndRing("Flak Nest", Material.IRON_BARS, Material.IRON_BLOCK, Material.CHAIN);
    }

    @Nonnull
    public static MultiblockPattern bunkerRing() {
        return capAndRing("Bunker Ring", Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_TILES, Material.COBBLESTONE_WALL);
    }

    @Nonnull
    public static MultiblockPattern siegePlatform() {
        return capAndRing("Siege Platform", Material.BRICKS, Material.STONE_BRICKS, Material.IRON_BLOCK);
    }

    @Nonnull
    public static MultiblockPattern coastalFort() {
        return capAndRing("Coastal Fort", Material.PRISMARINE_BRICKS, Material.DEEPSLATE_BRICKS, Material.IRON_BLOCK);
    }

    @Nonnull
    public static MultiblockPattern navalPlatform() {
        return capAndRing("Naval Platform", Material.PRISMARINE, Material.PRISMARINE_BRICKS, Material.DARK_PRISMARINE);
    }

    @Nonnull
    public static MultiblockPattern railroadMount() {
        Map<BlockOffset, Material> map = new HashMap<>();

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                map.put(new BlockOffset(x, 1, z), Material.DEEPSLATE_BRICKS);
            }
        }

        map.put(new BlockOffset(0, 0, -1), Material.IRON_BLOCK);
        map.put(new BlockOffset(0, 0, 1), Material.IRON_BLOCK);
        map.put(new BlockOffset(-1, 0, 0), Material.IRON_BLOCK);
        map.put(new BlockOffset(1, 0, 0), Material.IRON_BLOCK);
        map.put(new BlockOffset(-1, 0, -1), Material.NETHERITE_BLOCK);
        map.put(new BlockOffset(1, 0, -1), Material.NETHERITE_BLOCK);
        map.put(new BlockOffset(-1, 0, 1), Material.NETHERITE_BLOCK);
        map.put(new BlockOffset(1, 0, 1), Material.NETHERITE_BLOCK);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                map.put(new BlockOffset(x, 2, z), Material.IRON_BLOCK);
            }
        }

        map.put(new BlockOffset(-2, 0, 0), Material.IRON_BLOCK);
        map.put(new BlockOffset(2, 0, 0), Material.IRON_BLOCK);
        map.put(new BlockOffset(0, 0, -2), Material.IRON_BLOCK);
        map.put(new BlockOffset(0, 0, 2), Material.IRON_BLOCK);

        return new MultiblockPattern("Railroad Gun Mount", map);
    }

    public record BlockOffset(int x, int y, int z) {
    }
}