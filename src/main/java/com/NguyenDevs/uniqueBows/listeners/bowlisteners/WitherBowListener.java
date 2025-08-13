package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class WitherBowListener implements Listener {

    private final UniqueBows plugin;

    public WitherBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("wither_bow")) return;

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

        // Spawn wither skull
        WitherSkull skull = player.getWorld().spawn(player.getEyeLocation(), WitherSkull.class);
        skull.setShooter(player);
        skull.setCharged(false);

        // Set velocity based on player's looking direction
        Vector direction = player.getLocation().getDirection().multiply(2.0);
        skull.setVelocity(direction);
    }
}