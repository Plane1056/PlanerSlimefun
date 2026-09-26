package com.wwsf.util;

import java.util.List;

import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

public final class CustomModelDataUtil {

    private CustomModelDataUtil() {
    }

    public static void apply(org.bukkit.inventory.meta.ItemMeta meta, int modelData) {
        if (meta != null && modelData > 0) {
            CustomModelDataComponent component = meta.getCustomModelDataComponent();
            component.setFloats(List.of((float) modelData));
            meta.setCustomModelDataComponent(component);
        }
    }

    public static void apply(org.bukkit.inventory.ItemStack stack, int modelData) {
        if (stack == null || modelData <= 0) {
            return;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            apply(meta, modelData);
            stack.setItemMeta(meta);
        }
    }
}
