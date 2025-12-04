package com.example.reportplugin.data;

import com.example.reportplugin.ReportPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MessageStorage {
    private final ReportPlugin plugin;
    private final File storageFile;
    private final Map<UUID, List<PendingMessage>> pendingMessages = new ConcurrentHashMap<>();

    public MessageStorage(ReportPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "pending_messages.yml");
        load();
    }

    public void addMessage(PendingMessage message) {
        pendingMessages.computeIfAbsent(message.getPlayerUUID(), k -> new ArrayList<>()).add(message);
        save();
    }

    public List<PendingMessage> getMessages(UUID playerUUID) {
        return pendingMessages.getOrDefault(playerUUID, new ArrayList<>());
    }

    public void removeMessages(UUID playerUUID) {
        pendingMessages.remove(playerUUID);
        save();
    }

    public boolean hasMessages(UUID playerUUID) {
        return pendingMessages.containsKey(playerUUID) && !pendingMessages.get(playerUUID).isEmpty();
    }

    private void load() {
        if (!storageFile.exists()) {
            return;
        }

        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(storageFile);

            for (String uuidString : config.getKeys(false)) {
                try {
                    UUID playerUUID = UUID.fromString(uuidString);
                    List<PendingMessage> messages = new ArrayList<>();

                    @SuppressWarnings("unchecked")
                    List<Map<?, ?>> messageList = (List<Map<?, ?>>) config.getList(uuidString);
                    if (messageList != null) {
                        for (Map<?, ?> messageData : messageList) {
                            String playerName = (String) messageData.get("playerName");
                            String adminName = (String) messageData.get("adminName");
                            String message = (String) messageData.get("message");

                            Object timestampObj = messageData.get("timestamp");
                            long timestamp = 0;
                            if (timestampObj instanceof Long) {
                                timestamp = (Long) timestampObj;
                            } else if (timestampObj instanceof Integer) {
                                timestamp = ((Integer) timestampObj).longValue();
                            }

                            if (playerName != null && adminName != null && message != null) {
                                messages.add(new PendingMessage(playerUUID, playerName, adminName, message, timestamp));
                            }
                        }
                    }

                    if (!messages.isEmpty()) {
                        pendingMessages.put(playerUUID, messages);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in storage: " + uuidString);
                }
            }

            plugin.getLogger().info("Loaded " + pendingMessages.size() + " players with pending messages");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load pending messages: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            if (!storageFile.exists()) {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            }

            FileConfiguration config = new YamlConfiguration();

            for (Map.Entry<UUID, List<PendingMessage>> entry : pendingMessages.entrySet()) {
                List<Map<String, Object>> messageList = entry.getValue().stream()
                        .map(msg -> {
                            Map<String, Object> map = new HashMap<>();
                            map.put("playerName", msg.getPlayerName());
                            map.put("adminName", msg.getAdminName());
                            map.put("message", msg.getMessage());
                            map.put("timestamp", msg.getTimestamp());
                            return map;
                        })
                        .collect(Collectors.toList());

                config.set(entry.getKey().toString(), messageList);
            }

            config.save(storageFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save pending messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
