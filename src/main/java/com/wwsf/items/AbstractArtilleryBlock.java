package com.wwsf.items;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

import com.wwsf.WWSFPlugin;
import com.wwsf.artillery.ArtilleryDefinition;
import com.wwsf.artillery.ArtillerySpec;
import com.wwsf.util.Messages;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockUseHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;

public class AbstractArtilleryBlock extends SlimefunItem {

    private final ArtillerySpec spec;

    public AbstractArtilleryBlock(ItemGroup category, ArtilleryDefinition definition) {
        super(category, createStack(definition), RecipeType.ENHANCED_CRAFTING_TABLE, definition.recipe());
        this.spec = definition.spec();
    }

    private static SlimefunItemStack createStack(ArtilleryDefinition definition) {
        ArtillerySpec spec = definition.spec();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String line : spec.extraLore()) {
            lore.add(line);
        }
        if (spec.multiblock() != null) {
            lore.add("");
            lore.add("&8Frame: &7" + spec.multiblock().getFrameName());
        }
        lore.add("");
        lore.add("&8Ammo: &fany shell type &7(chest or inventory)");
        String fireMode = spec.isTurret() ? "&bDirect fire"
            : spec.isMortar() ? "&eMortar arc (charge affects range)"
            : spec.usesArc() ? "&eMortar/Rocket arc" : "&bDirect fire";
        lore.add("&7Fire: " + fireMode);
        lore.add("&7Range: &f" + (int) spec.minRange() + "–" + (int) spec.maxRange() + " blocks");
        if (spec.power() != 1.0) {
            lore.add("&7Power: &f" + spec.power() + "x &7(blast multiplier)");
        }

        return new SlimefunItemStack(
            "WWSF_" + spec.id(),
            spec.blockMaterial(),
            spec.displayName(),
            lore.toArray(new String[0])
        );
    }

    @Nonnull
    public ArtillerySpec getSpec() {
        return spec;
    }

    public boolean isMultiblockValid(@Nonnull Block core) {
        if (spec.multiblock() == null) {
            return true;
        }
        return spec.multiblock().isValid(core);
    }

    @Override
    public void preRegister() {
        addItemHandler(onPlace());
        addItemHandler(onUse());
    }

    @Nonnull
    protected BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(false) {

            @Override
            public void onPlayerPlace(@Nonnull BlockPlaceEvent event) {
                WWSFPlugin plugin = WWSFPlugin.getInstance();
                BlockStorage.addBlockInfo(
                    event.getBlockPlaced(),
                    plugin.getOwnerKey().getKey(),
                    event.getPlayer().getUniqueId().toString()
                );

                java.util.Optional<java.util.UUID> vesselId = com.wwsf.vehicle.VehicleQuery.vesselIdAt(event.getBlockPlaced().getLocation());
                if (vesselId.isPresent()) {
                    com.wwsf.vehicle.VehicleArtilleryRegistry.register(vesselId.get(), event.getBlockPlaced().getLocation());
                }

                if (spec.multiblock() != null) {
                    spec.multiblock().placeMultiblock(event.getBlockPlaced());
                    event.getPlayer().sendMessage(Messages.get("messages.multiblock-placed",
                        "&7Weapon emplaced with multiblock frame."));
                } else {
                    event.getPlayer().sendMessage(Messages.get("messages.placed",
                        "&7Weapon emplaced. &eRight-click &7to aim, &flook &7at target, &fleft-click &7to fire."));
                }
            }
        };
    }

    @Nonnull
    protected BlockUseHandler onUse() {
        return this::handleRightClick;
    }

    protected void handleRightClick(PlayerRightClickEvent event) {
            Block block = event.getClickedBlock().orElse(null);
            if (block == null) {
                return;
            }

            WWSFPlugin plugin = WWSFPlugin.getInstance();

            AbstractArtilleryBlock artillery = getArtilleryAt(block);
            if (artillery == null) {
                return;
            }

            Location cannonLocation = block.getLocation();
            UUID currentUser = plugin.getAimingManager().getWeaponUser(cannonLocation);
            if (currentUser != null && !currentUser.equals(event.getPlayer().getUniqueId())) {
                Player other = plugin.getServer().getPlayer(currentUser);
                String name = other != null ? other.getName() : "Someone";
                event.getPlayer().sendMessage(Messages.color("&cThis weapon is currently in use by &f" + name + "&c."));
                event.cancel();
                return;
            }

            if (!artillery.isMultiblockValid(block)) {
                event.getPlayer().sendMessage(Messages.get("messages.multiblock-incomplete",
                    "&cMultiblock frame is incomplete. Build the structure around this core."));
                event.cancel();
                return;
            }

            event.cancel();

            if (event.getPlayer().isSneaking()) {
                plugin.getAimingManager().stopAiming(event.getPlayer());
                event.getPlayer().sendMessage(Messages.get("messages.cancelled", "&7Aiming cancelled."));
                return;
            }

            ArtillerySpec weaponSpec = artillery.getSpec();
            plugin.getAimingManager().startAiming(event.getPlayer(), block.getLocation(), weaponSpec);
        }

    @Nullable
    public static ArtillerySpec getSpecAt(@Nonnull Block block) {
        AbstractArtilleryBlock artillery = getArtilleryAt(block);
        return artillery != null ? artillery.getSpec() : null;
    }

    @Nullable
    public static AbstractArtilleryBlock getArtilleryAt(@Nonnull Block block) {
        SlimefunItem item = BlockStorage.check(block.getLocation());
        if (item instanceof AbstractArtilleryBlock artillery) {
            return artillery;
        }
        return null;
    }
}
