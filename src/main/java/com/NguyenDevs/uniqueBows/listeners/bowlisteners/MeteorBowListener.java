package com.NguyenDevs.uniqueBows.listeners.bowlisteners;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class MeteorBowListener implements Listener {

    private static final int MIN_METEOR_SIZE = 3;
    private static final int MAX_METEOR_SIZE = 10;
    private static final int SOUND_RADIUS = 100;
    private static final int SPAWN_HEIGHT = 80;
    private static final int VIEW_RANGE = 128;

    private static final Material[] METEOR_MATERIALS = {
            Material.DEEPSLATE, Material.DEEPSLATE, Material.DEEPSLATE,
            Material.BLACKSTONE, Material.BLACKSTONE, Material.BLACKSTONE,
            Material.MAGMA_BLOCK, Material.GILDED_BLACKSTONE
    };

    private static final Material[] SCORCHED_MATERIALS = {
            Material.COBBLESTONE, Material.DEEPSLATE, Material.MAGMA_BLOCK
    };

    private static final Material[] METEOR_CRUST_MATERIALS = {
            Material.DEEPSLATE, Material.BLACKSTONE, Material.COBBLED_DEEPSLATE
    };

    private final UniqueBows plugin;
    private final Map<UUID, String> arrowBowMap = new HashMap<>();
    private final Map<UUID, Long> chargeTimes = new HashMap<>();
    private final Map<UUID, MeteorTask> activeMeteors = new HashMap<>();
    private final Map<BlockPosition, Set<UUID>> phantomBlockTracking = new HashMap<>();

    public MeteorBowListener(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction().toString().contains("RIGHT_CLICK")) {
            Player player = event.getPlayer();
            ItemStack bow = player.getInventory().getItemInMainHand();
            String bowId = plugin.getBowManager().getCustomBowId(bow);

            if (bowId != null && bowId.equals("meteor_bow") && !chargeTimes.containsKey(player.getUniqueId())) {
                chargeTimes.put(player.getUniqueId(), System.currentTimeMillis());
            }
        }
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null) return;

        String bowId = plugin.getBowManager().getCustomBowId(bow);
        if (bowId == null || !bowId.equals("meteor_bow")) return;

        if (!player.hasPermission("ub.use")) {
            event.setCancelled(true);
            event.setConsumeItem(false);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("no-permission")));
            return;
        }

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            event.setConsumeItem(false);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String message = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + message.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }



    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Arrow)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack bow = player.getInventory().getItemInMainHand();

        String bowId = plugin.getBowManager().getCustomBowId(bow);

        if (bowId == null || !bowId.equals("meteor_bow")) return;

        if (!player.hasPermission("ub.use")) {
            event.setCancelled(true);
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getMessages().getString("no-permission")));
            return;
        }

        if (plugin.getBowManager().isOnCooldown(player.getUniqueId(), bowId)) {
            event.setCancelled(true);
            long remaining = plugin.getBowManager().getRemainingCooldown(player.getUniqueId(), bowId);
            String prefix = plugin.getConfigManager().getMessages().getString("prefix");
            String message = plugin.getConfigManager().getMessages().getString("bow-delay");
            player.sendMessage(ColorUtils.colorize(prefix + " " + message.replace("{time}", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }

        plugin.getBowManager().setCooldown(player.getUniqueId(), bowId);

        arrowBowMap.put(event.getEntity().getUniqueId(), bowId);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow arrow)) return;

        String bowId = arrowBowMap.get(arrow.getUniqueId());
        if (bowId == null || !bowId.equals("meteor_bow")) return;

        Player shooter = (Player) arrow.getShooter();
        Location impactLocation = arrow.getLocation();

        impactLocation.getWorld().spawnParticle(Particle.LAVA, impactLocation, 15, 0.5, 0.5, 0.5, 0);
        impactLocation.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impactLocation, 10, 0.3, 0.3, 0.3, 0.02);

        long chargeTime = System.currentTimeMillis() - chargeTimes.getOrDefault(shooter.getUniqueId(), System.currentTimeMillis());
        int meteorSize = Math.min(MAX_METEOR_SIZE, Math.max(MIN_METEOR_SIZE, (int) (chargeTime / 200)));

        chargeTimes.remove(shooter.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                spawnMeteor(impactLocation, meteorSize, shooter);
            }
        }.runTaskLater(plugin, 10L);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Arrow arrow = (Arrow) event.getDamager();
        String bowId = arrowBowMap.get(arrow.getUniqueId());
        if (bowId == null || !bowId.equals("meteor_bow")) return;

        arrowBowMap.remove(arrow.getUniqueId());
    }

    private void spawnMeteor(Location targetLocation, int meteorSize, Player shooter) {
        Vector diagonalDirection = generateRandomDiagonalDirection();
        double spawnDistance = ThreadLocalRandom.current().nextDouble(60, 100);
        Location startLocation = targetLocation.clone()
                .add(diagonalDirection.getX() * spawnDistance, SPAWN_HEIGHT, diagonalDirection.getZ() * spawnDistance);

        UUID meteorId = UUID.randomUUID();
        MeteorTask meteorTask = new MeteorTask(startLocation, targetLocation, meteorSize, meteorId, shooter);
        activeMeteors.put(meteorId, meteorTask);
        meteorTask.runTaskTimer(plugin, 0L, 1L);
    }

    private Vector generateRandomDiagonalDirection() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(0.3, 2 * Math.PI - 0.3);
        double[] avoidAngles = {0, Math.PI / 2, Math.PI, 3 * Math.PI / 2};
        for (double avoidAngle : avoidAngles) {
            if (Math.abs(angle - avoidAngle) < 0.2) angle += 0.3;
        }
        double x = Math.cos(angle);
        double z = Math.sin(angle);
        return new Vector(x, 0, z).normalize();
    }

    private void sendBlockChange(Player player, BlockPosition pos, WrappedBlockData data) {
        try {
            ProtocolManager pm = ProtocolLibrary.getProtocolManager();
            PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
            packet.getBlockPositionModifier().write(0, pos);
            packet.getBlockData().write(0, data);
            pm.sendServerPacket(player, packet, false);
        } catch (Exception ignored) {
        }
    }

    private boolean inHorizontalRange(Player p, Location c, int range) {
        if (!p.getWorld().equals(c.getWorld())) return false;
        Location pl = p.getLocation();
        double dx = pl.getX() - c.getX();
        double dz = pl.getZ() - c.getZ();
        return (dx * dx + dz * dz) <= (range * range);
    }

    private enum MeteorShape {
        SPHERICAL, ARROW, IRREGULAR, TAILED, COMPLEX
    }

    private class MeteorTask extends BukkitRunnable {
        private final ProtocolManager pm = ProtocolLibrary.getProtocolManager();
        private final Location startLocation;
        private final Location targetLocation;
        private final int meteorSize;
        private final UUID meteorId;
        private final Vector flightDirection;
        private final MeteorShape meteorShape;
        private final Player shooter;
        private Location currentLocation;
        private int tickCount = 0;
        private boolean hasImpacted = false;
        private final double speed;
        private final Map<UUID, Set<BlockPosition>> lastSentPerPlayer = new HashMap<>();
        private final List<MeteorVoxel> meteorVoxels;
        private final int MAX_TICKS = 20 * 30;

        private class MeteorVoxel {
            final Vector offset;
            final WrappedBlockData data;

            MeteorVoxel(Vector offset, WrappedBlockData data) {
                this.offset = offset;
                this.data = data;
            }
        }

        public MeteorTask(Location start, Location target, int size, UUID meteorId, Player shooter) {
            this.startLocation = start.clone();
            this.targetLocation = target.clone();
            this.meteorSize = size;
            this.meteorId = meteorId;
            this.shooter = shooter;
            this.currentLocation = start.clone();
            this.meteorShape = selectRandomMeteorShape(size);
            Vector baseDirection = target.toVector().subtract(start.toVector()).normalize();
            this.flightDirection = new Vector(
                    baseDirection.getX(),
                    Math.min(baseDirection.getY(), -0.55),
                    baseDirection.getZ()
            ).normalize();
            this.speed = ThreadLocalRandom.current().nextDouble(2.0, 3.0);
            this.meteorVoxels = generateMeteorVoxels(size, meteorId, meteorShape);
        }

        private MeteorShape selectRandomMeteorShape(int size) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            if (size >= 9) {
                double rand = random.nextDouble();
                if (rand < 0.25) return MeteorShape.COMPLEX;
                if (rand < 0.45) return MeteorShape.IRREGULAR;
                if (rand < 0.65) return MeteorShape.TAILED;
                if (rand < 0.85) return MeteorShape.ARROW;
                return MeteorShape.SPHERICAL;
            } else if (size >= 6) {
                double rand = random.nextDouble();
                if (rand < 0.2) return MeteorShape.COMPLEX;
                if (rand < 0.4) return MeteorShape.IRREGULAR;
                if (rand < 0.6) return MeteorShape.TAILED;
                if (rand < 0.8) return MeteorShape.ARROW;
                return MeteorShape.SPHERICAL;
            } else {
                double rand = random.nextDouble();
                if (rand < 0.1) return MeteorShape.COMPLEX;
                if (rand < 0.3) return MeteorShape.IRREGULAR;
                if (rand < 0.5) return MeteorShape.TAILED;
                if (rand < 0.7) return MeteorShape.ARROW;
                return MeteorShape.SPHERICAL;
            }
        }

        @Override
        public void run() {
            if (hasImpacted || tickCount++ > MAX_TICKS) {
                if (!hasImpacted) impact(currentLocation.clone());
                cancel();
                return;
            }

            Location prev = currentLocation.clone();
            Vector movement = flightDirection.clone().multiply(speed);
            currentLocation.add(movement);

            World w = currentLocation.getWorld();
            if (w != null) {
                org.bukkit.util.RayTraceResult r = w.rayTraceBlocks(
                        prev, movement.normalize(), movement.length(),
                        FluidCollisionMode.NEVER, true
                );
                if (r != null && r.getHitPosition() != null) {
                    Block hitBlock = r.getHitBlock();
                    if (hitBlock != null && isSolidTerrain(hitBlock.getType())) {
                        Location hit = new Location(w,
                                r.getHitPosition().getX(),
                                r.getHitPosition().getY(),
                                r.getHitPosition().getZ());
                        hasImpacted = true;
                        impact(hit);
                        cancel();
                        return;
                    }
                }
            }

            double distanceToTarget = currentLocation.distance(targetLocation);
            if (distanceToTarget <= speed + 1.0) {
                hasImpacted = true;
                impact(targetLocation.clone());
                cancel();
                return;
            }

            createTrailEffect();
            if (tickCount % 6 == 0) playMeteorFlightSound();
            if (tickCount % 2 == 0) renderFakeMeteor();

            if (currentLocation.getBlockY() <= getGroundLevel(currentLocation)) {
                hasImpacted = true;
                impact(currentLocation.clone());
                cancel();
            }
        }

        private void createTrailEffect() {
            World world = currentLocation.getWorld();
            if (world == null) return;
            double offset = meteorSize / 2.0;
            world.spawnParticle(Particle.FLAME, currentLocation, 20, offset, offset, offset, 0.03);
            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, currentLocation, 15, offset, offset, offset, 0.05);
            world.spawnParticle(Particle.LAVA, currentLocation, 8, offset / 2, offset / 2, offset / 2, 0);
        }

        private void playMeteorFlightSound() {
            World world = currentLocation.getWorld();
            if (world == null) return;
            for (Player player : world.getPlayers()) {
                if (player.getLocation().distance(currentLocation) <= SOUND_RADIUS) {
                    player.playSound(currentLocation, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.AMBIENT, 0.8f, 0.6f);
                    player.playSound(currentLocation, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.AMBIENT, 1.0f, 0.4f);
                }
            }
        }

        private void renderFakeMeteor() {
            World world = currentLocation.getWorld();
            if (world == null) return;

            Map<BlockPosition, WrappedBlockData> currentBlocks = new HashMap<>();

            for (MeteorVoxel voxel : meteorVoxels) {
                Location blockLoc = currentLocation.clone().add(voxel.offset);
                BlockPosition pos = new BlockPosition(
                        blockLoc.getBlockX(),
                        blockLoc.getBlockY(),
                        blockLoc.getBlockZ()
                );
                currentBlocks.put(pos, voxel.data);
            }

            for (Player player : world.getPlayers()) {
                if (!inHorizontalRange(player, currentLocation, VIEW_RANGE)) continue;

                Set<BlockPosition> lastSent = lastSentPerPlayer.computeIfAbsent(
                        player.getUniqueId(), k -> new HashSet<>()
                );

                for (BlockPosition oldPos : lastSent) {
                    sendBlockChange(player, oldPos, WrappedBlockData.createData(Material.AIR.createBlockData()));
                    removePhantomBlockTracking(oldPos, player.getUniqueId());
                }
                lastSent.clear();

                for (Map.Entry<BlockPosition, WrappedBlockData> entry : currentBlocks.entrySet()) {
                    sendBlockChange(player, entry.getKey(), entry.getValue());
                    addPhantomBlockTracking(entry.getKey(), player.getUniqueId());
                }

                lastSent.addAll(currentBlocks.keySet());
            }
        }

        private void addPhantomBlockTracking(BlockPosition pos, UUID playerId) {
            phantomBlockTracking.computeIfAbsent(pos, k -> new HashSet<>()).add(playerId);
        }

        private void removePhantomBlockTracking(BlockPosition pos, UUID playerId) {
            Set<UUID> players = phantomBlockTracking.get(pos);
            if (players != null) {
                players.remove(playerId);
                if (players.isEmpty()) {
                    phantomBlockTracking.remove(pos);
                }
            }
        }

        private void clearAllFakeBlocks() {
            for (Map.Entry<UUID, Set<BlockPosition>> entry : lastSentPerPlayer.entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null || !player.isOnline()) continue;

                for (BlockPosition pos : entry.getValue()) {
                    sendBlockChange(player, pos, WrappedBlockData.createData(Material.AIR.createBlockData()));
                    removePhantomBlockTracking(pos, entry.getKey());
                }
                entry.getValue().clear();
            }
        }

        private void impact(Location impactPoint) {
            World world = impactPoint.getWorld();
            if (world == null) return;

            clearAllFakeBlocks();

            world.playSound(impactPoint, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.AMBIENT, 2.0f, 0.5f);
            world.spawnParticle(Particle.EXPLOSION_LARGE, impactPoint, 5, 2, 2, 2, 0);
            world.spawnParticle(Particle.LAVA, impactPoint, 20, 3, 3, 3, 0);

            applyShockwaveAt(impactPoint);
            applyImpactDamage(impactPoint);
            createMeteorCrater(impactPoint);
        }

        private void applyImpactDamage(Location center) {
            World world = center.getWorld();
            double damageRadius = meteorSize * 1.5;
            double maxDamage = 10.0 + meteorSize * 1.5;

            for (Player player : world.getPlayers()) {
                if (player.equals(shooter)) continue;
                double distance = player.getLocation().distance(center);
                if (distance <= damageRadius) {
                    double damage = maxDamage * (1.0 - (distance / damageRadius));
                    player.damage(Math.max(1.0, damage), shooter);
                }
            }
        }

        private int getGroundLevel(Location loc) {
            World world = loc.getWorld();
            if (world == null) return loc.getBlockY();

            for (int y = loc.getBlockY(); y >= world.getMinHeight(); y--) {
                Material mat = world.getBlockAt(loc.getBlockX(), y, loc.getBlockZ()).getType();
                if (isSolidTerrain(mat)) {
                    return y;
                }
            }
            return world.getMinHeight();
        }

        private boolean isSolidTerrain(Material material) {
            return material.isSolid() &&
                    material != Material.WATER &&
                    !material.name().contains("LEAVES") &&
                    !material.name().contains("LOG") &&
                    !material.name().contains("WOOD") &&
                    !isVegetation(material);
        }

        private boolean isVegetation(Material material) {
            return material == Material.GRASS ||
                    material == Material.TALL_GRASS ||
                    material == Material.FERN ||
                    material == Material.LARGE_FERN ||
                    material == Material.DEAD_BUSH ||
                    material == Material.DANDELION ||
                    material == Material.POPPY ||
                    material == Material.BLUE_ORCHID ||
                    material == Material.ALLIUM ||
                    material == Material.AZURE_BLUET ||
                    material == Material.RED_TULIP ||
                    material == Material.ORANGE_TULIP ||
                    material == Material.WHITE_TULIP ||
                    material == Material.PINK_TULIP ||
                    material == Material.OXEYE_DAISY ||
                    material == Material.SUNFLOWER ||
                    material == Material.LILAC ||
                    material == Material.ROSE_BUSH ||
                    material == Material.PEONY ||
                    material == Material.GLOWSTONE ||
                    material.name().contains("SAPLING") ||
                    material.name().contains("MUSHROOM");
        }

        private void applyShockwaveAt(Location center) {
            World world = center.getWorld();
            double knockRadius = meteorSize * 2.0;
            for (LivingEntity entity : world.getNearbyEntities(center, knockRadius, knockRadius, knockRadius)
                    .stream().filter(e -> e instanceof LivingEntity && !e.equals(shooter))
                    .map(e -> (LivingEntity) e).toList()) {
                double distance = entity.getLocation().distance(center);
                if (distance <= knockRadius) {
                    Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize();
                    double strength = (1.0 - distance / knockRadius) * (meteorSize / 3.0) + 1.0;
                    entity.setVelocity(direction.multiply(strength).add(new Vector(0, 0.5 + meteorSize * 0.03, 0)));
                    double baseDamage = 4.0 + meteorSize * 0.8;
                    double damage = Math.max(1.0, baseDamage * (1.0 - (distance / knockRadius)));
                    entity.damage(damage, shooter);
                }
            }
        }

        private void createMeteorCrater(Location impactPoint) {
            World world = impactPoint.getWorld();
            if (world == null) return;

            clearVegetationAndFloatingObjects(impactPoint);
            createDrillingCrater(impactPoint);
            createScorchedArea(impactPoint);
        }

        private void clearVegetationAndFloatingObjects(Location center) {
            World world = center.getWorld();
            int radius = Math.max(5, meteorSize + 2);
            for (int x = -radius; x <= radius; x++) {
                for (int y = -10; y <= 10; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        double distance = Math.sqrt(x * x + z * z);
                        if (distance > radius) continue;

                        Location loc = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(loc);

                        if (isVegetation(block.getType()) ||
                                block.getType() == Material.FIRE ||
                                block.getType() == Material.SOUL_FIRE ||
                                block.getType().name().contains("TORCH") ||
                                block.getType().name().contains("SIGN") ||
                                block.getType().name().contains("BANNER") ||
                                block.getType() == Material.SNOW ||
                                block.getType() == Material.POWDER_SNOW ||
                                (!block.getType().isSolid() && block.getType() != Material.AIR && block.getType() != Material.WATER)) {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        private void createDrillingCrater(Location impactPoint) {
            World world = impactPoint.getWorld();
            if (world == null) return;

            Vector direction = flightDirection.clone().normalize();
            int totalDepth = meteorSize * 2;
            double maxRadius = meteorSize * 1.0;

            new BukkitRunnable() {
                int currentDepth = 0;
                double rotationAngle = 0;

                @Override
                public void run() {
                    if (currentDepth >= totalDepth) {
                        placeMeteorWithCrust(impactPoint, direction, totalDepth);
                        cancel();
                        return;
                    }

                    double depthRatio = (double) currentDepth / totalDepth;
                    double currentRadius = maxRadius * (1.0 - depthRatio * 0.7);
                    if (currentRadius < 1.0) currentRadius = 1.0;

                    drillCraterLayer(impactPoint, direction, currentDepth, currentRadius, rotationAngle);
                    currentDepth++;
                    rotationAngle += 0.15;

                    if (currentDepth % 6 == 0) {
                        Location drillPoint = impactPoint.clone().add(direction.clone().multiply(currentDepth * 0.8));
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, drillPoint, 2, 0.4, 0.4, 0.4, 0.01);
                    }
                }
            }.runTaskTimer(plugin, 3L, 1L);
        }

        private void drillCraterLayer(Location impactPoint, Vector direction, int depth, double radius, double rotation) {
            World world = impactPoint.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            Location layerCenter = impactPoint.clone().add(direction.clone().multiply(depth * 0.8));

            if (depth < 3) {
                addCrustAndScorchedMaterials(layerCenter, radius * 1.5, depth);
            }

            Vector right = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
            Vector up = direction.clone().crossProduct(right).normalize();

            int numPoints = Math.max(10, (int) (radius * 4));
            for (int i = 0; i < numPoints; i++) {
                double angle = (2.0 * Math.PI * i / numPoints) + rotation;

                for (double r = 0; r <= radius; r += 0.5) {
                    double localX = Math.cos(angle) * r;
                    double localY = Math.sin(angle) * r;

                    Vector offset = right.clone().multiply(localX).add(up.clone().multiply(localY));
                    Location blockLoc = layerCenter.clone().add(offset);
                    Block block = world.getBlockAt(blockLoc);

                    if (block.getType() != Material.BEDROCK && block.getType().isSolid()) {
                        if (random.nextDouble() < 0.3) {
                            world.spawnParticle(Particle.BLOCK_CRACK, blockLoc.add(0.5, 0.5, 0.5), 2,
                                    0.2, 0.2, 0.2, 0, block.getBlockData());
                        }

                        if (depth <= 2 && random.nextDouble() < 0.15) {
                            Material scorchedMaterial = SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)];
                            block.setType(scorchedMaterial, false);
                        } else {
                            block.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        private void createScorchedArea(Location center) {
            World world = center.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            int radius = Math.max(3, meteorSize / 2);
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= radius) {
                        double placementChance = distance <= radius * 0.5 ? 0.1 : 0.05;

                        if (random.nextDouble() < placementChance) {
                            int groundY = world.getHighestBlockYAt(center.getBlockX() + x, center.getBlockZ() + z);
                            for (int y = 0; y >= -1; y--) {
                                Location blockLoc = new Location(world, center.getBlockX() + x, groundY + y, center.getBlockZ() + z);
                                Block block = blockLoc.getBlock();

                                if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                                    double replaceChance = y == 0 ? 0.5 : 0.2;
                                    if (random.nextDouble() < replaceChance) {
                                        Material scorchedMaterial = SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)];
                                        block.setType(scorchedMaterial, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        private void addCrustAndScorchedMaterials(Location center, double radius, int depth) {
            World world = center.getWorld();
            ThreadLocalRandom random = ThreadLocalRandom.current();

            for (double angle = 0; angle < 2 * Math.PI; angle += 0.3) {
                for (double r = radius * 0.6; r <= radius * 1.2; r += 0.5) {
                    double x = Math.cos(angle) * r;
                    double z = Math.sin(angle) * r;

                    for (int y = -1; y <= 1; y++) {
                        Location loc = center.clone().add(x, y + random.nextInt(2), z);
                        Block block = world.getBlockAt(loc);

                        if (block.getType().isSolid() && block.getType() != Material.BEDROCK) {
                            double placementChance = depth == 0 ? 0.3 : 0.2;
                            if (random.nextDouble() < placementChance) {
                                Material material = random.nextDouble() < 0.5 ?
                                        SCORCHED_MATERIALS[random.nextInt(SCORCHED_MATERIALS.length)] :
                                        METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];
                                block.setType(material, false);
                            }
                        }
                    }
                }
            }
        }

        private void placeMeteorWithCrust(Location impactPoint, Vector direction, int depth) {
            World world = impactPoint.getWorld();
            Location meteorCenter = impactPoint.clone().add(direction.clone().multiply(depth * 0.8));

            double yaw = Math.atan2(-direction.getX(), direction.getZ());
            double pitch = Math.atan2(-direction.getY(), Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ()));

            ThreadLocalRandom random = ThreadLocalRandom.current();
            double crustRadius = meteorSize * 0.6;

            for (double x = -crustRadius; x <= crustRadius; x += 0.8) {
                for (double y = -crustRadius; y <= crustRadius; y += 0.8) {
                    for (double z = -crustRadius; z <= crustRadius; z += 0.8) {
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        if (distance >= crustRadius * 0.4 && distance <= crustRadius && random.nextDouble() < 0.7) {
                            Vector offset = new Vector(x, y, z);
                            Vector rotatedOffset = rotateOffset(offset, yaw, pitch);
                            Location blockLoc = meteorCenter.clone().add(rotatedOffset);
                            Block block = world.getBlockAt(blockLoc);
                            if (block.getType() != Material.BEDROCK && block.getType().isAir()) {
                                Material crustMaterial = METEOR_CRUST_MATERIALS[random.nextInt(METEOR_CRUST_MATERIALS.length)];
                                block.setType(crustMaterial, false);
                            }
                        }
                    }
                }
            }
        }

        private Vector rotateOffset(Vector o, double yaw, double pitch) {
            double cy = Math.cos(yaw), sy = Math.sin(yaw);
            double x1 = o.getX() * cy - o.getZ() * sy;
            double z1 = o.getX() * sy + o.getZ() * cy;
            double cp = Math.cos(pitch), sp = Math.sin(pitch);
            double y2 = o.getY() * cp - z1 * sp;
            double z2 = o.getY() * sp + z1 * cp;
            return new Vector(x1, y2, z2);
        }

        @Override
        public void cancel() {
            super.cancel();
            clearAllFakeBlocks();
            activeMeteors.remove(this.meteorId);
        }

        private List<MeteorVoxel> generateMeteorVoxels(int size, UUID seed, MeteorShape shape) {
            long s = seed.getMostSignificantBits() ^ seed.getLeastSignificantBits();
            Random random = new Random(s);

            switch (shape) {
                case SPHERICAL:
                    return generateSphericalMeteor(size, random);
                case ARROW:
                    return generateArrowMeteor(size, random);
                case IRREGULAR:
                    return generateIrregularMeteor(size, random);
                case TAILED:
                    return generateTailedMeteor(size, random);
                case COMPLEX:
                    return generateComplexMeteor(size, random);
                default:
                    return generateSphericalMeteor(size, random);
            }
        }

        private List<MeteorVoxel> generateSphericalMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double rx = size * (0.55 + random.nextDouble() * 0.25);
            double ry = size * (0.50 + random.nextDouble() * 0.25);
            double rz = size * (0.55 + random.nextDouble() * 0.25);
            int maxX = (int) Math.ceil(rx) + 1;
            int maxY = (int) Math.ceil(ry) + 1;
            int maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double nx = x / rx;
                        double ny = y / ry;
                        double nz = z / rz;
                        double distance = nx * nx + ny * ny + nz * nz;
                        double noise = (random.nextDouble() * 2 - 1) * 0.12;
                        if (distance <= 1.0 + noise && random.nextDouble() < 0.85) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, z),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateArrowMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double rx = size * (0.35 + random.nextDouble() * 0.2);
            double ry = size * (0.35 + random.nextDouble() * 0.2);
            double rz = size * (1.0 + random.nextDouble() * 0.5);
            int maxX = (int) Math.ceil(rx) + 1;
            int maxY = (int) Math.ceil(ry) + 1;
            int maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double tapering = z < 0 ? 1.0 + (z / rz) * 0.8 : 1.0;
                        double nx = x / (rx * tapering);
                        double ny = y / (ry * tapering);
                        double nz = z / rz;
                        double distance = nx * nx + ny * ny + nz * nz;
                        double noise = (random.nextDouble() * 2 - 1) * 0.1;
                        if (distance <= 1.0 + noise && random.nextDouble() < 0.82) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, z),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateIrregularMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            int numChunks = 2 + random.nextInt(3);
            for (int chunk = 0; chunk < numChunks; chunk++) {
                Vector chunkCenter = new Vector(
                        random.nextDouble(-size * 0.6, size * 0.6),
                        random.nextDouble(-size * 0.6, size * 0.6),
                        random.nextDouble(-size * 0.6, size * 0.6)
                );
                double chunkSize = size * (0.3 + random.nextDouble() * 0.4);
                double rx = chunkSize * (0.5 + random.nextDouble() * 0.5);
                double ry = chunkSize * (0.5 + random.nextDouble() * 0.5);
                double rz = chunkSize * (0.5 + random.nextDouble() * 0.5);
                int maxX = (int) Math.ceil(rx + Math.abs(chunkCenter.getX())) + 1;
                int maxY = (int) Math.ceil(ry + Math.abs(chunkCenter.getY())) + 1;
                int maxZ = (int) Math.ceil(rz + Math.abs(chunkCenter.getZ())) + 1;
                for (int x = -maxX; x <= maxX; x++) {
                    for (int y = -maxY; y <= maxY; y++) {
                        for (int z = -maxZ; z <= maxZ; z++) {
                            Vector pos = new Vector(x, y, z).subtract(chunkCenter);
                            double nx = pos.getX() / rx;
                            double ny = pos.getY() / ry;
                            double nz = pos.getZ() / rz;
                            double distance = nx * nx + ny * ny + nz * nz;
                            double noise = (random.nextDouble() * 2 - 1) * 0.25;
                            if (distance <= 1.0 + noise && random.nextDouble() < 0.75) {
                                Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                                voxels.add(new MeteorVoxel(
                                        new Vector(x, y, z),
                                        WrappedBlockData.createData(material.createBlockData())
                                ));
                            }
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateTailedMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            double headSize = size * 0.8;
            double rx = headSize * (0.55 + random.nextDouble() * 0.25);
            double ry = headSize * (0.50 + random.nextDouble() * 0.25);
            double rz = headSize * (0.35 + random.nextDouble() * 0.25);
            int maxX = (int) Math.ceil(rx) + 1;
            int maxY = (int) Math.ceil(ry) + 1;
            int maxZ = (int) Math.ceil(rz) + 1;
            for (int x = -maxX; x <= maxX; x++) {
                for (int y = -maxY; y <= maxY; y++) {
                    for (int z = -maxZ; z <= maxZ; z++) {
                        double nx = x / rx;
                        double ny = y / ry;
                        double nz = z / rz;
                        double distance = nx * nx + ny * ny + nz * nz;
                        if (distance <= 1.0 && random.nextDouble() < 0.88) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, z),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
                        }
                    }
                }
            }
            int tailLength = Math.max(3, size + random.nextInt(size));
            for (int t = 1; t <= tailLength; t++) {
                double tailFactor = 1.0 - (double) t / tailLength;
                double tailRadius = Math.max(1, rx * tailFactor * 0.6);
                for (int x = (int) -tailRadius; x <= tailRadius; x++) {
                    for (int y = (int) -tailRadius; y <= tailRadius; y++) {
                        double distance = Math.sqrt(x * x + y * y);
                        if (distance <= tailRadius && random.nextDouble() < 0.4 * tailFactor) {
                            Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                            voxels.add(new MeteorVoxel(
                                    new Vector(x, y, maxZ + t),
                                    WrappedBlockData.createData(material.createBlockData())
                            ));
                        }
                    }
                }
            }
            return voxels;
        }

        private List<MeteorVoxel> generateComplexMeteor(int size, Random random) {
            List<MeteorVoxel> voxels = new ArrayList<>();
            voxels.addAll(generateSphericalMeteor(size, random));
            int numProtrusions = 2 + random.nextInt(3);
            for (int i = 0; i < numProtrusions; i++) {
                double angle1 = random.nextDouble() * 2 * Math.PI;
                double angle2 = random.nextDouble() * Math.PI;
                Vector direction = new Vector(
                        Math.cos(angle1) * Math.sin(angle2),
                        Math.cos(angle2),
                        Math.sin(angle1) * Math.sin(angle2)
                ).normalize();
                double protrusionSize = size * (0.3 + random.nextDouble() * 0.4);
                int protrusionLength = (int) (protrusionSize * (1.0 + random.nextDouble()));
                for (int j = 1; j <= protrusionLength; j++) {
                    double shrinkFactor = 1.0 - (double) j / protrusionLength * 0.8;
                    double radius = Math.max(1, protrusionSize * 0.3 * shrinkFactor);
                    Vector basePos = direction.clone().multiply(size * 0.7 + j);
                    for (int x = (int) -radius; x <= radius; x++) {
                        for (int y = (int) -radius; y <= radius; y++) {
                            for (int z = (int) -radius; z <= radius; z++) {
                                double distance = Math.sqrt(x * x + y * y + z * z);
                                if (distance <= radius && random.nextDouble() < 0.7) {
                                    Vector finalPos = basePos.clone().add(new Vector(x, y, z));
                                    Material material = METEOR_MATERIALS[random.nextInt(METEOR_MATERIALS.length)];
                                    voxels.add(new MeteorVoxel(
                                            finalPos,
                                            WrappedBlockData.createData(material.createBlockData())
                                    ));
                                }
                            }
                        }
                    }
                }
            }
            return voxels;
        }
    }
    private int getBowInt(String bowId, String path, int def) {
        org.bukkit.configuration.file.FileConfiguration bows = plugin.getConfigManager().getBows();
        String fullPath = bowId + "." + path;
        return bows != null ? bows.getInt(fullPath, def) : def;
    }

}