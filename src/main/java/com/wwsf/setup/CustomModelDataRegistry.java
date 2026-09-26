package com.wwsf.setup;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.annotation.Nonnull;

import org.bukkit.plugin.java.JavaPlugin;

import io.github.thebusybiscuit.slimefun4.core.services.CustomTextureService;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.config.Config;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.wwsf.util.CustomModelDataUtil;

/**
 * Registers every WWSF item's CustomModelData with Slimefun's {@link CustomTextureService}.
 *
 * <p>Slimefun's {@code SlimefunItemStack} applies a texture via
 * {@code CustomTextureService#setTexture(ItemMeta, id)} which reads the configured model value
 * from {@code item-models.yml}. If an id has no entry (or a value of 0) Slimefun calls
 * {@code ItemMeta#setCustomModelData(null)} and wipes any CMD we set. By injecting our ids with
 * their CMD values into that config before any item is constructed, {@code setTexture} will
 * <em>apply</em> the CMD on construction and on every clone, so the resource pack models work
 * for items given via the guide, recipe output, drops, and direct clones.</p>
 */
public final class CustomModelDataRegistry {

    private CustomModelDataRegistry() {
    }

    private static final Map<String, Integer> MODEL_DATA = new LinkedHashMap<>();

    static {
        // Gun parts
        MODEL_DATA.put("GUN_PART", 1);

        // Gas bomb (merged 2026-07-31); the two retired ids stay mapped so
        // stacks already in player inventories keep their model.
        MODEL_DATA.put("WWSF_GAS_BOMB", 10006);
        MODEL_DATA.put("GAS_GRENADE", 10006);
        MODEL_DATA.put("WWSF_MUSTARD_GAS_BOMB", 10009);

        // Support
        MODEL_DATA.put("WWSF_BANDAGE", 10007);
        MODEL_DATA.put("WWSF_SIGNAL_FLARE", 10008);
        MODEL_DATA.put("WWSF_MORPHINE_SYRINGE", 10011);

        // Fortifications
        MODEL_DATA.put("WWSF_GAS_ALARM_BELL", 10010);
        // Must remain identical to HowitzerArtillery's BARBED_WIRE_COIL output.
        MODEL_DATA.put("WWSF_BARBED_WIRE", 1010);

        // Tools (unique value — was colliding with GAS_ALARM_BELL on 10010)
        MODEL_DATA.put("WWSF_ENTRENCHING_TOOL", 10012);

        // Deployables
        MODEL_DATA.put("WWSF_PONTOON_BRIDGE_KIT", 2001);

        // Ammo shells must match the integer CMD values that AmmoItem applies directly
        // for each ShellType, so Slimefun's config and the resource pack selectors agree.
        MODEL_DATA.put("WWSF_MORTAR_SHELL", 10001);
        MODEL_DATA.put("WWSF_SHRAPNEL_SHELL", 10002);
        MODEL_DATA.put("WWSF_INCENDIARY_SHELL", 10003);
        MODEL_DATA.put("WWSF_AP_SHELL", 10004);
        MODEL_DATA.put("WWSF_CLUSTER_SHELL", 10005);
        MODEL_DATA.put("WWSF_SMOKE_SHELL", 10006);
        MODEL_DATA.put("WWSF_MACHINE_GUN_AMMO", 10007);

        // Components
        MODEL_DATA.put("WWSF_SHELL_CASING", 10020);
    }

    static Map<String, Integer> registeredModelData() {
        return Map.copyOf(MODEL_DATA);
    }

    public static void registerAll(@Nonnull JavaPlugin plugin) {
        CustomTextureService service = Slimefun.getItemTextureService();
        if (service == null) {
            plugin.getLogger().warning("CustomTextureService unavailable — custom model data will not be applied.");
            return;
        }

        try {
            Field configField = CustomTextureService.class.getDeclaredField("config");
            configField.setAccessible(true);
            Config config = (Config) configField.get(service);

            for (Map.Entry<String, Integer> entry : MODEL_DATA.entrySet()) {
                config.setValue(entry.getKey(), entry.getValue());
            }

            config.save();
            plugin.getLogger().info("Registered " + MODEL_DATA.size() + " custom model data values with Slimefun.");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to register custom model data with Slimefun", e);
        }
    }

    public static ItemStack applyCustomModelData(@Nonnull ItemStack item, int modelData) {
        if (item == null || modelData <= 0) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            CustomModelDataUtil.apply(meta, modelData);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createWithModelData(@Nonnull org.bukkit.Material material, int modelData) {
        return applyCustomModelData(new ItemStack(material), modelData);
    }

    public static ItemStack createWithModelData(@Nonnull org.bukkit.inventory.ItemStack item, int modelData) {
        return applyCustomModelData(item.clone(), modelData);
    }
}
