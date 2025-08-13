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
import org.bukkit.entity.Entity;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PoisonBowListener implements Listener {

    private static final String BOW_ID = "poison_bow";
    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public PoisonBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!BOW_ID.equals(bowId)) return;

        event.setCancelled(true);
        event.setConsumeItem(false); // Không trừ mũi tên

        UUID playerId = player.getUniqueId();
        String prefix = plugin.getConfigManager().getMessages().getString("prefix");

        if (!player.hasPermission("ub.use")) {
            player.sendMessage(ColorUtils.colorize(prefix + " " +
                    plugin.getConfigManager().getMessages().getString("no-permission")));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        FileConfiguration bows = plugin.getConfigManager().getBows();
        int delay = bows.getInt(bowId + ".delay", 4);

        if (plugin.getBowManager().isOnCooldown(playerId, bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(playerId, bowId);
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        // Không cần check hoặc trừ mũi tên
        shootPoisonArrow(player, bowId);
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

        event.setCancelled(true);
    }

    private void shootPoisonArrow(Player player, String bowId) {
        Arrow arrow = player.getWorld().spawnArrow(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                3.0f,
                1.0f
        );
        arrow.setShooter(player);
        arrow.setMetadata("poison_bow", new FixedMetadataValue(plugin, true));
        arrowBowMap.put(arrow.getUniqueId(), bowId);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.5f, 1.0f);

        // Trail: chỉ 1 loại particle, spawn liên tục ở phía sau mũi tên
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead()) {
                    arrowBowMap.remove(arrow.getUniqueId());
                    cancel();
                    return;
                }
                Location behind = arrow.getLocation().clone().subtract(arrow.getVelocity().normalize().multiply(0.3));
                arrow.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, behind, 1, 0, 0, 0, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata("poison_bow")) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        String bowId = arrowBowMap.remove(arrow.getUniqueId());
        if (bowId == null) return;

        FileConfiguration bows = plugin.getConfigManager().getBows();
        int poisonDuration = bows.getInt(bowId + ".poison-duration", 5) * 20;
        double radius = 4.0;

        Location impactLocation = arrow.getLocation();
        if (event.getHitEntity() != null) {
            impactLocation = event.getHitEntity().getLocation();
        } else if (event.getHitBlock() != null) {
            impactLocation = event.getHitBlock().getLocation().add(0.5, 0, 0.5);
        }

        applyPoisonInArea(impactLocation, radius, poisonDuration, player);
        startPoisonAreaEffect(impactLocation, radius, poisonDuration, player);

        arrow.getWorld().playSound(impactLocation, Sound.ENTITY_SPLASH_POTION_BREAK, 0.5f, 1.0f);
        arrow.remove();
    }

    private void applyPoisonInArea(Location center, double radius, int poisonDuration, Player shooter) {
        for (Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
            if (entity instanceof LivingEntity target && entity != shooter) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, 1, false, true));
            }
        }
    }

    private void startPoisonAreaEffect(Location center, double radius, int duration, Player shooter) {
        new BukkitRunnable() {
            int ticksRemaining = duration;
            @Override
            public void run() {
                if (ticksRemaining <= 0) {
                    cancel();
                    return;
                }
                applyPoisonInArea(center, radius, duration, shooter);
                center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, center, 10, radius / 2, 0.5, radius / 2, 0);
                ticksRemaining -= 20;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
