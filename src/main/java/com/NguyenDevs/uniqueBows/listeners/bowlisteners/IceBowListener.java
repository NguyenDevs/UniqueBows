package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class IceBowListener implements Listener {

    private static final String BOW_ID = "ice_bow";
    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public IceBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    private boolean checkAndNotifyCooldown(Player player, String bowId) {
        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!BOW_ID.equals(bowId)) return;

        event.setCancelled(true);
        event.setConsumeItem(false); // không trừ arrow

        if (!player.hasPermission("ub.use")) {
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("no-permission");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        if (checkAndNotifyCooldown(player, bowId)) {
            return;
        }

        shootIceArrow(player, bowId);
        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!BOW_ID.equals(bowId)) return;

        if (checkAndNotifyCooldown(player, bowId)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true); // Ice Bow không bắn arrow mặc định
    }

    private void shootIceArrow(Player player, String bowId) {
        Arrow arrow = player.getWorld().spawnArrow(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                3.0f,
                1.0f
        );
        arrow.setShooter(player);
        arrow.setMetadata("ice_bow", new FixedMetadataValue(plugin, true));
        arrowBowMap.put(arrow.getUniqueId(), bowId);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f);

        // Trail đơn giản: 1 particle sau đuôi mũi tên
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead()) {
                    arrowBowMap.remove(arrow.getUniqueId());
                    cancel();
                    return;
                }
                Location behind = arrow.getLocation().clone().subtract(arrow.getVelocity().normalize().multiply(0.3));
                arrow.getWorld().spawnParticle(Particle.SNOWFLAKE, behind, 1, 0, 0, 0, 0.0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata("ice_bow")) return;

        String bowId = arrowBowMap.remove(arrow.getUniqueId());
        if (bowId == null) return;

        FileConfiguration bows = plugin.getConfigManager().getBows();
        int freezeTicks = Math.max(0, bows.getInt(bowId + ".freeze-duration", 5)) * 20 + 200;

        if (event.getHitEntity() instanceof LivingEntity target) {
            target.setFreezeTicks(freezeTicks);
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.5f, 1.0f);
            target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
        }

        arrow.remove();
    }
}
