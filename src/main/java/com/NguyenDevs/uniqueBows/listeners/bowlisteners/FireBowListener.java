package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FireBowListener implements Listener {

    private static final String BOW_ID = "fire_bow";
    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public FireBowListener(UniqueBows plugin) {
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

        shootFireArrow(player, bowId);
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

        event.setCancelled(true); // Fire Bow không bắn arrow mặc định
    }

    private void shootFireArrow(Player player, String bowId) {
        Arrow arrow = player.getWorld().spawnArrow(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                3.0f,
                1.0f
        );
        arrow.setShooter(player);
        arrow.setMetadata("fire_bow", new FixedMetadataValue(plugin, true));
        arrowBowMap.put(arrow.getUniqueId(), bowId);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.5f, 1.0f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.05);

        // Flame trail
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead()) {
                    arrowBowMap.remove(arrow.getUniqueId());
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.FLAME, arrow.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                arrow.getWorld().spawnParticle(Particle.SMOKE_NORMAL, arrow.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata("fire_bow")) return;

        String bowId = arrowBowMap.remove(arrow.getUniqueId());
        if (bowId == null) return;

        Location hitLoc = arrow.getLocation();

        // Nếu trúng entity
        if (event.getHitEntity() instanceof LivingEntity target) {
            FileConfiguration bows = plugin.getConfigManager().getBows();
            int burnDuration = bows.getInt(bowId + ".burn-duration", 5) * 20;
            target.setFireTicks(burnDuration);
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_BLAZE_BURN, 0.5f, 1.0f);
            target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
            hitLoc = target.getLocation();
        }

        arrow.remove();

        // Gọi hàm tạo vòng lửa bán kính 3 block
        createExpandingFire(hitLoc, 3);
    }

    private void createExpandingFire(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            double currentRadius = 0;

            @Override
            public void run() {
                if (currentRadius > radius) {
                    cancel();
                    return;
                }

                for (double angle = 0; angle < 360; angle += 10) {
                    double rad = Math.toRadians(angle);
                    double x = center.getX() + Math.cos(rad) * currentRadius;
                    double z = center.getZ() + Math.sin(rad) * currentRadius;
                    Location loc = new Location(world, x, center.getY(), z);

                    Block blockBelow = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
                    if (blockBelow.getType().isSolid()) {
                        Location fireLoc = blockBelow.getLocation().add(0, 1, 0);
                        if (world.getBlockAt(fireLoc).getType() == Material.AIR) {
                            world.getBlockAt(fireLoc).setType(Material.FIRE);
                        }
                    }
                }

                currentRadius += 1; // Lửa lan ra 1 block mỗi tick chạy
            }
        }.runTaskTimer(plugin, 0L, 5L); // 5 ticks giữa mỗi vòng => hiệu ứng lan
    }
}
