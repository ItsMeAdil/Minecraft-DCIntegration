package com.poke.dcintegration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.poke.dcintegration.config.ModConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.JDA;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

public class PlayerLinker {
    private static final Path LINKS_PATH = Paths.get("config/dcintegration_links.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Map<String, String> linkedPlayers = new HashMap<>();
    private Map<String, String> pendingCodes = new HashMap<>();

    private final ModConfig config;
    private final JDA jda;
    private MinecraftServer server;
    private EventListener eventListener;

    public PlayerLinker(ModConfig config, JDA jda) {
        this.config = config;
        this.jda = jda;
        load();
    }

    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    public void setEventListener(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    // Step 1 - Player runs /link <discordtag> in MC
    public String generateCode(String minecraftName, String discordTag) {
        if (linkedPlayers.containsKey(minecraftName)) {
            return "ALREADY_LINKED";
        }

        Guild guild = jda.getGuildById(config.guildID);
        if (guild == null) return "GUILD_NOT_FOUND";

        guild.retrieveMembersByPrefix(discordTag, 1).onSuccess(members -> {
            if (members.isEmpty()) return;

            String code = String.valueOf(new Random().nextInt(900000) + 100000);
            pendingCodes.put(minecraftName, code);

            // DM the player
            members.get(0).getUser().openPrivateChannel().queue(channel -> {
                channel.sendMessage(
                        "🔗 **DCIntegration Verification**\n\n" +
                                "Your verification code is: **" + code + "**\n\n" +
                                "Go to the **#verify** channel and run:\n" +
                                "`/verify " + code + "`\n\n" +
                                "This code expires in **10 minutes**."
                ).queue();
            });

            // Expire code after 10 minutes
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    pendingCodes.remove(minecraftName);
                }
            }, 10 * 60 * 1000);
        });

        return "CODE_SENT";
    }

    // Step 2 - Player runs /verify <code> in Discord
    public String verifyCode(String discordUserID, String code, String discordTag) {
        // Find minecraft name with this code
        String minecraftName = null;
        for (Map.Entry<String, String> entry : pendingCodes.entrySet()) {
            if (entry.getValue().equals(code)) {
                minecraftName = entry.getKey();
                break;
            }
        }

        if (minecraftName == null) return "INVALID_CODE";

        // Link the accounts
        linkedPlayers.put(minecraftName, discordUserID);
        pendingCodes.remove(minecraftName);
        save();

        // Unfreeze and notify player in game
        final String linkedName = minecraftName;
        if (server != null) {
            server.execute(() -> {
                ServerPlayer player = server.getPlayerList().getPlayerByName(linkedName);
                if (player != null) {
                    // Unfreeze
                    if (eventListener != null) {
                        eventListener.unfreezePlayer(player.getUUID());
                    }
                    // Notify
                    player.sendSystemMessage(Component.literal(
                            "§a§lYour account has been linked successfully!\n" +
                                    "§r§aWelcome to the server, §f" + linkedName + "§a!"
                    ));
                }
            });
        }

        // Give verified role in Discord
        Guild guild = jda.getGuildById(config.guildID);
        if (guild != null) {
            Role verifiedRole = guild.getRoleById(config.verifiedRoleID);
            if (verifiedRole != null) {
                guild.retrieveMemberById(discordUserID).queue(member -> {
                    guild.addRoleToMember(member, verifiedRole).queue();

                    // Sync nickname if enabled
                    if (config.syncNicknames) {
                        guild.retrieveMemberById(discordUserID).queue(user -> {
                            // Check if bot can modify this member's nickname
                            Member selfMember = guild.getSelfMember();
                            if (selfMember.canInteract(user)) {
                                guild.modifyNickname(user, linkedName).queue(
                                        success -> System.out.println("[DCIntegration] Nickname synced for " + linkedName),
                                        error -> System.out.println("[DCIntegration] Failed to sync nickname: " + error.getMessage())
                                );
                            } else {
                                // Bot cannot modify this member's nickname, notify them in game
                                System.out.println("[DCIntegration] Skipping nickname sync for " + linkedName + " — role is higher than bot.");
                                if (server != null) {
                                    server.execute(() -> {
                                        ServerPlayer p = server.getPlayerList().getPlayerByName(linkedName);
                                        if (p != null) {
                                            p.sendSystemMessage(Component.literal(
                                                    "§e⚠ Your account is linked but your Discord nickname could not be changed because your Discord role is higher than the bot's role."
                                            ));
                                        }
                                    });
                                }
                            }
                        });
                    }
                });
            }
        }

        return minecraftName;
    }

    public boolean isLinked(String minecraftName) {
        return linkedPlayers.containsKey(minecraftName);
    }

    public String getDiscordID(String minecraftName) {
        return linkedPlayers.get(minecraftName);
    }

    public String getMinecraftName(String discordID) {
        for (Map.Entry<String, String> entry : linkedPlayers.entrySet()) {
            if (entry.getValue().equals(discordID)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String unlink(String minecraftName) {
        if (!linkedPlayers.containsKey(minecraftName)) {
            return "NOT_LINKED";
        }

        String discordID = linkedPlayers.remove(minecraftName);
        save();

        // Remove verified role
        // Remove verified role
        Guild guild = jda.getGuildById(config.guildID);
        if (guild != null) {
            Role verifiedRole = guild.getRoleById(config.verifiedRoleID);
            guild.retrieveMemberById(discordID).queue(member -> {
                // Remove role
                if (verifiedRole != null) {
                    guild.removeRoleFromMember(member, verifiedRole).queue();
                }

                // Reset nickname if enabled
                if (config.syncNicknames) {
                    guild.retrieveMemberById(discordID).queue(user -> {
                        Member selfMember = guild.getSelfMember();
                        if (selfMember.canInteract(user)) {
                            guild.modifyNickname(user, null).queue(
                                    success -> System.out.println("[DCIntegration] Nickname reset for " + discordID),
                                    error -> System.out.println("[DCIntegration] Failed to reset nickname: " + error.getMessage())
                            );
                        } else {
                            System.out.println("[DCIntegration] Skipping nickname reset for " + discordID + " — role is higher than bot.");
                        }
                    });
                }
            });
        }

        return "UNLINKED";
    }

    private void load() {
        if (Files.exists(LINKS_PATH)) {
            try (Reader reader = new FileReader(LINKS_PATH.toFile())) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> loaded = GSON.fromJson(reader, type);
                if (loaded != null) linkedPlayers = loaded;
            } catch (IOException e) {
                System.out.println("[DCIntegration] Failed to load links: " + e.getMessage());
            }
        }
    }

    private void save() {
        try {
            Files.createDirectories(LINKS_PATH.getParent());
            try (Writer writer = new FileWriter(LINKS_PATH.toFile())) {
                GSON.toJson(linkedPlayers, writer);
            }
        } catch (IOException e) {
            System.out.println("[DCIntegration] Failed to save links: " + e.getMessage());
        }
    }
}