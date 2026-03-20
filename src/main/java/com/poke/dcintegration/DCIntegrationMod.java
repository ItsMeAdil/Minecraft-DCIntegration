package com.poke.dcintegration;

import com.poke.dcintegration.config.CustomCommand;
import com.poke.dcintegration.config.ModConfig;
import com.poke.dcintegration.discord.CommandHandler;
import com.poke.dcintegration.discord.DiscordBot;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class DCIntegrationMod implements ModInitializer {
	private static DCIntegrationMod instance;
	private ModConfig config;
	private DiscordBot bot;
	private ChatBridge chatBridge;
	private EventListener eventListener;
	private CommandHandler commandHandler;
	private PlayerLinker playerLinker;
	private LinkCommand linkCommand;
	private PerformanceMonitor performanceMonitor;
	private List<CustomCommand> customCommands;

	@Override
	public void onInitialize() {
		instance = this;

		// Load config
		config = ModConfig.load();
		System.out.println("[DCIntegration] Config loaded!");

		// Start Discord bot
		bot = new DiscordBot(config);
		bot.start();

		if (bot.getJDA() == null) {
			System.out.println("[DCIntegration] Bot failed to connect! Check your config.");
			return;
		}

		// Start console forwarding
		if (config.consoleChannelEnabled) {
			ConsoleAppender.setBot(bot, config.consoleChannelID);
		}

		// Set up chat bridge
		if (config.chatBridgeEnabled) {
			chatBridge = new ChatBridge(bot);
			chatBridge.register();
		}

		// Set up event listener
		eventListener = new EventListener(bot, config);
		eventListener.register();

		// Set up command handler
		customCommands = com.poke.dcintegration.config.CustomCommandLoader.load();
		commandHandler = new CommandHandler(config, customCommands, bot);
		commandHandler.registerCommands(bot.getJDA());

		// Set up linking system
		if (config.linkingEnabled) {
			playerLinker = new PlayerLinker(config, bot.getJDA());
			playerLinker.setEventListener(eventListener);
			linkCommand = new LinkCommand(playerLinker, eventListener);
			linkCommand.register();

			eventListener.setLinker(playerLinker);
			commandHandler.setLinker(playerLinker);

			System.out.println("[DCIntegration] Linking system enabled!");
		}

		// Set up performance monitor
		if (config.performanceMonitorEnabled) {
			performanceMonitor = new PerformanceMonitor(bot, config);
			performanceMonitor.register();
			System.out.println("[DCIntegration] Performance monitor enabled!");
		}

		// Pass server to PlayerLinker when it starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			eventListener.setUnfreezeCallback(uuid -> {
				ServerPlayer player = server.getPlayerList().getPlayer(uuid);
				if (player != null) {
					player.setInvulnerable(false);
					player.sendSystemMessage(Component.literal(
							"§a§lYou are now free to play!"
					));
				}
			});
		});

		// Register JDA listeners
		if (config.chatBridgeEnabled) {
			bot.getJDA().addEventListener(chatBridge);
		}
		bot.getJDA().addEventListener(commandHandler);

		// Pass server instance to classes that need it
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			if (config.chatBridgeEnabled) {
				chatBridge.setServer(server);
			}
			commandHandler.setServer(server);
		});

		// Unfreeze player when they link successfully
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			server.execute(() -> {
				if (playerLinker != null) {
					server.getPlayerList().getPlayers().forEach(player -> {
						if (playerLinker.isLinked(player.getName().getString())) {
							eventListener.unfreezePlayer(player.getUUID());
						}
					});
				}
			});
		});

		System.out.println("[DCIntegration] Mod initialized successfully!");
	}

	public static DCIntegrationMod getInstance() {
		return instance;
	}

	public PlayerLinker getPlayerLinker() {
		return playerLinker;
	}

	public EventListener getEventListener() {
		return eventListener;
	}
}