package com.oildrills;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Field drill rig. Origin {@code (0,0,0)} is the oil drill (replaces the piston).
 * <p>
 * Main row (left → right): air, drill, tier stone, air, anvil — along {@code +right}.
 * Layer 1-4 stack upward on that row. Hopper {@code behind}, chest on {@code +right} of drill.
 */
final class DrillStructure {

    static final String CORE_KEY = "oil_drill_core";

    private record Offset(int right, int up, int behind) {
    }

    private enum StairSide {
        NONE,
        /** Lower stair on the row (faces toward -right). */
        LEFT,
        /** Upper stair on the row (faces toward +right). */
        RIGHT
    }

    private record Part(Offset offset, Material material, boolean tierBlock, StairSide stairSide) {
        Part(Offset offset, Material material, boolean tierBlock) {
            this(offset, material, tierBlock, StairSide.NONE);
        }
    }

    /**
     * Layer 1 (y=0): Air | Drill | StoneBricks | Air | Grindstone — plus hopper behind, chest on +right.
     * Layer 2 (y=1): NetherBrickWall | air | CobblestoneWall | air | IronBars
     * Layer 3 (y=2): NetherBricks | StoneBricks | StoneBricks | air | IronBars
     * Layer 4 (y=3): air | air | NetherBrickStairs | NetherBricks | NetherBrickStairs
     * Layer 5 (y=4): air | air | air | air | air
     */
    private static final List<Part> PARTS = List.of(
        // Layer 1 (y=0): main row (x: -1,0,1,2,3 = 5 wide) behind z=1 (hopper)
        new Part(new Offset(1, 0, 0), null, true),
        new Part(new Offset(2, 0, 0), Material.CHEST, false),
        new Part(new Offset(3, 0, 0), Material.GRINDSTONE, false),
        new Part(new Offset(0, 0, 1), Material.HOPPER, false),
        // Layer 2 (y=1)
        new Part(new Offset(-1, 1, 0), Material.NETHER_BRICK_WALL, false),
        new Part(new Offset(1, 1, 0), Material.COBBLESTONE_WALL, false),
        new Part(new Offset(3, 1, 0), Material.IRON_BARS, false),
        // Layer 3 (y=2)
        new Part(new Offset(-1, 2, 0), Material.NETHER_BRICKS, false),
        new Part(new Offset(0, 2, 0), null, true),
        new Part(new Offset(1, 2, 0), null, true),
        new Part(new Offset(3, 2, 0), Material.IRON_BARS, false),
        // Layer 4 (y=3)
        new Part(new Offset(1, 3, 0), Material.NETHER_BRICK_STAIRS, false, StairSide.LEFT),
        new Part(new Offset(2, 3, 0), Material.NETHER_BRICKS, false),
        new Part(new Offset(3, 3, 0), Material.NETHER_BRICK_STAIRS, false, StairSide.RIGHT)
    );

    private DrillStructure() {
    }

    static Material tierMaterial(OilDrillTier tier) {
        return switch (tier) {
            case MARK_I -> Material.STONE_BRICKS;
            case MARK_II -> Material.DEEPSLATE_BRICKS;
            case MARK_III -> Material.RED_NETHER_BRICKS;
        };
    }

    static BlockFace horizontalFacing(Player player) {
        return horizontalFacing(player.getLocation().getYaw());
    }

    static BlockFace horizontalFacing(float yaw) {
        int index = Math.round(yaw / 90f) & 0x3;
        return switch (index) {
            case 0 -> BlockFace.SOUTH;
            case 1 -> BlockFace.WEST;
            case 2 -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    static BlockFace clockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    static BlockFace counterclockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    static BlockFace opposite(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.NORTH;
            case EAST -> BlockFace.WEST;
            case WEST -> BlockFace.EAST;
            case UP -> BlockFace.DOWN;
            case DOWN -> BlockFace.UP;
            default -> face;
        };
    }

    static BlockFace getDrillFacing(Block drill) {
        if (drill.getBlockData() instanceof Directional directional) {
            BlockFace face = directional.getFacing();
            if (face != BlockFace.UP && face != BlockFace.DOWN) {
                return face;
            }
        }
        String stored = BlockStorage.getLocationInfo(drill.getLocation(), "oil_drill_facing");
        if (stored != null) {
            try {
                BlockFace face = BlockFace.valueOf(stored);
                if (face != BlockFace.UP && face != BlockFace.DOWN) {
                    return face;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return BlockFace.NORTH;
    }

    /**
     * Recovers the direction of an older rig whose Slimefun metadata was lost.
     * The authored structure is asymmetric, so the orientation with the most
     * matching physical parts is the original direction.
     */
    static BlockFace inferFacing(Block drill, OilDrillTier tier) {
        BlockFace bestFacing = BlockFace.NORTH;
        int bestScore = -1;
        for (BlockFace candidate : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
            int score = 0;
            for (Part part : PARTS) {
                Material expected = resolveMaterial(part, tier);
                if (matches(blockAt(drill, candidate, part.offset()), expected, part.tierBlock())) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestFacing = candidate;
            }
        }
        return bestFacing;
    }

    static void storeFacing(Block drill, BlockFace facing) {
        BlockStorage.addBlockInfo(drill, "oil_drill_facing", facing.name());
    }

    static void faceDrillUp(Block drill) {
        BlockData data = drill.getBlockData();
        if (data instanceof Directional directional) {
            directional.setFacing(BlockFace.UP);
            // Drill pistons are animated cosmetically. Physics here can turn
            // startup restoration into a real moving-piston transition.
            drill.setBlockData(directional, false);
        }
    }

    static boolean canBuild(Block drill, OilDrillTier tier, BlockFace facing) {
        for (Part part : PARTS) {
            Material expected = resolveMaterial(part, tier);
            Block block = blockAt(drill, facing, part.offset());
            if (!canPlace(block, expected, part.tierBlock())) {
                return false;
            }
        }
        return true;
    }

    static void build(Block drill, OilDrillTier tier, BlockFace facing) {
        Location core = drill.getLocation();
        tagCore(drill, core);
        for (Part part : PARTS) {
            Material expected = resolveMaterial(part, tier);
            Block block = blockAt(drill, facing, part.offset());
            Material current = block.getType();
            // Only place if air, non-solid, or already matches the expected block
            if (current.isAir() || !current.isSolid() || matches(block, expected, part.tierBlock())) {
                block.setType(expected, false);
                applyBlockData(block, part, facing);
                tagCore(block, core);
            }
        }
    }

    static void tagCore(Block block, Location core) {
        BlockStorage.addBlockInfo(block, CORE_KEY, locationKey(core));
    }

    static Location resolveCore(Block block) {
        String key = BlockStorage.getLocationInfo(block.getLocation(), CORE_KEY);
        if (key != null) {
            Location parsed = parseLocationKey(key);
            if (parsed != null) {
                return parsed;
            }
        }
        if (BlockStorage.check(block) instanceof OilDrillItem) {
            return block.getLocation();
        }
        return null;
    }

    static String locationKey(Location loc) {
        return loc.getWorld().getName() + ';' + loc.getBlockX() + ';' + loc.getBlockY() + ';' + loc.getBlockZ();
    }

    private static Location parseLocationKey(String key) {
        String[] parts = key.split(";", 4);
        if (parts.length != 4) {
            return null;
        }
        World world = org.bukkit.Bukkit.getWorld(parts[0]);
        if (world == null) {
            return null;
        }
        try {
            return new Location(
                world,
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3])
            );
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static boolean isComplete(Block drill, OilDrillTier tier) {
        return isComplete(drill, tier, getDrillFacing(drill));
    }

    static boolean isComplete(Block drill, OilDrillTier tier, BlockFace facing) {
        for (Part part : PARTS) {
            Material expected = resolveMaterial(part, tier);
            if (!matches(blockAt(drill, facing, part.offset()), expected, part.tierBlock())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Repairs the exact frame damage caused by the old Mark II animation. The
     * real sticky-piston core used to retract the tier block at y=2 into the
     * animation gap at y=1, after which normal validation paused the drill.
     */
    static boolean repairLegacyStickyPistonDamage(Block drill, OilDrillTier tier, BlockFace facing) {
        Offset towerOffset = new Offset(0, 2, 0);
        Block pulled = blockAt(drill, facing, new Offset(0, 1, 0));
        Block tower = blockAt(drill, facing, towerOffset);
        if (!isLegacyStickyPistonPullDamage(tier, pulled.getType(), tower.getType())) {
            return false;
        }

        for (Part part : PARTS) {
            if (part.offset().equals(towerOffset)) {
                continue;
            }
            Material expected = resolveMaterial(part, tier);
            if (!matches(blockAt(drill, facing, part.offset()), expected, part.tierBlock())) {
                return false;
            }
        }

        BlockStorage.clearBlockInfo(pulled);
        pulled.setType(Material.AIR, false);
        tower.setType(tierMaterial(tier), false);
        tagCore(tower, drill.getLocation());
        return true;
    }

    static boolean isLegacyStickyPistonPullDamage(OilDrillTier tier, Material pulled, Material tower) {
        return tier == OilDrillTier.MARK_II
            && pulled == tierMaterial(tier)
            && (tower == Material.AIR || tower == Material.CAVE_AIR || tower == Material.VOID_AIR);
    }

    /** Returns false without reading block data if any rig part is unloaded. */
    static boolean areStructureChunksLoaded(Block drill, BlockFace facing) {
        World world = drill.getWorld();
        if (!world.isChunkLoaded(drill.getX() >> 4, drill.getZ() >> 4)) {
            return false;
        }
        for (Part part : PARTS) {
            Block block = blockAt(drill, facing, part.offset());
            if (!world.isChunkLoaded(block.getX() >> 4, block.getZ() >> 4)) {
                return false;
            }
        }
        return true;
    }

    static Hopper findCoalHopper(Block drill) {
        Block block = blockAt(drill, getDrillFacing(drill), new Offset(0, 0, 1));
        if (block.getState() instanceof Hopper hopper) {
            return hopper;
        }
        return null;
    }

    static org.bukkit.inventory.Inventory findOutputInventory(Block drill) {
        Block block = blockAt(drill, getDrillFacing(drill), new Offset(2, 0, 0));
        if (block.getState() instanceof Chest chest) {
            return chest.getInventory();
        }
        if (block.getState() instanceof org.bukkit.block.Barrel barrel) {
            return barrel.getInventory();
        }
        return null;
    }

    private static Material resolveMaterial(Part part, OilDrillTier tier) {
        if (part.tierBlock()) {
            return tierMaterial(tier);
        }
        return part.material();
    }

    private static Block blockAt(Block drill, BlockFace forward, Offset offset) {
        BlockFace right = clockwise(forward);
        BlockFace behind = opposite(forward);
        int dx = faceModX(right) * offset.right() + faceModX(behind) * offset.behind();
        int dz = faceModZ(right) * offset.right() + faceModZ(behind) * offset.behind();
        return drill.getRelative(dx, offset.up(), dz);
    }

    private static int faceModX(BlockFace face) {
        return switch (face) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    private static int faceModZ(BlockFace face) {
        return switch (face) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
        };
    }

    private static void applyBlockData(Block block, Part part, BlockFace forward) {
        if (part.stairSide() == StairSide.NONE) {
            return;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Stairs stairs)) {
            return;
        }
        BlockFace stairFacing = part.stairSide() == StairSide.LEFT
            ? counterclockwise(forward)
            : clockwise(forward);
        stairs.setFacing(stairFacing);
        stairs.setHalf(org.bukkit.block.data.Bisected.Half.BOTTOM);
        block.setBlockData(stairs);
    }

    private static boolean canPlace(Block block, Material expected, boolean tierBlock) {
        Material type = block.getType();
        // Only allow air, non-solid blocks (grass, torches, fluids, etc.), or existing matching blocks
        if (type.isAir() || !type.isSolid()) {
            return true;
        }
        return matches(block, expected, tierBlock);
    }

    private static boolean matches(Block block, Material expected, boolean tierBlock) {
        Material type = block.getType();
        return type == expected;
    }

    static List<Block> allStructureBlocks(Block drill) {
        return allStructureBlocks(drill, getDrillFacing(drill));
    }

    static List<Block> allStructureBlocks(Block drill, BlockFace facing) {
        List<Block> blocks = new ArrayList<>();
        for (Part part : PARTS) {
            blocks.add(blockAt(drill, facing, part.offset()));
        }
        return blocks;
    }

    static List<ItemStack> collectInventoryDrops(Block drill) {
        List<ItemStack> drops = new ArrayList<>();
        for (Block block : allStructureBlocks(drill)) {
            if (block.getState() instanceof InventoryHolder holder) {
                Inventory inventory = holder.getInventory();
                for (ItemStack stack : inventory.getContents()) {
                    if (stack != null && !stack.getType().isAir()) {
                        drops.add(stack.clone());
                    }
                }
                inventory.clear();
            }
        }
        return drops;
    }

    static void dismantle(Block drill, Location skipBreak, OilDrillTier tier) {
        dismantle(drill, skipBreak, tier, List.of(), getDrillFacing(drill));
    }

    static void dismantle(Block drill, Location skipBreak, OilDrillTier tier, List<ItemStack> inventoryDrops) {
        dismantle(drill, skipBreak, tier, inventoryDrops, getDrillFacing(drill));
    }

    static void dismantle(Block drill, Location skipBreak, OilDrillTier tier, List<ItemStack> inventoryDrops, BlockFace facing) {
        dismantle(drill, skipBreak, tier, inventoryDrops, facing, null);
    }

    static void dismantle(Block drill, Location skipBreak, OilDrillTier tier, List<ItemStack> inventoryDrops, BlockFace facing, Player player) {
        DrillPistonAnimator.retract(drill);
        Block above = drill.getRelative(BlockFace.UP);
        if (above.getType() == Material.PISTON_HEAD) {
            above.setType(Material.AIR, false);
        }

        for (Block block : allStructureBlocks(drill, facing)) {
            BlockStorage.clearBlockInfo(block);
            block.setType(Material.AIR, false);
        }

        Location dropAt = drill.getLocation().clone().add(0.5, 0.5, 0.5);
        if (tier != null && player != null) {
            player.getInventory().addItem(tier.createDrillItem()).forEach((k, v) -> 
                drill.getWorld().dropItemNaturally(dropAt, v));
        } else if (tier != null) {
            drill.getWorld().dropItemNaturally(dropAt, tier.createDrillItem());
        }
        for (ItemStack stack : inventoryDrops) {
            drill.getWorld().dropItemNaturally(dropAt, stack);
        }

        BlockStorage.clearBlockInfo(drill);
        drill.setType(Material.AIR, false);
    }
    
    /**
     * Dismantle a drill when the drill block is already broken (air).
     * Uses the stored facing direction to locate and remove structure blocks.
     */
    static void dismantleFromAir(Location drillLoc, OilDrillTier tier, BlockFace facing, List<ItemStack> inventoryDrops, Player player) {
        World world = drillLoc.getWorld();
        if (world == null) {
            return;
        }
        
        Block drill = drillLoc.getBlock();
        
        // Remove all structure blocks using the stored facing
        for (Block block : allStructureBlocks(drill, facing)) {
            BlockStorage.clearBlockInfo(block);
            block.setType(Material.AIR, false);
        }
        
        // Drop items
        Location dropAt = drillLoc.clone().add(0.5, 0.5, 0.5);
        if (tier != null && player != null) {
            player.getInventory().addItem(tier.createDrillItem()).forEach((k, v) -> 
                world.dropItemNaturally(dropAt, v));
        } else if (tier != null) {
            world.dropItemNaturally(dropAt, tier.createDrillItem());
        }
        for (ItemStack stack : inventoryDrops) {
            world.dropItemNaturally(dropAt, stack);
        }
        
        // Clear block info at drill location
        BlockStorage.clearBlockInfo(drill);
    }
}
