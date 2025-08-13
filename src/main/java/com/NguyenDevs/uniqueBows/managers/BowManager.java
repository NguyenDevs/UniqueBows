package com.NguyenDevs.uniqueBows.managers;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.models.CustomBow;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BowManager {

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
        List<String> colorizedLore = new ArrayList<>();
        for (String line : loreList) {
            colorizedLore.add(ColorUtils.colorize(line));
        }

        boolean craftable = section.getBoolean("craftable", true);
        boolean unbreakable = section.getBoolean("unbreakable", false);
        int delay = section.getInt("delay", 5);

        return new CustomBow(bowId, name, colorizedLore, craftable, unbreakable, delay);
    }

    public ItemStack createBowItem(String bowId) {
        CustomBow bow = customBows.get(bowId);
        if (bow == null) return null;

        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(bow.getName());
        meta.setLore(bow.getLore());

        if (bow.isUnbreakable()) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        // Add enchant glow
        meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        // Add custom model data or NBT to identify the bow
        meta.setCustomModelData(getBowModelData(bowId));

        item.setItemMeta(meta);
        return item;
    }

    public boolean isCustomBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW) return false;
        if (!item.hasItemMeta()) return false;

        ItemMeta meta = item.getItemMeta();
        return meta.hasCustomModelData() && getCustomBowId(item) != null;
    }

    public String getCustomBowId(ItemStack item) {
        if (!isCustomBow(item)) return null;

        int modelData = item.getItemMeta().getCustomModelData();
        for (Map.Entry<String, CustomBow> entry : customBows.entrySet()) {
            if (getBowModelData(entry.getKey()) == modelData) {
                return entry.getKey();
            }
        }
        return null;
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

        long cooldownTime = bow.getDelay() * 1000L; // Convert to milliseconds
        return (System.currentTimeMillis() - lastUse) < cooldownTime;
    }

    public void setCooldown(UUID playerId, String bowId) {
        playerCooldowns.computeIfAbsent(playerId, k -> new HashMap<>())
                .put(bowId, System.currentTimeMillis());
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
        return Math.max(0, remaining / 1000L); // Convert back to seconds
    }

    private int getBowModelData(String bowId) {
        // Simple hash to generate unique model data for each bow
        return Math.abs(bowId.hashCode() % 1000000) + 100000;
    }
}