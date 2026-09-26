package com.wwsf.support;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.wwsf.WWSFPlugin;
import com.wwsf.config.WWSFItem;
import com.wwsf.setup.SupportCategory;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;

/**
 * Morphine Syringe - High-risk combat stimulant with Rush and Crash phases.
 * Grants temporary combat buffs followed by severe debuffs.
 */
public class MorphineSyringe extends WWSFItem implements Listener {

    public static final String ID = "WWSF_MORPHINE_SYRINGE";
    private static final int CUSTOM_MODEL_DATA = 10011;
    
    private static final int RUSH_DURATION_TICKS = 400; // 20 seconds
    private static final int CRASH_DURATION_TICKS = 240; // 12 seconds
    private static final int SLOWNESS_REFRESH_INTERVAL = 100; // 5 seconds
    
    // Track players currently under morphine influence to prevent overdose
    private static final Set<String> ACTIVE_MORPHINE = new HashSet<>();

    public MorphineSyringe(@Nonnull ItemGroup category) {
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
            Material.GLASS_BOTTLE,
            ChatColor.LIGHT_PURPLE + "Morphine Syringe",
            meta -> {
                com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
                meta.setLore(java.util.Arrays.asList(
                    ChatColor.GRAY + "Combat stimulant - High risk, high reward",
                    "",
                    ChatColor.GREEN + "Rush (20s): " + ChatColor.WHITE + "Speed II, Resistance II",
                    ChatColor.GREEN + "Immunity: " + ChatColor.WHITE + "Slowness",
                    "",
                    ChatColor.RED + "Crash (12s): " + ChatColor.WHITE + "Blindness I, Weakness III",
                    ChatColor.RED + "Overdose: " + ChatColor.WHITE + "Half health + Nausea",
                    "",
                    ChatColor.YELLOW + "Right-click: Self-inject (Shift+Right-click)",
                    ChatColor.YELLOW + "Right-click player: Inject teammate"
                ));
            }
        );
        // Bake custom model data directly into the ItemStack's meta
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            com.wwsf.util.CustomModelDataUtil.apply(meta, CUSTOM_MODEL_DATA);
            meta.setLore(java.util.Arrays.asList(
                ChatColor.GRAY + "Combat stimulant - High risk, high reward",
                "",
                ChatColor.GREEN + "Rush (20s): " + ChatColor.WHITE + "Speed II, Resistance II",
                ChatColor.GREEN + "Immunity: " + ChatColor.WHITE + "Slowness",
                "",
                ChatColor.RED + "Crash (12s): " + ChatColor.WHITE + "Blindness I, Weakness III",
                ChatColor.RED + "Overdose: " + ChatColor.WHITE + "Half health + Nausea",
                "",
                ChatColor.YELLOW + "Right-click: Self-inject (Shift+Right-click)",
                ChatColor.YELLOW + "Right-click player: Inject teammate"
            ));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    @Nonnull
    private ItemUseHandler onUse() {
        return (PlayerRightClickEvent event) -> {
            event.cancel();
            Player user = event.getPlayer();
            ItemStack syringe = event.getItem();
            
            if (syringe == null || syringe.getType() == Material.AIR) {
                return;
            }

            // Check if shift-right-click (self-inject) or right-clicking player
            if (user.isSneaking()) {
                // Self-inject
                applyMorphine(user, user, syringe);
            } else {
                // Default to self-inject on regular right-click
                applyMorphine(user, user, syringe);
            }
        };
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player user = event.getPlayer();
        ItemStack syringe = user.getInventory().getItemInMainHand();
        
        if (syringe == null || syringe.getType() == Material.AIR) {
            return;
        }
        
        // Check if this is a morphine syringe
        SlimefunItem sfItem = SlimefunItem.getByItem(syringe);
        if (sfItem instanceof MorphineSyringe) {
            event.setCancelled(true);
            
            if (event.getRightClicked() instanceof Player target) {
                // Don't self-inject via entity click
                if (!target.equals(user)) {
                    applyMorphine(user, target, syringe);
                }
            }
        }
    }

    private void applyMorphine(Player applier, Player target, ItemStack syringe) {
        String targetId = target.getUniqueId().toString();
        
        // Check for overdose
        if (ACTIVE_MORPHINE.contains(targetId)) {
            // OVERDOSE!
            target.damage(0.001); // Trigger damage sound
            target.setHealth(Math.max(1.0, target.getHealth() / 2)); // Half health
            target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 200, 1, false, true));
            target.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "⚠ OVERDOSE! ⚠");
            target.sendMessage(ChatColor.RED + "Administering morphine while already under its effects causes overdose!");
            target.sendMessage(ChatColor.GRAY + "Health reduced to 50% and severe nausea induced.");
            
            // Consume syringe even on overdose
            consumeSyringe(applier);
            return;
        }

        // Check if target is already at half heart (prevent abuse)
        if (target.getHealth() <= 1.0) {
            applier.sendMessage(ChatColor.RED + "Cannot administer morphine - target is at critical health!");
            return;
        }

        // Mark as under morphine influence
        ACTIVE_MORPHINE.add(targetId);

        // Consume syringe
        consumeSyringe(applier);

        // Apply Rush phase buffs
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, RUSH_DURATION_TICKS, 1, false, true));
        target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, RUSH_DURATION_TICKS, 1, false, true));
        
        // Notify
        target.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "💉 Morphine administered!");
        target.sendMessage(ChatColor.GREEN + "Rush phase: " + ChatColor.WHITE + "20 seconds of enhanced combat ability");
        target.sendMessage(ChatColor.RED + "Warning: " + ChatColor.GRAY + "Severe crash will follow...");
        
        if (applier != target) {
            applier.sendMessage(ChatColor.GREEN + "Administered morphine to " + target.getName());
        }

        // Play sound
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_SPLASH_POTION_THROW, 1.0f, 1.0f);

        // Slowness immunity: Re-apply Speed II every 5 seconds during Rush
        new BukkitRunnable() {
            int elapsed = 0;
            
            @Override
            public void run() {
                if (!target.isOnline() || elapsed >= RUSH_DURATION_TICKS) {
                    cancel();
                    return;
                }
                
                // Refresh Speed II to override any Slowness
                if (target.hasPotionEffect(PotionEffectType.SPEED)) {
                    target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 1, false, true));
                }
                
                elapsed += SLOWNESS_REFRESH_INTERVAL;
            }
        }.runTaskTimer(WWSFPlugin.getInstance(), 0L, SLOWNESS_REFRESH_INTERVAL);

        // Schedule Crash phase
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline()) {
                    ACTIVE_MORPHINE.remove(targetId);
                    return;
                }

                // Apply Crash debuffs
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, CRASH_DURATION_TICKS, 0, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, CRASH_DURATION_TICKS, 2, false, true));
                
                target.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "💥 CRASH!");
                target.sendMessage(ChatColor.RED + "Morphine effects have worn off - severe withdrawal symptoms!");
                target.sendMessage(ChatColor.GRAY + "Blindness and Weakness for 12 seconds...");
                
                target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);

                // Schedule cleanup
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ACTIVE_MORPHINE.remove(targetId);
                        if (target.isOnline()) {
                            target.sendMessage(ChatColor.GRAY + "Morphine effects have fully worn off.");
                        }
                    }
                }.runTaskLater(WWSFPlugin.getInstance(), CRASH_DURATION_TICKS);
            }
        }.runTaskLater(WWSFPlugin.getInstance(), RUSH_DURATION_TICKS);
    }

    private void consumeSyringe(Player user) {
        ItemStack hand = user.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() != Material.AIR) {
            if (hand.getAmount() <= 1) {
                user.getInventory().setItemInMainHand(null);
            } else {
                hand.setAmount(hand.getAmount() - 1);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        // Cancel any ongoing morphine if player dies
        if (event.getEntity() instanceof Player player) {
            if (event.getFinalDamage() >= player.getHealth()) {
                ACTIVE_MORPHINE.remove(player.getUniqueId().toString());
            }
        }
    }

    @Nonnull
    private static ItemStack[] createRecipe() {
        return new ItemStack[] {
            new ItemStack(Material.GLASS_BOTTLE), new ItemStack(Material.SPIDER_EYE), new ItemStack(Material.IRON_NUGGET),
            new ItemStack(Material.SUGAR), null, null,
            null, null, null
        };
    }

    public static void registerStatic(WWSFPlugin plugin) {
        MorphineSyringe syringe = new MorphineSyringe(SupportCategory.SUPPORT);
        syringe.register(plugin);
        plugin.getServer().getPluginManager().registerEvents(syringe, plugin);
    }
}
