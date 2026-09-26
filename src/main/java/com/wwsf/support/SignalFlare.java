package com.wwsf.support;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.SupportCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;

public class SignalFlare extends WWSFItem {

    public static final String ID = "WWSF_SIGNAL_FLARE";
    private static final int CUSTOM_MODEL_DATA = 10008;

    public SignalFlare(@Nonnull ItemGroup category) {
        super(category, createStack(), RecipeType.ENHANCED_CRAFTING_TABLE, createRecipe());
    }

    @Override
    public void preRegister() {
        addItemHandler(onUse());
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
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Nonnull
    private static SlimefunItemStack createStack() {
        SlimefunItemStack stack = new SlimefunItemStack(
            ID,
            Material.FIREWORK_ROCKET,
            ChatColor.YELLOW + "Signal Flare",
            meta -> {
                com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
                meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Field support",
                    "",
                    ChatColor.GRAY + "Right-click to launch.",
                    ChatColor.GRAY + "Reveals troops in the area."
                ));
            }
        );
        // Bake custom model data directly into the ItemStack's meta so it survives
        // cloning, serialization, and recipe output creation.
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Field support",
                "",
                ChatColor.GRAY + "Right-click to launch.",
                ChatColor.GRAY + "Reveals troops in the area."
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Nonnull
    private ItemUseHandler onUse() {
        return (PlayerRightClickEvent event) -> {
            event.cancel();
            Player player = event.getPlayer();
            ItemStack hand = event.getItem();
            if (hand.getAmount() <= 1) {
                player.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }
            launch(player);
        };
    }

    private void launch(Player player) {
        WWSFPlugin plugin = WWSFPlugin.getInstance();
        double radius = ItemConfigHelper.getDouble(this, "flare.reveal-radius", 24.0);
        int glowDuration = ItemConfigHelper.getInt(this, "flare.glow-duration", 160);
        int ascentTicks = ItemConfigHelper.getInt(this, "flare.ascent-ticks", 30);
        java.util.UUID shooterId = player.getUniqueId();

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 0.8f);

        new BukkitRunnable() {
            int tick = 0;
            org.bukkit.Location pos = player.getEyeLocation().clone();

            @Override
            public void run() {
                if (tick++ >= ascentTicks) {
                    world.playSound(pos, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0f, 1.2f);
                    world.spawnParticle(Particle.FLASH, pos, 1, 0, 0, 0, 0);
                    revealEntities(world, pos, radius, glowDuration, shooterId);
                    cancel();
                    return;
                }
                pos.add(new Vector(0, 0.65, 0));
                world.spawnParticle(Particle.FLAME, pos, 6, 0.05, 0.05, 0.05, 0.01);
                world.spawnParticle(Particle.LARGE_SMOKE, pos, 2, 0.05, 0.05, 0.05, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void revealEntities(World world, org.bukkit.Location center, double radius, int glowDuration, java.util.UUID shooterId) {
        for (Entity entity : world.getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity living && !living.getUniqueId().equals(shooterId)) {
                living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDuration, 0, false, true));
            }
        }
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.FIREWORK_STAR), new ItemStack(Material.GUNPOWDER), new ItemStack(Material.REDSTONE),
            new ItemStack(Material.PAPER), null, null,
            null, null, null
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new SignalFlare(SupportCategory.SUPPORT).register(plugin);
    }
}
