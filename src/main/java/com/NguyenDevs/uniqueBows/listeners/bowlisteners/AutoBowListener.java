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

public class AutoBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, Long> chargeTimes = new HashMap<>();

    public AutoBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("auto_bow")) return;

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

        // Calculate charge time (simulate holding bow)
        long currentTime = System.currentTimeMillis();
        Long startTime = chargeTimes.get(player.getUniqueId());
        if (startTime == null) startTime = currentTime - 1000;

        long chargeTime = currentTime - startTime;
        int arrows = Math.min(50, Math.max(5, (int) (chargeTime / 100))); // 5-50 arrows based on charge time

        // Shoot multiple arrows
        for (int i = 0; i < arrows; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Arrow arrow = player.launchProjectile(Arrow.class);
                Vector velocity = player.getLocation().getDirection().multiply(3.0);
                // Add slight randomness
                velocity.add(new Vector(
                        (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1
                ));
                arrow.setVelocity(velocity);
            }, i * 2); // 0.1 second delay between arrows
        }

        chargeTimes.remove(player.getUniqueId());
        event.setCancelled(true); // Cancel the original arrow
    }
}