# FriendsSMP

FriendsSMP is a PaperMC 1.21.x plugin for small survival multiplayer servers. It bundles teams, land claims, daily coin rewards, a configurable coin shop, player stats, and polished inventory GUIs without requiring Vault or an external database.

## Features

- Coin economy stored in SQLite
- `/balance` and `/pay` commands
- Daily rewards with streak-based coin scaling
- Configurable chest GUI shop from `shop.yml`
- Region claims with a single-use Claim Wand
- Claim trust management GUI with player heads
- Block, container, and entity-damage claim protection
- Team creation, team chat, team homes, and team info GUI
- Player stats and leaderboards
- SQLite persistence with async writes

## Commands

| Command | Description |
| --- | --- |
| `/balance` | View your coin balance |
| `/pay <player> <amount>` | Send coins to another online player |
| `/daily` | Claim your daily coin reward |
| `/shop` | Open the category shop |
| `/shop reload` | Reload `shop.yml` |
| `/claim` | Receive a single-use Claim Wand |
| `/unclaim` | Remove the claim you are standing in |
| `/claims` | Open your claim management GUI |
| `/trust <player>` | Trust a player in your current claim |
| `/untrust <player>` | Remove trust in your current claim |
| `/team create <name>` | Create a team |
| `/team invite <player>` | Invite a player |
| `/team accept` | Accept a team invite |
| `/team leave` | Leave your team |
| `/team disband` | Disband your team |
| `/team info` | Open the team info GUI |
| `/team sethome` | Set team home |
| `/team home` | Teleport to team home |
| `/teamchat <message>` | Send a team-only chat message |
| `/stats [player]` | View stats |
| `/top <kills|deaths|playtime>` | View leaderboards |

## Permissions

- `friendssmp.admin`: administrative bypass and shop reload
- `friendssmp.claim`: claim and trust commands
- `friendssmp.team`: team commands and team chat

## Installation

1. Build with Java 21 and Maven:

   ```bash
   mvn clean package
   ```

2. Copy `target/FriendsSMP-1.0.1.jar` into the Paper server `plugins/` folder.
3. Start the server once to generate config files.
4. Edit `plugins/FriendsSMP/config.yml` for limits and reward scaling.
5. Edit `plugins/FriendsSMP/shop.yml` for categories, icons, slots, items, and prices.
6. Restart the server, or use `/shop reload` after shop-only changes.

## Shop Configuration

The shop is fully controlled by `shop.yml`. Server operators can add or remove categories, change category slots, change icons, add items, and update prices without editing code.

## Storage

All persistent data is stored in:

```text
plugins/FriendsSMP/friendssmp.db
```

SQLite JDBC is shaded into the plugin jar.
