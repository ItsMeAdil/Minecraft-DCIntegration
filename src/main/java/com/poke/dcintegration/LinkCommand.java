package com.poke.dcintegration;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class LinkCommand {
    private final PlayerLinker linker;
    private final EventListener eventListener;

    public LinkCommand(PlayerLinker linker, EventListener eventListener) {
        this.linker = linker;
        this.eventListener = eventListener;
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {

            // /link <discordtag>
            dispatcher.register(
                    Commands.literal("link")
                            .then(Commands.argument("discordtag", StringArgumentType.word())
                                    .executes(this::executeLink)
                            )
            );

            // /unlink
            dispatcher.register(
                    Commands.literal("unlink")
                            .executes(this::executeUnlink)
            );

            // /linkstatus
            dispatcher.register(
                    Commands.literal("linkstatus")
                            .executes(this::executeLinkStatus)
            );
        });
    }

    private int executeLink(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String discordTag = StringArgumentType.getString(ctx, "discordtag");
        String minecraftName = player.getName().getString();

        String result = linker.generateCode(minecraftName, discordTag);

        switch (result) {
            case "ALREADY_LINKED" -> player.sendSystemMessage(
                    Component.literal("§cYour account is already linked! Use /unlink first.")
            );
            case "GUILD_NOT_FOUND" -> player.sendSystemMessage(
                    Component.literal("§cCould not find the Discord server. Contact an admin.")
            );
            case "CODE_SENT" -> player.sendSystemMessage(
                    Component.literal("§aVerification code sent! Check your Discord DMs and run /verify <code> in the #verify channel.")
            );
        }
        return 1;
    }

    private int executeUnlink(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String minecraftName = player.getName().getString();
        String result = linker.unlink(minecraftName);

        switch (result) {
            case "NOT_LINKED" -> player.sendSystemMessage(
                    Component.literal("§cYour account is not linked!")
            );
            case "UNLINKED" -> {
                player.sendSystemMessage(
                        Component.literal("§aYour account has been unlinked successfully!")
                );
                // Freeze the player again
                if (eventListener != null) {
                    eventListener.addFrozenPlayer(player.getUUID());
                    player.sendSystemMessage(Component.literal(
                            "§cYou have been frozen! Run /link <discordtag> to relink your account."
                    ));
                }
            }
        }
        return 1;
    }

    private int executeLinkStatus(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) return 0;

        String minecraftName = player.getName().getString();

        if (linker.isLinked(minecraftName)) {
            player.sendSystemMessage(
                    Component.literal("§aYour account is linked to Discord!")
            );
        } else {
            player.sendSystemMessage(
                    Component.literal("§cYour account is not linked. Use /link <discordtag> to get started!")
            );
        }
        return 1;
    }
}