package com.wwsf.gas;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import com.oildrills.OilItems;
import com.wwsf.WWSFPlugin;
import com.wwsf.setup.ElectricityCategory;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;

/** Registers the single merged Gas Bomb plus its two retired identities. */
public final class GasGrenadeRegistry {

    private GasGrenadeRegistry() {
    }

    public static void register(WWSFPlugin plugin) {
        ItemGroup category = ElectricityCategory.AMMUNITION;
        new GasBomb(category, gasBottle()).register(plugin);
        GasBomb.legacy(category, GasBomb.LEGACY_CHLORINE_ID).register(plugin);
        GasBomb.legacy(category, GasBomb.LEGACY_MUSTARD_ID).register(plugin);
    }

    public static boolean isGasBomb(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return SlimefunItem.getByItem(item) instanceof GasBomb;
    }

    /**
     * Prefers the real registered Gas Bottle so the guide shows its exact
     * appearance; falls back to its stable Slimefun id if the oil items have
     * not been registered yet. Slimefun matches recipe inputs by id either way.
     */
    private static ItemStack gasBottle() {
        return OilItems.GAS_BOTTLE != null
            ? OilItems.GAS_BOTTLE.clone()
            : new SlimefunItemStack("WWSF_GAS_BOTTLE", Material.POTION, "§f§lGas Bottle");
    }
}
