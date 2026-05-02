# FriendsSMP

FriendsSMP is a lightweight PaperMC 1.21.x plugin for small survival multiplayer servers. It includes economy, shop, claims, teams, reviews, jail moderation, TPA, performance monitoring, and a low-overhead anti-cheat tuned for 8-10 player SMPs.

## Anti-Cheat System

- Tracks suspicious ore mining with rolling mining windows.
- Scores ore-per-minute spikes, ore-to-stone ratios, and short direct paths between valuable ores.
- Uses suspicion thresholds:
  - `10`: warning broadcast
  - `20`: second warning broadcast
  - `30`: automatic jail
- Spawns client-side decoy ores only after suspicion rises above normal mining behavior.
- Decoy hits add heavy suspicion, disappear instantly, and do not drop items.
- Evidence is written to `plugins/FriendsSMP/evidence/<player>.log`.

## Jail System

- `/jail <player>` and `/unjail <player>` for admins.
- Automatic jail cage is created at server start from the `jail` config location.
- Jailed players are teleported to jail, given Mining Fatigue and Weakness, and kept inside the jail area.
- Breaking, placing, and teleport commands are blocked while jailed.
- Player inventory is saved to `plugins/FriendsSMP/jailed-inventories/` and restored on unjail.
- Admins receive a shulker-box snapshot of the jailed inventory when available.

## Economy + Shop

- Built-in SQLite coin economy, no Vault dependency.
- `/balance`, `/pay`, and streak-based `/daily` coin rewards.
- `/shop` opens a chest GUI category menu.
- `shop.yml` controls categories, slots, icons, items, and prices.
- `/shop reload` reloads shop configuration for admins.

## Claims + Teams

- `/claim` gives a single-use Claim Wand.
- Claims are region-based with SQLite persistence.
- `/claims` opens claim management.
- Trust GUI uses player heads, selected claim state, left-click trust, and right-click untrust.
- Claims protect blocks, containers, and entity damage.
- Teams support invites, team chat, team homes, and `/team info` GUI.

## Reviews & Appeals

- `/review` opens a GUI and captures the next chat message as a private review.
- `/reviews` opens an admin view of recent review entries.
- Reviews are stored in `plugins/FriendsSMP/reviews.txt`.
- `/appeal` shows the Discord contact: `mklwde`.

## Performance Monitor

- `/performance` opens a lightweight chest GUI.
- Displays CPU load, RAM usage, TPS, player ping, and server uptime.
- Refreshes a few times at short intervals without tick-heavy polling.

## TPA

- Built-in replacement for SimpleTPA:
  - `/tpa <player>`
  - `/tpahere <player>`
  - `/tpaccept`
  - `/tpdeny`
- Requests expire automatically and have cooldowns.
- Jailed players cannot send or accept teleport requests.

## Commands

| Command | Description |
| --- | --- |
| `/balance` | View your coin balance |
| `/pay <player> <amount>` | Send coins |
| `/daily` | Claim daily coins |
| `/shop` | Open shop |
| `/shop reload` | Reload `shop.yml` |
| `/claim` | Receive Claim Wand |
| `/unclaim` | Remove current claim |
| `/claims` | Open claim GUI |
| `/trust <player>` | Trust player in current claim |
| `/untrust <player>` | Untrust player in current claim |
| `/team ...` | Team management |
| `/teamchat <message>` | Team chat |
| `/stats [player]` | Player stats |
| `/top <kills\|deaths\|playtime>` | Leaderboards |
| `/tpa <player>` | Request teleport to player |
| `/tpahere <player>` | Request player teleport to you |
| `/tpaccept` | Accept TPA |
| `/tpdeny` | Deny TPA |
| `/jail <player>` | Jail player |
| `/unjail <player>` | Release player |
| `/setadmin <player>` | Add plugin admin |
| `/removeadmin <player>` | Remove plugin admin |
| `/review` | Submit review |
| `/reviews` | Admin review viewer |
| `/appeal` | Show appeal contact |
| `/performance` | Open performance monitor |

## Permissions

- `friendssmp.admin`: admin commands, jail/unjail, reviews, shop reload, claim bypass.
- `friendssmp.claim`: claim and trust commands.
- `friendssmp.team`: team commands and team chat.

## Installation

1. Build with Java 21 and Maven:

   ```bash
   mvn clean package
   ```

2. Copy `target/FriendsSMP-1.0.1.jar` into the Paper server `plugins/` folder.
3. Start the server once to generate config files.
4. Edit `plugins/FriendsSMP/config.yml` and `plugins/FriendsSMP/shop.yml`.
5. Restart the server. Use `/shop reload` for shop-only changes.

## Storage

- SQLite database: `plugins/FriendsSMP/friendssmp.db`
- Evidence logs: `plugins/FriendsSMP/evidence/`
- Reviews: `plugins/FriendsSMP/reviews.txt`
- Jailed inventories: `plugins/FriendsSMP/jailed-inventories/`
