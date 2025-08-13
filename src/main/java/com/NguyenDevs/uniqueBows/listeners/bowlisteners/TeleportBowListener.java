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

public class TeleportBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public TeleportBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("teleport_bow")) return;

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

        // Schedule teleport when arrow lands
        new BukkitRunnable() {
            @Override
            public void run() {
                if (event.getEntity().isDead() || event.getEntity().isOnGround()) {
                    Location teleportLoc = event.getEntity().getLocation();
                    teleportLoc.setY(teleportLoc.getY() + 1); // Teleport slightly above ground

                    // Teleport player
                    player.teleport(teleportLoc);

                    // Effects
                    player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation(), 50, 1, 1, 1, 0.1);
                    player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANTMENT_TABLE, player.getLocation(), 30, 1, 1, 1, 0.5);

                    arrowBowMap.remove(event.getEntity().getUniqueId());
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
}
