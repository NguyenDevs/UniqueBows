package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
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
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BlazeBowListener implements Listener {

    private static final String BOW_ID = "blaze_bow";

    private final UniqueBows plugin;

    public BlazeBowListener(UniqueBows plugin) {
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
        event.setConsumeItem(false); // Không trừ mũi tên khi bắn bằng Blaze Bow

        UUID playerId = player.getUniqueId();
        if (!player.hasPermission("ub.use")) {
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("no-permission");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        // Kiểm tra cooldown
        if (checkAndNotifyCooldown(player, bowId)) {
            return;
        }

        // Nếu không ở chế độ sáng tạo thì phải có ít nhất 1 mũi tên mới bắn được
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && !hasAnyArrow(player)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        // Tiêu hao 1 mũi tên nếu không ở chế độ sáng tạo
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
            if (!consumeOneArrow(player)) {
                return;
            }
        }

        shootBlazeBeam(player, bowId);
        plugin.getBowManager().setCooldown(playerId, bowId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!BOW_ID.equals(bowId)) return;

        // Chặn bắn nếu đang cooldown
        if (checkAndNotifyCooldown(player, bowId)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true); // Blaze Bow không bắn mũi tên thật
    }

    private void shootBlazeBeam(Player player, String bowId) {
        FileConfiguration bows = plugin.getConfigManager().getBows();
        double damage = bows.getDouble(bowId + ".damage", 8.0);
        int burnDuration = bows.getInt(bowId + ".burn-duration", 5) * 20; // giây → ticks

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1.0f, 1.0f);

        double maxDistance = 30.0;
        double stepSize = 0.5;
        Vector direction = player.getLocation().getDirection().normalize();
        Vector currentPos = player.getEyeLocation().toVector();

        RayTraceResult result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                direction,
                maxDistance,
                0.5,
                entity -> entity instanceof LivingEntity && entity != player
        );

        for (double distance = 0; distance <= maxDistance; distance += stepSize) {
            currentPos.add(direction.clone().multiply(stepSize));
            player.getWorld().spawnParticle(Particle.FLAME, currentPos.getX(), currentPos.getY(), currentPos.getZ(), 5, 0.1, 0.1, 0.1, 0.02);
            player.getWorld().spawnParticle(Particle.SMOKE_NORMAL, currentPos.getX(), currentPos.getY(), currentPos.getZ(), 3, 0.1, 0.1, 0.1, 0.01);

            if (result != null && result.getHitEntity() != null) {
                double hitDistance = player.getEyeLocation().distance(result.getHitEntity().getLocation());
                if (distance >= hitDistance) {
                    LivingEntity target = (LivingEntity) result.getHitEntity();
                    target.damage(damage, player);
                    target.setFireTicks(burnDuration);
                    break;
                }
            }
        }

        player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.05);
    }

    private boolean hasAnyArrow(Player player) {
        return player.getInventory().contains(Material.ARROW) ||
                player.getInventory().contains(Material.TIPPED_ARROW) ||
                player.getInventory().contains(Material.SPECTRAL_ARROW);
    }

    private boolean consumeOneArrow(Player player) {
        List<Material> order = Arrays.asList(Material.ARROW, Material.TIPPED_ARROW, Material.SPECTRAL_ARROW);
        for (Material m : order) {
            int first = player.getInventory().first(m);
            if (first >= 0) {
                ItemStack stack = player.getInventory().getItem(first);
                if (stack != null && stack.getAmount() > 0) {
                    if (stack.getAmount() == 1) {
                        player.getInventory().setItem(first, null);
                    } else {
                        stack.setAmount(stack.getAmount() - 1);
                    }
                    player.updateInventory();
                    return true;
                }
            }
        }
        return false;
    }
}
