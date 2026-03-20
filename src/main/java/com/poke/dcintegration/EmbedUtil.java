package com.poke.dcintegration;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.Color;
import java.time.Instant;

public class EmbedUtil {

    // Crafatar base URL for player skins
    private static final String SKIN_URL = "https://crafatar.com/avatars/%s?size=64&overlay";
    private static final String SERVER_ICON = "https://crafatar.com/renders/head/8667ba71b85a4004af54457a9734eed7";

    // Get skin URL from player UUID
    public static String getSkinUrl(String uuid) {
        return String.format(SKIN_URL, uuid);
    }

    // Chat message embed
    public static MessageEmbed chatEmbed(String playerName, String uuid, String message) {
        return new EmbedBuilder()
                .setAuthor(playerName, null, getSkinUrl(uuid))
                .setDescription(message)
                .setColor(Color.decode("#2ecc71"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Player join embed
    public static MessageEmbed joinEmbed(String playerName, String uuid) {
        return new EmbedBuilder()
                .setAuthor(playerName, null, getSkinUrl(uuid))
                .setDescription("➕ **" + playerName + "** joined the server!")
                .setColor(Color.decode("#2ecc71"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Player leave embed
    public static MessageEmbed leaveEmbed(String playerName, String uuid) {
        return new EmbedBuilder()
                .setAuthor(playerName, null, getSkinUrl(uuid))
                .setDescription("➖ **" + playerName + "** left the server!")
                .setColor(Color.decode("#e74c3c"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Death message embed
    public static MessageEmbed deathEmbed(String playerName, String uuid, String deathMessage) {
        return new EmbedBuilder()
                .setAuthor(playerName, null, getSkinUrl(uuid))
                .setDescription("💀 " + deathMessage)
                .setColor(Color.decode("#992d22"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Achievement embed
    public static MessageEmbed achievementEmbed(String playerName, String uuid, String achievement) {
        return new EmbedBuilder()
                .setAuthor(playerName, null, getSkinUrl(uuid))
                .setDescription("🏆 **" + playerName + "** has made the advancement **[" + achievement + "]**")
                .setColor(Color.decode("#f1c40f"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Server start embed
    public static MessageEmbed serverStartEmbed() {
        return new EmbedBuilder()
                .setAuthor("Server", null, SERVER_ICON)
                .setDescription("✅ **Server is now online!**")
                .setColor(Color.decode("#2ecc71"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Server stop embed
    public static MessageEmbed serverStopEmbed() {
        return new EmbedBuilder()
                .setAuthor("Server", null, SERVER_ICON)
                .setDescription("🔴 **Server is shutting down!**")
                .setColor(Color.decode("#e74c3c"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Broadcast embed
    public static MessageEmbed broadcastEmbed(String message, String adminName) {
        return new EmbedBuilder()
                .setAuthor("📢 Server Broadcast", null, SERVER_ICON)
                .setDescription(message)
                .setFooter("Sent by " + adminName)
                .setColor(Color.decode("#3498db"))
                .setTimestamp(Instant.now())
                .build();
    }

    // Performance alert embed
    public static MessageEmbed performanceAlertEmbed(double tps, long usedRamMB, long maxRamMB) {
        Color color = tps < 10 ? Color.decode("#e74c3c") : Color.decode("#e67e22");
        return new EmbedBuilder()
                .setAuthor("⚠ Performance Alert", null, SERVER_ICON)
                .setDescription("Server performance is degrading!")
                .addField("TPS", String.format("%.1f / 20.0", tps), true)
                .addField("RAM Usage", usedRamMB + "MB / " + maxRamMB + "MB", true)
                .setColor(color)
                .setTimestamp(Instant.now())
                .build();
    }

    // Performance ok embed
    public static MessageEmbed performanceOkEmbed(double tps, long usedRamMB, long maxRamMB) {
        return new EmbedBuilder()
                .setAuthor("✅ Performance Recovered", null, SERVER_ICON)
                .setDescription("Server performance is back to normal!")
                .addField("TPS", String.format("%.1f / 20.0", tps), true)
                .addField("RAM Usage", usedRamMB + "MB / " + maxRamMB + "MB", true)
                .setColor(Color.decode("#2ecc71"))
                .setTimestamp(Instant.now())
                .build();
    }
}