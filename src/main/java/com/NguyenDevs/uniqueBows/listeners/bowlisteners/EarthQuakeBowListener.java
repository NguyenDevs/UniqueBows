package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;

public class EarthQuakeBowListener implements Listener {

    private static final String BOW_ID = "earthquake_bow";
    private final UniqueBows plugin;
    private final Set<UUID> earthQuakeArrows = new HashSet<>();
    private final Map<UUID, BlockData> fallingBlockData = new HashMap<>();

    public EarthQuakeBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    private double getBowDouble(String bowId, String path, double def) {
        FileConfiguration bows = plugin.getConfigManager().getBows();
        return bows != null ? bows.getDouble(bowId + "." + path, def) : def;
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

    private boolean hasAnyArrow(Player player) {
        return player.getInventory().contains(Material.ARROW)
                || player.getInventory().contains(Material.TIPPED_ARROW)
                || player.getInventory().contains(Material.SPECTRAL_ARROW);
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (!BOW_ID.equals(bowId)) return;

        // Ngăn bắn mũi tên vanilla & không tiêu hao mũi tên bởi event
        event.setCancelled(true);
        event.setConsumeItem(false);

        if (!player.hasPermission("ub.use")) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("prefix") + " " +
                    plugin.getConfigManager().getMessages().getString("no-permission")));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }

        // Đang cooldown -> chỉ báo, KHÔNG trừ mũi tên
        if (checkAndNotifyCooldown(player, bowId)) {
            return;
        }

        // Nếu không phải CREATIVE thì cần có mũi tên
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && !hasAnyArrow(player)) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.6f);
            return;
        }
        // Tiêu hao 1 mũi tên khi bắn thành công (không phải CREATIVE)
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && !consumeOneArrow(player)) {
            return;
        }

        // Đặt cooldown và bắn arrow custom
        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);

        float force = event.getForce();
        Vector dir = player.getEyeLocation().getDirection().normalize();
        double velocity = 2.8 + (force * 1.7);

        Arrow custom = player.getWorld().spawnArrow(player.getEyeLocation(), dir, (float) velocity, 0f);
        custom.setShooter(player);
        custom.setGravity(true);
        custom.setCritical(false);
        earthQuakeArrows.add(custom.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        // Nếu người bắn đang dùng EarthQuakeBow và bị cooldown -> hủy, KHÔNG trừ mũi tên
        if (arrow.getShooter() instanceof Player player) {
            ItemStack bow = player.getInventory().getItemInMainHand();
            if (bow != null) {
                String bowId = plugin.getBowManager().getCustomBowId(bow);
                if (BOW_ID.equals(bowId) && checkAndNotifyCooldown(player, bowId)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        // Arrow spawn bởi listener này chỉ dùng để theo dõi; không cần thêm gì ở đây
        if (!earthQuakeArrows.contains(arrow.getUniqueId())) return;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;
        UUID id = arrow.getUniqueId();
        if (!earthQuakeArrows.remove(id)) return;

        Player shooter = (arrow.getShooter() instanceof Player) ? (Player) arrow.getShooter() : null;

        Location impactLocation;
        if (event.getHitBlock() != null) {
            impactLocation = event.getHitBlock().getLocation().add(0.5, 1.0, 0.5);
        } else if (event.getHitEntity() != null) {
            impactLocation = event.getHitEntity().getLocation();
        } else {
            impactLocation = arrow.getLocation();
        }
        createEarthQuakeEffect(impactLocation, shooter);
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (!(event.getEntity() instanceof FallingBlock)) return;
        FallingBlock fb = (FallingBlock) event.getEntity();
        if (fallingBlockData.containsKey(fb.getUniqueId())) {
            event.setCancelled(true);
            fb.remove();
            fallingBlockData.remove(fb.getUniqueId());
        }
    }

    private void createEarthQuakeEffect(Location center, Player shooter) {
        int radius = 5;
        double maxHeight = 1.5;
        double maxEntityHeight = 2.0;
        double maxEntityPush = 1.0;
        double shockStrength = getBowDouble(BOW_ID, "shock-strength", 1.0);

        World world = center.getWorld();
        if (world == null) return;

        Set<Block> affectedBlocks = new HashSet<>();
        Set<LivingEntity> affectedEntities = new HashSet<>();
        Map<Block, BlockData> originalBlockData = new HashMap<>();

        int baseY = center.getBlockY();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double dist2d = Math.sqrt(dx * dx + dz * dz);
                if (dist2d > radius) continue;

                Block top = findGroundBlockAt(world, center.getBlockX() + dx, center.getBlockZ() + dz, baseY, 3, 8);
                if (top != null) {
                    affectedBlocks.add(top);
                    originalBlockData.put(top, top.getBlockData().clone());
                }

                Location col = new Location(world, center.getBlockX() + dx + 0.5, baseY + 0.5, center.getBlockZ() + dz + 0.5);
                affectedEntities.addAll(
                        world.getNearbyEntities(col, 0.75, 2.5, 0.75).stream()
                                .filter(e -> e instanceof LivingEntity && (shooter == null || !e.equals(shooter)))
                                .map(e -> (LivingEntity) e)
                                .collect(Collectors.toSet())
                );
            }
        }

        new BukkitRunnable() {
            int wave = 0;
            @Override
            public void run() {
                if (wave > radius) {
                    cancel();
                    return;
                }
                float basePitch = 0.8f + (wave * 0.04f);
                float vol = 1.2f - (wave * 0.08f);
                if (vol < 0.3f) vol = 0.3f;

                world.playSound(center, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, Math.max(0.6f, vol), basePitch);
                world.playSound(center, Sound.BLOCK_STONE_BREAK, Math.max(0.4f, vol * 0.75f), 0.6f + wave * 0.03f);

                for (Block block : affectedBlocks) {
                    double d = block.getLocation().toVector().setY(0).distance(center.toVector().setY(0));
                    if (Math.abs(d - wave) <= 0.6) {
                        double lift = Math.max(0, 1.5 * (1 - d / radius));
                        if (lift > 0.05) {
                            spawnFallingBlock(block, lift, originalBlockData.get(block));
                        }
                    }
                }

                for (LivingEntity entity : affectedEntities) {
                    double d = entity.getLocation().toVector().setY(0).distance(center.toVector().setY(0));
                    if (Math.abs(d - wave) <= 0.8) {
                        Vector dir = entity.getLocation().toVector().subtract(center.toVector()).setY(0).normalize();
                        double height = Math.max(0, 2.0 * (1 - d / radius)) * shockStrength;
                        double push = Math.max(0, 1.0 * (1 - d / radius)) * shockStrength;
                        entity.setVelocity(new Vector(dir.getX() * push, height * 0.5, dir.getZ() * push));
                    }
                }
                wave++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private boolean isValidBlock(Block block) {
        Material type = block.getType();
        return type.isSolid() && !type.isAir() && type != Material.BEDROCK && type != Material.WATER && type != Material.LAVA;
    }

    private Block findGroundBlockAt(World world, int x, int z, int startY, int searchUp, int searchDown) {
        for (int dy = 0; dy <= searchDown; dy++) {
            int y = startY - dy;
            if (y < world.getMinHeight()) break;
            Block b = world.getBlockAt(x, y, z);
            if (isValidBlock(b) && b.getRelative(0, 1, 0).getType().isAir()) return b;
        }
        for (int dy = 1; dy <= searchUp; dy++) {
            int y = startY + dy;
            if (y > world.getMaxHeight()) break;
            Block b = world.getBlockAt(x, y, z);
            if (isValidBlock(b) && b.getRelative(0, 1, 0).getType().isAir()) return b;
        }
        return null;
    }

    private void spawnFallingBlock(Block baseBlock, double height, BlockData originalData) {
        if (!isValidBlock(baseBlock)) return;
        Location spawnLoc = baseBlock.getLocation().add(0.5, 0.1, 0.5);
        baseBlock.setType(Material.AIR, false);

        FallingBlock fb = baseBlock.getWorld().spawnFallingBlock(spawnLoc, originalData);
        fb.setDropItem(false);
        fb.setHurtEntities(false);
        fb.setVelocity(new Vector(0, Math.max(0.35, height * 1.05), 0));

        fallingBlockData.put(fb.getUniqueId(), originalData);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (baseBlock.getType() == Material.AIR) {
                    baseBlock.setBlockData(originalData, false);
                }
                if (fb.isValid()) fb.remove();
                fallingBlockData.remove(fb.getUniqueId());
            }
        }.runTaskLater(plugin, 16);
    }
}
