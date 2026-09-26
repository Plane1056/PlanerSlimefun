package com.oildrills;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

public class FuelHandler implements Listener {

    private static final int FUEL_BUCKET_BURN_TIME = 20000;

    @EventHandler
    public void onFuelBurn(FurnaceBurnEvent event) {
        ItemStack fuel = event.getFuel();
        if (fuel == null) {
            return;
        }

        if (isFuelBucket(fuel)) {
            event.setBurnTime(FUEL_BUCKET_BURN_TIME);
        }
    }

    public static boolean isFuelBucket(ItemStack stack) {
        return stack != null
                && (SlimefunUtils.isItemSimilar(stack, SlimefunItems.FUEL_BUCKET, true)
                || OilItems.LEGACY_FUEL_BUCKET != null
                && SlimefunUtils.isItemSimilar(stack, OilItems.LEGACY_FUEL_BUCKET, true));
    }

    public static boolean isOilBucket(ItemStack stack) {
        return stack != null && SlimefunUtils.isItemSimilar(stack, SlimefunItems.OIL_BUCKET, true);
    }

    public static int getFuelValue(ItemStack stack) {
        if (isFuelBucket(stack)) {
            return 800;
        }
        if (isOilBucket(stack)) {
            return 200;
        }
        return 0;
    }
}
