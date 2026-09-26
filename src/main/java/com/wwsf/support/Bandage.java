package com.wwsf.support;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.ItemConfigHelper;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.SupportCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;

public class Bandage extends WWSFItem implements Listener {

    public static final String ID = "WWSF_BANDAGE";
    private static final int CUSTOM_MODEL_DATA = 10007;
    
    private static final long CHANNEL_DURATION_TICKS = 50; // 2.5 seconds
    private static final long PROGRESS_UPDATE_INTERVAL = 2; // Update every 2 ticks
    private static final Map<UUID, ChannelSession> CHANNELING = new HashMap<>();

    public Bandage(@Nonnull ItemGroup category) {
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
            Material.PAPER,
            ChatColor.WHITE + "Field Bandage",
            meta -> {
                com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
                meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Field support",
                    "",
                    ChatColor.GRAY + "Right-click to dress wounds.",
                    ChatColor.GRAY + "Hold still for 2.5 seconds to heal."
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
                ChatColor.GRAY + "Right-click to dress wounds.",
                ChatColor.GRAY + "Hold still for 2.5 seconds to heal."
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
            UUID playerId = player.getUniqueId();

            // Check if already channeling
            if (CHANNELING.containsKey(playerId)) {
                player.sendMessage(ChatColor.RED + "You're already bandaging!");
                return;
            }

            double healAmount = ItemConfigHelper.getDouble(this, "bandage.heal-amount", 4.0);
            double maxHealth = player.getMaxHealth();
            
            if (player.getHealth() >= maxHealth) {
                return;
            }

            // Check if player has bandage in hand
            ItemStack hand = event.getItem();
            if (hand == null || hand.getType() == Material.AIR) {
                return;
            }

            // Start channeling
            ChannelSession session = new ChannelSession(player, hand.clone(), healAmount);
            CHANNELING.put(playerId, session);
            session.start();
        };
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            UUID playerId = player.getUniqueId();
            ChannelSession session = CHANNELING.get(playerId);
            if (session != null) {
                session.cancel("You took damage!");
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        ChannelSession session = CHANNELING.get(playerId);
        
        if (session != null && !session.isCompleted()) {
            // Check if player moved more than 0.5 blocks from start position
            Location from = session.getStartLocation();
            Location to = player.getLocation();
            
            if (from != null && to != null) {
                double distance = from.distance(to);
                if (distance > 0.5) {
                    session.cancel("You moved too much!");
                }
            }
        }
    }

    private static class ChannelSession {
        private final Player player;
        private final ItemStack bandage;
        private final double healAmount;
        private final Location startLocation;
        private BukkitRunnable task;
        private boolean completed = false;
        private boolean cancelled = false;

        public ChannelSession(Player player, ItemStack bandage, double healAmount) {
            this.player = player;
            this.bandage = bandage;
            this.healAmount = healAmount;
            this.startLocation = player.getLocation().clone();
        }

        public Location getStartLocation() {
            return startLocation;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void start() {
            task = new BukkitRunnable() {
                int elapsed = 0;
                int progressUpdates = 0;

                @Override
                public void run() {
                    if (cancelled || !player.isOnline()) {
                        cleanup();
                        return;
                    }

                    elapsed += PROGRESS_UPDATE_INTERVAL;
                    double progress = (double) elapsed / CHANNEL_DURATION_TICKS;

                    // Update progress bar every tick
                    if (elapsed % PROGRESS_UPDATE_INTERVAL == 0) {
                        sendProgressBar(progress);
                    }

                    if (elapsed >= CHANNEL_DURATION_TICKS) {
                        complete();
                    }
                }
            };
            task.runTaskTimer(WWSFPlugin.getInstance(), 0L, PROGRESS_UPDATE_INTERVAL);
        }

        private void sendProgressBar(double progress) {
            int percent = (int) (progress * 100);
            int filledBars = (int) (progress * 10);
            StringBuilder bar = new StringBuilder();
            
            for (int i = 0; i < 10; i++) {
                if (i < filledBars) {
                    bar.append(ChatColor.GREEN).append("█");
                } else {
                    bar.append(ChatColor.GRAY).append("░");
                }
            }
            
            player.sendActionBar(ChatColor.YELLOW + "Bandaging... " + ChatColor.WHITE + bar.toString() + ChatColor.GRAY + " " + percent + "%");
        }

        public void complete() {
            if (completed || cancelled) {
                return;
            }

            completed = true;
            CHANNELING.remove(player.getUniqueId());

            // Consume bandage
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand != null && hand.getType() != Material.AIR) {
                if (hand.getAmount() <= 1) {
                    player.getInventory().setItemInMainHand(null);
                } else {
                    hand.setAmount(hand.getAmount() - 1);
                }
            }

            // Apply heal
            double maxHealth = player.getMaxHealth();
            player.setHealth(Math.min(maxHealth, player.getHealth() + healAmount));
            
            // Play sound
            player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 1.0f, 1.2f);
            player.sendMessage(ChatColor.GREEN + "Bandage applied!");
            
            cleanup();
        }

        public void cancel(String reason) {
            if (completed || cancelled) {
                return;
            }

            cancelled = true;
            CHANNELING.remove(player.getUniqueId());
            
            if (player.isOnline()) {
                player.sendMessage(ChatColor.RED + "Bandage interrupted! " + reason);
                player.sendActionBar(ChatColor.RED + "Bandage interrupted!");
            }
            
            cleanup();
        }

        private void cleanup() {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.WHITE_WOOL), new ItemStack(Material.WHITE_WOOL), new ItemStack(Material.STRING),
            null, null, null,
            null, null, null
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        new Bandage(SupportCategory.SUPPORT).register(plugin);
        plugin.getServer().getPluginManager().registerEvents(new Bandage(SupportCategory.SUPPORT), plugin);
    }
}
