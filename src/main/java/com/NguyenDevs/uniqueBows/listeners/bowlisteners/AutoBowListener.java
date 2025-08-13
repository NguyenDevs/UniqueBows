package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AutoBowListener implements Listener {

    private final UniqueBows plugin;
    private final Map<UUID, Long> chargeStart = new HashMap<>();
    private final Map<UUID, Integer> chargeLevels = new HashMap<>();
    private final Set<UUID> isShooting = new HashSet<>();

    public AutoBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack bow = event.getItem();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"auto_bow".equals(bowId)) return;

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            event.setCancelled(true);
            return;
        }

        if (chargeStart.containsKey(player.getUniqueId()) || isShooting.contains(player.getUniqueId())) return;

        chargeStart.put(player.getUniqueId(), System.currentTimeMillis());
        chargeLevels.put(player.getUniqueId(), 0);
        startChargeWatcher(player, bowId);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"auto_bow".equals(bowId)) return;

        event.setCancelled(true);
        event.setConsumeItem(false);

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player player)) return;

        ItemStack bow = player.getInventory().getItemInMainHand();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!"auto_bow".equals(bowId)) return;

        event.setCancelled(true);

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
        }
    }

    private void startChargeWatcher(Player player, String bowId) {
        new BukkitRunnable() {
            @Override
            public void run() {
                UUID id = player.getUniqueId();

                if (!player.isOnline() || player.isDead()) {
                    clearState(player);
                    cancel();
                    return;
                }
                if (!chargeStart.containsKey(id)) {
                    cancel();
                    return;
                }
                if (!isHoldingAutoBow(player)) {
                    handleRelease(player, bowId);
                    cancel();
                    return;
                }

                long elapsed = System.currentTimeMillis() - chargeStart.get(id);
                int percent = Math.min(100, (int) Math.floor((elapsed / 5000.0) * 100));
                int level = percent / 20;
                int prev = chargeLevels.getOrDefault(id, 0);

                if (level > prev) {
                    chargeLevels.put(id, level);
                    float pitch = 0.5f + (level * 0.3f);
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1.0f, pitch);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, level, false, false, false));
                    if (level == 5) {
                        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.05);
                    }
                }

                if (!player.isHandRaised()) {
                    handleRelease(player, bowId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);
    }

    private boolean isHoldingAutoBow(Player player) {
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (inHand == null) return false;
        String id = plugin.getBowManager().getCustomBowId(inHand);
        return "auto_bow".equals(id);
    }

    private void handleRelease(Player player, String bowId) {
        UUID id = player.getUniqueId();
        player.removePotionEffect(PotionEffectType.SLOW);

        Long start = chargeStart.get(id);
        chargeStart.remove(id);
        chargeLevels.remove(id);
        if (start == null) return;

        if (!player.hasPermission("ub.use")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("prefix") + " " + plugin.getConfigManager().getMessages().getString("no-permission")));
            return;
        }

        if (plugin.getBowManager().isOnCooldown(id, bowId)) {
            long remaining = plugin.getBowManager().getRemainingCooldown(id, bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String msg = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + msg.replace("{time}", String.valueOf(remaining))));
            return;
        }

        long chargeTime = System.currentTimeMillis() - start;
        int percent = Math.min(100, (int) Math.floor((chargeTime / 5000.0) * 100));
        if (percent <= 0) return;

        int min = getBowInt(bowId, "min-arrow", 5);
        int max = Math.max(min, getBowInt(bowId, "max-arrow", 50));
        int arrowsToFire = (int) Math.round(min + (max - min) * (percent / 100.0));
        arrowsToFire = Math.max(min, Math.min(max, arrowsToFire));
        if (arrowsToFire <= 0) return;

        if (player.getGameMode() != GameMode.CREATIVE && !hasAnyArrow(player)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        if (isShooting.contains(id)) return;
        isShooting.add(id);

        shootBurst(player, bowId, arrowsToFire);
    }

    private int getBowInt(String bowId, String path, int def) {
        FileConfiguration bows = plugin.getConfigManager().getBows();
        String full = bowId + "." + path;
        return bows != null ? bows.getInt(full, def) : def;
    }

    private void shootBurst(Player player, String bowId, int total) {
        new BukkitRunnable() {
            int shot = 0;
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) { end(); return; }
                if (shot >= total) { end(); return; }

                if (player.getGameMode() != GameMode.CREATIVE) {
                    if (!consumeOneArrow(player)) { end(); return; }
                }

                Arrow arrow = player.getWorld().spawnArrow(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        3.0f,
                        1.0f
                );
                arrow.setShooter(player);

                Vector v = player.getLocation().getDirection().multiply(3.0);
                v.add(new Vector((Math.random() - 0.5) * 0.12, (Math.random() - 0.5) * 0.12, (Math.random() - 0.5) * 0.12));
                arrow.setVelocity(v);

                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.0f);
                shot++;
            }

            private void end() {
                plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);
                isShooting.remove(player.getUniqueId());
                cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
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

    private void clearState(Player player) {
        UUID id = player.getUniqueId();
        chargeStart.remove(id);
        chargeLevels.remove(id);
        isShooting.remove(id);
        player.removePotionEffect(PotionEffectType.SLOW);
    }
}
