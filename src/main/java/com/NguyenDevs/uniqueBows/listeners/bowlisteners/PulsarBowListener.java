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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PulsarBowListener implements Listener {

    private static final String BOW_ID = "pulsar_bow";
    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();

    public PulsarBowListener(UniqueBows plugin) {
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
        event.setConsumeItem(false);

        UUID playerId = player.getUniqueId();
        if (!player.hasPermission("ub.use")) {
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("no-permission");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        FileConfiguration bows = plugin.getConfigManager().getBows();
        int delay = bows.getInt(bowId + ".delay", 6);

        if (plugin.getBowManager().isOnCooldown(playerId, bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(playerId, bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE && !hasAnyArrow(player)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("prefix") + " You need arrows to shoot!"));
            return;
        }

        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!consumeOneArrow(player)) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
                return;
            }
        }

        shootPulsarArrow(player, bowId);
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

    private void shootPulsarArrow(Player player, String bowId) {
        Arrow arrow = player.getWorld().spawnArrow(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                3.0f,
                1.0f
        );
        arrow.setShooter(player);
        arrow.setMetadata("pulsar_bow", new FixedMetadataValue(plugin, true));
        arrowBowMap.put(arrow.getUniqueId(), bowId);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.05);

        // Pulsar trail effect
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead()) {
                    arrowBowMap.remove(arrow.getUniqueId());
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.PORTAL, arrow.getLocation(), 5, 0.1, 0.1, 0.1, 0.05);
                arrow.getWorld().spawnParticle(Particle.SMOKE_LARGE, arrow.getLocation(), 3, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        if (!arrow.hasMetadata("pulsar_bow")) return;
        if (!(arrow.getShooter() instanceof Player)) return;

        String bowId = arrowBowMap.remove(arrow.getUniqueId());
        if (bowId == null) return;

        Location center = arrow.getLocation().clone();
        if (event.getHitEntity() != null) {
            center = event.getHitEntity().getLocation();
        } else if (event.getHitBlock() != null) {
            center = event.getHitBlock().getLocation().add(0.5, 0, 0.5);
        }

        arrow.remove();

        FileConfiguration bows = plugin.getConfigManager().getBows();
        int pullTicks = Math.max(0, bows.getInt(bowId + ".pull-duration", 5)) * 20;

        center.getWorld().playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 3.0f, 1.0f);

        new BukkitRunnable() {
            int ticks = 0;
            double radius = 4.0;

            @Override
            public void run() {
                if (ticks > pullTicks) {
                    cancel();
                    return;
                }
                Location center = arrow.getLocation().clone();
                radius = Math.max(0.5, radius - 0.02);

                center.getWorld().playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 1.5f, 2.0f);
                center.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, center, 0);

                for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                    double x = Math.cos(angle + ticks * 0.2) * radius;
                    double z = Math.sin(angle + ticks * 0.2) * radius;
                    Location particleLoc = center.clone().add(x, 0.2 * Math.sin(ticks * 0.1), z);

                    Vector vel = center.toVector().subtract(particleLoc.toVector())
                            .normalize().multiply(0.05);

                    center.getWorld().spawnParticle(Particle.SMOKE_LARGE, particleLoc, 0,
                            vel.getX(), vel.getY(), vel.getZ(), 0.1);
                    center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 1,
                            0, 0, 0, 0.05);
                }

                for (Entity entity : center.getWorld().getNearbyEntities(center, 5, 5, 5)) {
                    if (entity instanceof LivingEntity && entity != arrow.getShooter()) {
                        Vector pullDirection = center.toVector()
                                .subtract(entity.getLocation().toVector())
                                .normalize().multiply(0.4);
                        entity.setVelocity(pullDirection);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
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