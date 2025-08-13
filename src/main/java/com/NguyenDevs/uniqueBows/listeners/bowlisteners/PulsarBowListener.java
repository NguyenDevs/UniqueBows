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

public class PulsarBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public PulsarBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("pulsar_bow")) return;

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

        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);
        arrowBowMap.put(event.getEntity().getUniqueId(), bowId);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;

        Arrow arrow = (Arrow) event.getDamager();
        String bowId = arrowBowMap.get(arrow.getUniqueId());
        if (bowId == null || !bowId.equals("pulsar_bow")) return;

        arrowBowMap.remove(arrow.getUniqueId());

        Location center = arrow.getLocation();

        // Create black hole effect
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > 100) { // 5 seconds
                    this.cancel();
                    return;
                }

                // Visual effects
                center.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_LARGE, center, 20, 2, 2, 2, 0.1);
                center.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, center, 30, 1, 1, 1, 0.5);

                // Pull entities towards center
                for (Entity entity : center.getWorld().getNearbyEntities(center, 10, 10, 10)) {
                    if (entity instanceof LivingEntity && !(entity instanceof Player && ((Player) entity).hasPermission("ub.admin"))) {
                        Vector pullDirection = center.toVector().subtract(entity.getLocation().toVector()).normalize();
                        entity.setVelocity(pullDirection.multiply(0.5));
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
