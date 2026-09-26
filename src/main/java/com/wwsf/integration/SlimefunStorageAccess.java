package com.wwsf.integration;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.BlockDataController;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunChunkData;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;

/**
 * Release-server adapter for Slimefun-United's database-backed block storage.
 *
 * <p>Upstream Slimefun RC-37 exposed {@code BlockStorage.getRawStorage(World)}. The
 * Slimefun-United build used by this server deliberately removed that legacy method, so addon code
 * must enumerate the controller's loaded chunk data instead.</p>
 */
public final class SlimefunStorageAccess {

    private SlimefunStorageAccess() {
    }

    public static Set<Location> loadedLocations(World world) {
        Set<Location> locations = new HashSet<>();
        for (SlimefunChunkData chunkData : controller().getAllLoadedChunkData(world)) {
            addLocations(locations, chunkData, world, null, null, null);
        }
        return locations;
    }

    /**
     * Returns loaded Slimefun records with the requested persistent item ID.
     * This never resolves a Bukkit block and therefore cannot load a chunk.
     */
    public static Set<Location> loadedLocations(World world, String slimefunId) {
        Set<Location> locations = new HashSet<>();
        for (SlimefunChunkData chunkData : controller().getAllLoadedChunkData(world)) {
            Chunk chunk = chunkData.getChunk();
            addLocations(locations, chunkData, world, chunk.getX(), chunk.getZ(), slimefunId);
        }
        return locations;
    }

    public static Set<Location> loadedLocations(Chunk chunk) {
        Set<Location> locations = new HashSet<>();
        SlimefunChunkData chunkData = controller().getChunkDataFromCache(chunk);
        if (chunkData != null) {
            addLocations(locations, chunkData, chunk.getWorld(), chunk.getX(), chunk.getZ(), null);
        }
        return locations;
    }

    /**
     * Returns only records carrying {@code slimefunId} and physically belonging
     * to {@code chunk}. The coordinate check happens before callers can touch a
     * block, preventing malformed/stale records from causing sync chunk loads.
     */
    public static Set<Location> loadedLocations(Chunk chunk, String slimefunId) {
        Set<Location> locations = new HashSet<>();
        SlimefunChunkData chunkData = controller().getChunkDataFromCache(chunk);
        if (chunkData != null) {
            addLocations(
                locations,
                chunkData,
                chunk.getWorld(),
                chunk.getX(),
                chunk.getZ(),
                slimefunId
            );
        }
        return locations;
    }

    private static BlockDataController controller() {
        return Slimefun.getDatabaseManager().getBlockDataController();
    }

    private static void addLocations(
        Set<Location> locations,
        SlimefunChunkData chunkData,
        World expectedWorld,
        Integer expectedChunkX,
        Integer expectedChunkZ,
        String expectedSlimefunId
    ) {
        for (SlimefunBlockData blockData : chunkData.getAllBlockData()) {
            if (blockData == null || blockData.isPendingRemove()) {
                continue;
            }

            if (expectedSlimefunId != null && !expectedSlimefunId.equals(blockData.getSfId())) {
                continue;
            }

            Location location = blockData.getLocation();
            if (location == null || !expectedWorld.equals(location.getWorld())) {
                continue;
            }
            if (expectedChunkX != null
                && ((location.getBlockX() >> 4) != expectedChunkX
                    || (location.getBlockZ() >> 4) != expectedChunkZ)) {
                continue;
            }
            locations.add(location);
        }
    }
}
