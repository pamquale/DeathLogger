> 💡 **Inspired by [DeathLog](https://github.com/glisco03/deathlog) by glisco03 — not a fork.** DeathLogger is written from scratch: its own storage format, networking, GUI and multiloader build, with no code carried over.

[![GitHub release](https://img.shields.io/github/v/release/pamquale/deathlogger?logo=github&style=for-the-badge)](https://github.com/pamquale/deathlogger/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1%20--%2026.2-green?style=for-the-badge)](https://minecraft.net)
[![Loaders](https://img.shields.io/badge/Fabric%20%7C%20NeoForge%20%7C%20Forge-orange?style=for-the-badge)](https://modrinth.com/mod/death-logger)

An advanced death history mod. Keeps track of your deaths, restores inventory layouts, and saves coordinates. Essential for any survival world.

## ✨ Features

- 🎒 **Inventory Preview** — Full inventory snapshot at the moment of death, including armor and offhand
- 📍 **Death Coordinates** — Exact location with dimension info for easy navigation back
- 🔍 **Searchable History** — Filter deaths by time, death message, or location
- 🔄 **Inventory Restoration** — Restore items via GUI
- 📦 **Container Preview** — View contents of Shulker Boxes and Bundles
- 📸 **Death Screenshots** — A screenshot is captured at the moment of death and shown with the entry
- 📋 **Copy as Command** — Middle-click an item to copy a `/give` for it, enchantments and container contents included
- 🖥️ **Server Support** — Dedicated servers keep their own logs, with `/deathlogger` commands to inspect and restore them

## 📸 Screenshots

## 📦 Compatibility

| Minecraft | Fabric | NeoForge | Forge |
| :-- | :--: | :--: | :--: |
| 26.2 | ✅ | ✅ | — |
| 26.1.2 | ✅ | ✅ | ✅ |
| 26.1.1 | ✅ | ✅ | — |
| 26.1 | ✅ | ✅ | — |

Forge builds are published for 26.1.2 only. Forge has no recommended build for
26.1 or 26.1.1, and those versions fail to start regardless of which mods are
installed.

### Requirements

- **Fabric** — [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api)
- **NeoForge** — nothing beyond the loader
- **Forge** — nothing beyond the loader
- Java 25, which is what Minecraft 26.1+ ships with

### Optional integrations

- **[Better Statistics Screen](https://modrinth.com/mod/better-stats)** — adds a DeathLogger entry to its menu bar, next to File, View and About. Fabric and NeoForge only; there is no Forge build of it.
- **[Mod Menu](https://modrinth.com/mod/modmenu)** — settings reachable from the mod list on Fabric. NeoForge and Forge use their built-in mod list for the same thing.

## ⌨️ Usage

- Open from the **Statistics** screen, or bind a key in **Controls** (unbound by default)
- Select a death on the left to see its details on the right
- **Restore** returns the inventory and experience; **Delete** removes the entry
- **Middle-click** an item to copy a `/give` command for it to the clipboard
- **Settings** opens the screenshot and death-detection options

## ⚙️ Configuration

`config/deathlogger.json`, or the Settings button in the mod's own screen.

| Option | Default | What it does |
| :-- | :-- | :-- |
| `screenshotsEnabled` | `true` | Capture a screenshot at the moment of death |
| `useLegacyDeathDetection` | `false` | Record a death when health hits zero, for servers that never send a death message. No effect in singleplayer, and can produce false positives. |

## 🖥️ Server commands

Operators only:

- `/deathlogger list <player> [search]` — print a player's recorded deaths
- `/deathlogger view <player>` — open that player's log in your own GUI
- `/deathlogger restore <player> <index|latest>` — restore a recorded inventory

## 🔨 Building

```bash
./gradlew build
```

Jars land in `fabric/build/libs`, `neoforge/build/libs` and `forge/build/libs`.
The target Minecraft version and every dependency version live in
`gradle.properties`. `common` holds the loader-agnostic code; each loader module
only carries its own entry points and networking.

## 📄 License

MIT — see [LICENSE](LICENSE).

## 🙏 Credits

- **glisco03** — author of [DeathLog](https://github.com/glisco03/deathlog), the mod that inspired this one
- **pamquale** — DeathLogger author and maintainer
