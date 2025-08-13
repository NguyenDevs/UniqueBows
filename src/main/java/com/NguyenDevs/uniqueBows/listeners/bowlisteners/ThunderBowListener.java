package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ThunderBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, Long> chargeStart = new HashMap<>();
    private final Map<UUID, Integer> chargeLevels = new HashMap<>();
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    private static final long MAX_CHARGE_TIME = 5000;

    public ThunderBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack bow = event.getItem();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"thunder_bow".equals(bowId)) return;

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        if (chargeStart.containsKey(player.getUniqueId())) return;

        chargeStart.put(player.getUniqueId(), System.currentTimeMillis());
        chargeLevels.put(player.getUniqueId(), 0);
        startChargeEffect(player);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!(arrow.getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"thunder_bow".equals(bowId)) return;

        if (!player.hasPermission("ub.use")) {
            event.setCancelled(true);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            player.sendMessage(ColorUtils.colorize(prefix + " " +
                    plugin.getConfigManager().getMessages().getString("no-permission")));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);
        arrowBowMap.put(arrow.getUniqueId(), bowId);

        player.removePotionEffect(PotionEffectType.SLOW);
        chargeStart.remove(player.getUniqueId());
        chargeLevels.remove(player.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround()) {
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, arrow.getLocation(), 1, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        String bowId = arrowBowMap.get(arrow.getUniqueId());
        if (bowId == null || !"thunder_bow".equals(bowId)) return;

        arrowBowMap.remove(arrow.getUniqueId());

        arrow.getWorld().strikeLightning(arrow.getLocation());
    }

    private void startChargeEffect(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID id = player.getUniqueId();

                if (!chargeStart.containsKey(id) || !player.isOnline() || player.isDead()) {
                    chargeLevels.remove(id);
                    player.removePotionEffect(PotionEffectType.SLOW);
                    cancel();
                    return;
                }

                long chargeTime = System.currentTimeMillis() - chargeStart.get(id);
                if (chargeTime >= MAX_CHARGE_TIME) chargeTime = MAX_CHARGE_TIME;
                int chargePercent = Math.min(100, (int) (chargeTime / (double) MAX_CHARGE_TIME * 100));
                int currentLevel = chargeLevels.getOrDefault(id, 0);
                int newLevel = chargePercent / 20;

                if (newLevel > currentLevel) {
                    chargeLevels.put(id, newLevel);
                    float pitch = 0.5f + (newLevel * 0.3f);
                    player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, pitch);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, newLevel, false, false, false));
                    if (chargePercent >= 100) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                    }
                }

                if (!player.isHandRaised() && player.getGameMode() != GameMode.CREATIVE) {
                    player.removePotionEffect(PotionEffectType.SLOW);
                    chargeStart.remove(id);
                    chargeLevels.remove(id);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }
}
