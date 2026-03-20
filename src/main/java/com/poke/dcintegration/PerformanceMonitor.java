package com.poke.dcintegration;

import com.poke.dcintegration.config.ModConfig;
import com.poke.dcintegration.discord.DiscordBot;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;

public class PerformanceMonitor {
    private final DiscordBot bot;
    private final ModConfig config;

    // TPS tracking
    private final long[] tickTimes = new long[20];
    private int tickIndex = 0;
    private long lastTickTime = System.currentTimeMillis();

    // Alert state
    private boolean alertSent = false;

    // Thresholds
    private static final double TPS_THRESHOLD = 15.0;
    private static final int RAM_THRESHOLD_PERCENT = 85;

    // Only send alert every 60 seconds max
    private long lastAlertTime = 0;
    private static final long ALERT_COOLDOWN = 60 * 1000;

    public PerformanceMonitor(DiscordBot bot, ModConfig config) {
        this.bot = bot;
        this.config = config;
    }

    public void register() {
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
    }

    private void onTick(MinecraftServer server) {
        // Track tick times
        long now = System.currentTimeMillis();
        tickTimes[tickIndex % 20] = now - lastTickTime;
        lastTickTime = now;
        tickIndex++;

        // Only check every 20 ticks (1 second)
        if (tickIndex % 20 != 0) return;

        double tps = calculateTPS();
        long usedRam = getUsedRamMB();
        long maxRam = getMaxRamMB();
        int ramPercent = (int) ((usedRam * 100) / maxRam);

        boolean isPoorPerformance = tps < TPS_THRESHOLD || ramPercent >= RAM_THRESHOLD_PERCENT;
        long timeSinceLastAlert = now - lastAlertTime;

        if (isPoorPerformance && !alertSent && timeSinceLastAlert > ALERT_COOLDOWN) {
            // Send alert
            bot.sendEmbedToChannel(
                    config.consoleChannelID,
                    EmbedUtil.performanceAlertEmbed(tps, usedRam, maxRam)
            );
            alertSent = true;
            lastAlertTime = now;
            System.out.println("[DCIntegration] Performance alert sent! TPS: " + tps + " RAM: " + ramPercent + "%");

        } else if (!isPoorPerformance && alertSent) {
            // Send recovery message
            bot.sendEmbedToChannel(
                    config.consoleChannelID,
                    EmbedUtil.performanceOkEmbed(tps, usedRam, maxRam)
            );
            alertSent = false;
            System.out.println("[DCIntegration] Performance recovered! TPS: " + tps + " RAM: " + ramPercent + "%");
        }
    }

    private double calculateTPS() {
        long total = 0;
        for (long tickTime : tickTimes) {
            total += tickTime;
        }
        double avgTickTime = total / 20.0;
        // TPS = 1000ms / avgTickTime, capped at 20
        return Math.min(1000.0 / avgTickTime, 20.0);
    }

    private long getUsedRamMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
    }

    private long getMaxRamMB() {
        return Runtime.getRuntime().maxMemory() / 1024 / 1024;
    }
}