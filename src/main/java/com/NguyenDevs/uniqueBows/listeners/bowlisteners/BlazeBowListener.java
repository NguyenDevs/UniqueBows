package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Blaze Bow Listener
public class BlazeBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public BlazeBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("blaze_bow")) return;

        if (!player.hasPermission("ub.use")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("no-permission")));
            return;
        }

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String message = plugin.getConfigManager().getMessages().getString("bow-delay", "&cBạn phải đợi {time} giây nữa mới có thể sử dụng cung này!");
            message = message.replace("{time}", String.valueOf(remaining));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Cancel normal arrow
        event.setCancelled(true);
        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);

        // Create flame trail effect
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection();

        new BukkitRunnable() {
            int distance = 0;
            Location current = start.clone();

            @Override
            public void run() {
                if (distance > 30) {
                    this.cancel();
                    return;
                }

                current.add(direction.clone().multiply(0.5));
                current.getWorld().spawnParticle(org.bukkit.Particle.FLAME, current, 5, 0.1, 0.1, 0.1, 0.05);

                // Check for entities hit
                for (Entity entity : current.getWorld().getNearbyEntities(current, 1, 1, 1)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity target = (LivingEntity) entity;
                        target.damage(8.0, player);
                        target.setFireTicks(100);
                        target.getWorld().spawnParticle(org.bukkit.Particle.FLAME, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                        this.cancel();
                        return;
                    }
                }

                distance++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}