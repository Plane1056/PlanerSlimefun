package com.wwsf.gas;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.WWSFItem;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;

/**
 * The single throwable chemical weapon.
 *
 * <p>Design decision: the separate chlorine grenade and mustard bomb
 * are merged into one Gas Bomb. Its cloud is HowitzerArtillery's real
 * concentration-cell simulation at a configurable cell count, so masks, flow,
 * decay, corrosion and vacuums all behave exactly as they do for a gas shell.
 * The two retired identities stay registered as hidden items with no recipe so
 * copies already in player inventories keep working.</p>
 */
public class GasBomb extends WWSFItem {

    public static final String ID = "WWSF_GAS_BOMB";
    /** Retired chlorine grenade id, kept only so existing stacks still throw. */
    public static final String LEGACY_CHLORINE_ID = "GAS_GRENADE";
    /** Retired mustard bomb id, kept only so existing stacks still throw. */
    public static final String LEGACY_MUSTARD_ID = "WWSF_MUSTARD_GAS_BOMB";

    public static final int CUSTOM_MODEL_DATA = 10006;
    private static final Set<UUID> COOLDOWN = new HashSet<>();
    private static final long COOLDOWN_TICKS = 200; // 10 seconds

    private final int customModelData;

    public GasBomb(@Nonnull ItemGroup category, @Nonnull ItemStack gasBottle) {
        super(
            category,
            createStack(ID, ChatColor.GREEN + "Gas Bomb", CUSTOM_MODEL_DATA),
            RecipeType.ENHANCED_CRAFTING_TABLE,
            starRecipe(gasBottle)
        );
        this.customModelData = CUSTOM_MODEL_DATA;
    }

    private GasBomb(
        @Nonnull ItemGroup category,
        @Nonnull String id,
        @Nonnull String displayName,
        int customModelData
    ) {
        super(
            category,
            createStack(id, displayName, customModelData),
            RecipeType.NULL,
            new ItemStack[9]
        );
        this.customModelData = customModelData;
        setHidden(true);
    }

    /**
     * The retired identities. They carry no recipe and never appear in the
     * guide; they exist purely so a legacy stack is still a live gas bomb.
     */
    @Nonnull
    public static GasBomb legacy(@Nonnull ItemGroup category, @Nonnull String id) {
        return switch (id) {
            case LEGACY_CHLORINE_ID -> new GasBomb(
                category, LEGACY_CHLORINE_ID, ChatColor.GREEN + "Gas Bomb", 10006);
            case LEGACY_MUSTARD_ID -> new GasBomb(
                category, LEGACY_MUSTARD_ID, ChatColor.GREEN + "Gas Bomb", 10009);
            default -> throw new IllegalArgumentException("Unknown legacy gas bomb id: " + id);
        };
    }

    @Override
    public void preRegister() {
        addItemHandler(onThrow());
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
            // The field is still 0 if Slimefun calls this during construction.
            com.wwsf.util.CustomModelDataUtil.apply(
                meta, customModelData > 0 ? customModelData : CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    private static SlimefunItemStack createStack(
        @Nonnull String id,
        @Nonnull String displayName,
        int customModelData
    ) {
        SlimefunItemStack stack = new SlimefunItemStack(
            id, Material.EGG, displayName, meta -> applyPresentation(meta, customModelData));
        // Bake the presentation into the stack itself so it survives cloning,
        // serialisation and recipe-output creation.
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            applyPresentation(meta, customModelData);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    private static void applyPresentation(ItemMeta meta, int customModelData) {
        com.wwsf.util.CustomModelDataUtil.apply(meta, customModelData);
        meta.setLore(Arrays.asList(
            ChatColor.GRAY + "Throwable chemical weapon",
            "",
            ChatColor.GREEN + "Releases a real drifting gas cloud",
            ChatColor.GRAY + "A gas mask or full armour keeps you alive",
            ChatColor.GRAY + "Right-click in the air to throw."
        ));
    }

    @Nonnull
    private ItemUseHandler onThrow() {
        return (PlayerRightClickEvent event) -> {
            if (event.getClickedBlock().isPresent()) {
                return;
            }

            event.cancel();
            Player player = event.getPlayer();
            UUID playerId = player.getUniqueId();

            if (COOLDOWN.contains(playerId)) {
                player.sendMessage(ChatColor.RED + "You can't throw another gas bomb yet!");
                return;
            }

            COOLDOWN.add(playerId);
            new org.bukkit.scheduler.BukkitRunnable() {
                @Override
                public void run() {
                    COOLDOWN.remove(playerId);
                }
            }.runTaskLater(WWSFPlugin.getInstance(), COOLDOWN_TICKS);

            ItemStack hand = event.getItem();
            if (hand.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }

            Egg egg = player.launchProjectile(Egg.class);
            egg.setItem(getItem().clone());
        };
    }

    /** Star arms: north, west, east and south of the centre slot. */
    public static final int[] STAR_ARM_SLOTS = {1, 3, 5, 7};
    /** The TNT slot. */
    public static final int STAR_CENTRE_SLOT = 4;
    /** Deliberately empty corners. */
    public static final int[] STAR_EMPTY_SLOTS = {0, 2, 6, 8};

    /**
     * One TNT in the centre with four gas bottles in a star: north, west, east
     * and south. The four corners stay empty.
     */
    @Nonnull
    public static ItemStack[] starRecipe(@Nonnull ItemStack gasBottle) {
        ItemStack[] recipe = new ItemStack[9];
        for (int arm : STAR_ARM_SLOTS) {
            recipe[arm] = gasBottle.clone();
        }
        recipe[STAR_CENTRE_SLOT] = new ItemStack(Material.TNT);
        return recipe;
    }
}
