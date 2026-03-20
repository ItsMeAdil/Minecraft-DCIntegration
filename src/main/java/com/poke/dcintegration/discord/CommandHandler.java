package com.poke.dcintegration.discord;

import com.poke.dcintegration.EmbedUtil;
import com.poke.dcintegration.PlayerLinker;
import com.poke.dcintegration.config.CustomCommand;
import com.poke.dcintegration.config.ModConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandHandler extends ListenerAdapter {
    private final ModConfig config;
    private final List<CustomCommand> customCommands;
    private MinecraftServer server;
    private PlayerLinker linker;

    private final DiscordBot bot;

    public CommandHandler(ModConfig config, List<CustomCommand> customCommands, DiscordBot bot) {
        this.config = config;
        this.customCommands = customCommands;
        this.bot = bot;
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public void setLinker(PlayerLinker linker) {
        this.linker = linker;
    }

    public void registerCommands(JDA jda) {
        List<CommandData> commands = new ArrayList<>();

        // Add linking commands only if linking is enabled
        if (config.linkingEnabled) {
            commands.add(Commands.slash("verify", "Verify your Minecraft account")
                    .addOptions(new OptionData(OptionType.STRING, "code", "Your verification code", true)));
            commands.add(Commands.slash("linked", "Check if your account is linked"));
            commands.add(Commands.slash("unlink", "Unlink a player's account")
                    .addOptions(new OptionData(OptionType.STRING, "player", "Minecraft username", true)));
        }

        // Add custom commands from JSON
        for (CustomCommand cmd : customCommands) {
            var slash = Commands.slash(cmd.name, cmd.description);

            if (cmd.args != null) {
                for (CustomCommand.CustomCommandArg arg : cmd.args) {
                    slash.addOptions(new OptionData(
                            OptionType.STRING,
                            arg.name,
                            arg.description,
                            !arg.optional
                    ));
                }
            }

            commands.add(slash);
        }

        // First clear ALL existing commands then register fresh
        jda.getGuildById(config.guildID)
                .updateCommands()
                .queue(cleared -> {
                    System.out.println("[DCIntegration] Cleared existing commands!");
                    jda.getGuildById(config.guildID)
                            .updateCommands()
                            .addCommands(commands)
                            .queue(
                                    success -> System.out.println("[DCIntegration] Registered " + commands.size() + " commands!"),
                                    error -> System.out.println("[DCIntegration] Failed to register commands: " + error.getMessage())
                            );
                });
    }

    private boolean isAdmin(SlashCommandInteractionEvent event) {
        if (event.getMember() == null) return false;
        return event.getMember().getRoles().stream()
                .anyMatch(role -> Arrays.asList(config.adminRoleIDs).contains(role.getId()));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String name = event.getName();

        // Handle linking commands
        switch (name) {
            case "verify" -> handleVerify(event);
            case "linked" -> handleLinked(event);
            case "unlink" -> handleUnlink(event);
            default -> handleCustomCommand(event);
        }
    }

    private void handleCustomCommand(SlashCommandInteractionEvent event) {
        String name = event.getName();

        // Find matching custom command
        CustomCommand cmd = customCommands.stream()
                .filter(c -> c.name.equals(name))
                .findFirst()
                .orElse(null);

        if (cmd == null) {
            event.reply("❌ Unknown command!").setEphemeral(true).queue();
            return;
        }

        // Check admin
        if (cmd.adminOnly && !isAdmin(event)) {
            event.reply("❌ You don't have permission!").setEphemeral(true).queue();
            return;
        }

        // Build the MC command by replacing placeholders
        String mcCommand = cmd.mcCommand;
        if (cmd.args != null) {
            for (CustomCommand.CustomCommandArg arg : cmd.args) {
                var option = event.getOption(arg.name);
                String value = option != null ? option.getAsString() : "";
                mcCommand = mcCommand.replace("%" + arg.name + "%", value).trim();
            }
        }

        // Special handling for broadcast command
        if (name.equals("broadcast")) {
            String message = event.getOption("message") != null
                    ? event.getOption("message").getAsString() : "";
            String adminName = event.getUser().getName();
            final String finalMessage = message;

            if (server != null) {
                server.execute(() -> {
                    server.getPlayerList().broadcastSystemMessage(
                            Component.literal("§b[Broadcast] §f" + finalMessage), false
                    );
                });
            }

            bot.sendEmbed(EmbedUtil.broadcastEmbed(finalMessage, adminName));
            event.reply("✅ Broadcast sent!").setEphemeral(true).queue();
            return;
        }

        // Run the MC command
        final String finalCmd = mcCommand;
        runCommand(finalCmd);
        event.reply("✅ Executed: `" + finalCmd + "`").setEphemeral(true).queue();
    }

    private void handleVerify(SlashCommandInteractionEvent event) {
        if (!event.getChannel().getId().equals(config.verifyChannelID)) {
            event.reply("❌ Please use the <#" + config.verifyChannelID + "> channel!")
                    .setEphemeral(true).queue();
            return;
        }

        if (linker == null) {
            event.reply("❌ Linking system is not enabled!").setEphemeral(true).queue();
            return;
        }

        String code = event.getOption("code").getAsString();
        String discordUserID = event.getUser().getId();
        String discordTag = event.getUser().getName();
        String result = linker.verifyCode(discordUserID, code, discordTag);

        switch (result) {
            case "INVALID_CODE" -> event.reply(
                    "❌ Invalid or expired code! Run `/link <discordtag>` in Minecraft again."
            ).setEphemeral(true).queue();
            default -> event.reply(
                    "✅ Successfully linked to **" + result + "**! Rejoining the server..."
            ).setEphemeral(true).queue();
        }
    }

    private void handleLinked(SlashCommandInteractionEvent event) {
        if (linker == null) {
            event.reply("❌ Linking system is not enabled!").setEphemeral(true).queue();
            return;
        }

        String discordUserID = event.getUser().getId();
        String minecraftName = linker.getMinecraftName(discordUserID);

        if (minecraftName != null) {
            event.reply("✅ Your Discord is linked to: **" + minecraftName + "**")
                    .setEphemeral(true).queue();
        } else {
            event.reply("❌ Not linked! Join the server and run `/link <discordtag>`")
                    .setEphemeral(true).queue();
        }
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        if (!isAdmin(event)) {
            event.reply("❌ You don't have permission!").setEphemeral(true).queue();
            return;
        }

        if (linker == null) {
            event.reply("❌ Linking system is not enabled!").setEphemeral(true).queue();
            return;
        }

        String player = event.getOption("player").getAsString();
        String result = linker.unlink(player);

        switch (result) {
            case "NOT_LINKED" -> event.reply(
                    "❌ **" + player + "** is not linked!"
            ).setEphemeral(true).queue();
            case "UNLINKED" -> event.reply(
                    "✅ **" + player + "** has been unlinked!"
            ).setEphemeral(true).queue();
        }
    }

    private void runCommand(String command) {
        if (server == null) return;
        server.execute(() -> {
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack(), command
            );
        });
    }
}