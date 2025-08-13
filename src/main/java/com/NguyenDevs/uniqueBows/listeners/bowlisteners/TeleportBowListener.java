package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, UUID> arrowShooter = new HashMap<>(); // arrowId -> playerId

    public TeleportBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    private boolean isTeleportBow(ItemStack bow) {
        if (bow == null) return false;
        String id = plugin.getBowManager().getCustomBowId(bow);
        return "teleport_bow".equals(id);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isTeleportBow(event.getBow())) return;

        String bowId = "teleport_bow";
        String prefix = plugin.getConfigManager().getMessages().getString("prefix");

        if (!player.hasPermission("ub.use")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.colorize(prefix + " " +
                    plugin.getConfigManager().getMessages().getString("no-permission")));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        event.setConsumeItem(false);

        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.5f, 1.0f);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;
        if (!isTeleportBow(player.getInventory().getItemInMainHand())) return;

        arrowShooter.put(arrow.getUniqueId(), player.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround()) {
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.SPELL_WITCH, arrow.getLocation(), 1, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        UUID shooterId = arrowShooter.remove(arrow.getUniqueId());
        if (shooterId == null) return;

        Player player = plugin.getServer().getPlayer(shooterId);
        if (player == null) return;

        Location loc = arrow.getLocation().clone();
        loc.setY(loc.getY() + 1);
        player.teleport(loc);

        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 50, 1, 1, 1, 0.1);
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, loc, 30, 1, 1, 1, 0.5);

        arrow.remove();
    }
}
