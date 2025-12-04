package com.example.reportplugin.listeners;

import com.example.reportplugin.ReportPlugin;
import com.example.reportplugin.data.PendingMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoinListener implements Listener {
    private final ReportPlugin plugin;

    public PlayerJoinListener(ReportPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Check if player has pending messages
        if (!plugin.getMessageStorage().hasMessages(player.getUniqueId())) {
            return;
        }

        // Schedule delayed message delivery (10 seconds = 200 ticks)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            // Check if player is still online
            if (!player.isOnline()) {
                return;
            }

            List<PendingMessage> messages = plugin.getMessageStorage().getMessages(player.getUniqueId());

            if (messages.isEmpty()) {
                return;
            }

            // Send notification header
            String headerMessage = plugin.getConfig().getString("messages.pending-messages-header");
            if (headerMessage != null && !headerMessage.isEmpty()) {
                player.sendMessage(headerMessage);
            }

            // Send all pending messages
            for (PendingMessage msg : messages) {
                player.sendMessage(msg.getMessage());
            }

            // Send footer
            String footerMessage = plugin.getConfig().getString("messages.pending-messages-footer");
            if (footerMessage != null && !footerMessage.isEmpty()) {
                player.sendMessage(footerMessage);
            }

            // Remove delivered messages
            plugin.getMessageStorage().removeMessages(player.getUniqueId());

            plugin.getLogger().info("Delivered " + messages.size() + " pending message(s) to " + player.getName());
        }, 200L); // 10 seconds delay
    }
}
