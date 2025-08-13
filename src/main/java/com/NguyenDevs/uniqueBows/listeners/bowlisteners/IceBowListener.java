package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class IceBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public IceBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("ice_bow")) return;

        // Check permission
        if (!player.hasPermission("ub.use")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("no-permission")));
            return;
        }

        // Check cooldown
        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String message = plugin.getConfigManager().getMessages().getString("bow-delay", "&cBạn phải đợi {time} giây nữa mới có thể sử dụng cung này!");
            message = message.replace("{time}", String.valueOf(remaining));
            player.sendMessage(ColorUtils.colorize(message));
            return;
        }

        // Set cooldown
        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);

        // Mark arrow as ice bow arrow
        arrowBowMap.put(event.getEntity().getUniqueId(), bowId);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Arrow arrow = (Arrow) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        String bowId = arrowBowMap.get(arrow.getUniqueId());
        if (bowId == null || !bowId.equals("ice_bow")) return;

        // Remove arrow from map
        arrowBowMap.remove(arrow.getUniqueId());

        // Apply ice effect
        target.setFreezeTicks(200); // 10 seconds (200 ticks)

        // Visual effect
        target.getWorld().spawnParticle(org.bukkit.Particle.SNOWBALL, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    }
}