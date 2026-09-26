package com.wwsf.artillery;

import org.bukkit.inventory.ItemStack;

public record ArtilleryDefinition(ArtillerySpec spec, ItemStack[] recipe) {
}
