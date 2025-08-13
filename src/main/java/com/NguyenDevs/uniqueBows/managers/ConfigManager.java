package com.NguyenDevs.uniqueBows.managers;

import com.NguyenDevs.uniqueBows.UniqueBows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class ConfigManager {

    private final UniqueBows plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration bows;
    private FileConfiguration recipes;

    private File configFile;
    private File messagesFile;
    private File bowsFile;
    private File recipesFile;

    public ConfigManager(UniqueBows plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        createFiles();
        loadDefaults();
    }

    private void createFiles() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        bowsFile = new File(plugin.getDataFolder(), "bows.yml");
        recipesFile = new File(plugin.getDataFolder(), "recipes.yml");

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }

        if (!messagesFile.exists()) {
            try {
                messagesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!bowsFile.exists()) {
            try {
                bowsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!recipesFile.exists()) {
            try {
                recipesFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        bows = YamlConfiguration.loadConfiguration(bowsFile);
        recipes = YamlConfiguration.loadConfiguration(recipesFile);
    }

    private void loadDefaults() {
        loadDefaultConfig();
        loadDefaultMessages();
        loadDefaultBows();
        loadDefaultRecipes();
        saveConfigs();
    }

    private void loadDefaultConfig() {
        if (!config.contains("version")) {
            config.set("version", "1.0");
        }
        if (!config.contains("debug")) {
            config.set("debug", false);
        }
        if (!config.contains("update-check")) {
            config.set("update-check", true);
        }
    }

    private void loadDefaultMessages() {
        if (!messages.contains("no-permission")) {
            messages.set("no-permission", "&cBạn không có quyền để sử dụng lệnh này!");
        }
        if (!messages.contains("plugin-reloaded")) {
            messages.set("plugin-reloaded", "&aPlugin đã được reload thành công!");
        }
        if (!messages.contains("bow-received")) {
            messages.set("bow-received", "&aB bạn đã nhận được {bow}!");
        }
        if (!messages.contains("bow-delay")) {
            messages.set("bow-delay", "&cBạn phải đợi {time} giây nữa mới có thể sử dụng cung này!");
        }
        if (!messages.contains("menu-title")) {
            messages.set("menu-title", "&6&lUnique Bows Menu");
        }
        if (!messages.contains("admin-menu-title")) {
            messages.set("admin-menu-title", "&c&lAdmin Bows Menu");
        }
        if (!messages.contains("recipe-menu-title")) {
            messages.set("recipe-menu-title", "&e&lBow Recipes");
        }
    }

    private void loadDefaultBows() {
        String[] bowTypes = {"ice_bow", "fire_bow", "wither_bow", "earthquake_bow",
                "meteor_bow", "poison_bow", "blaze_bow", "auto_bow",
                "thunder_bow", "pulsar_bow", "teleport_bow"};

        for (String bowType : bowTypes) {
            if (!bows.contains(bowType)) {
                bows.set(bowType + ".enabled", true);
                bows.set(bowType + ".name", getDefaultBowName(bowType));
                bows.set(bowType + ".lore", getDefaultBowLore(bowType));
                bows.set(bowType + ".craftable", true);
                bows.set(bowType + ".unbreakable", true);
                bows.set(bowType + ".delay", getDefaultDelay(bowType));
            }
        }
    }

    private void loadDefaultRecipes() {
        // Ice Bow Recipe
        if (!recipes.contains("ice_bow")) {
            recipes.set("ice_bow.shape", Arrays.asList(" SI", "S B", " SI"));
            recipes.set("ice_bow.ingredients.S", "STICK");
            recipes.set("ice_bow.ingredients.I", "ICE");
            recipes.set("ice_bow.ingredients.B", "BOW");
        }

        // Fire Bow Recipe
        if (!recipes.contains("fire_bow")) {
            recipes.set("fire_bow.shape", Arrays.asList(" SF", "S B", " SF"));
            recipes.set("fire_bow.ingredients.S", "STICK");
            recipes.set("fire_bow.ingredients.F", "FIRE_CHARGE");
            recipes.set("fire_bow.ingredients.B", "BOW");
        }

        // Continue for other bows...
        String[] bowTypes = {"wither_bow", "earthquake_bow", "meteor_bow", "poison_bow",
                "blaze_bow", "auto_bow", "thunder_bow", "pulsar_bow", "teleport_bow"};

        for (String bowType : bowTypes) {
            if (!recipes.contains(bowType)) {
                recipes.set(bowType + ".shape", Arrays.asList(" SM", "S B", " SM"));
                recipes.set(bowType + ".ingredients.S", "STICK");
                recipes.set(bowType + ".ingredients.M", getDefaultRecipeItem(bowType));
                recipes.set(bowType + ".ingredients.B", "BOW");
            }
        }
    }

    private String getDefaultBowName(String bowType) {
        switch (bowType) {
            case "ice_bow": return "&b&lIce Bow";
            case "fire_bow": return "&c&lFire Bow";
            case "wither_bow": return "&8&lWither Bow";
            case "earthquake_bow": return "&6&lEarthquake Bow";
            case "meteor_bow": return "&4&lMeteor Bow";
            case "poison_bow": return "&2&lPoison Bow";
            case "blaze_bow": return "&e&lBlaze Bow";
            case "auto_bow": return "&9&lAuto Bow";
            case "thunder_bow": return "&d&lThunder Bow";
            case "pulsar_bow": return "&5&lPulsar Bow";
            case "teleport_bow": return "&a&lTeleport Bow";
            default: return "&f&lUnknown Bow";
        }
    }

    private String[] getDefaultBowLore(String bowType) {
        switch (bowType) {
            case "ice_bow": return new String[]{"&7Đóng băng kẻ địch khi bắn trúng", "&7Delay: 5 giây"};
            case "fire_bow": return new String[]{"&7Gây cháy kẻ địch khi bắn trúng", "&7Delay: 3 giây"};
            case "wither_bow": return new String[]{"&7Bắn đầu lâu wither", "&7Delay: 8 giây"};
            case "earthquake_bow": return new String[]{"&7Gây động đất tại vị trí kẻ địch", "&7Delay: 15 giây"};
            case "meteor_bow": return new String[]{"&7Triệu hồi thiên thạch", "&7Delay: 20 giây"};
            case "poison_bow": return new String[]{"&7Gây độc cho kẻ địch", "&7Delay: 4 giây"};
            case "blaze_bow": return new String[]{"&7Bắn tia lửa gây bỏng cháy", "&7Delay: 6 giây"};
            case "auto_bow": return new String[]{"&7Bắn liên tục nhiều mũi tên", "&7Delay: 10 giây"};
            case "thunder_bow": return new String[]{"&7Gây sấm sét tại kẻ địch", "&7Delay: 12 giây"};
            case "pulsar_bow": return new String[]{"&7Tạo hố đen hút kẻ địch", "&7Delay: 25 giây"};
            case "teleport_bow": return new String[]{"&7Dịch chuyển đến vị trí bắn", "&7Delay: 8 giây"};
            default: return new String[]{"&7Unknown bow"};
        }
    }

    private int getDefaultDelay(String bowType) {
        switch (bowType) {
            case "ice_bow": return 5;
            case "fire_bow": return 3;
            case "wither_bow": return 8;
            case "earthquake_bow": return 15;
            case "meteor_bow": return 20;
            case "poison_bow": return 4;
            case "blaze_bow": return 6;
            case "auto_bow": return 10;
            case "thunder_bow": return 12;
            case "pulsar_bow": return 25;
            case "teleport_bow": return 8;
            default: return 5;
        }
    }

    private String getDefaultRecipeItem(String bowType) {
        switch (bowType) {
            case "wither_bow": return "WITHER_SKELETON_SKULL";
            case "earthquake_bow": return "COBBLESTONE";
            case "meteor_bow": return "MAGMA_BLOCK";
            case "poison_bow": return "SPIDER_EYE";
            case "blaze_bow": return "BLAZE_ROD";
            case "auto_bow": return "REDSTONE";
            case "thunder_bow": return "LIGHTNING_ROD";
            case "pulsar_bow": return "OBSIDIAN";
            case "teleport_bow": return "ENDER_PEARL";
            default: return "DIAMOND";
        }
    }

    public void saveConfigs() {
        try {
            config.save(configFile);
            messages.save(messagesFile);
            bows.save(bowsFile);
            recipes.save(recipesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public FileConfiguration getBows() {
        return bows;
    }

    public FileConfiguration getRecipes() {
        return recipes;
    }
}