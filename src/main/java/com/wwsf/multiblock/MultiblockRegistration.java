package com.wwsf.multiblock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtilleryRegistry;
import com.wwsf.util.Messages;
import com.wwsf.vehicle.VehicleQuery;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * Shared logic for detecting a cannon multiblock from a clicked block, spawning its
 * tracking marker, and returning it. Used by both the manual trigger item and the
 * right-click-on-structure fallback so cannons register without a separate step — and
 * so they work identically on the ground and aboard Movecraft ships.
 */
public final class MultiblockRegistration {

    private MultiblockRegistration() {
    }

    /**
     * Validates the multiblock structure that contains {@code clicked} and, if valid,
     * spawns a marker for it (tagged with the owning vessel when aboard a ship).
     *
     * @return the spawned marker, or {@code null} if no valid multiblock was found.
     */
    @Nullable
    public static MarkerManager.MarkerData registerAt(@Nonnull Player player, @Nonnull Block clicked) {
        for (CannonDefinition def : ArtilleryRegistry.allCannonDefinitions()) {
            MarkerManager.MarkerData marker = attempt(def, clicked);
            if (marker != null) {
                player.sendMessage(Messages.color("&aMarker placed for &f" + def.getName() + "&a."));
                return marker;
            }
        }
        return null;
    }

    /**
     * Registers using only the definition tied to the trigger item the player placed. This
     * prevents other weapons' structures from being attempted (and their "missing blocks"
     * messages from spamming chat) when the wrong-looking weapon happens to be earlier in
     * the definition list.
     */
    @Nullable
    public static MarkerManager.MarkerData registerAt(@Nonnull Player player, @Nonnull Block clicked, @Nonnull CannonDefinition def) {
        MarkerManager.MarkerData marker = attempt(def, clicked);
        if (marker != null) {
            return marker;
        }
        player.sendMessage(Messages.get(
            "messages.multiblock-incomplete",
            "&cMultiblock needs to be built first."
        ));
        return null;
    }

    /**
     * Attempts to detect and spawn a marker for a single definition around {@code clicked}.
     * Returns the spawned marker, or {@code null} if this definition's structure is not valid.
     * Sends no chat messages so callers control messaging.
     */
    @Nullable
    private static MarkerManager.MarkerData attempt(@Nonnull CannonDefinition def, @Nonnull Block clicked) {
        // Normal case: the player clicked the trigger/anchor block itself (the cell at the
        // trigger offset). The validator treats its argument as that anchor and skips that cell,
        // so validate the clicked block directly. This works for any trigger-cell material
        // (e.g. a hopper) and any rotation.
        MarkerManager.MarkerData direct = trySpawn(def, clicked.getLocation());
        if (direct != null) {
            return direct;
        }

        // Fallback: the player clicked some other block of the structure (e.g. after the trigger
        // reverted to a vanilla block). Scan for a matching cell and derive the trigger position.
        for (int rot = 0; rot < 4; rot++) {
            for (int y = 0; y < def.getSizeY(); y++) {
                for (int x = 0; x < def.getSizeX(); x++) {
                    for (int z = 0; z < def.getSizeZ(); z++) {
                        Material expected = def.getMaterialAt(x, y, z);
                        if (expected == Material.AIR) {
                            continue;
                        }
                        if (x == def.triggerOffsetX() && y == def.triggerOffsetY() && z == def.triggerOffsetZ()) {
                            continue;
                        }
                        if (expected != clicked.getType()) {
                            continue;
                        }

                        int[] rotated = def.rotateOffset(x, z, rot);
                        Location core = clicked.getLocation().clone().subtract(rotated[0], y, rotated[1]);

                        int[] triggerRotated = def.rotateOffset(def.triggerOffsetX(), def.triggerOffsetZ(), rot);
                        Location triggerLoc = core.clone().add(triggerRotated[0], def.triggerOffsetY(), triggerRotated[1]);

                        MarkerManager.MarkerData marker = trySpawn(def, triggerLoc);
                        if (marker != null) {
                            return marker;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static MarkerManager.MarkerData trySpawn(@Nonnull CannonDefinition def, @Nonnull Location triggerLoc) {
        try {
            if (!def.isValid(triggerLoc.getBlock())) {
                return null;
            }
            // Use the rotation the validator actually detected, then derive the true origin from
            // the real trigger block, so the marker's barrel direction and per-tick validity
            // stay correct for any orientation.
            int actualRot = def.getLastRotation();
            int[] actualTriggerRotated = def.rotateOffset(def.triggerOffsetX(), def.triggerOffsetZ(), actualRot);
            Location actualOrigin = triggerLoc.clone().subtract(actualTriggerRotated[0], def.triggerOffsetY(), actualTriggerRotated[1]);
            java.util.UUID vesselId = VehicleQuery.vesselIdAt(actualOrigin).orElse(null);
            MarkerManager.spawnMarker(actualOrigin, def, actualRot, vesselId);
            MarkerManager.MarkerData created = MarkerManager.findMarkerAtLocation(actualOrigin);
            if (created != null) {
                return created;
            }
            // Fallback: re-scan by definition + core if the location lookup missed.
            return MarkerManager.getMarkerAt(actualOrigin, def);
        } catch (Throwable t) {
            WWSFPlugin.getInstance().getLogger().fine("[MultiblockRegistration] Validation failed: " + t.getMessage());
            return null;
        }
    }
}
