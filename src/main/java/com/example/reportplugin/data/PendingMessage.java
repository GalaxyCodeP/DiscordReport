package com.example.reportplugin.data;

import java.util.UUID;

public class PendingMessage {
    private final UUID playerUUID;
    private final String playerName;
    private final String adminName;
    private final String message;
    private final long timestamp;

    public PendingMessage(UUID playerUUID, String playerName, String adminName, String message, long timestamp) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.adminName = adminName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
