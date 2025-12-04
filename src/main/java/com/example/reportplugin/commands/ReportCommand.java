package com.example.reportplugin.commands;

import com.example.reportplugin.ReportPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReportCommand implements CommandExecutor {
    private final ReportPlugin plugin;

    public ReportCommand(ReportPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cคำสั่งนี้สามารถใช้ได้โดยผู้เล่นเท่านั้น");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(plugin.getConfig().getString("messages.report-usage"));
            return true;
        }

        // Join all arguments into a single message
        String message = String.join(" ", args);

        // Check if Discord is connected
        if (!plugin.getDiscordManager().isConnected()) {
            player.sendMessage(plugin.getConfig().getString("messages.discord-error"));
            plugin.getLogger().warning(
                    "Player " + player.getName() + " tried to send a report but Discord bot is not connected.");
            return true;
        }

        // Send report to Discord
        plugin.getDiscordManager().sendReport(player.getUniqueId(), player.getName(), message);

        // Confirm to player
        player.sendMessage(plugin.getConfig().getString("messages.report-sent"));

        // Log
        plugin.getLogger().info("Report from " + player.getName() + ": " + message);

        return true;
    }
}
