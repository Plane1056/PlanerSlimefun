package com.wwsf.artillery;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

import com.wwsf.WWSFPlugin;
import com.wwsf.items.AbstractArtilleryBlock;
import com.wwsf.items.GunPart;
import com.wwsf.items.TriggerBlock;
import com.wwsf.multiblock.CannonDefinition;
import com.wwsf.multiblock.FieldCannonDefinition;
import com.wwsf.multiblock.FieldCannonLoader;
import com.wwsf.multiblock.MaximMachineGunDefinition;
import com.wwsf.multiblock.MaximMachineGunLoader;
import com.wwsf.multiblock.ShortCannonDefinition;
import com.wwsf.multiblock.ShortCannonLoader;
import com.wwsf.multiblock.TrenchCannonDefinition;
import com.wwsf.multiblock.TrenchCannonLoader;
import com.wwsf.setup.WWSFCategory;

import java.util.logging.Level;

public final class ArtilleryRegistry {

    private static final List<ArtilleryDefinition> DEFINITIONS = new ArrayList<>();
    private static final List<CannonDefinition> CANNON_DEFINITIONS = new ArrayList<>();
    private static final java.util.Map<String, ItemStack> TRIGGER_ITEMS = new java.util.HashMap<>();

    private ArtilleryRegistry() {}

    public static void registerAll(@Nonnull WWSFPlugin plugin) {
        FieldCannonDefinition fieldDef = FieldCannonLoader.load();
        if (fieldDef != null) {
            plugin.getLogger().info("Loaded multiblock: " + fieldDef.getName());
        } else {
            plugin.getLogger().warning("Field Cannon multiblock not loaded.");
        }

        ShortCannonDefinition shortDef = ShortCannonLoader.load();
        if (shortDef != null) {
            plugin.getLogger().info("Loaded multiblock: " + shortDef.getName());
        } else {
            plugin.getLogger().warning("Short Cannon multiblock not loaded.");
        }

        TrenchCannonDefinition trenchDef = TrenchCannonLoader.load();
        if (trenchDef != null) {
            plugin.getLogger().info("Loaded multiblock: " + trenchDef.getName());
        } else {
            plugin.getLogger().warning("Trench Cannon multiblock not loaded.");
        }

        if (fieldDef != null) {
            registerTrigger(plugin, fieldDef, "FIELD_CANNON_TRIGGER",
                "&8Field Cannon Trigger", "Place this and build a Field Cannon around it.",
                new ItemStack[]{
                    ironBlock(), ironBlock(), ironBlock(),
                    gunPart(), redstoneBlock(), gunPart(),
                    ironBlock(), null, ironBlock()
                });
        }

        if (shortDef != null) {
            registerTrigger(plugin, shortDef, "LIGHT_FIELD_GUN_TRIGGER",
                "&8Light Field Gun Trigger", "Place this and build a Light Field Gun around it.",
                new ItemStack[]{
                    steel(), steel(), steel(),
                    gunPart(), redstoneBlock(), gunPart(),
                    steel(), ironBlock(), steel()
                });
        }

        if (trenchDef != null) {
            registerTrigger(plugin, trenchDef, "TRENCH_CANNON_TRIGGER",
                "&8Trench Cannon Trigger", "Place this and build a Trench Cannon around it.",
                new ItemStack[]{
                    ironBlock(), ironBlock(), ironBlock(),
                    gunPart(), new ItemStack(Material.TNT), gunPart(),
                    null, null, null
                });
        }

        MaximMachineGunDefinition maximDef = MaximMachineGunLoader.load();
        if (maximDef != null) {
            plugin.getLogger().info("Loaded multiblock: " + maximDef.getName());
        } else {
            plugin.getLogger().warning("Maxim Machine Gun multiblock not loaded.");
        }

        if (maximDef != null) {
            registerTrigger(plugin, maximDef, "MAXIM_MACHINE_GUN_TRIGGER",
                "&8Maxim Machine Gun Trigger", "Place this and build a Maxim Machine Gun around it.",
                new ItemStack[]{
                    ironBlock(), ironBlock(), ironBlock(),
                    gunPart(), redstoneBlock(), gunPart(),
                    null, gunPart(), null
                });
        }

    }

    private static ItemStack gunPart() {
        return new SlimefunItemStack(GunPart.ID, Material.PRISMARINE_CRYSTALS, "&fGun Part");
    }

    private static ItemStack steel() {
        return SlimefunItems.STEEL_INGOT;
    }

    private static ItemStack ironBlock() {
        return new ItemStack(Material.IRON_BLOCK);
    }

    private static ItemStack redstoneBlock() {
        return new ItemStack(Material.REDSTONE_BLOCK);
    }

    private static void registerTrigger(
        @Nonnull WWSFPlugin plugin,
        @Nonnull CannonDefinition cannon,
        @Nonnull String id,
        @Nonnull String displayName,
        @Nonnull String lore,
        @Nonnull ItemStack[] recipe
    ) {
        ArtillerySpec s = cannon.getWeaponSpec();
        ArtillerySpec triggerSpec = new ArtillerySpec(
            id,
            s.blockMaterial(),
            displayName,
            new String[]{lore},
            null,
            s.role(),
            s.trajectory(),
            s.maxRange(),
            s.minRange(),
            s.shellSpeed(),
            s.minArcHeight(),
            s.arcHeightFactor(),
            s.power(),
            s.ammoItemId()
        );
        ArtilleryDefinition def = new ArtilleryDefinition(triggerSpec, recipe);
        try {
            TriggerBlock triggerBlock = new TriggerBlock(WWSFCategory.WWSF, def, cannon);
            triggerBlock.register(plugin);
            TRIGGER_ITEMS.put(s.id(), triggerBlock.getItem());
            DEFINITIONS.add(def);
            CANNON_DEFINITIONS.add(cannon);
            plugin.getLogger().info("Registered " + displayName);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register " + displayName, t);
        }
    }

    public static ItemStack getTriggerItem(@Nonnull String id) {
        ItemStack item = TRIGGER_ITEMS.get(id);
        if (item == null) {
            WWSFPlugin.getInstance().getLogger().warning("[ArtilleryRegistry] No trigger item found for id: " + id + ". Available: " + TRIGGER_ITEMS.keySet());
        }
        return item;
    }

    public static ArtilleryDefinition[] all() {
        return DEFINITIONS.toArray(new ArtilleryDefinition[0]);
    }

    public static List<CannonDefinition> allCannonDefinitions() {
        return List.copyOf(CANNON_DEFINITIONS);
    }

    @Nullable
    public static CannonDefinition getCannonDefinition(@Nonnull String id) {
        for (CannonDefinition def : CANNON_DEFINITIONS) {
            if (def.getId().equals(id)) {
                return def;
            }
        }
        return null;
    }
}
