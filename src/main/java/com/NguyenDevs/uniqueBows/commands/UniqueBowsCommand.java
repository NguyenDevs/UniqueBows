package com.NguyenDevs.uniqueBows.commands;

import com.NguyenDevs.uniqueBows.UniqueBows;
import com.NguyenDevs.uniqueBows.guis.BowMenuGUI;
import com.NguyenDevs.uniqueBows.guis.RecipeMenuGUI;
import com.NguyenDevs.uniqueBows.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class UniqueBowsCommand implements CommandExecutor {

    private final UniqueBows plugin;

    public UniqueBowsCommand(UniqueBows plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();

        if (args.length == 0) {
            // Display command help using messages from config
            sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.header")));

            if (sender.hasPermission("ub.use")) {
                sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.user-commands")));
                sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.menu")));
                sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.recipe")));
            }

            if (sender.hasPermission("ub.admin")) {
                sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.admin-commands")));
                sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.admin")));
                sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.reload")));
            }

            sender.sendMessage(ColorUtils.colorize(messages.getString("command-help.footer")));
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "menu":
                handleMenuCommand(sender);
                break;
            case "recipe":
                handleRecipeCommand(sender);
                break;
            case "admin":
                handleAdminCommand(sender);
                break;
            case "reload":
                handleReloadCommand(sender);
                break;
            default:
                sender.sendMessage(ColorUtils.colorize(messages.getString("unknown-command")));
                break;
        }

        return true;
    }

    private void handleMenuCommand(CommandSender sender) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();

        if (!sender.hasPermission("ub.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("no-permission")));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("player-only")));
            return;
        }

        Player player = (Player) sender;
        BowMenuGUI gui = new BowMenuGUI(plugin, false);
        gui.openMenu(player);
    }

    private void handleRecipeCommand(CommandSender sender) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();

        if (!sender.hasPermission("ub.use")) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("no-permission")));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("player-only")));
            return;
        }

        Player player = (Player) sender;
        RecipeMenuGUI gui = new RecipeMenuGUI(plugin);
        gui.openMenu(player);
    }

    private void handleAdminCommand(CommandSender sender) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();

        if (!sender.hasPermission("ub.admin")) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("no-permission")));
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("player-only")));
            return;
        }

        Player player = (Player) sender;
        BowMenuGUI gui = new BowMenuGUI(plugin, true);
        gui.openMenu(player);
    }

    private void handleReloadCommand(CommandSender sender) {
        FileConfiguration messages = plugin.getConfigManager().getMessages();

        if (!sender.hasPermission("ub.admin")) {
            sender.sendMessage(ColorUtils.colorize(messages.getString("no-permission")));
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(ColorUtils.colorize(messages.getString("plugin-reloaded")));
    }
}