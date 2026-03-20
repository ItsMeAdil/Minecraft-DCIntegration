package com.poke.dcintegration.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class CustomCommandLoader {
    private static final Path COMMANDS_PATH = Paths.get("config/dcintegration_commands.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static List<CustomCommand> load() {
        if (!Files.exists(COMMANDS_PATH)) {
            // Generate default commands file
            List<CustomCommand> defaults = generateDefaults();
            save(defaults);
            return defaults;
        }

        try (Reader reader = new FileReader(COMMANDS_PATH.toFile())) {
            Type type = new TypeToken<List<CustomCommand>>(){}.getType();
            List<CustomCommand> commands = GSON.fromJson(reader, type);
            if (commands != null) return commands;
        } catch (IOException e) {
            System.out.println("[DCIntegration] Failed to load commands: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    private static void save(List<CustomCommand> commands) {
        try {
            Files.createDirectories(COMMANDS_PATH.getParent());
            try (Writer writer = new FileWriter(COMMANDS_PATH.toFile())) {
                GSON.toJson(commands, writer);
            }
            System.out.println("[DCIntegration] Default commands file generated at config/dcintegration_commands.json");
        } catch (IOException e) {
            System.out.println("[DCIntegration] Failed to save commands: " + e.getMessage());
        }
    }

    private static List<CustomCommand> generateDefaults() {
        List<CustomCommand> commands = new ArrayList<>();

        // Whitelist command
        CustomCommand whitelist = new CustomCommand();
        whitelist.name = "whitelist";
        whitelist.description = "Manage the server whitelist";
        whitelist.adminOnly = true;
        whitelist.mcCommand = "whitelist %action% %player%";
        whitelist.args = new ArrayList<>();
        CustomCommand.CustomCommandArg actionArg = new CustomCommand.CustomCommandArg();
        actionArg.name = "action";
        actionArg.description = "add/remove/on/off/list";
        actionArg.optional = false;
        CustomCommand.CustomCommandArg playerArg = new CustomCommand.CustomCommandArg();
        playerArg.name = "player";
        playerArg.description = "Player name";
        playerArg.optional = true;
        whitelist.args.add(actionArg);
        whitelist.args.add(playerArg);
        commands.add(whitelist);

        // Kick command
        CustomCommand kick = new CustomCommand();
        kick.name = "kick";
        kick.description = "Kick a player from the server";
        kick.adminOnly = true;
        kick.mcCommand = "kick %player% %reason%";
        kick.args = new ArrayList<>();
        CustomCommand.CustomCommandArg kickPlayerArg = new CustomCommand.CustomCommandArg();
        kickPlayerArg.name = "player";
        kickPlayerArg.description = "Player to kick";
        kickPlayerArg.optional = false;
        CustomCommand.CustomCommandArg kickReasonArg = new CustomCommand.CustomCommandArg();
        kickReasonArg.name = "reason";
        kickReasonArg.description = "Reason for kick";
        kickReasonArg.optional = true;
        kick.args.add(kickPlayerArg);
        kick.args.add(kickReasonArg);
        commands.add(kick);

        // Seed command
        CustomCommand seed = new CustomCommand();
        seed.name = "seed";
        seed.description = "Get the server seed";
        seed.adminOnly = false;
        seed.mcCommand = "seed";
        seed.args = new ArrayList<>();
        commands.add(seed);

        // Restart command
        CustomCommand restart = new CustomCommand();
        restart.name = "restart";
        restart.description = "Restart the server";
        restart.adminOnly = true;
        restart.mcCommand = "stop";
        restart.args = new ArrayList<>();
        commands.add(restart);

        // Broadcast command
        CustomCommand broadcast = new CustomCommand();
        broadcast.name = "broadcast";
        broadcast.description = "Broadcast a message to the server";
        broadcast.adminOnly = true;
        broadcast.mcCommand = "broadcast %message%";
        broadcast.args = new ArrayList<>();
        CustomCommand.CustomCommandArg broadcastArg = new CustomCommand.CustomCommandArg();
        broadcastArg.name = "message";
        broadcastArg.description = "Message to broadcast";
        broadcastArg.optional = false;
        broadcast.args.add(broadcastArg);
        commands.add(broadcast);

        return commands;
    }
}