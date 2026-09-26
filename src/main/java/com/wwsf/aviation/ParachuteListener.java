package com.wwsf.aviation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.wwsf.WWSFPlugin;

public class ParachuteListener implements Listener {

    private static ParachuteListener instance;

    private final WWSFPlugin plugin;
    private final Map<UUID, Deployment> activeDeployments = new HashMap<>();
    private final Set<UUID> protectedLandings = new HashSet<>();

    public ParachuteListener(WWSFPlugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static void tryDeploy(Player player, Parachute parachute) {
        if (instance == null) {
            return;
        }
        instance.deploy(player, parachute);
    }

    private void deploy(Player player, Parachute parachute) {
        if (player.isOnGround() || player.getVelocity().getY() > -0.15) {
            return;
        }

        if (activeDeployments.containsKey(player.getUniqueId())) {
            return;
        }

        player.playSound(
            player.getLocation(),
            Sound.ITEM_ELYTRA_FLYING,
            SoundCategory.PLAYERS,
            0.8f,
            0.6f
        );
        player.sendActionBar(com.wwsf.util.Messages.color("&fParachute deployed"));

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(player, parachute), 0L, 1L);
        BukkitTask expiry = plugin.getServer().getScheduler().runTaskLater(
            plugin,
            () -> stop(player, false),
            parachute.deployDurationTicks()
        );
        activeDeployments.put(player.getUniqueId(), new Deployment(task, expiry));
    }

    private void tick(Player player, Parachute parachute) {
        if (!player.isOnline() || player.isOnGround()) {
            stop(player, player.isOnline());
            return;
        }

        Vector velocity = player.getVelocity();
        Location predicted = player.getLocation().clone().add(velocity);
        if (player.collidesAt(predicted)) {
            stopFlightSound(player);
        }
        double targetY = ParachutePhysics.downwardVelocityPerTick(
            parachute.descentSpeedBlocksPerSecond()
        );
        player.setVelocity(new Vector(velocity.getX(), targetY, velocity.getZ()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isOnGround() && activeDeployments.containsKey(player.getUniqueId())) {
            stop(player, true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (activeDeployments.containsKey(playerId) || protectedLandings.remove(playerId)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stop(event.getPlayer(), false);
    }

    private void stop(Player player, boolean protectLanding) {
        UUID playerId = player.getUniqueId();
        Deployment deployment = activeDeployments.remove(playerId);
        if (deployment != null) {
            deployment.tickTask.cancel();
            deployment.expiryTask.cancel();
        }
        stopFlightSound(player);

        if (protectLanding) {
            protectedLandings.add(playerId);
            plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> protectedLandings.remove(playerId),
                2L
            );
        }
    }

    private void stopFlightSound(Player player) {
        player.stopSound(Sound.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS);
    }

    private record Deployment(BukkitTask tickTask, BukkitTask expiryTask) {
    }
}
