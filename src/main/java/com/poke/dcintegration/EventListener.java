package com.poke.dcintegration;

import com.poke.dcintegration.config.ModConfig;
import com.poke.dcintegration.discord.DiscordBot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

import java.awt.Color;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class EventListener {
    private final DiscordBot bot;
    private final ModConfig config;
    private PlayerLinker linker;
    private Consumer<UUID> unfreezeCallback;

    private final Set<UUID> frozenPlayers = new HashSet<>();

    public EventListener(DiscordBot bot, ModConfig config) {
        this.bot = bot;
        this.config = config;
    }

    public void setLinker(PlayerLinker linker) {
        this.linker = linker;
    }

    public void setUnfreezeCallback(Consumer<UUID> callback) {
        this.unfreezeCallback = callback;
    }

    public void addFrozenPlayer(UUID uuid) {
        frozenPlayers.add(uuid);
    }

    public void unfreezePlayer(UUID uuid) {
        frozenPlayers.remove(uuid);
        if (unfreezeCallback != null) {
            unfreezeCallback.accept(uuid);
        }
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public void register() {
        // Server start
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            if (config.playerEventsEnabled) {
                bot.sendEmbed(EmbedUtil.serverStartEmbed());
            }
        });

        // Server stop
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> {
            if (config.playerEventsEnabled) {
                bot.sendEmbed(EmbedUtil.serverStopEmbed());
            }
        });

        // Player join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.player;
            String playerName = player.getName().getString();
            String uuid = player.getUUID().toString();

            if (config.playerEventsEnabled) {
                bot.sendEmbed(EmbedUtil.joinEmbed(playerName, uuid));
            }

            // Check if linking is enabled and player is not linked
            if (config.linkingEnabled && config.restrictUnlinkedPlayers
                    && linker != null && !linker.isLinked(playerName)) {
                frozenPlayers.add(player.getUUID());
                server.execute(() -> {
                    player.sendSystemMessage(Component.literal(
                            "§e§lWelcome to the server!\n" +
                                    "§r§cYour account is not linked to Discord.\n" +
                                    "§r§aTo play, link your account:\n" +
                                    "§r§f1. Run §b/link <your discord username> §fin chat\n" +
                                    "§r§f2. Check your Discord DMs for a code\n" +
                                    "§r§f3. Run §b/verify <code> §fin the #verify channel"
                    ));
                });
            }
        });

        // Player leave
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            String playerName = player.getName().getString();
            String uuid = player.getUUID().toString();
            frozenPlayers.remove(player.getUUID());

            if (config.playerEventsEnabled) {
                bot.sendEmbed(EmbedUtil.leaveEmbed(playerName, uuid));
            }
        });

        // Death messages and advancement alerts
        // Death messages and advancement alerts
        ServerMessageEvents.GAME_MESSAGE.register((server, message, overlay) -> {
            String msg = message.getString();

            // Death messages
            if (msg.contains("was slain") || msg.contains("drowned") ||
                    msg.contains("blew up") || msg.contains("fell") ||
                    msg.contains("burned") || msg.contains("tried to swim") ||
                    msg.contains("starved") || msg.contains("suffocated") ||
                    msg.contains("killed") || msg.contains("died") ||
                    msg.contains("struck by lightning") || msg.contains("went up in flames") ||
                    msg.contains("walked into fire") || msg.contains("hit the ground")) {

                if (config.playerEventsEnabled) {
                    bot.sendEmbed(new EmbedBuilder()
                            .setDescription("💀 " + msg)
                            .setColor(Color.decode("#992d22"))
                            .setTimestamp(Instant.now())
                            .build()
                    );
                }
                return;
            }

            // Advancement messages
            if (msg.contains("has made the advancement") ||
                    msg.contains("has completed the challenge") ||
                    msg.contains("has reached the goal")) {

                if (config.playerEventsEnabled) {
                    bot.sendEmbed(new EmbedBuilder()
                            .setDescription("🏆 " + msg)
                            .setColor(Color.decode("#f1c40f"))
                            .setTimestamp(Instant.now())
                            .build()
                    );
                }
            }
        });

        // Tick based freezing
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (UUID uuid : frozenPlayers) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    player.teleportTo(
                            player.serverLevel(),
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            player.getYRot(),
                            player.getXRot()
                    );
                    player.getFoodData().setFoodLevel(20);
                    player.setInvulnerable(true);
                }
            }
        });

        // Block breaking
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            if (frozenPlayers.contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal(
                        "§cYou must link your Discord account to play! Use /link <discordtag>"
                ));
                return false;
            }
            return true;
        });

        // Block interaction
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (frozenPlayers.contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal(
                        "§cYou must link your Discord account to play! Use /link <discordtag>"
                ));
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Item use
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (frozenPlayers.contains(player.getUUID())) {
                player.sendSystemMessage(Component.literal(
                        "§cYou must link your Discord account to play! Use /link <discordtag>"
                ));
                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });

        // Attack block
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (frozenPlayers.contains(player.getUUID())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });

        // Attack entity
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (frozenPlayers.contains(player.getUUID())) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}