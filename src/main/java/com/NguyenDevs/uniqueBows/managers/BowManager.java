package com.NguyenDevs.uniqueBows.managers;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.models.CustomBow;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class BowManager {

    private static final String PDC_KEY_BOW_ID = "bow-id";

    private final UniqueBows plugin;
    private final Map<String, CustomBow> customBows;
    private final Map<UUID, Map<String, Long>> playerCooldowns;

    public BowManager(UniqueBows plugin) {
        this.plugin = plugin;
        this.customBows = new HashMap<>();
        this.playerCooldowns = new HashMap<>();
    }

    public void loadBows() {
        customBows.clear();
        ConfigurationSection bowsSection = plugin.getConfigManager().getBows();
        if (bowsSection == null) {
            plugin.getLogger().warning("No 'bows' section found in config.");
            return;
        }
        for (String bowId : bowsSection.getKeys(false)) {
            ConfigurationSection bowSection = bowsSection.getConfigurationSection(bowId);
            if (bowSection != null && bowSection.getBoolean("enabled", true)) {
                CustomBow bow = loadBowFromConfig(bowId, bowSection);
                customBows.put(bowId, bow);
            }
        }
        plugin.getLogger().info("Loaded " + customBows.size() + " custom bows!");
    }

    private CustomBow loadBowFromConfig(String bowId, ConfigurationSection section) {
        String name = ColorUtils.colorize(section.getString("name", "&fUnnamed Bow"));
        List<String> loreList = section.getStringList("lore");
        List<String> colorizedLore = new ArrayList<>(loreList.size());
        for (String line : loreList) colorizedLore.add(ColorUtils.colorize(line));
        boolean craftable = section.getBoolean("craftable", true);
        boolean unbreakable = section.getBoolean("unbreakable", false);
        int delay = section.getInt("delay", 5);

        boolean customModelSpecified = section.contains("custom-model-data");
        Integer customModelData = null;
        if (customModelSpecified) {
            if (section.isInt("custom-model-data")) {
                customModelData = section.getInt("custom-model-data");
            } else {
                Object raw = section.get("custom-model-data");
                if (raw != null) {
                    String s = String.valueOf(raw).trim();
                    if (!s.equalsIgnoreCase("none")) {
                        try { customModelData = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        return new CustomBow(bowId, name, colorizedLore, craftable, unbreakable, delay, customModelSpecified, customModelData);
    }

    public ItemStack createBowItem(String bowId) {
        CustomBow bow = customBows.get(bowId);
        if (bow == null) return null;

        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        meta.setDisplayName(bow.getName());
        meta.setLore(bow.getLore());

        if (bow.isUnbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        if (bow.isCustomModelSpecified()) {
            Integer cmd = bow.getCustomModelData();
            if (cmd != null) meta.setCustomModelData(cmd);
        } else {
            meta.setCustomModelData(getBowModelData(bowId));
        }

        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_BOW_ID);
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, bowId);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isCustomBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        if (!item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_BOW_ID);
        String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return id != null && customBows.containsKey(id);
    }

    public String getCustomBowId(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return null;
        if (!item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey key = new NamespacedKey(plugin, PDC_KEY_BOW_ID);
        return meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public CustomBow getCustomBow(String bowId) {
        return customBows.get(bowId);
    }

    public Collection<CustomBow> getAllBows() {
        return customBows.values();
    }

    public boolean isOnCooldown(UUID playerId, String bowId) {
        Map<String, Long> playerCooldown = playerCooldowns.get(playerId);
        if (playerCooldown == null) return false;
        Long lastUse = playerCooldown.get(bowId);
        if (lastUse == null) return false;
        CustomBow bow = customBows.get(bowId);
        if (bow == null) return false;
        long cooldownTime = bow.getDelay() * 1000L;
        return (System.currentTimeMillis() - lastUse) < cooldownTime;
    }

    public void setCooldown(UUID playerId, String bowId) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(bowId, System.currentTimeMillis());
    }

    public long getRemainingCooldown(UUID playerId, String bowId) {
        Map<String, Long> playerCooldown = playerCooldowns.get(playerId);
        if (playerCooldown == null) return 0;
        Long lastUse = playerCooldown.get(bowId);
        if (lastUse == null) return 0;
        CustomBow bow = customBows.get(bowId);
        if (bow == null) return 0;
        long cooldownTime = bow.getDelay() * 1000L;
        long remaining = cooldownTime - (System.currentTimeMillis() - lastUse);
        return Math.max(0, remaining / 1000L);
    }

    private int getBowModelData(String bowId) {
        return Math.abs(bowId.hashCode() % 1_000_000) + 100_000;
    }
}
