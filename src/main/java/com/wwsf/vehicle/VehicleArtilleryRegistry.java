package com.wwsf.vehicle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;

import com.wwsf.WWSFPlugin;

public final class VehicleArtilleryRegistry {

    private static final Map<UUID, Set<Location>> artilleryByVessel = new ConcurrentHashMap<>();
    private static final Map<Location, UUID> vesselByArtillery = new ConcurrentHashMap<>();

    private VehicleArtilleryRegistry() {
    }

    public static void register(UUID vesselId, Location loc) {
        if (vesselId == null || loc == null) {
            return;
        }
        artilleryByVessel.computeIfAbsent(vesselId, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(loc.clone());
        vesselByArtillery.put(loc.clone(), vesselId);
    }

    public static void unregister(Location loc) {
        if (loc == null) {
            return;
        }
        UUID vesselId = vesselByArtillery.remove(loc);
        if (vesselId != null) {
            Set<Location> set = artilleryByVessel.get(vesselId);
            if (set != null) {
                set.remove(loc);
                if (set.isEmpty()) {
                    artilleryByVessel.remove(vesselId);
                }
            }
        }
    }

    public static void unregisterVessel(UUID vesselId) {
        if (vesselId == null) {
            return;
        }
        Set<Location> set = artilleryByVessel.remove(vesselId);
        if (set != null) {
            for (Location loc : set) {
                vesselByArtillery.remove(loc);
            }
        }
    }

    public static Set<Location> getForVessel(UUID vesselId) {
        if (vesselId == null) {
            return Collections.emptySet();
        }
        Set<Location> set = artilleryByVessel.get(vesselId);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    public static UUID getVessel(Location loc) {
        if (loc == null) {
            return null;
        }
        return vesselByArtillery.get(loc);
    }

    public static void clear() {
        artilleryByVessel.clear();
        vesselByArtillery.clear();
    }
}
