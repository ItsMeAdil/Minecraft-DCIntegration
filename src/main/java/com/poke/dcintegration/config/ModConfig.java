package com.poke.dcintegration.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.*;

public class ModConfig {
    private static final Path CONFIG_PATH = Paths.get("config/dcintegration.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String botToken = "YOUR_BOT_TOKEN_HERE";
    public String channelID = "YOUR_CHANNEL_ID_HERE";
    public String consoleChannelID = "YOUR_CONSOLE_CHANNEL_ID_HERE";
    public String guildID = "YOUR_GUILD_ID_HERE";
    public String[] adminRoleIDs = {"YOUR_ROLE_ID_HERE"};

    // Linking settings
    public String verifiedRoleID = "YOUR_VERIFIED_ROLE_ID_HERE";
    public String verifyChannelID = "YOUR_VERIFY_CHANNEL_ID_HERE";

    // Performance Monitor
    public double tpsAlertThreshold = 15.0;
    public int ramAlertThreshold = 85;

    // Feature toggles
    public boolean linkingEnabled = true;
    public boolean restrictUnlinkedPlayers = true;
    public boolean consoleChannelEnabled = true;
    public boolean chatBridgeEnabled = true;
    public boolean playerEventsEnabled = true;
    public boolean syncNicknames = true;
    public boolean performanceMonitorEnabled = true;

    public static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = new FileReader(CONFIG_PATH.toFile())) {
                return GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                System.out.println("[DCIntegration] Failed to load config: " + e.getMessage());
            }
        }
        ModConfig defaultConfig = new ModConfig();
        defaultConfig.save();
        return defaultConfig;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = new FileWriter(CONFIG_PATH.toFile())) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            System.out.println("[DCIntegration] Failed to save config: " + e.getMessage());
        }
    }
}