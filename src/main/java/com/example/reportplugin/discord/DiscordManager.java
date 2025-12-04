package com.example.reportplugin.discord;

import com.example.reportplugin.ReportPlugin;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.awt.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordManager extends ListenerAdapter {
    private final ReportPlugin plugin;
    private JDA jda;
    private String channelId;
    private final Map<String, PendingReport> pendingReports = new ConcurrentHashMap<>();

    public DiscordManager(ReportPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        try {
            String token = plugin.getConfig().getString("discord.bot-token");
            this.channelId = plugin.getConfig().getString("discord.report-channel-id");

            if (token == null || token.equals("YOUR_BOT_TOKEN_HERE")) {
                plugin.getLogger().severe("Discord bot token not configured! Please set it in config.yml");
                return;
            }

            if (channelId == null || channelId.equals("YOUR_CHANNEL_ID_HERE")) {
                plugin.getLogger().severe("Discord channel ID not configured! Please set it in config.yml");
                return;
            }

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(this)
                    .build();

            jda.awaitReady();
            plugin.getLogger().info("Discord bot connected successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize Discord bot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }

    public void sendReport(UUID playerUUID, String playerName, String message) {
        if (jda == null) {
            plugin.getLogger().warning("Discord bot not initialized!");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            plugin.getLogger().warning("Could not find Discord channel with ID: " + channelId);
            return;
        }

        // Generate unique ID for this report
        String reportId = UUID.randomUUID().toString();

        // Store pending report
        pendingReports.put(reportId, new PendingReport(playerUUID, playerName, message));

        // Create embed
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("📝 รายงานจากผู้เล่น");
        embed.setColor(Color.ORANGE);
        embed.addField("ผู้เล่น", playerName, true);
        embed.addField("ข้อความ", message, false);
        embed.setTimestamp(Instant.now());
        embed.setFooter("Report ID: " + reportId.substring(0, 8));

        // Create buttons
        Button acceptButton = Button.success("accept:" + reportId, "✅ ยอมรับ");
        Button rejectButton = Button.danger("reject:" + reportId, "❌ ปฏิเสธ");

        // Send message
        channel.sendMessageEmbeds(embed.build())
                .setActionRow(acceptButton, rejectButton)
                .queue(
                        success -> plugin.getLogger().info("Report sent to Discord for player: " + playerName),
                        error -> plugin.getLogger().severe("Failed to send report to Discord: " + error.getMessage()));
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();

        if (!buttonId.startsWith("accept:") && !buttonId.startsWith("reject:")) {
            return;
        }

        String action = buttonId.split(":")[0];
        String reportId = buttonId.split(":")[1];

        PendingReport report = pendingReports.get(reportId);
        if (report == null) {
            event.reply("❌ รายงานนี้ได้รับการดำเนินการแล้ว หรือไม่พบข้อมูล").setEphemeral(true).queue();
            return;
        }

        // Create modal for reason
        TextInput reasonInput = TextInput.create("reason", "เหตุผล", TextInputStyle.PARAGRAPH)
                .setPlaceholder("กรุณาระบุเหตุผลในการตอบกลับ...")
                .setRequired(true)
                .setMinLength(1)
                .setMaxLength(500)
                .build();

        Modal modal = Modal.create("modal:" + action + ":" + reportId,
                action.equals("accept") ? "ยอมรับรายงาน" : "ปฏิเสธรายงาน")
                .addComponents(ActionRow.of(reasonInput))
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        String modalId = event.getModalId();

        if (!modalId.startsWith("modal:")) {
            return;
        }

        String[] parts = modalId.split(":");
        String action = parts[1];
        String reportId = parts[2];

        PendingReport report = pendingReports.remove(reportId);
        if (report == null) {
            event.reply("❌ รายงานนี้ได้รับการดำเนินการแล้ว").setEphemeral(true).queue();
            return;
        }

        String reason = event.getValue("reason").getAsString();
        String adminName = event.getUser().getName();

        // Update Discord message
        String statusEmoji = action.equals("accept") ? "✅" : "❌";
        String statusText = action.equals("accept") ? "ยอมรับ" : "ปฏิเสธ";

        EmbedBuilder updatedEmbed = new EmbedBuilder();
        updatedEmbed.setTitle("📝 รายงานจากผู้เล่น - " + statusText);
        updatedEmbed.setColor(action.equals("accept") ? Color.GREEN : Color.RED);
        updatedEmbed.addField("ผู้เล่น", report.getPlayerName(), true);
        updatedEmbed.addField("สถานะ", statusEmoji + " " + statusText, true);
        updatedEmbed.addField("ข้อความ", report.getMessage(), false);
        updatedEmbed.addField("ผู้ดำเนินการ", adminName, true);
        updatedEmbed.addField("เหตุผล", reason, false);
        updatedEmbed.setTimestamp(Instant.now());

        // Update the message and acknowledge the interaction
        event.editMessageEmbeds(updatedEmbed.build())
                .setComponents()
                .queue(
                        success -> plugin.getLogger().info("Report " + reportId + " processed by " + adminName),
                        error -> plugin.getLogger().severe("Failed to update Discord message: " + error.getMessage()));

        // Send message to player in Minecraft
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(report.getPlayerUUID());
            if (player != null && player.isOnline()) {
                // Player is online - send message immediately
                String messageKey = action.equals("accept") ? "report-accepted" : "report-rejected";
                String message = plugin.getConfig().getString("messages." + messageKey)
                        .replace("%admin%", adminName)
                        .replace("%reason%", reason);
                player.sendMessage(message);
                plugin.getLogger().info("Sent report response to online player: " + report.getPlayerName());
            } else {
                // Player is offline - save message for later delivery
                String messageKey = action.equals("accept") ? "report-accepted" : "report-rejected";
                String message = plugin.getConfig().getString("messages." + messageKey)
                        .replace("%admin%", adminName)
                        .replace("%reason%", reason);

                plugin.getMessageStorage().addMessage(
                        new com.example.reportplugin.data.PendingMessage(
                                report.getPlayerUUID(),
                                report.getPlayerName(),
                                adminName,
                                message,
                                System.currentTimeMillis()));
                plugin.getLogger()
                        .info("Player " + report.getPlayerName() + " is offline, message saved for later delivery.");
            }
        });
    }

    public boolean isConnected() {
        return jda != null && jda.getStatus() == JDA.Status.CONNECTED;
    }
}
