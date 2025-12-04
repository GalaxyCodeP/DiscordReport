package com.example.reportplugin.discord;

import java.util.UUID;

public class PendingReport {
    private final UUID playerUUID;
    private final String playerName;
    private final String message;
    private final long timestamp;

    public PendingReport(UUID playerUUID, String playerName, String message) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
