package com.wwsf.items;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;

/**
 * Pontoon Bridge Kit - deployable bridge that unfolds on water.
 */
public class PontoonBridgeKit extends SlimefunItem {

    public static final String ID = "WWSF_PONTOON_BRIDGE_KIT";
    private static final int CUSTOM_MODEL_DATA = 2001;

    private static final SlimefunItemStack BRIDGE_STACK = createStack();

    public PontoonBridgeKit(@Nonnull ItemGroup category) {
        super(category, BRIDGE_STACK, RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    @Override
    public void preRegister() {
        addItemHandler(onDeploy());
    }

    @Nonnull
    private static SlimefunItemStack createStack() {
        // CRITICAL: Set custom model data INSIDE the Consumer lambda so it's captured
        // in SlimefunItemStack's internal ItemMetaSnapshot. Setting it after construction
        // gets wiped when Slimefun clones the stack for recipe output.
        return new SlimefunItemStack(ID, Material.SNOWBALL, meta -> {
            meta.setDisplayName(ChatColor.GOLD + "Pontoon Bridge Kit");
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Deployable bridge kit",
                "",
                ChatColor.GOLD + "Right-click water to deploy",
                ChatColor.GRAY + "Creates a 5-section bridge"
            ));
        });
    }

    @Override
    public @Nonnull ItemStack getItem() {
        ItemStack item = BRIDGE_STACK.clone();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemUseHandler onDeploy() {
        return (PlayerRightClickEvent event) -> {
            event.cancel();
            Player player = event.getPlayer();
            ItemStack hand = event.getItem();

            if (hand.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }

            Snowball snowball = player.launchProjectile(Snowball.class);
            // Use a fresh item with full meta including custom model data
            ItemStack projItem = BRIDGE_STACK.clone();
            snowball.setItem(projItem);
        };
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.OAK_PLANKS),
            new ItemStack(Material.OAK_PLANKS), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.SLIME_BALL),
            new ItemStack(Material.SLIME_BALL), new ItemStack(Material.IRON_INGOT), new ItemStack(Material.STRING)
        };
    }
}
