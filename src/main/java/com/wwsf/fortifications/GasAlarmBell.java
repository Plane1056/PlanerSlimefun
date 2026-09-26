package com.wwsf.fortifications;

import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.WWSFItem;
import com.wwsf.gas.GasDetonationEvent;
import com.wwsf.integration.SlimefunStorageAccess;
import com.wwsf.setup.FortificationsCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

/**
 * Gas Alarm Bell - Detects chemical weapon deployments within 24 blocks
 * and alerts nearby players with a loud alarm.
 */
public class GasAlarmBell extends WWSFItem implements Listener {

    public static final String ID = "WWSF_GAS_ALARM_BELL";
    private static final int CUSTOM_MODEL_DATA = 10010;
    private static final int DETECTION_RADIUS = 24;
    private static final int ALARM_DURATION_TICKS = 200; // 10 seconds
    private static final int ALARM_INTERVAL_TICKS = 10; // Sound every 0.5 seconds
    private static final long DEFAULT_HOWITZER_SCAN_INTERVAL_TICKS = 40L;
    private static final long DEFAULT_INDEX_REFRESH_INTERVAL_TICKS = 6000L;

    // Track active alarms to prevent spam
    private static final Set<Location> ACTIVE_ALARMS = new HashSet<>();

    // Index only Gas Alarm Bells instead of walking every loaded Slimefun block
    // twice per second. All access happens on the server thread.
    private final Map<UUID, Set<Location>> placedBellsByWorld = new HashMap<>();

    public GasAlarmBell(@Nonnull ItemGroup category) {
        super(category, createStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createSlimefunRecipe());
    }

    @Override
    public void preRegister() {
        addItemHandler(onPlace());
        addItemHandler(onBreak());
        addItemHandler(onUse());
    }

    @Override
    public void postRegister() {
        super.postRegister();
        setRecipeOutput(getItem());
    }

    @Override
    public @Nonnull ItemStack getItem() {
        ItemStack item = super.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    private static SlimefunItemStack createStack() {
        SlimefunItemStack stack = new SlimefunItemStack(
            ID,
            Material.BELL,
            ChatColor.RED + "Gas Alarm Bell",
            meta -> {
                com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
                meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Fortification",
                    "",
                    ChatColor.GRAY + "Detects chemical weapons within 24 blocks.",
                    ChatColor.GRAY + "Requires sky access or horizontal clearance.",
                    "",
                    ChatColor.RED + "⚠ WARNING: Loud alarm when triggered"
                ));
            }
        );
        // Bake custom model data directly into the ItemStack's meta
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Fortification",
                "",
                ChatColor.GRAY + "Detects chemical weapons within 24 blocks.",
                ChatColor.GRAY + "Requires sky access or horizontal clearance.",
                "",
                ChatColor.RED + "⚠ WARNING: Loud alarm when triggered"
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@Nonnull org.bukkit.event.block.BlockPlaceEvent event) {
                WWSFPlugin plugin = WWSFPlugin.getInstance();
                BlockStorage.addBlockInfo(
                    event.getBlockPlaced(),
                    plugin.getOwnerKey().getKey(),
                    event.getPlayer().getUniqueId().toString()
                );
                addBell(event.getBlockPlaced().getLocation());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Gas Alarm Bell placed. Monitoring for chemical threats...");
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(true, true) {
            @Override
            public void onPlayerBreak(
                org.bukkit.event.block.BlockBreakEvent event,
                ItemStack tool,
                List<ItemStack> drops
            ) {
                removeBell(event.getBlock().getLocation());
            }
        };
    }

    @Nonnull
    private BlockUseHandler onUse() {
        return this::handleRightClick;
    }

    private void handleRightClick(PlayerRightClickEvent event) {
        Block block = event.getClickedBlock().orElse(null);
        if (block == null || block.getType() != Material.BELL) {
            return;
        }

        WWSFPlugin plugin = WWSFPlugin.getInstance();

        // Owner check
        String owner = BlockStorage.getLocationInfo(block.getLocation(), plugin.getOwnerKey().getKey());
        String playerId = event.getPlayer().getUniqueId().toString();
        if (owner != null && !owner.equals(playerId)) {
            event.getPlayer().sendMessage(ChatColor.RED + "This is not your alarm bell!");
            event.cancel();
            return;
        }

        // Right-click just shows info, doesn't do anything special
        event.getPlayer().sendMessage(ChatColor.YELLOW + "Gas Alarm Bell: " + ChatColor.GRAY + 
            "Monitoring for chemical weapons within " + DETECTION_RADIUS + " blocks.");
        event.cancel();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGasDetonation(GasDetonationEvent event) {
        Location detonationLoc = event.getDetonationLocation();
        World world = detonationLoc.getWorld();
        if (world == null) return;

        // Find all placed Gas Alarm Bells within detection radius
        for (Block block : getNearbyBells(detonationLoc, DETECTION_RADIUS)) {
            Location bellLoc = block.getLocation();
            
            // Check if this bell can detect the gas (line of sight)
            if (hasLineOfSight(bellLoc, detonationLoc)) {
                triggerAlarm(bellLoc, GasThreat.GAS_BOMB);
            }
        }
    }

    private Iterable<Block> getNearbyBells(Location center, int radius) {
        Set<Block> bells = new HashSet<>();
        World world = center.getWorld();
        if (world == null) return bells;

        double radiusSquared = radius * radius;
        for (Location bellLocation : placedBellLocations(world)) {
            if (bellLocation.distanceSquared(center) <= radiusSquared) {
                bells.add(bellLocation.getBlock());
            }
        }
        return bells;
    }

    private void scanHowitzerGas() {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }

        for (World world : plugin.getServer().getWorlds()) {
            List<Location> bells = List.copyOf(placedBellLocations(world));
            if (bells.isEmpty()) {
                continue;
            }

            List<Location> harmfulCells = plugin.getHowitzerBridge()
                .harmfulGasCellsNear(world, bells, DETECTION_RADIUS);
            if (harmfulCells.isEmpty()) {
                continue;
            }

            double radiusSquared = DETECTION_RADIUS * DETECTION_RADIUS;
            for (Location bell : bells) {
                if (ACTIVE_ALARMS.contains(bell)) {
                    continue;
                }
                for (Location gasCell : harmfulCells) {
                    if (bell.distanceSquared(gasCell) <= radiusSquared
                        && hasLineOfSight(bell, gasCell)) {
                        triggerAlarm(bell, GasThreat.HOWITZER_POISON);
                        break;
                    }
                }
            }
        }
    }

    private Set<Location> placedBellLocations(World world) {
        Set<Location> indexed = placedBellsByWorld.get(world.getUID());
        if (indexed == null || indexed.isEmpty()) {
            return Set.of();
        }

        Set<Location> loaded = new HashSet<>();
        for (Location location : indexed) {
            if (world.isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                loaded.add(location);
            }
        }
        return loaded;
    }

    private void addBell(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        Location blockLocation = new Location(
            world,
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ()
        );
        placedBellsByWorld
            .computeIfAbsent(world.getUID(), ignored -> new HashSet<>())
            .add(blockLocation);
    }

    private void removeBell(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        Set<Location> bells = placedBellsByWorld.get(world.getUID());
        if (bells != null) {
            Location blockLocation = new Location(
                world,
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ()
            );
            bells.remove(blockLocation);
            ACTIVE_ALARMS.remove(blockLocation);
            if (bells.isEmpty()) {
                placedBellsByWorld.remove(world.getUID());
            }
        }
    }

    private void rebuildBellIndex() {
        placedBellsByWorld.clear();
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        if (plugin == null || !plugin.isEnabled()) {
            return;
        }

        for (World world : plugin.getServer().getWorlds()) {
            for (Location location : SlimefunStorageAccess.loadedLocations(world, ID)) {
                addBell(location);
            }
        }
    }

    private void reindexChunk(Chunk chunk) {
        World world = chunk.getWorld();
        Set<Location> bells = placedBellsByWorld.get(world.getUID());
        if (bells != null) {
            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();
            bells.removeIf(location ->
                (location.getBlockX() >> 4) == chunkX
                    && (location.getBlockZ() >> 4) == chunkZ
            );
        }

        for (Location location : SlimefunStorageAccess.loadedLocations(chunk, ID)) {
            addBell(location);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        if (plugin != null && plugin.isEnabled()) {
            // Slimefun restores its cached chunk data during chunk loading. Index
            // one tick later so that cache is available without forcing disk IO.
            plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> reindexChunk(event.getChunk()),
                1L
            );
        }
    }

    /**
     * Checks if the bell has line of sight to the gas detonation.
     * Returns true if:
     * 1. Bell has direct sky access (vertical clearance), OR
     * 2. There's an unobstructed horizontal path from gas to bell
     */
    private boolean hasLineOfSight(Location bellLoc, Location gasLoc) {
        World world = bellLoc.getWorld();
        if (world == null) return false;

        int bellX = bellLoc.getBlockX();
        int bellY = bellLoc.getBlockY();
        int bellZ = bellLoc.getBlockZ();
        int worldHeight = world.getMaxHeight();

        if (!world.isChunkLoaded(bellX >> 4, bellZ >> 4)) {
            return false;
        }

        // Check 1: Sky access using the chunk heightmap instead of reading up to
        // hundreds of blocks above every alarm on every gas scan.
        boolean hasSkyAccess = world.getHighestBlockYAt(bellX, bellZ) <= bellY;

        if (hasSkyAccess) {
            return true;
        }

        // Check 2: Horizontal clearance (raycast from gas to bell at bell's Y level)
        return hasHorizontalClearance(gasLoc, bellLoc);
    }

    /**
     * Checks if there's an unobstructed horizontal path from gas to bell.
     */
    private boolean hasHorizontalClearance(Location from, Location to) {
        World world = from.getWorld();
        if (world == null) return false;

        int x1 = from.getBlockX();
        int z1 = from.getBlockZ();
        int x2 = to.getBlockX();
        int z2 = to.getBlockZ();
        int y = to.getBlockY(); // Check at bell's Y level

        // Bresenham's line algorithm for horizontal path
        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;
        int err = dx - dz;

        int cx = x1;
        int cz = z1;

        while (true) {
            // Skip the start and end points
            if ((cx != x1 || cz != z1) && (cx != x2 || cz != z2)) {
                if (!world.isChunkLoaded(cx >> 4, cz >> 4)) {
                    return false;
                }
                Material mat = world.getBlockAt(cx, y, cz).getType();
                // If we hit a solid block, path is blocked
                if (mat.isSolid() && mat != Material.BELL) {
                    return false;
                }
            }

            if (cx == x2 && cz == z2) break;

            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                cx += sx;
            }
            if (e2 < dx) {
                err += dx;
                cz += sz;
            }
        }

        return true;
    }

    private void triggerAlarm(Location bellLoc, GasThreat threat) {
        // Prevent duplicate alarms at same location
        if (ACTIVE_ALARMS.contains(bellLoc)) {
            return;
        }

        World world = bellLoc.getWorld();
        if (world == null) return;
        ACTIVE_ALARMS.add(bellLoc);

        String gasType = threat.color + threat.displayName;
        String alertMessage = ChatColor.RED + "" + ChatColor.BOLD + "[!] " + 
            gasType + ChatColor.RED + "" + ChatColor.BOLD + " DETECTED [!]";

        // Get owner
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        String ownerUUID = BlockStorage.getLocationInfo(bellLoc, plugin.getOwnerKey().getKey());

        // Notify owner and nearby players
        Set<Player> notifiedPlayers = new HashSet<>();

        // Notify owner first
        if (ownerUUID != null) {
            try {
                Player owner = plugin.getServer().getPlayer(java.util.UUID.fromString(ownerUUID));
                if (owner != null && owner.isOnline()) {
                    notifyPlayer(owner, bellLoc, alertMessage, threat);
                    notifiedPlayers.add(owner);
                }
            } catch (IllegalArgumentException e) {
                // Invalid UUID, skip
            }
        }

        // Notify all players within radius
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(bellLoc) <= DETECTION_RADIUS && !notifiedPlayers.contains(player)) {
                notifyPlayer(player, bellLoc, alertMessage, threat);
            }
        }

        // Start alarm sound effect
        new BukkitRunnable() {
            int elapsed = 0;

            @Override
            public void run() {
                if (elapsed >= ALARM_DURATION_TICKS) {
                    ACTIVE_ALARMS.remove(bellLoc);
                    cancel();
                    return;
                }

                // Play loud bell sound
                world.playSound(bellLoc, Sound.BLOCK_BELL_USE, 2.0f, 0.8f);
                
                // Redstone pulse visual effect
                world.spawnParticle(org.bukkit.Particle.DUST, bellLoc.clone().add(0, 1, 0), 
                    10, 0.5, 0.5, 0.5, 0.1,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.RED, 1.0f));

                elapsed += ALARM_INTERVAL_TICKS;
            }
        }.runTaskTimer(plugin, 0L, ALARM_INTERVAL_TICKS);
    }

    private void notifyPlayer(
        Player player,
        Location bellLoc,
        String alertMessage,
        GasThreat threat
    ) {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        
        // Send flashing action bar message
        new BukkitRunnable() {
            int flash = 0;

            @Override
            public void run() {
                if (flash >= 20) { // 10 seconds of flashing
                    cancel();
                    return;
                }

                // Alternate between red and yellow
                ChatColor color = flash % 2 == 0 ? ChatColor.RED : ChatColor.YELLOW;
                player.sendActionBar(color + "" + ChatColor.BOLD + alertMessage);

                flash++;
            }
        }.runTaskTimer(plugin, 0L, 10L);

        // Also send chat message
        player.sendMessage("");
        player.sendMessage(alertMessage);
        player.sendMessage(ChatColor.GRAY + "Gas Alarm Bell at " + 
            ChatColor.WHITE + "[" + bellLoc.getBlockX() + ", " + bellLoc.getBlockY() + ", " + bellLoc.getBlockZ() + "]" + 
            ChatColor.GRAY + " detected " + threat.description + "!");
        player.sendMessage("");
    }

    private enum GasThreat {
        GAS_BOMB(ChatColor.GREEN, "GAS BOMB", "a gas bomb"),
        HOWITZER_POISON(ChatColor.GREEN, "POISON GAS", "Howitzer artillery poison gas");

        private final ChatColor color;
        private final String displayName;
        private final String description;

        GasThreat(ChatColor color, String displayName, String description) {
            this.color = color;
            this.displayName = displayName;
            this.description = description;
        }
    }

    @Nonnull
    private static ItemStack[] createSlimefunRecipe() {
        return new ItemStack[] {
            SteelBlock.recipeIngredient(), new ItemStack(Material.BELL), SteelBlock.recipeIngredient(),
            new ItemStack(Material.REDSTONE), SlimefunItems.REINFORCED_PLATE, new ItemStack(Material.REDSTONE),
            SteelBlock.recipeIngredient(), new ItemStack(Material.COPPER_BLOCK), SteelBlock.recipeIngredient()
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        GasAlarmBell bell = new GasAlarmBell(FortificationsCategory.FORTIFICATIONS);
        bell.register(plugin);
        plugin.getServer().getPluginManager().registerEvents(bell, plugin);
        long scanInterval = Math.max(
            20L,
            plugin.getConfig().getLong(
                "fortifications.gas-alarm.howitzer-scan-interval-ticks",
                DEFAULT_HOWITZER_SCAN_INTERVAL_TICKS
            )
        );
        long refreshInterval = Math.max(
            1200L,
            plugin.getConfig().getLong(
                "fortifications.gas-alarm.index-refresh-interval-ticks",
                DEFAULT_INDEX_REFRESH_INTERVAL_TICKS
            )
        );
        plugin.getServer().getScheduler().runTaskLater(plugin, bell::rebuildBellIndex, 1L);
        plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            bell::scanHowitzerGas,
            scanInterval,
            scanInterval
        );
        plugin.getServer().getScheduler().runTaskTimer(
            plugin,
            bell::rebuildBellIndex,
            refreshInterval,
            refreshInterval
        );
        plugin.getLogger().info(
            "Gas Alarm Bell indexed scanning enabled (detection every "
                + scanInterval + " ticks, index refresh every " + refreshInterval + " ticks)."
        );
    }
}
