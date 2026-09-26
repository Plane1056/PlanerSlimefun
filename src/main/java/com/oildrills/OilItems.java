package com.oildrills;

import com.wwsf.setup.MachinesCategory;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.items.CustomItemStack;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OilItems {

    public static ItemGroup GROUP;
    public static SlimefunItemStack CRUDE_OIL;
    public static SlimefunItemStack LEGACY_FUEL_BUCKET;
    public static SlimefunItemStack GAS_BOTTLE;
    public static SlimefunItemStack OIL_REFINERY;
    public static SlimefunItemStack FUEL_REFINERY_STACK;
    public static SlimefunItemStack OIL_LIQUEFACTION_PLANT;

    public static SlimefunItemStack OIL_DRILL_MARK_I;
    public static SlimefunItemStack OIL_DRILL_MARK_II;
    public static SlimefunItemStack OIL_DRILL_MARK_III;

    public static OilDrillItem DRILL_MARK_I;
    public static OilDrillItem DRILL_MARK_II;
    public static OilDrillItem DRILL_MARK_III;

    public static OilRefineryItem REFINERY;
    public static FuelRefineryItem FUEL_REFINERY;
    public static OilLiquefactionItem LIQUEFACTION_PLANT;
    public static DowsingRodItem DOWSING_ROD_ITEM;

    /** @deprecated use {@link #DRILL_MARK_I} */
    @Deprecated
    public static OilDrillItem DRILL_ITEM;

    private static final List<OilDrillItem> ALL_DRILLS = new ArrayList<>();

    private OilItems() {
    }

    public static List<OilDrillItem> allDrills() {
        return Collections.unmodifiableList(ALL_DRILLS);
    }

    public static void register(org.bukkit.plugin.java.JavaPlugin plugin) {
        SlimefunAddon addon = (SlimefunAddon) plugin;

        GROUP = MachinesCategory.MACHINES;
        if (GROUP == null) {
            throw new IllegalStateException("Great War Industries category was not registered");
        }

        OIL_LIQUEFACTION_PLANT = liquefactionStack();
        CRUDE_OIL = new SlimefunItemStack(
            "CRUDE_OIL",
            Material.BROWN_DYE,
            "§8§lCrude Oil",
            "§7Obtained with an §eOil Liquefaction Plant",
            "§7or an §eOil Pump§7."
        );
        RecipeType crudeOilSources = new RecipeType(
            new NamespacedKey(plugin, "crude_oil_sources"),
            OIL_LIQUEFACTION_PLANT,
            "§eOil Liquefaction Plant or Oil Pump"
        );
        new SlimefunItem(GROUP, CRUDE_OIL, crudeOilSources, crudeOilSourceGuide()).register(addon);

        LEGACY_FUEL_BUCKET = new SlimefunItemStack(
            "FUEL_BUCKET",
            Material.BUCKET,
            "§c§lFuel Bucket",
            "§8Legacy WWSF fuel bucket"
        );
        SlimefunItem legacyFuelBucket = new SlimefunItem(
            GROUP,
            LEGACY_FUEL_BUCKET,
            RecipeType.NULL,
            new ItemStack[0]
        );
        legacyFuelBucket.setHidden(true);
        legacyFuelBucket.register(addon);
        GAS_BOTTLE = new SlimefunItemStack(
            "WWSF_GAS_BOTTLE",
            Material.POTION,
            "§f§lGas Bottle",
            meta -> {
                if (meta instanceof PotionMeta potionMeta) {
                    potionMeta.setColor(org.bukkit.Color.LIME);
                }
                meta.setLore(List.of(
                    "§7A pressurized bottle of refined fuel gas.",
                    "§7Made in a §eHeated Pressure Chamber §7in §e30 seconds§7."
                ));
            }
        );
        new SlimefunItem(
            GROUP,
            GAS_BOTTLE,
            RecipeType.HEATED_PRESSURE_CHAMBER,
            new ItemStack[] { SlimefunItems.FUEL_BUCKET }
        ).register(addon);

        // The nine-Gas-Bottle drum recipe now lives in HowitzerRecipeRegistry,
        // which registers it without Slimefun drops so breaking a placed drum
        // cannot duplicate it.

        OIL_REFINERY = refineryStack();
        REFINERY = new OilRefineryItem(GROUP, OIL_REFINERY, RecipeType.ENHANCED_CRAFTING_TABLE, refineryRecipe());
        REFINERY.register(addon);
        Research oilRefineryResearch = createResearch(new NamespacedKey(plugin, "oil_refinery"), 4001,
            "§eOil Refinery", 15, 1500.0);
        oilRefineryResearch.addItems(REFINERY);
        oilRefineryResearch.register();

        FUEL_REFINERY_STACK = fuelRefineryStack();
        FUEL_REFINERY = new FuelRefineryItem(GROUP, FUEL_REFINERY_STACK, RecipeType.ENHANCED_CRAFTING_TABLE, fuelRefineryRecipe());
        FUEL_REFINERY.register(addon);
        Research fuelRefineryResearch = createResearch(new NamespacedKey(plugin, "fuel_refinery"), 4002,
            "§cFuel Refinery", 20, 2500.0);
        fuelRefineryResearch.addItems(FUEL_REFINERY);
        fuelRefineryResearch.register();

        LIQUEFACTION_PLANT = new OilLiquefactionItem(
            GROUP,
            OIL_LIQUEFACTION_PLANT,
            RecipeType.ENHANCED_CRAFTING_TABLE,
            liquefactionRecipe()
        );
        LIQUEFACTION_PLANT.register(addon);
        Research liquefactionResearch = createResearch(
            new NamespacedKey(plugin, "oil_liquefaction"),
            4006,
            "§6Oil Liquefaction",
            24,
            3000.0
        );
        liquefactionResearch.addItems(LIQUEFACTION_PLANT);
        liquefactionResearch.register();

        DOWSING_ROD_ITEM = new DowsingRodItem(GROUP);
        DOWSING_ROD_ITEM.register(addon);

        OIL_DRILL_MARK_I = drillStack(OilDrillTier.MARK_I, Material.PISTON,
            "§7Slow industrial rig.",
            extractionLore(plugin, OilDrillTier.MARK_I));
        DRILL_MARK_I = new OilDrillItem(GROUP, OIL_DRILL_MARK_I,
            RecipeType.ENHANCED_CRAFTING_TABLE, OilDrillItem.recipeMarkI(), OilDrillTier.MARK_I);
        DRILL_MARK_I.register(addon);
        Research drillResearchI = createResearch(new NamespacedKey(plugin, "drill_mark_i"), 4003,
            "§fOil Drilling I", 10, 1000.0);
        drillResearchI.addItems(DRILL_MARK_I);
        drillResearchI.register();

        OIL_DRILL_MARK_II = drillStack(OilDrillTier.MARK_II, Material.STICKY_PISTON,
            "§7Upgraded pump assembly.",
            extractionLore(plugin, OilDrillTier.MARK_II));
        DRILL_MARK_II = new OilDrillItem(GROUP, OIL_DRILL_MARK_II,
            RecipeType.ENHANCED_CRAFTING_TABLE, OilDrillItem.recipeMarkII(OIL_DRILL_MARK_I), OilDrillTier.MARK_II);
        DRILL_MARK_II.register(addon);
        Research drillResearchII = createResearch(new NamespacedKey(plugin, "drill_mark_ii"), 4004,
            "§8Oil Drilling II", 18, 2000.0);
        drillResearchII.addItems(DRILL_MARK_II);
        drillResearchII.register();

        OIL_DRILL_MARK_III = drillStack(OilDrillTier.MARK_III, Material.OBSERVER,
            "§7Front-line extraction unit.",
            extractionLore(plugin, OilDrillTier.MARK_III));
        DRILL_MARK_III = new OilDrillItem(GROUP, OIL_DRILL_MARK_III,
            RecipeType.ENHANCED_CRAFTING_TABLE, OilDrillItem.recipeMarkIII(OIL_DRILL_MARK_II), OilDrillTier.MARK_III);
        DRILL_MARK_III.register(addon);
        Research drillResearchIII = createResearch(new NamespacedKey(plugin, "drill_mark_iii"), 4005,
            "§7Oil Drilling III", 30, 4000.0);
        drillResearchIII.addItems(DRILL_MARK_III);
        drillResearchIII.register();

        ALL_DRILLS.clear();
        ALL_DRILLS.add(DRILL_MARK_I);
        ALL_DRILLS.add(DRILL_MARK_II);
        ALL_DRILLS.add(DRILL_MARK_III);
        DRILL_ITEM = DRILL_MARK_I;

        OilRecipeRegistry.register(plugin);
    }

    private static Research createResearch(
            NamespacedKey key,
            int id,
            String name,
            int levelCost,
            double currencyCost
    ) {
        try {
            return Research.class
                    .getConstructor(NamespacedKey.class, int.class, String.class, int.class, double.class)
                    .newInstance(key, id, name, levelCost, currencyCost);
        } catch (NoSuchMethodException ex) {
            return new Research(key, id, name, levelCost);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("Could not create Slimefun research '" + name + "'", ex);
        }
    }

    private static SlimefunItemStack refineryStack() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Refines §8Crude Oil §7into §6Slimefun Oil Buckets§7.");
        lore.add("§7Recipe: §89x Crude Oil §7→ §61x Oil Bucket");
        lore.add("§7Processing time: §e1 minute");
        lore.add("§7Energy: §e40 J/t §7· §e5,000 J capacity");
        return new SlimefunItemStack(
            "OIL_REFINERY",
            Material.BLAST_FURNACE,
            "§e§lOil Refinery",
            lore.toArray(new String[0])
        );
    }

    private static ItemStack[] refineryRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.REDSTONE_BLOCK), new ItemStack(Material.IRON_BLOCK),
            new ItemStack(Material.BLAST_FURNACE), new ItemStack(Material.COPPER_BLOCK), new ItemStack(Material.BLAST_FURNACE),
            new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.REDSTONE_BLOCK), new ItemStack(Material.IRON_BLOCK)
        };
    }

    private static SlimefunItemStack fuelRefineryStack() {
        List<String> lore = new ArrayList<>();
        lore.add("§7Processes §6Slimefun Oil Buckets §7into §cFuel Buckets§7.");
        lore.add("§7Recipe: §61x Oil Bucket §7→ §c1x Slimefun Fuel Bucket");
        lore.add("§7Processing time: §e1 minute");
        lore.add("§7Energy: §e40 J/t §7· §e10,000 J capacity");
        lore.add("§7Fuel is used to power §eMovecraft vehicles§7.");
        return new SlimefunItemStack(
            "FUEL_REFINERY",
            Material.SMOKER,
            "§c§lFuel Refinery",
            lore.toArray(new String[0])
        );
    }

    private static ItemStack[] fuelRefineryRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.GOLD_BLOCK), new ItemStack(Material.IRON_BLOCK),
            new ItemStack(Material.SMOKER), new ItemStack(Material.BLAST_FURNACE), new ItemStack(Material.SMOKER),
            new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.GOLD_BLOCK), new ItemStack(Material.IRON_BLOCK)
        };
    }

    private static SlimefunItemStack liquefactionStack() {
        return new SlimefunItemStack(
            "OIL_LIQUEFACTION_PLANT",
            Material.BREWING_STAND,
            "§6§lOil Liquefaction Plant",
            "§7Converts §e64 Coal §7into §81 Crude Oil§7.",
            "§7Processing time: §e1 minute",
            "§7Energy: §e200 J/t §7· §e10,000 J capacity"
        );
    }

    private static ItemStack[] liquefactionRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.COAL_BLOCK), new ItemStack(Material.IRON_BLOCK),
            new ItemStack(Material.PISTON), new ItemStack(Material.BLAST_FURNACE), new ItemStack(Material.PISTON),
            new ItemStack(Material.IRON_BLOCK), SlimefunItems.ELECTRIC_MOTOR, new ItemStack(Material.IRON_BLOCK)
        };
    }

    private static ItemStack[] crudeOilSourceGuide() {
        ItemStack[] guide = new ItemStack[9];
        guide[3] = OIL_LIQUEFACTION_PLANT.clone();
        guide[5] = new CustomItemStack(
            Material.PISTON,
            "§eOil Pump",
            "§7Field drills extract crude oil."
        );
        return guide;
    }

    private static String extractionLore(
        org.bukkit.plugin.java.JavaPlugin plugin,
        OilDrillTier tier
    ) {
        int coal = tier.getCoalPerBarrel(plugin.getConfig());
        int seconds = tier.getSeconds(plugin.getConfig());
        return "§7§c" + coal + " coal§7/oil · §e" + seconds + " seconds";
    }

    private static SlimefunItemStack drillStack(OilDrillTier tier, Material material, String... extraLore) {
        List<String> lore = new ArrayList<>();
        lore.add(switch (tier) {
            case MARK_I -> "§7Rig casing: §fstone bricks§7.";
            case MARK_II -> "§7Rig casing: §8deepslate bricks§7.";
            case MARK_III -> "§7Rig casing: §7steel blocks§7.";
        });
        lore.add("§7Place in open space — §aauto-builds§7 the rig.");
        lore.add("§7§eHopper§7 behind drill, §eChest§7 on your right.");
        lore.add("§7Drill piston faces §eup§7 automatically.");
        lore.add("§7Right-click for control panel.");
        Collections.addAll(lore, extraLore);
        return new SlimefunItemStack(
            tier.getItemId(),
            material,
            tier.getDisplayPrefix(),
            lore.toArray(new String[0])
        );
    }
}
