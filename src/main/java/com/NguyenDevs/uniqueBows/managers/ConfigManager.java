package com.NguyenDevs.uniqueBows.managers;

import com.NguyenDevs.uniqueBows.UniqueBows;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

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
        ensureDataFolder();
        config = loadOrCopy("config.yml");
        messages = loadOrCopy("messages.yml");
        bows = loadOrCopy("bows.yml");
        recipes = loadOrCopy("recipes.yml");
        ensureDefaultConfig(config);
        ensureDefaultMessages(messages);
        ensureDefaultBows(bows);
        ensureDefaultRecipes(recipes);
        saveConfigs();
    }

    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(getConfigFile());
        messages = YamlConfiguration.loadConfiguration(getMessagesFile());
        bows = YamlConfiguration.loadConfiguration(getBowsFile());
        recipes = YamlConfiguration.loadConfiguration(getRecipesFile());
        mergeDefaults(config, "config.yml");
        mergeDefaults(messages, "messages.yml");
        mergeDefaults(bows, "bows.yml");
        mergeDefaults(recipes, "recipes.yml");
        ensureDefaultConfig(config);
        ensureDefaultMessages(messages);
        ensureDefaultBows(bows);
        ensureDefaultRecipes(recipes);
        saveConfigs();
    }

    private void ensureDataFolder() {
        File data = plugin.getDataFolder();
        if (!data.exists()) data.mkdirs();
    }

    private FileConfiguration loadOrCopy(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, false);
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        mergeDefaults(cfg, fileName);
        switch (fileName) {
            case "config.yml": configFile = file; break;
            case "messages.yml": messagesFile = file; break;
            case "bows.yml": bowsFile = file; break;
            case "recipes.yml": recipesFile = file; break;
        }
        return cfg;
    }

    private void mergeDefaults(FileConfiguration cfg, String resourceName) {
        try (InputStream in = plugin.getResource(resourceName)) {
            if (in != null) {
                YamlConfiguration def = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(in, StandardCharsets.UTF_8));
                for (String key : def.getKeys(true)) {
                    if (!cfg.contains(key)) {
                        cfg.set(key, def.get(key));
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    private void ensureDefaultConfig(FileConfiguration cfg) {
        addIfAbsent(cfg, "version", "1.0");
        addIfAbsent(cfg, "debug", false);
        addIfAbsent(cfg, "update-check", true);
    }

    private void ensureDefaultMessages(FileConfiguration msg) {
        Map<String, Object> defaults = new LinkedHashMap<>();
        defaults.put("prefix", "&6[UniqueBows] &r");
        defaults.put("no-permission", "&cYou don't have permission to use this command!");
        defaults.put("plugin-reloaded", "&aPlugin reloaded successfully!");
        defaults.put("player-only", "&cOnly players can use this command!");
        defaults.put("unknown-command", "&cUnknown command! Use &e/ub &cto see the command list.");
        defaults.put("bow-received", "&aYou have received {bow}&a!");
        defaults.put("bow-delay", "&cYou must wait &e{time} &cseconds before using this bow again!");
        defaults.put("bow-not-found", "&cBow not found!");
        defaults.put("recipe-not-found", "&cRecipe for this bow not found!");
        defaults.put("menu-title", "&6&lUnique Bows Menu");
        defaults.put("admin-menu-title", "&c&lAdmin Bows Menu");
        defaults.put("recipe-menu-title", "&e&lBow Recipes");
        defaults.put("menu-border-name", "&7");
        defaults.put("recipe-view-lore", "&e&lClick to view the recipe!");
        defaults.put("admin-get-bow", "&a&lClick to get this bow!");
        defaults.put("back-button", "&c&lBack");
        defaults.put("recipe-result", "&a&lResult");
        defaults.put("command-help.header", "&6&l=== UniqueBows Commands ===");
        defaults.put("command-help.user-commands", "&e&lPlayer Commands:");
        defaults.put("command-help.admin-commands", "&c&lAdmin Commands:");
        defaults.put("command-help.menu", "&e/ub menu &7- Open the bow list menu");
        defaults.put("command-help.recipe", "&e/ub recipe &7- View crafting recipes");
        defaults.put("command-help.admin", "&c/ub admin &7- Open the admin menu");
        defaults.put("command-help.reload", "&c/ub reload &7- Reload the plugin");
        defaults.put("command-help.footer", "&6&l==========================");
        defaults.put("effects.ice.freeze", "&b{target} has been frozen!");
        defaults.put("effects.fire.burn", "&c{target} is burning!");
        defaults.put("effects.wither.hit", "&8A wither skull has struck {target}!");
        defaults.put("effects.poison.poisoned", "&2{target} has been poisoned!");
        defaults.put("effects.thunder.struck", "&d{target} was struck by lightning!");
        defaults.put("effects.teleport.success", "&aTeleport successful!");
        defaults.put("effects.teleport.failed", "&cUnable to teleport to this location!");
        defaults.put("effects.auto.arrows-shot", "&9Shot {count} arrows!");
        defaults.put("effects.blaze.flame-hit", "&eFlame hit {target}!");
        defaults.put("effects.pulsar.black-hole", "&5A black hole has been created!");
        defaults.put("errors.bow-creation-failed", "&cFailed to create bow!");
        defaults.put("errors.recipe-load-failed", "&cFailed to load recipe!");
        defaults.put("errors.config-save-failed", "&cFailed to save configuration!");
        defaults.put("errors.permission-denied", "&cYou don't have permission to use this feature!");
        defaults.put("success.bow-given", "&aGave {bow} &ato {player}!");
        defaults.put("success.config-saved", "&aConfiguration saved!");
        defaults.put("success.recipe-registered", "&aRecipe for {bow} has been registered!");
        defaults.put("admin.bow-list-header", "&c&l=== Admin Bow List ===");
        defaults.put("admin.bow-info", "&7ID: &f{id} &7| &7Name: {name} &7| &7Delay: &f{delay}s");
        defaults.put("admin.bow-status", "&7Enabled: {enabled} &7| &7Craftable: {craftable}");
        defaults.put("admin.reload-complete", "&aPlugin reload complete! Loaded {bows} bows and {recipes} recipes.");
        for (Map.Entry<String, Object> e : defaults.entrySet()) {
            addIfAbsent(msg, e.getKey(), e.getValue());
        }
    }

    private void ensureDefaultBows(FileConfiguration b) {
        List<String> ids = Arrays.asList(
                "ice_bow", "fire_bow", "wither_bow", "earthquake_bow",
                "meteor_bow", "poison_bow", "blaze_bow", "auto_bow",
                "thunder_bow", "pulsar_bow", "teleport_bow"
        );
        for (String id : ids) {
            addIfAbsent(b, id + ".enabled", true);
            addIfAbsent(b, id + ".custom-model-data", "none");
            addIfAbsent(b, id + ".name", getDefaultBowName(id));
            addIfAbsent(b, id + ".lore", getDefaultBowLore(id));
            addIfAbsent(b, id + ".craftable", true);
            addIfAbsent(b, id + ".unbreakable", true);
            addIfAbsent(b, id + ".delay", getDefaultDelay(id));
        }
    }

    private void ensureDefaultRecipes(FileConfiguration r) {
        if (!r.contains("ice_bow")) {
            r.set("ice_bow.shape", Arrays.asList(" SI", "S B", " SI"));
            r.set("ice_bow.ingredients.S", "STICK");
            r.set("ice_bow.ingredients.I", "ICE");
            r.set("ice_bow.ingredients.B", "BOW");
        }
        if (!r.contains("fire_bow")) {
            r.set("fire_bow.shape", Arrays.asList(" SF", "S B", " SF"));
            r.set("fire_bow.ingredients.S", "STICK");
            r.set("fire_bow.ingredients.F", "FIRE_CHARGE");
            r.set("fire_bow.ingredients.B", "BOW");
        }
        List<String> others = Arrays.asList(
                "wither_bow", "earthquake_bow", "meteor_bow", "poison_bow",
                "blaze_bow", "auto_bow", "thunder_bow", "pulsar_bow", "teleport_bow"
        );
        for (String id : others) {
            if (!r.contains(id)) {
                r.set(id + ".shape", Arrays.asList(" SM", "S B", " SM"));
                r.set(id + ".ingredients.S", "STICK");
                r.set(id + ".ingredients.M", getDefaultRecipeItem(id));
                r.set(id + ".ingredients.B", "BOW");
            }
        }
    }

    private void addIfAbsent(FileConfiguration cfg, String path, Object value) {
        if (!cfg.contains(path)) cfg.set(path, value);
    }

    private String getDefaultBowName(String id) {
        switch (id) {
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

    private List<String> getDefaultBowLore(String id) {
        switch (id) {
            case "ice_bow": return Arrays.asList("&7Freeze enemies on hit", "&7Delay: 5s");
            case "fire_bow": return Arrays.asList("&7Ignite enemies on hit", "&7Delay: 3s");
            case "wither_bow": return Arrays.asList("&7Shoot wither skulls", "&7Delay: 8s");
            case "earthquake_bow": return Arrays.asList("&7Trigger an earthquake", "&7Delay: 15s");
            case "meteor_bow": return Arrays.asList("&7Summon a meteor", "&7Delay: 20s");
            case "poison_bow": return Arrays.asList("&7Poison on hit", "&7Delay: 4s");
            case "blaze_bow": return Arrays.asList("&7Flame burst on hit", "&7Delay: 6s");
            case "auto_bow": return Arrays.asList("&7Rapid multi-shot", "&7Delay: 10s");
            case "thunder_bow": return Arrays.asList("&7Call down lightning", "&7Delay: 12s");
            case "pulsar_bow": return Arrays.asList("&7Create a black hole", "&7Delay: 25s");
            case "teleport_bow": return Arrays.asList("&7Teleport to arrow", "&7Delay: 8s");
            default: return Collections.singletonList("&7Unknown bow");
        }
    }

    private int getDefaultDelay(String id) {
        switch (id) {
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

    private String getDefaultRecipeItem(String id) {
        switch (id) {
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
            if (config != null) config.save(getConfigFile());
            if (messages != null) messages.save(getMessagesFile());
            if (bows != null) bows.save(getBowsFile());
            if (recipes != null) recipes.save(getRecipesFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File getConfigFile() {
        if (configFile == null) configFile = new File(plugin.getDataFolder(), "config.yml");
        return configFile;
    }

    private File getMessagesFile() {
        if (messagesFile == null) messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        return messagesFile;
    }

    private File getBowsFile() {
        if (bowsFile == null) bowsFile = new File(plugin.getDataFolder(), "bows.yml");
        return bowsFile;
    }

    private File getRecipesFile() {
        if (recipesFile == null) recipesFile = new File(plugin.getDataFolder(), "recipes.yml");
        return recipesFile;
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getMessages() { return messages; }
    public FileConfiguration getBows() { return bows; }
    public FileConfiguration getRecipes() { return recipes; }
}
