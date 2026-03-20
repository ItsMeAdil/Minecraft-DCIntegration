# 🎮 DCIntegration — Discord × Minecraft Bridge

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![License](https://img.shields.io/badge/License-MIT-purple)

> A powerful Fabric mod that bridges your Minecraft server with Discord — featuring chat sync, slash commands, player linking, performance monitoring, and much more.

---

## ✨ Features

### 🔗 Discord ↔ Minecraft Chat Bridge
- Messages sent in Minecraft appear in Discord as rich embeds with player skin avatars
- Messages sent in Discord appear in Minecraft chat in real time
- Player join/leave events posted as embeds
- Death messages and advancement alerts automatically forwarded

### 🆔 Account Linking System
- Players link their Minecraft account to Discord via a verification code
- Optional: Restrict unlinked players from interacting until they verify
- Auto assigns a verified Discord role on successful link
- Syncs Discord nickname to Minecraft username
- Works with cracked accounts

### ⚙️ Custom Slash Commands
- Define your own Discord slash commands via a simple JSON file
- Use `%placeholders%` to pass arguments directly to Minecraft commands
- Set admin-only restrictions per command
- Default commands included: `/whitelist`, `/kick`, `/seed`, `/restart`, `/broadcast`

### 📊 Performance Monitor
- Monitors server TPS and RAM usage in real time
- Sends alert embeds to your console channel when performance degrades
- Sends recovery embed when performance returns to normal
- Configurable thresholds

### 🖥️ Console Channel
- Forwards server console output to a dedicated Discord channel
- Batches messages to avoid Discord rate limits
- Filters out noisy DEBUG messages

### 🎛️ Fully Configurable
Every feature can be toggled on or off in the config file — perfect for server owners who only need specific features.

---

## 📋 Requirements

- Minecraft **1.21.1**
- [Fabric Loader](https://fabricmc.net/use/installer/) **0.16.10+**
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.107.0+1.21.1**
- Java **21**

---

## 🚀 Installation

### 1. Install Fabric
Download and run the [Fabric Installer](https://fabricmc.net/use/installer/) and select Minecraft version **1.21.1**.

### 2. Download the Mod
Download the latest `dcintegrationmod-x.x.x.jar` from the [Releases](https://modrinth.com) page.

### 3. Install Dependencies
Download [Fabric API](https://modrinth.com/mod/fabric-api) for **1.21.1**.

### 4. Place in Mods Folder
Copy both jars into your server's `mods/` folder:
```
server/
  mods/
    dcintegrationmod-1.0.0.jar
    fabric-api-x.x.x+1.21.1.jar
```

### 5. Start the Server
Start your server once to generate the config files, then stop it.

---

## ⚙️ Configuration

### Step 1 — Create a Discord Bot
1. Go to [discord.com/developers/applications](https://discord.com/developers/applications)
2. Click **New Application** and name it
3. Go to **Bot** tab and click **Add Bot**
4. Copy the **Bot Token** — you'll need this later
5. Enable these **Privileged Gateway Intents**:
    - ✅ Message Content Intent
    - ✅ Server Members Intent
    - ✅ Presence Intent

### Step 2 — Invite the Bot
1. Go to **OAuth2 > URL Generator**
2. Select scopes: `bot` + `applications.commands`
3. Select permissions:
    - ✅ Send Messages
    - ✅ Read Messages
    - ✅ Manage Messages
    - ✅ Manage Nicknames (for nickname sync)
    - ✅ Manage Roles (for verified role)
4. Open the generated URL and invite the bot to your server

### Step 3 — Get IDs
Enable **Developer Mode** in Discord (`Settings > Advanced > Developer Mode`), then:
- Right click your server name → **Copy Server ID** = `guildID`
- Right click your main chat channel → **Copy Channel ID** = `channelID`
- Right click your console channel → **Copy Channel ID** = `consoleChannelID`
- Right click your verify channel → **Copy Channel ID** = `verifyChannelID`
- Right click your verified role → **Copy Role ID** = `verifiedRoleID`
- Right click your admin role → **Copy Role ID** = `adminRoleIDs`

### Step 4 — Edit Config
Open `config/dcintegration.json` and fill in your values:

```json
{
  "botToken": "YOUR_BOT_TOKEN",
  "channelID": "YOUR_MAIN_CHANNEL_ID",
  "consoleChannelID": "YOUR_CONSOLE_CHANNEL_ID",
  "guildID": "YOUR_GUILD_ID",
  "adminRoleIDs": ["YOUR_ADMIN_ROLE_ID"],
  "verifiedRoleID": "YOUR_VERIFIED_ROLE_ID",
  "verifyChannelID": "YOUR_VERIFY_CHANNEL_ID",

  "linkingEnabled": true,
  "restrictUnlinkedPlayers": true,
  "syncNicknames": true,
  "consoleChannelEnabled": true,
  "chatBridgeEnabled": true,
  "playerEventsEnabled": true,
  "performanceMonitorEnabled": true,

  "tpsAlertThreshold": 15.0,
  "ramAlertThreshold": 85
}
```

### Step 5 — Custom Commands
Edit `config/dcintegration_commands.json` to define your own slash commands:

```json
[
  {
    "name": "whitelist",
    "description": "Manage the server whitelist",
    "adminOnly": true,
    "mcCommand": "whitelist %action% %player%",
    "args": [
      {
        "name": "action",
        "description": "add/remove/on/off/list",
        "optional": false
      },
      {
        "name": "player",
        "description": "Player name",
        "optional": true
      }
    ]
  },
  {
    "name": "seed",
    "description": "Get the server seed",
    "adminOnly": false,
    "mcCommand": "seed",
    "args": []
  }
]
```

### Step 6 — Start the Server
Start your server and watch the console for:
```
[DCIntegration] Config loaded!
[DCIntegration] Bot connected to Discord!
[DCIntegration] Mod initialized successfully!
```

---

## 🔗 Account Linking Setup

### For Players
1. Join the Minecraft server
2. Run `/link <your_discord_username>` in Minecraft chat
3. Check your Discord DMs for a 6-digit verification code
4. Go to the `#verify` channel in Discord
5. Run `/verify <code>`
6. You'll be kicked and can rejoin fully linked!

### For Server Owners
- Set `linkingEnabled: true` in config to enable the system
- Set `restrictUnlinkedPlayers: true` to freeze unlinked players until they verify
- Set `syncNicknames: true` to automatically rename Discord users to their MC name
- Create a `#verify` channel and set its ID in `verifyChannelID`
- Create a `Verified` role and set its ID in `verifiedRoleID`

---

## 📝 Discord Channels Setup

We recommend creating these channels in your Discord server:

| Channel | Purpose | Config Key |
|---|---|---|
| `#minecraft-chat` | Bridged chat | `channelID` |
| `#console` | Server console output | `consoleChannelID` |
| `#verify` | Account verification | `verifyChannelID` |

---

## 🛡️ Permissions

Make sure your bot's role is placed **high in the role hierarchy** in your Discord server so it can:
- Assign the verified role to members
- Change nicknames of members below it
- Note: The bot cannot change the nickname of the server owner or anyone with a higher role

---

## 🐛 Troubleshooting

**Bot not connecting?**
- Check your bot token is correct in config
- Make sure all Privileged Gateway Intents are enabled in the developer portal

**Commands not appearing?**
- Make sure `guildID` is set correctly
- Wait a few seconds after server start for commands to register
- Try running `/` in your Discord server to refresh

**Linking not working?**
- Make sure the bot can DM the player (they need to allow DMs from server members)
- Check the verify channel ID is correct
- Make sure the verified role exists and the bot has permission to assign it

**Nickname sync failing?**
- Make sure the bot's role is higher than the member's highest role
- Server owners cannot have their nickname changed by bots

---

## 📜 License

This project is licensed under the MIT License — see the [LICENSE](https://github.com/ItsMeAdil/Minecraft-DCIntegration/blob/main/LICENSE) file for details.

---

## 🤝 Contributing

Pull requests are welcome! For major changes please open an issue first to discuss what you would like to change.

---

## 💬 Support

- Open an issue on [GitHub](https://github.com)
- Join our [Discord Server](https://discord.gg)