package com.poke.dcintegration;

import com.poke.dcintegration.discord.DiscordBot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;

public class ConsoleAppender extends AbstractAppender {
    private static DiscordBot bot;
    private static String consoleChannelID;

    private static final java.util.concurrent.BlockingQueue<String> messageQueue =
            new java.util.concurrent.LinkedBlockingQueue<>();

    public ConsoleAppender() {
        super("DCIntegrationConsoleAppender", null,
                PatternLayout.createDefaultLayout(), true, null);
    }

    public static void setBot(DiscordBot discordBot, String channelID) {
        bot = discordBot;
        consoleChannelID = channelID;

        // Attach to root logger
        ConsoleAppender appender = new ConsoleAppender();
        appender.start();

        Logger rootLogger = (Logger) LogManager.getRootLogger();
        rootLogger.addAppender(appender);

        System.out.println("[DCIntegration] Console appender attached!");
        startMessageSender();
    }

    private static void startMessageSender() {
        Thread senderThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (bot == null || messageQueue.isEmpty()) continue;

                    StringBuilder batch = new StringBuilder();
                    String msg;
                    int count = 0;

                    while ((msg = messageQueue.poll()) != null && count < 10) {
                        batch.append(msg).append("\n");
                        count++;
                    }

                    if (batch.length() > 0) {
                        // Discord code blocks have 2000 char limit
                        String finalMsg = "```\n" + batch.toString() + "```";
                        if (finalMsg.length() > 1990) {
                            finalMsg = finalMsg.substring(0, 1990) + "```";
                        }
                        bot.sendMessageToChannel(consoleChannelID, finalMsg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        senderThread.setDaemon(true);
        senderThread.start();
        System.out.println("[DCIntegration] Console message sender started!");
    }

    @Override
    public void append(LogEvent event) {
        if (bot == null) return;

        String message = event.getMessage().getFormattedMessage();
        String loggerName = event.getLoggerName();

        // Filter out noisy/irrelevant messages
        if (message.contains("DCIntegration")) return;
        if (message.contains("WebSocket")) return;
        if (message.contains("Unsafe")) return;
        if (message.contains("native access")) return;
        if (message.contains("JNA")) return;
        if (message.contains("cf-rays")) return;
        if (message.contains("Received response")) return;

        // Filter out DEBUG level entirely
        if (event.getLevel() == org.apache.logging.log4j.Level.DEBUG) return;

        // Filter out JDA internal loggers
        if (loggerName.contains("Requester")) return;
        if (loggerName.contains("RateLimiter")) return;
        if (loggerName.contains("RestAction")) return;
        if (loggerName.contains("JDA")) return;

        String level = event.getLevel().name();

        // Shorten logger name
        if (loggerName.contains(".")) {
            loggerName = loggerName.substring(loggerName.lastIndexOf(".") + 1);
        }

        messageQueue.offer("[" + level + "] [" + loggerName + "] " + message);
    }
}
