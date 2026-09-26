package com.wwsf.gas;

import org.bukkit.Location;
import org.bukkit.entity.Egg;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;

import com.wwsf.WWSFPlugin;

public class GasGrenadeListener implements Listener {

    private final WWSFPlugin plugin;

    public GasGrenadeListener(WWSFPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        ItemStack item = null;
        Location impact = null;
        Player thrower = null;

        if (event.getEntity() instanceof Egg egg) {
            item = egg.getItem();
            impact = egg.getLocation();
            thrower = egg.getShooter() instanceof Player p ? p : null;
            egg.remove();
        } else if (event.getEntity() instanceof Snowball snowball) {
            item = snowball.getItem();
            impact = snowball.getLocation();
            thrower = snowball.getShooter() instanceof Player p ? p : null;
            snowball.remove();
        } else {
            return;
        }

        if (item == null || impact == null) {
            return;
        }

        if (GasGrenadeRegistry.isGasBomb(item)) {
            event.setCancelled(true);
            GasEffectService.detonate(plugin, impact, item, thrower);
        }
    }
}
