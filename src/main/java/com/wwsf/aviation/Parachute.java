package com.wwsf.aviation;

import javax.annotation.Nonnull;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.AviationCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;

/**
 * WW1-style parachute — right-click mid-air to slow descent.
 */
public class Parachute extends WWSFItem {

    public static final String ID = "WWSF_PARACHUTE";

    public Parachute(@Nonnull ItemGroup category) {
        super(category, new SlimefunItemStack(
            ID,
            Material.ELYTRA,
            "&fParachute Pack",
            "&8Early aviation",
            "",
            "&7Right-click while falling to deploy.",
            "&7Descends at about 5 blocks per second."
        ), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    @Override
    public void preRegister() {
        addItemHandler(onDeploy());
    }

    @Nonnull
    private ItemUseHandler onDeploy() {
        return (PlayerRightClickEvent event) -> {
            event.cancel();
            ParachuteListener.tryDeploy(event.getPlayer(), this);
        };
    }

    public double descentSpeedBlocksPerSecond() {
        double configured = ItemConfigHelper.getDouble(
            this,
            "parachute.descent-speed-blocks-per-second",
            ParachutePhysics.DEFAULT_DESCENT_BLOCKS_PER_SECOND
        );
        return Double.isFinite(configured) && configured > 0.0
            ? configured
            : ParachutePhysics.DEFAULT_DESCENT_BLOCKS_PER_SECOND;
    }

    public int deployDurationTicks() {
        return ItemConfigHelper.getInt(this, "parachute.deploy-duration-ticks", 600);
    }
    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.STRING), SlimefunItems.CLOTH, new ItemStack(Material.STRING),
            SlimefunItems.CLOTH, new ItemStack(Material.LEATHER), SlimefunItems.CLOTH,
            new ItemStack(Material.STRING), SlimefunItems.CLOTH, new ItemStack(Material.STRING)
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new Parachute(AviationCategory.AVIATION).register(plugin);
    }
}
