package com.oildrills;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Barrel;
import org.bukkit.block.Chest;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import io.github.thebusybiscuit.slimefun4.api.events.SlimefunBlockPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

public class DrillManager implements Listener {

    private final org.bukkit.plugin.java.JavaPlugin plugin;
    private final ChunkOilStorage storage;
    private final DrillPersistence persistence;
    private final DrillGui drillGui;
    private final Map<Location, ActiveDrillData> activeDrills = new HashMap<>();
    private final Map<Location, Boolean> pistonExtended = new HashMap<>();
    private final Set<Location> pendingAutobuild = new HashSet<>();
    private final Set<String> dismantlingRigs = new HashSet<>();
    private final Set<ScannedChunk> recoveryScannedChunks = new HashSet<>();
    private BukkitTask tickTask;
    private BukkitTask animTask;
    private int saveTicker;
    private boolean stopped;

    public DrillManager(org.bukkit.plugin.java.JavaPlugin plugin, ChunkOilStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.persistence = new DrillPersistence(plugin);
        this.drillGui = new DrillGui(this);
    }

    public org.bukkit.plugin.java.JavaPlugin getPlugin() {
        return plugin;
    }

    public void start() {
        stopped = false;
        activeDrills.putAll(persistence.load());
        int restored = restorePersistedDrills();
        plugin.getLogger().info("Loaded " + activeDrills.size() + " active oil drills (restored " + restored + ").");

        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getPluginManager().registerEvents(drillGui, plugin);

        int recovered = recoverLoadedRigs();
        if (recovered > 0) {
            plugin.getLogger().info("Recovered " + recovered + " unregistered oil drill rigs from loaded chunks.");
        }

        int interval = plugin.getConfig().getInt("drill-check-interval-ticks", 20);
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickDrills, interval, interval);

        long animInterval = plugin.getConfig().getLong("drill-piston-anim-ticks", 15L);
        animTask = Bukkit.getScheduler().runTaskTimer(plugin, this::animatePistons, animInterval, animInterval);
    }

    public void shutdown() {
        if (stopped) {
            return;
        }
        stopped = true;
        if (tickTask != null) {
            tickTask.cancel();
        }
        if (animTask != null) {
            animTask.cancel();
        }
        for (Location loc : activeDrills.keySet()) {
            if (isLocationChunkLoaded(loc)) {
                DrillPistonAnimator.retract(loc.getBlock());
            }
        }
        persistence.save(activeDrills);
        activeDrills.clear();
        pistonExtended.clear();
        recoveryScannedChunks.clear();
    }

    ActiveDrillData getDrill(Location loc) {
        return activeDrills.get(loc);
    }

    void saveDrills() {
        persistence.save(activeDrills);
    }

    void onDrillPowerChanged(Location loc, boolean enabled) {
        if (!enabled) {
            pistonExtended.remove(loc);
            if (isLocationChunkLoaded(loc)) {
                DrillPistonAnimator.retract(loc.getBlock());
            }
        }
    }

    public void openControlPanel(Player player, Location loc) {
        ensureRegistered(loc);
        ActiveDrillData data = activeDrills.get(loc);
        if (data == null) {
            player.sendMessage("§cThis is not an active oil drill.");
            return;
        }
        drillGui.open(player, loc, data);
    }

    public void handleDrillBreak(Player player, Location loc) {
        Block block = loc.getBlock();
        if (block.getType() == Material.AIR) {
            return;
        }

        Location drillCore = findDrillCoreForBlock(block);
        if (drillCore == null || !drillCore.equals(loc)) {
            return;
        }

        dismantleDrillRig(drillCore, player);
    }

    private void dismantleDrillRig(Location drillLoc, Player player) {
        String rigKey = DrillStructure.locationKey(drillLoc);

        if (!dismantlingRigs.add(rigKey)) {
            plugin.getLogger().warning("Duplicate dismantle attempt for rig at " + rigKey);
            return;
        }

        plugin.getLogger().info("Dismantling drill rig at " + rigKey + " broken by " + (player != null ? player.getName() : "console"));

        if (pendingAutobuild.contains(drillLoc)) {
            final String storedTier = BlockStorage.getLocationInfo(drillLoc, "oil_drill_tier");
            Bukkit.getScheduler().runTask(plugin, () -> {
                OilDrillTier tier = null;
                try {
                    tier = OilDrillTier.valueOf(storedTier);
                } catch (IllegalArgumentException | NullPointerException ignored) {
                }
                if (tier != null) {
                    Location dropAt = drillLoc.clone().add(0.5, 0.5, 0.5);
                    drillLoc.getWorld().dropItemNaturally(dropAt, tier.createDrillItem());
                }
                Block drillBlock = drillLoc.getBlock();
                BlockStorage.clearBlockInfo(drillBlock);
                dismantlingRigs.remove(rigKey);
            });
            pendingAutobuild.remove(drillLoc);
            return;
        }

        OilDrillTier tier = resolveTier(drillLoc);
        BlockFace facing = DrillStructure.getDrillFacing(drillLoc.getBlock());
        List<ItemStack> inventoryDrops = DrillStructure.collectInventoryDrops(drillLoc.getBlock());

        plugin.getLogger().info("Dismantling tier " + tier + " drill with facing " + facing);

        unregisterDrill(drillLoc);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                DrillStructure.dismantleFromAir(drillLoc, tier, facing, inventoryDrops, player);
                plugin.getLogger().info("Successfully dismantled drill rig at " + rigKey);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error dismantling drill at " + rigKey, e);
            } finally {
                dismantlingRigs.remove(rigKey);
            }
        });
    }

    private void ensureRegistered(Location loc) {
        if (activeDrills.containsKey(loc)) {
            return;
        }
        SlimefunItem item = BlockStorage.check(loc.getBlock());
        OilDrillTier tier = OilDrillTier.fromSlimefunItem(item);
        if (tier != null) {
            registerDrill(loc, tier, DrillStructure.getDrillFacing(loc.getBlock()));
        }
    }

@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrillPlaceCheck(BlockPlaceEvent event) {
        OilDrillItem drillItem = getPlacedDrillItem(event);
        if (drillItem == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("oildrills.drill.place")) {
            event.setCancelled(true);
            player.sendMessage("§cYou cannot place oil drills.");
            return;
        }

        Block drill = event.getBlockPlaced();
        DrillStructure.faceDrillUp(drill);
        if (!canPlaceOnGround(drill)) {
            event.setCancelled(true);
            player.sendMessage("§cOil drills must be placed on solid ground.");
            return;
        }

        if (!isFarEnoughFromOtherDrills(drill)) {
            event.setCancelled(true);
            player.sendMessage("§cDrills must be at least 6 blocks away from each other.");
            return;
        }

        BlockFace facing = DrillStructure.horizontalFacing(player);
        if (!DrillStructure.canBuild(drill, drillItem.getTier(), facing)) {
            event.setCancelled(true);
            player.sendMessage("§cBlocks are in the way! The drill rig needs §e2x4x4 §cblocks of clear space above and around the drill.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrillPlace(BlockPlaceEvent event) {
        OilDrillItem drillItem = getPlacedDrillItem(event);
        if (drillItem == null) {
            return;
        }
        scheduleAutobuild(event.getPlayer(), event.getBlockPlaced(), drillItem.getTier());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSlimefunPlaceCheck(SlimefunBlockPlaceEvent event) {
        if (!(event.getSlimefunItem() instanceof OilDrillItem drillItem)) {
            return;
        }
        Block drill = event.getBlockPlaced();
        if (!isFarEnoughFromOtherDrills(drill)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cDrills must be at least 6 blocks away from each other.");
        } else if (!DrillStructure.canBuild(drill, drillItem.getTier(), DrillStructure.horizontalFacing(event.getPlayer()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cBlocks are in the way! The drill rig needs §e2x4x4 §cblocks of clear space.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSlimefunPlace(SlimefunBlockPlaceEvent event) {
        if (!(event.getSlimefunItem() instanceof OilDrillItem drillItem)) {
            return;
        }
        scheduleAutobuild(event.getPlayer(), event.getBlockPlaced(), drillItem.getTier());
    }

    private OilDrillItem getPlacedDrillItem(BlockPlaceEvent event) {
        ItemStack item = event.getHand() == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getItemInHand();
        if (item == null || item.getType().isAir()) {
            return null;
        }
        SlimefunItem sf = SlimefunItem.getByItem(item);
        if (sf instanceof OilDrillItem drill) {
            return drill;
        }
        return null;
    }

    private void scheduleAutobuild(Player player, Block drill, OilDrillTier tier) {
        Location loc = drill.getLocation();
        if (!pendingAutobuild.add(loc)) {
            return;
        }

        BlockFace facing = DrillStructure.horizontalFacing(player);

        // Tag core immediately so breaking before autobuild works
        DrillStructure.tagCore(drill, loc);
        DrillStructure.storeFacing(drill, facing);
        BlockStorage.addBlockInfo(drill, "oil_drill_tier", tier.name());

        Bukkit.getScheduler().runTask(plugin, () -> {
            pendingAutobuild.remove(loc);
            Block placed = loc.getBlock();
            if (!isDrillBlock(placed, tier)) {
                clearPendingTags(placed);
                return;
            }
            DrillStructure.faceDrillUp(placed);
            DrillStructure.build(placed, tier, facing);
            if (!activeDrills.containsKey(loc)) {
                registerDrill(loc, tier, facing);
            }
            sendSetupMessage(player, tier, placed);
        });
    }

    private void clearPendingTags(Block drill) {
        BlockStorage.clearBlockInfo(drill);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRigBreakCancelDrops(BlockBreakEvent event) {
        Block broken = event.getBlock();

        // Check if this block is part of any active drill's structure.
        Location drillCore = findDrillCoreForBlock(broken);
        
        // If breaking any part of the rig (core or structure), cancel vanilla drops
        if (drillCore != null) {
            event.setDropItems(false);
            
            // If breaking a structure block (not the core), prevent the break entirely
            if (!broken.getLocation().equals(drillCore)) {
                plugin.getLogger().warning("Cancelling break of structure block at " + broken.getLocation());
                event.setCancelled(true);
                Player player = event.getPlayer();
                player.sendMessage("§cYou cannot break individual rig blocks. Break the drill block to dismantle the entire rig.");
                return;
            }
            
            // Breaking the drill core - trigger teardown HERE before the block is actually broken
            Player player = event.getPlayer();
            dismantleDrillRig(drillCore, player);
        }
    }

    /**
     * No rig part sits further than this from its core on any axis. Blocks
     * beyond it cannot belong to that rig, so the expensive structure walk is
     * skipped. The widest authored offset is 3 blocks plus one for slack.
     */
    private static final int RIG_REACH_BLOCKS = 4;

    private Location findDrillCoreForBlock(Block broken) {
        // Fast path: a built rig stores its core key on every structure block,
        // so this resolves without touching the drill collections at all.
        Location core = DrillStructure.resolveCore(broken);
        if (core != null) {
            return core;
        }

        // Fallback for rigs whose stored key is missing. This runs for every
        // ordinary block break in the world, so each candidate is rejected by a
        // world and bounding-box test before its blocks are enumerated.
        for (Location loc : activeDrills.keySet()) {
            if (!withinRigReach(loc, broken)) {
                continue;
            }
            if (!isLocationChunkLoaded(loc)) {
                continue;
            }
            Block drill = loc.getBlock();
            BlockFace facing = DrillStructure.getDrillFacing(drill);
            for (Block block : DrillStructure.allStructureBlocks(drill, facing)) {
                if (block.equals(broken)) {
                    return loc;
                }
            }
        }

        // Check for drills pending autobuild
        for (Location loc : pendingAutobuild) {
            if (!withinRigReach(loc, broken)) {
                continue;
            }
            if (!isLocationChunkLoaded(loc)) {
                continue;
            }
            Block drill = loc.getBlock();
            if (BlockStorage.check(drill) instanceof OilDrillItem) {
                // Check if broken block is part of this pending drill's structure
                BlockFace facing = DrillStructure.getDrillFacing(drill);
                for (Block block : DrillStructure.allStructureBlocks(drill, facing)) {
                    if (block.equals(broken)) {
                        return loc;
                    }
                }
            }
        }

        return null;
    }

    private static boolean withinRigReach(Location core, Block broken) {
        return core.getWorld() != null
                && core.getWorld().equals(broken.getWorld())
                && Math.abs(core.getBlockX() - broken.getX()) <= RIG_REACH_BLOCKS
                && Math.abs(core.getBlockY() - broken.getY()) <= RIG_REACH_BLOCKS
                && Math.abs(core.getBlockZ() - broken.getZ()) <= RIG_REACH_BLOCKS;
    }

    private Location resolveBrokenDrillCore(Block broken) {
        Location core = DrillStructure.resolveCore(broken);
        if (core != null) {
            return core;
        }
        if (broken.getType() == Material.PISTON_HEAD) {
            return DrillStructure.resolveCore(broken.getRelative(BlockFace.DOWN));
        }
        if (BlockStorage.check(broken) instanceof OilDrillItem) {
            return broken.getLocation();
        }
        for (Location loc : activeDrills.keySet()) {
            if (!withinRigReach(loc, broken) || !isLocationChunkLoaded(loc)) {
                continue;
            }
            Block drill = loc.getBlock();
            BlockFace facing = DrillStructure.getDrillFacing(drill);
            for (Block block : DrillStructure.allStructureBlocks(drill, facing)) {
                if (block.equals(broken)) {
                    return loc;
                }
            }
        }
        // Check for drills pending autobuild
        for (Location loc : pendingAutobuild) {
            if (!withinRigReach(loc, broken) || !isLocationChunkLoaded(loc)) {
                continue;
            }
            Block drill = loc.getBlock();
            if (BlockStorage.check(drill) instanceof OilDrillItem) {
                return drill.getLocation();
            }
        }
        return null;
    }

    private OilDrillTier resolveTier(Location drillLoc) {
        ActiveDrillData data = activeDrills.get(drillLoc);
        if (data != null) {
            return data.tier();
        }
        String stored = BlockStorage.getLocationInfo(drillLoc, "oil_drill_tier");
        if (stored != null) {
            try {
                return OilDrillTier.valueOf(stored);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return OilDrillTier.fromSlimefunItem(BlockStorage.check(drillLoc.getBlock()));
    }

    private void unregisterDrill(Location drillLoc) {
        activeDrills.remove(drillLoc);
        pistonExtended.remove(drillLoc);
        persistence.save(activeDrills);
    }

    private void registerDrill(Location loc, OilDrillTier tier, BlockFace facing) {
        long delay = tier.rollDelayMs(plugin.getConfig());
        activeDrills.put(loc, new ActiveDrillData(tier, System.currentTimeMillis() + delay, false, facing));
        Block block = loc.getBlock();
        BlockStorage.addBlockInfo(block, "oil_drill_tier", tier.name());
        DrillStructure.storeFacing(block, facing);
        DrillStructure.tagCore(block, loc);
        persistence.save(activeDrills);
    }

    /**
     * Restores vanilla-looking drill cores as Slimefun blocks before the first
     * runtime validation. This prevents valid rigs from being discarded after
     * Slimefun's block cache has not retained their identity across a restart.
     */
    private int restorePersistedDrills() {
        int restored = 0;
        boolean changed = false;
        for (Map.Entry<Location, ActiveDrillData> entry : activeDrills.entrySet()) {
            Location loc = entry.getKey();
            ActiveDrillData data = entry.getValue();
            boolean entryRestored = false;
            if (!isLocationChunkLoaded(loc)) {
                continue;
            }
            Block drill = loc.getBlock();

            BlockFace facing = data.facing();
            if (!isHorizontal(facing)) {
                if (!allFacingCandidateChunksLoaded(drill)) {
                    continue;
                }
                facing = DrillStructure.inferFacing(drill, data.tier());
                data.setFacing(facing);
                changed = true;
            }

            if (drill.getType() != data.tier().getCoreMaterial()) {
                if (!DrillStructure.areStructureChunksLoaded(drill, facing)) {
                    continue;
                }
                if (repairPersistedCore(drill, data.tier(), facing)) {
                    entryRestored = true;
                    changed = true;
                } else {
                    // Never erase the only persistent record merely because a
                    // piston or another plugin exposed a transient block state.
                    // A real player break unregisters the rig through the break
                    // handler before the core disappears.
                    if (data.shouldWarn(30_000L)) {
                        plugin.getLogger().warning(
                            "Saved oil drill at " + DrillStructure.locationKey(loc)
                                + " is waiting for its core: expected "
                                + data.tier().getCoreMaterial() + " but found " + drill.getType()
                        );
                    }
                    continue;
                }
            }

            if (restoreDrillIdentity(drill, data.tier(), facing)) {
                entryRestored = true;
                changed = true;
            }
            if (DrillStructure.repairLegacyStickyPistonDamage(drill, data.tier(), facing)) {
                plugin.getLogger().info(
                    "Repaired legacy sticky-piston damage on oil drill at "
                        + DrillStructure.locationKey(loc)
                );
                entryRestored = true;
                changed = true;
            }
            // An empty animation-state entry means this is startup or the core
            // has just returned from an unload. Normalize exactly then; do not
            // retract every running rig whenever an unrelated chunk loads.
            if (!pistonExtended.containsKey(loc)) {
                DrillPistonAnimator.normalizeAfterLoad(drill);
            }
            if (entryRestored) {
                restored++;
            }
        }

        if (changed) {
            persistence.save(activeDrills);
        }
        return restored;
    }

    private static boolean isHorizontal(BlockFace facing) {
        return facing == BlockFace.NORTH
            || facing == BlockFace.EAST
            || facing == BlockFace.SOUTH
            || facing == BlockFace.WEST;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        Bukkit.getScheduler().runTask(plugin, () -> {
            int restored = restorePersistedDrills();
            int recovered = recoverRigsInChunk(chunk);
            if (restored > 0 || recovered > 0) {
                persistence.save(activeDrills);
                plugin.getLogger().info(
                    "Restored " + restored + " saved and recovered " + recovered
                        + " oil drill rig(s) after chunk " + chunk.getX() + "," + chunk.getZ() + " loaded."
                );
            }
        });
    }

    private int recoverLoadedRigs() {
        int recovered = 0;
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                recovered += recoverRigsInChunk(chunk);
            }
        }
        if (recovered > 0) {
            persistence.save(activeDrills);
        }
        return recovered;
    }

    /**
     * Finds the output chest/barrel authored into a complete rig. This makes
     * old rigs self-healing even when a previous version already lost both the
     * active-drill YAML record and the Slimefun block record.
     */
    private int recoverRigsInChunk(Chunk chunk) {
        if (!recoveryScannedChunks.add(new ScannedChunk(
            chunk.getWorld().getUID(), chunk.getX(), chunk.getZ()
        ))) {
            return 0;
        }

        int recovered = 0;
        for (BlockState state : chunk.getTileEntities(
            block -> block.getType() == Material.CHEST || block.getType() == Material.BARREL,
            false
        )) {
            if (!(state instanceof Chest) && !(state instanceof Barrel)) {
                continue;
            }
            Block output = state.getBlock();
            for (BlockFace facing : List.of(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)) {
                BlockFace right = DrillStructure.clockwise(facing);
                Block core = output.getRelative(DrillStructure.opposite(right), 2);
                if (!core.getWorld().isChunkLoaded(core.getX() >> 4, core.getZ() >> 4)
                    || !DrillStructure.areStructureChunksLoaded(core, facing)) {
                    continue;
                }
                Location loc = core.getLocation();
                if (activeDrills.containsKey(loc)) {
                    continue;
                }
                OilDrillTier tier = OilDrillTier.fromCoreMaterial(core.getType());
                if (tier == null) {
                    tier = recoverMissingCoreFromFrame(core, facing);
                }
                if (tier == null || !DrillStructure.isComplete(core, tier, facing)) {
                    continue;
                }

                long delay = tier.rollDelayMs(plugin.getConfig());
                ActiveDrillData data = new ActiveDrillData(
                    tier,
                    System.currentTimeMillis() + delay,
                    false,
                    facing
                );
                activeDrills.put(loc, data);
                restoreDrillIdentity(core, tier, facing);
                DrillPistonAnimator.normalizeAfterLoad(core);
                recovered++;
            }
        }
        return recovered;
    }

    private boolean restoreDrillIdentity(Block drill, OilDrillTier tier, BlockFace facing) {
        String currentId = BlockStorage.getLocationInfo(drill.getLocation(), "id");
        boolean restored = !tier.getItemId().equals(currentId);
        if (restored) {
            if (currentId != null) {
                BlockStorage.clearBlockInfo(drill);
            }
            BlockStorage.addBlockInfo(drill, "id", tier.getItemId());
        }
        BlockStorage.addBlockInfo(drill, "oil_drill_tier", tier.name());
        DrillStructure.storeFacing(drill, facing);
        DrillStructure.tagCore(drill, drill.getLocation());
        DrillStructure.faceDrillUp(drill);
        return restored;
    }

    private boolean repairPersistedCore(Block drill, OilDrillTier tier, BlockFace facing) {
        if (!isRecoverableCoreMaterial(drill.getType())
            || !DrillStructure.isComplete(drill, tier, facing)) {
            return false;
        }

        Material previous = drill.getType();
        BlockStorage.clearBlockInfo(drill);
        drill.setType(tier.getCoreMaterial(), false);
        DrillStructure.faceDrillUp(drill);
        DrillPistonAnimator.normalizeAfterLoad(drill);
        plugin.getLogger().info(
            "Repaired saved oil drill core at " + DrillStructure.locationKey(drill.getLocation())
                + " from " + previous + " to " + tier.getCoreMaterial()
        );
        return true;
    }

    private OilDrillTier recoverMissingCoreFromFrame(Block drill, BlockFace facing) {
        if (!isRecoverableCoreMaterial(drill.getType())) {
            return null;
        }
        for (OilDrillTier tier : OilDrillTier.values()) {
            if (DrillStructure.isComplete(drill, tier, facing)) {
                Material previous = drill.getType();
                BlockStorage.clearBlockInfo(drill);
                drill.setType(tier.getCoreMaterial(), false);
                DrillStructure.faceDrillUp(drill);
                DrillPistonAnimator.normalizeAfterLoad(drill);
                plugin.getLogger().info(
                    "Recovered oil drill core from its complete frame at "
                        + DrillStructure.locationKey(drill.getLocation()) + " (was " + previous + ")"
                );
                return tier;
            }
        }
        return null;
    }

    static boolean isRecoverableCoreMaterial(Material material) {
        return material == Material.AIR
            || material == Material.CAVE_AIR
            || material == Material.VOID_AIR
            || material == Material.MOVING_PISTON;
    }

    private static boolean isLocationChunkLoaded(Location location) {
        return location.getWorld() != null
            && location.getWorld().isChunkLoaded(
                location.getBlockX() >> 4,
                location.getBlockZ() >> 4
            );
    }

    private static boolean allFacingCandidateChunksLoaded(Block drill) {
        return DrillStructure.areStructureChunksLoaded(drill, BlockFace.NORTH)
            && DrillStructure.areStructureChunksLoaded(drill, BlockFace.EAST)
            && DrillStructure.areStructureChunksLoaded(drill, BlockFace.SOUTH)
            && DrillStructure.areStructureChunksLoaded(drill, BlockFace.WEST);
    }

    private record ScannedChunk(java.util.UUID worldId, int x, int z) {
    }

    private void sendSetupMessage(Player player, OilDrillTier tier, Block drill) {
        var cfg = plugin.getConfig();
        int coal = tier.getCoalPerBarrel(cfg);
        int seconds = tier.getSeconds(cfg);

        player.sendMessage("§a" + tier.getDisplayPrefix() + " §aplaced — §7field rig assembled.");
        player.sendMessage("§7Right-click the drill to open the §econtrol panel§7.");
        player.sendMessage("§7§c" + coal + " coal§7 per oil · §e" + seconds + " seconds§7 when ON.");
    }

    private void animatePistons() {
        for (Map.Entry<Location, ActiveDrillData> entry : activeDrills.entrySet()) {
            Location loc = entry.getKey();
            ActiveDrillData data = entry.getValue();
            if (!isLocationChunkLoaded(loc)) {
                pistonExtended.remove(loc);
                continue;
            }
            Block drill = loc.getBlock();

            if (!data.isEnabled() || !canPump(drill, data)) {
                if (pistonExtended.containsKey(loc)) {
                    DrillPistonAnimator.retract(drill);
                    pistonExtended.remove(loc);
                }
                continue;
            }

            boolean extended = pistonExtended.getOrDefault(loc, false);
            DrillPistonAnimator.setRunning(drill, extended);
            pistonExtended.put(loc, !extended);
        }
    }

    /** Coal, structure, hopper, and chest — required to run or animate. */
    boolean canPump(Block drill, ActiveDrillData data) {
        return evaluateOperation(drill, data, false) == DrillOperationCheck.OK;
    }

    boolean areRigChunksLoaded(Block drill, ActiveDrillData data) {
        BlockFace facing = isHorizontal(data.facing())
            ? data.facing()
            : DrillStructure.getDrillFacing(drill);
        return DrillStructure.areStructureChunksLoaded(drill, facing);
    }

    DrillOperationCheck evaluateForStart(Block drill, ActiveDrillData data) {
        if (!areRigChunksLoaded(drill, data)) {
            return DrillOperationCheck.CHUNKS_UNLOADED;
        }
        repairLegacyStickyPistonDamage(drill, data);
        if (!DrillStructure.isComplete(drill, data.tier())) {
            return DrillOperationCheck.STRUCTURE_INCOMPLETE;
        }
        if (findCoalHopper(drill) == null) {
            return DrillOperationCheck.NO_HOPPER;
        }
        if (findOutputInventory(drill) == null) {
            return DrillOperationCheck.NO_CHEST;
        }
        int fuelNeed = fuelRequiredPerBarrel(data);
        if (countFuelInHopper(drill) < fuelNeed) {
            return DrillOperationCheck.NO_COAL;
        }
        Inventory output = findOutputInventory(drill);
        if (output != null && !canFit(output, OilItems.CRUDE_OIL)) {
            return DrillOperationCheck.OUTPUT_FULL;
        }
        if (storage.getFieldId(drill.getLocation()) == null) {
            return DrillOperationCheck.NO_OIL;
        }
        if (!storage.isSurveyed(drill.getLocation())) {
            return DrillOperationCheck.NOT_SURVEYED;
        }
        if (storage.getSurveyedOrZero(drill.getLocation()) <= 0) {
            return DrillOperationCheck.NO_OIL;
        }
        return DrillOperationCheck.OK;
    }

    DrillOperationCheck evaluateOperation(Block drill, ActiveDrillData data, boolean requireReady) {
        if (!data.isEnabled()) {
            return DrillOperationCheck.DISABLED;
        }
        if (!areRigChunksLoaded(drill, data)) {
            return DrillOperationCheck.CHUNKS_UNLOADED;
        }
        repairLegacyStickyPistonDamage(drill, data);
        if (!DrillStructure.isComplete(drill, data.tier())) {
            return DrillOperationCheck.STRUCTURE_INCOMPLETE;
        }
        if (findCoalHopper(drill) == null) {
            return DrillOperationCheck.NO_HOPPER;
        }
        if (findOutputInventory(drill) == null) {
            return DrillOperationCheck.NO_CHEST;
        }
        int fuelNeed = data.tier().getCoalPerBarrel(plugin.getConfig());
        if (countFuelInHopper(drill) < fuelNeed) {
            return DrillOperationCheck.NO_COAL;
        }
        Inventory output = findOutputInventory(drill);
        if (output != null && !canFit(output, OilItems.CRUDE_OIL)) {
            return DrillOperationCheck.OUTPUT_FULL;
        }
        if (storage.getFieldId(drill.getLocation()) == null) {
            return DrillOperationCheck.NO_OIL;
        }
        if (!storage.isSurveyed(drill.getLocation())) {
            return DrillOperationCheck.NOT_SURVEYED;
        }
        if (storage.getSurveyedOrZero(drill.getLocation()) <= 0) {
            return DrillOperationCheck.NO_OIL;
        }
        if (requireReady && !data.isReady()) {
            return DrillOperationCheck.NOT_READY;
        }
        return DrillOperationCheck.OK;
    }

    private void repairLegacyStickyPistonDamage(Block drill, ActiveDrillData data) {
        BlockFace facing = isHorizontal(data.facing())
            ? data.facing()
            : DrillStructure.getDrillFacing(drill);
        if (DrillStructure.repairLegacyStickyPistonDamage(drill, data.tier(), facing)) {
            plugin.getLogger().info(
                "Repaired legacy sticky-piston damage on oil drill at "
                    + DrillStructure.locationKey(drill.getLocation())
            );
        }
    }

    int countFuelInHopper(Block drill) {
        Hopper hopper = findCoalHopper(drill);
        ActiveDrillData data = activeDrills.get(drill.getLocation());
        int credit = data == null ? 0 : data.fuelCredit();
        return hopper == null ? credit : saturatedAdd(countFuel(hopper.getInventory()), credit);
    }

    int countCoalInHopper(Block drill) {
        return countFuelInHopper(drill);
    }

    int fuelRequiredPerBarrel(ActiveDrillData data) {
        return data.tier().getCoalPerBarrel(plugin.getConfig());
    }

    int coalRequiredPerBarrel(ActiveDrillData data) {
        return fuelRequiredPerBarrel(data);
    }

    private void handleRuntimeFault(Location loc, ActiveDrillData data, Block drill, DrillOperationCheck fault) {
        if (fault == DrillOperationCheck.OK || fault == DrillOperationCheck.NOT_READY) {
            return;
        }
        if (fault == DrillOperationCheck.DISABLED) {
            return;
        }

        // A missing input or temporarily unavailable chunk pauses the drill,
        // but the player's ON request remains saved. The scheduler retries and
        // resumes automatically as soon as the condition clears.
        if (pistonExtended.remove(loc) != null) {
            DrillPistonAnimator.retract(drill);
        }

        if (data.shouldWarn(30_000L)) {
            plugin.getLogger().fine("Drill at " + loc + " is waiting: " + fault.message());
        }
    }

    private void tickDrills() {
        Iterator<Map.Entry<Location, ActiveDrillData>> iterator = activeDrills.entrySet().iterator();
        boolean changed = false;

        while (iterator.hasNext()) {
            Map.Entry<Location, ActiveDrillData> entry = iterator.next();
            Location loc = entry.getKey();
            ActiveDrillData data = entry.getValue();
            if (!isLocationChunkLoaded(loc)) {
                pistonExtended.remove(loc);
                continue;
            }
            Block drill = loc.getBlock();

            if (!isDrillBlock(drill, data.tier())) {
                if (drill.getType() == data.tier().getCoreMaterial()) {
                    BlockFace facing = data.facing();
                    if (!isHorizontal(facing)) {
                        if (!allFacingCandidateChunksLoaded(drill)) {
                            continue;
                        }
                        facing = DrillStructure.inferFacing(drill, data.tier());
                        data.setFacing(facing);
                        changed = true;
                    }
                    restoreDrillIdentity(drill, data.tier(), facing);
                    DrillPistonAnimator.normalizeAfterLoad(drill);
                    pistonExtended.remove(loc);
                }
            }
            if (!isDrillBlock(drill, data.tier())) {
                handleRuntimeFault(loc, data, drill, DrillOperationCheck.CORE_UNAVAILABLE);
                continue;
            }

            if (!data.isEnabled()) {
                continue;
            }

            DrillOperationCheck op = evaluateOperation(drill, data, true);
            if (op != DrillOperationCheck.OK) {
                handleRuntimeFault(loc, data, drill, op);
                continue;
            }

            Hopper hopper = findCoalHopper(drill);
            int fuelNeed = fuelRequiredPerBarrel(data);
            Inventory output = findOutputInventory(drill);
            Location drillLocation = drill.getLocation();

            if (!consumeFuel(data, hopper.getInventory(), fuelNeed)) {
                continue;
            }
            changed = true;

            if (!storage.extractIfSurveyed(drillLocation, 1)) {
                data.addFuelCredit(fuelNeed);
                continue;
            }

            ItemStack crude = OilItems.CRUDE_OIL.clone();
            HashMap<Integer, ItemStack> leftover = output.addItem(crude);
            if (!leftover.isEmpty()) {
                storage.refund(drillLocation, 1);
                data.addFuelCredit(fuelNeed);
                continue;
            }

            data.scheduleNext(data.tier().rollDelayMs(plugin.getConfig()));
            changed = true;
        }

        if (changed) {
            persistence.save(activeDrills);
        }
        saveTicker++;
        if (saveTicker >= 60) {
            if (!changed) {
                persistence.save(activeDrills);
            }
            storage.save();
            saveTicker = 0;
        }
    }

    private boolean isDrillBlock(Block block, OilDrillTier expected) {
        SlimefunItem item = BlockStorage.check(block);
        if (!(item instanceof OilDrillItem drill)) {
            return false;
        }
        return drill.getTier() == expected;
    }

    static Hopper findCoalHopper(Block drill) {
        return DrillStructure.findCoalHopper(drill);
    }

    private static final int COAL_VALUE = 1;
    private static final int COAL_BLOCK_VALUE = 9;
    private static final int CHARCOAL_VALUE = 1;
    private static final int OIL_BUCKET_VALUE = 200;
    private static final int FUEL_BUCKET_VALUE = 800;

    static Inventory findOutputInventory(Block drill) {
        return DrillStructure.findOutputInventory(drill);
    }

    private static int countFuel(Inventory inv) {
        int total = 0;
        for (ItemStack stack : inv.getContents()) {
            if (stack == null) continue;
            if (stack.getType() == Material.COAL) {
                total += stack.getAmount() * COAL_VALUE;
            } else if (stack.getType() == Material.COAL_BLOCK) {
                total += stack.getAmount() * COAL_BLOCK_VALUE;
            } else if (stack.getType() == Material.CHARCOAL) {
                total += stack.getAmount() * CHARCOAL_VALUE;
            } else if (FuelHandler.isOilBucket(stack)) {
                total += stack.getAmount() * OIL_BUCKET_VALUE;
            } else if (FuelHandler.isFuelBucket(stack)) {
                total += stack.getAmount() * FUEL_BUCKET_VALUE;
            }
        }
        return total;
    }

    private static boolean consumeFuel(ActiveDrillData data, Inventory inv, int amount) {
        if (saturatedAdd(countFuel(inv), data.fuelCredit()) < amount) {
            return false;
        }
        int left = amount - data.spendFuelCredit(amount);
        for (int slot = 0; slot < inv.getSize() && left > 0; slot++) {
            ItemStack stack = inv.getItem(slot);
            if (stack == null) continue;

            int value = 0;
            if (stack.getType() == Material.COAL) {
                value = COAL_VALUE;
            } else if (stack.getType() == Material.COAL_BLOCK) {
                value = COAL_BLOCK_VALUE;
            } else if (stack.getType() == Material.CHARCOAL) {
                value = CHARCOAL_VALUE;
            } else if (FuelHandler.isOilBucket(stack)) {
                value = OIL_BUCKET_VALUE;
            } else if (FuelHandler.isFuelBucket(stack)) {
                value = FUEL_BUCKET_VALUE;
            } else {
                continue;
            }

            int maxTake = left / value + (left % value > 0 ? 1 : 0);
            int take = Math.min(maxTake, stack.getAmount());
            stack.setAmount(stack.getAmount() - take);
            if (stack.getAmount() <= 0) {
                inv.setItem(slot, null);
            }
            left -= take * value;
        }
        if (left < 0) {
            data.addFuelCredit(-left);
        }
        return true;
    }

    private static int saturatedAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }

    private static boolean canFit(Inventory inventory, ItemStack item) {
        int remaining = item.getAmount();
        for (ItemStack existing : inventory.getContents()) {
            if (existing == null || existing.getType().isAir()) {
                return true;
            }
            if (existing.isSimilar(item)) {
                remaining -= Math.max(0, existing.getMaxStackSize() - existing.getAmount());
                if (remaining <= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean canPlaceOnGround(Block drill) {
        Block below = drill.getRelative(BlockFace.DOWN);
        Material type = below.getType();
        // disallow air, water, lava
        return !type.isAir() && type != Material.WATER && type != Material.LAVA;
    }

    private boolean isFarEnoughFromOtherDrills(Block drill) {
        Location newLoc = drill.getLocation();
        double minDistSq = 36.0; // 6 blocks squared

        for (Location loc : activeDrills.keySet()) {
            if (loc.getWorld() != null && loc.getWorld().equals(newLoc.getWorld())) {
                double dx = loc.getX() - newLoc.getX();
                double dy = loc.getY() - newLoc.getY();
                double dz = loc.getZ() - newLoc.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < minDistSq) {
                    return false;
                }
            }
        }

        for (Location loc : pendingAutobuild) {
            if (loc.getWorld() != null && loc.getWorld().equals(newLoc.getWorld())) {
                double dx = loc.getX() - newLoc.getX();
                double dy = loc.getY() - newLoc.getY();
                double dz = loc.getZ() - newLoc.getZ();
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < minDistSq) {
                    return false;
                }
            }
        }
        return true;
    }
}
