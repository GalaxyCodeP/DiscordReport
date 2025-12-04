package com.example.reportplugin;

import com.example.reportplugin.commands.ReportCommand;
import com.example.reportplugin.data.MessageStorage;
import com.example.reportplugin.discord.DiscordManager;
import com.example.reportplugin.listeners.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

public class ReportPlugin extends JavaPlugin {
    private DiscordManager discordManager;
    private MessageStorage messageStorage;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize message storage
        messageStorage = new MessageStorage(this);

        // Initialize Discord manager
        discordManager = new DiscordManager(this);

        // Initialize Discord bot asynchronously to avoid blocking server startup
        getServer().getScheduler().runTaskAsynchronously(this, () -> {
            discordManager.initialize();
        });

        // Register commands
        getCommand("report").setExecutor(new ReportCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);

        getLogger().info("ReportPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Shutdown Discord bot
        if (discordManager != null) {
            discordManager.shutdown();
        }

        getLogger().info("ReportPlugin has been disabled!");
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public MessageStorage getMessageStorage() {
        return messageStorage;
    }
}
