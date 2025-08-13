package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WitherBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, Long> chargeStart = new HashMap<>();
    private final Map<UUID, Integer> chargeLevels = new HashMap<>();

    private static final long MAX_CHARGE_TIME = 6000L; // 6s

    public WitherBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return; // tránh tay phụ

        Player player = event.getPlayer();
        ItemStack bow = event.getItem();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"wither_bow".equals(bowId)) return;

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            event.setCancelled(true);
            return;
        }

        UUID id = player.getUniqueId();
        if (chargeStart.containsKey(id)) return;

        chargeStart.put(id, System.currentTimeMillis());
        chargeLevels.put(id, 0);
        startChargeEffect(player);
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"wither_bow".equals(bowId)) return;

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

        Long start = chargeStart.remove(player.getUniqueId());
        chargeLevels.remove(player.getUniqueId());
        player.removePotionEffect(PotionEffectType.SLOW);

        float charge01 = 1.0f;
        if (start != null) {
            long dur = System.currentTimeMillis() - start;
            if (dur < 0) dur = 0;
            if (dur > MAX_CHARGE_TIME) dur = MAX_CHARGE_TIME;
            charge01 = (float) dur / (float) MAX_CHARGE_TIME;
        }

        event.setCancelled(true);
        int skullCount = Math.min(3, Math.max(1, Math.round(1 + charge01 * 2)));
        double speed = 1.4 + 0.8 * charge01;

        new BukkitRunnable() {
            int fired = 0;

            @Override public void run() {
                if (!player.isOnline() || player.isDead()) { cancel(); return; }
                if (fired >= skullCount) { cancel(); return; }

                WitherSkull skull = player.getWorld().spawn(player.getEyeLocation(), WitherSkull.class);
                skull.setShooter(player);
                skull.setCharged(false);
                Vector dir = player.getLocation().getDirection().normalize().multiply(speed);
                skull.setVelocity(dir);

                player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getEyeLocation(), 4, 0.1, 0.1, 0.1, 0.01);

                fired++;
            }
        }.runTaskTimer(plugin, 0L, 6L);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof WitherSkull skull)) return;
        if (!(skull.getShooter() instanceof Player shooter)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack main = shooter.getInventory().getItemInMainHand();
        String bowId = plugin.getBowManager().getCustomBowId(main);
        if (!"wither_bow".equals(bowId)) return;

        int witherTicks = Math.max(0, getBowInt(bowId, "wither-duration", 5)) * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, witherTicks, 1, false, true, true));
    }

    private void startChargeEffect(Player player) {
        new BukkitRunnable() {
            @Override public void run() {
                UUID id = player.getUniqueId();

                if (!chargeStart.containsKey(id) || !player.isOnline() || player.isDead()) {
                    chargeLevels.remove(id);
                    player.removePotionEffect(PotionEffectType.SLOW);
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - chargeStart.get(id);
                if (elapsed > MAX_CHARGE_TIME) elapsed = MAX_CHARGE_TIME;

                int percent = (int) Math.min(100, Math.round(elapsed * 100.0 / MAX_CHARGE_TIME));
                int current = chargeLevels.getOrDefault(id, 0);
                int next = percent / 25;

                if (next > current) {
                    chargeLevels.put(id, next);
                    float pitch = 0.6f + next * 0.25f;
                    player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, pitch);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, Math.max(0, next - 1), false, false, false));

                    if (percent >= 100) {
                        player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.9f);
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

    private int getBowInt(String bowId, String path, int def) {
        FileConfiguration bows = plugin.getConfigManager().getBows();
        String fullPath = bowId + "." + path;
        return bows != null ? bows.getInt(fullPath, def) : def;
    }
}
