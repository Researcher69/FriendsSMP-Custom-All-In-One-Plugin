# FriendsSMP

**Version:** 1.0.1  
**Game version:** 1.21.x  
**Loader:** Paper

FriendsSMP is an all-in-one Paper plugin for small survival multiplayer servers. It adds teams, region claims, a coin economy, daily rewards, a configurable chest shop, player stats, and polished inventory menus while keeping storage simple with SQLite.

## Features

- Built-in coin economy with `/balance` and `/pay`
- Streak-based `/daily` coin rewards
- Fully configurable `/shop` powered by `shop.yml`
- Category shop menus for Wood, Blocks, Food, and any custom categories you add
- Region claim system with a single-use Claim Wand
- Claim management GUI with trust controls
- Player-head trust menu with left-click trust and right-click untrust
- Block break, block place, container, and entity-damage protection inside claims
- Team system with invites, team chat, team homes, and a GUI info panel
- Stats and leaderboards for kills, deaths, and playtime
- SQLite persistence with async database writes

## Screenshots

Add screenshots before publishing:

- `shop-main.png` - category shop menu
- `shop-category.png` - item purchase menu
- `claims.png` - claim list menu
- `trust.png` - trust management menu
- `team-info.png` - team info menu

## Installation

1. Download the FriendsSMP jar.
2. Stop your Paper 1.21.x server.
3. Place the jar in the server `plugins/` folder.
4. Start the server to generate `config.yml` and `shop.yml`.
5. Edit claim limits, daily reward scaling, and shop contents as needed.
6. Restart the server. Use `/shop reload` for shop-only config changes.

## Commands

- `/balance`
- `/pay <player> <amount>`
- `/daily`
- `/shop`
- `/shop reload`
- `/claim`
- `/unclaim`
- `/claims`
- `/trust <player>`
- `/untrust <player>`
- `/team create <name>`
- `/team invite <player>`
- `/team accept`
- `/team leave`
- `/team disband`
- `/team info`
- `/team sethome`
- `/team home`
- `/teamchat <message>`
- `/stats [player]`
- `/top <kills|deaths|playtime>`

## Notes

FriendsSMP does not require Vault. The economy is internal to the plugin and stored in SQLite.
