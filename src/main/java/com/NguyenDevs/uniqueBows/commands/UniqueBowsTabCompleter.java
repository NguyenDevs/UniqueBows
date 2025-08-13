package com.NguyenDevs.uniqueBows.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UniqueBowsTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();

            // Add commands based on permissions
            if (sender.hasPermission("ub.use")) {
                subCommands.add("menu");
                subCommands.add("recipe");
            }

            if (sender.hasPermission("ub.admin")) {
                subCommands.add("admin");
                subCommands.add("reload");
            }

            String input = args[0].toLowerCase();
            for (String subCommand : subCommands) {
                if (subCommand.toLowerCase().startsWith(input)) {
                    completions.add(subCommand);
                }
            }
        }

        return completions;
    }
}