package com.poke.dcintegration.discord;

import com.poke.dcintegration.config.ModConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordBot {
    private JDA jda;
    private final ModConfig config;

    public DiscordBot(ModConfig config) {
        this.config = config;
    }

    public void start() {
        try {
            jda = JDABuilder.createDefault(config.botToken)
                    .enableIntents(
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS
                    )
                    .build();

            jda.awaitReady();
            System.out.println("[DCIntegration] Bot connected to Discord!");

        } catch (Exception e) {
            System.out.println("[DCIntegration] Failed to connect: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        if (jda == null) return;
        TextChannel channel = jda.getTextChannelById(config.channelID);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            System.out.println("[DCIntegration] Bot disconnected.");
        }
    }

    public JDA getJDA() {
        return jda;
    }

    public ModConfig getConfig() {
        return config;
    }

    public void sendMessageToChannel(String channelId, String message) {
        if (jda == null) return;
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel =
                jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }

    public void sendEmbed(net.dv8tion.jda.api.entities.MessageEmbed embed) {
        if (jda == null) return;
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel =
                jda.getTextChannelById(config.channelID);
        if (channel != null) {
            channel.sendMessageEmbeds(embed).queue();
        }
    }

    public void sendEmbedToChannel(String channelId, net.dv8tion.jda.api.entities.MessageEmbed embed) {
        if (jda == null) return;
        net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel =
                jda.getTextChannelById(channelId);
        if (channel != null) {
            channel.sendMessageEmbeds(embed).queue();
        }
    }
}