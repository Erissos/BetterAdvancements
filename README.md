# Better Advancements

Better Advancements is a configurable Paper and Spigot progression plugin for Minecraft 1.20+.

## Highlights

- 65 default advancements across 5 tiers
- Graph-based dependencies between achievements
- Inventory GUI for tier browsing, progression viewing, statistics and leaderboards
- SQLite and MySQL persistence through HikariCP
- Vault-compatible economy rewards
- Daily and weekly challenges
- Hidden achievements and advancement point currency
- Multi-language MiniMessage packs: EN, TR, RU, DE, ES, IT, FR, SK, CS, ZH, RO
- Public service API registered through Bukkit ServicesManager
- PlaceholderAPI-aware message rendering for player-facing texts

## Commands

- /ba menu
- /ba stats
- /ba leaderboard [global|session]
- /ba language <code>
- /ba reload
- /ba give <player> <achievement>
- /ba reset <player>

## Configuration

All bundled defaults live under src/main/resources and are copied into the plugin folder at first startup.

- config.yml: global behavior and notification toggles
- database.yml: SQLite or MySQL connection settings
- gui.yml: menu titles, filler slots, tier entry positions and path arrows
- achievements.yml: the full 65-achievement progression graph
- challenges.yml: daily and weekly challenge rotations
- lang/*.yml: localized command and notification messages

## Build

This is a Gradle project targeting Java 17.

Typical build command:

```powershell
gradle build
```

If Gradle is not installed on the host machine, install Gradle first or build inside an IDE with Gradle support enabled.

## Notes

- The default GUI and advancement graph are intentionally data-driven so server owners can replace titles, rewards, icons and dependency chains without touching Java code.
- Vault integration is optional and activates automatically when Vault and an economy provider are present.
- PlaceholderAPI integration is optional and automatically resolves placeholders in player-facing MiniMessage strings when the plugin is installed.