package com.poke.dcintegration;

import com.poke.dcintegration.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public class ChatBridge extends ListenerAdapter {
    private final DiscordBot bot;
    private MinecraftServer server;

    public ChatBridge(DiscordBot bot) {
        this.bot = bot;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    // MC -> Discord
    public void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            String playerName = sender.getName().getString();
            String uuid = sender.getUUID().toString();
            String content = message.decoratedContent().getString();
            bot.sendEmbed(EmbedUtil.chatEmbed(playerName, uuid, content));
        });
    }

    // Discord -> MC
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (!event.getChannel().getId().equals(bot.getConfig().channelID)) return;

        String discordName = event.getAuthor().getName();
        String content = event.getMessage().getContentDisplay();
        String formatted = "§9[Discord]§r <" + discordName + "> " + content;

        if (server != null) {
            server.execute(() -> {
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(formatted), false
                );
            });
        }
    }
}