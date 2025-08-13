package com.NguyenDevs.uniqueBows;

import com.NguyenDevs.uniqueBows.commands.UniqueBowsCommand;
import com.NguyenDevs.uniqueBows.commands.UniqueBowsTabCompleter;
import com.NguyenDevs.uniqueBows.listeners.bowlisteners.*;
import com.NguyenDevs.uniqueBows.managers.BowManager;
import com.NguyenDevs.uniqueBows.managers.ConfigManager;
import com.NguyenDevs.uniqueBows.managers.RecipeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public final class UniqueBows extends JavaPlugin {

    private static UniqueBows instance;
    private ConfigManager configManager;
    private BowManager bowManager;
    private RecipeManager recipeManager;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        configManager = new ConfigManager(this);
        bowManager = new BowManager(this);
        recipeManager = new RecipeManager(this);

        // Load configurations
        configManager.loadConfigs();
        bowManager.loadBows();
        recipeManager.loadRecipes();

        // Register commands
        getCommand("ub").setExecutor(new UniqueBowsCommand(this));
        getCommand("ub").setTabCompleter(new UniqueBowsTabCompleter());

        // Register listeners
        registerListeners();
        printLogo();
        getLogger().info("UniqueBows plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Save all configurations
        if (configManager != null) {
            configManager.saveConfigs();
        }

        getLogger().info("UniqueBows plugin has been disabled!");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new IceBowListener(this), this);
        getServer().getPluginManager().registerEvents(new FireBowListener(this), this);
        getServer().getPluginManager().registerEvents(new WitherBowListener(this), this);
        getServer().getPluginManager().registerEvents(new EarthQuakeBowListener(this), this);
        getServer().getPluginManager().registerEvents(new MeteorBowListener(this), this);
        getServer().getPluginManager().registerEvents(new PoisonBowListener(this), this);
        getServer().getPluginManager().registerEvents(new BlazeBowListener(this), this);
        getServer().getPluginManager().registerEvents(new AutoBowListener(this), this);
        getServer().getPluginManager().registerEvents(new ThunderBowListener(this), this);
        getServer().getPluginManager().registerEvents(new PulsarBowListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportBowListener(this), this);
    }

    public void reloadPlugin() {
        configManager.loadConfigs();
        bowManager.loadBows();
        recipeManager.loadRecipes();
    }

    public static UniqueBows getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public BowManager getBowManager() {
        return bowManager;
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }

    public void printLogo() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e   ██╗   ██╗███╗   ██╗██╗ ██████╗ ██╗   ██╗███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e   ██║   ██║████╗  ██║██║██╔═══██╗██║   ██║██╔════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e   ██║   ██║██╔██╗ ██║██║██║   ██║██║   ██║█████╗  "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e   ██║   ██║██║╚██╗██║██║██║▄▄ ██║██║   ██║██╔══╝  "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e   ╚██████╔╝██║ ╚████║██║╚██████╔╝╚██████╔╝███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e    ╚═════╝ ╚═╝  ╚═══╝╚═╝ ╚══▀▀═╝  ╚═════╝ ╚══════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6   ██████╗  ██████╗ ██╗    ██╗███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6   ██╔══██╗██╔═══██╗██║    ██║██╔════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6   ██████╔╝██║   ██║██║ █╗ ██║███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6   ██╔══██╗██║   ██║██║███╗██║╚════██║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6   ██████╔╝╚██████╔╝╚███╔███╔╝███████║"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6   ╚═════╝  ╚═════╝  ╚══╝╚══╝ ╚══════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&e         Unique Bows"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6         Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }
}