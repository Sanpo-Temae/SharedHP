# SharedHP

[日本語 README](README.md)

SharedHP is a **Paper-only** Minecraft Java Edition plugin that lets registered players share one HP pool.

It is designed for challenge videos, cooperative survival events, and custom multiplayer events where damage and healing should affect the whole team.

## Important: Paper Only

SharedHP is built against `paper-api:1.21.11-R0.1-SNAPSHOT`.

- Supported: **Paper 1.21.11**
- Required Java: **Java 21**
- Not supported: **Spigot / Bukkit / CraftBukkit**
- SpigotMC listing note: **Requires Paper. Spigot/Bukkit are not supported.**

The plugin may not load or may not work correctly on non-Paper servers.

## Features

- Register any number of participants with `/sharedhp add <player>`
- Registered online participants share one internal HP pool
- Damage subtracts from the shared HP pool once
- Healing restores only 25% of the original healing amount by default
- Healing efficiency can be changed in-game with `/sharedhp heal <percent>`
- Participants' HP is synchronized to the shared HP value
- Absorption hearts are disabled for participants
- Boss bar shows current shared HP
- Lethal shared damage kills all online participants
- Damage ranking command for participants
- Participant list is saved in `plugins/SharedHP/config.yml`

## Download

Download the latest `SharedHP-x.y.z.jar` from GitHub Releases or the SpigotMC resource page.

Only put **one** SharedHP jar in your server's `plugins` folder.

Do not install multiple versions at the same time, such as:

```text
plugins/
  SharedHP-0.1.3.jar
  SharedHP-0.1.4.jar
```

Keep only the latest jar:

```text
plugins/
  SharedHP-1.0.0.jar
```

## Installation

1. Stop the server.
2. Put the latest `SharedHP-x.y.z.jar` into the server's `plugins` folder.
3. Start the server.
4. Run `/plugins` and confirm `SharedHP` is shown in green.
5. Run `/sharedhp status`.
6. Add participants with `/sharedhp add <player>`.
7. Start the shared HP system with `/sharedhp start`.

Use `stop -> replace jar -> start` when updating. Do not use `/reload`.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/sharedhp status` | `sharedhp.view` | Shows active state, shared HP, participant count, online participants, boss bar state, and heal multiplier. |
| `/sharedhp list` | `sharedhp.view` | Lists registered participants and online/offline state. |
| `/sharedhp damage` | `sharedhp.view` | Shows the damage ranking to online participants. |
| `/sharedhp add <player>` | `sharedhp.admin` | Adds an online player as a participant. |
| `/sharedhp remove <player>` | `sharedhp.admin` | Removes a participant. |
| `/sharedhp start` | `sharedhp.admin` | Starts the shared HP system if at least one registered participant is online. |
| `/sharedhp start force` | `sharedhp.admin` | Compatibility alias. Currently uses the same conditions as `/sharedhp start`. |
| `/sharedhp stop` | `sharedhp.admin` | Stops shared HP processing and hides the boss bar. |
| `/sharedhp reset` | `sharedhp.admin` | Resets shared HP and damage ranking. |
| `/sharedhp set <value>` | `sharedhp.admin` | Sets shared HP, clamped between `0.0` and max shared HP. |
| `/sharedhp heal <percent>` | `sharedhp.admin` | Sets healing efficiency. Example: `/sharedhp heal 25` means 25%. |
| `/sharedhp damage reset` | `sharedhp.admin` | Resets the damage ranking. |

Alias:

```text
/shp
```

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `sharedhp.view` | `op` | Allows viewing status, participant list, and damage ranking. |
| `sharedhp.admin` | `op` | Allows all management commands. Includes `sharedhp.view`. |

Console can run all commands.

## Configuration

`config.yml` is generated at:

```text
plugins/SharedHP/config.yml
```

Default config:

```yaml
participants: []

max-shared-health: 20.0
heal-multiplier: 0.25
bossbar-title-format: "共有HP: %.1f / %.1f | 回復効率%.0f%%"
```

### `participants`

UUID list of registered participants.

Normally, manage this with:

```text
/sharedhp add <player>
/sharedhp remove <player>
```

Manual editing is possible while the server is stopped.

### `max-shared-health`

Maximum shared HP.

Minecraft health values:

- `20.0` = 10 hearts
- `2.0` = 1 heart
- `1.0` = half a heart

### `heal-multiplier`

How much normal healing is applied to shared HP.

Default:

```yaml
heal-multiplier: 0.25
```

This means 25% healing efficiency.

You can change it in-game:

```text
/sharedhp heal 25
/sharedhp heal 50
/sharedhp heal 100
```

The command value is a percent number. `/sharedhp heal 25` saves `heal-multiplier: 0.25`.

### `bossbar-title-format`

Boss bar title format.

The first `%.1f` is current shared HP. The second `%.1f` is max shared HP.
The `%.0f` value is the current healing percentage.

Use `%%` to show a literal percent sign.

## FAQ

### Does this plugin support Spigot?

No. SharedHP is a Paper-only plugin and is not supported on Spigot, Bukkit, or CraftBukkit.

### The command is red or unknown in-game.

Check that:

- The plugin loaded successfully with `/plugins`
- You are OP or have the required permission
- You installed only one SharedHP jar
- The server is Paper 1.21.11

### Should I use `/reload` after updating?

No. Stop the server, replace the jar, then start the server again.

### Why does healing feel weak?

By default, healing is multiplied by `0.25`. Use `/sharedhp heal <percent>` or change `heal-multiplier` if you want different behavior.

### Where are errors logged?

Check:

```text
logs/latest.log
```

If you report a bug, include the relevant log section and your Paper version.

## Troubleshooting

- If `SharedHP` does not appear in `/plugins`, check that the jar is directly inside `plugins`.
- If `SharedHP` is red in `/plugins`, check `logs/latest.log`.
- If you see `UnsupportedClassVersionError`, run the server with Java 21.
- If commands do not appear, check OP status or permissions.
- If config changes do not apply, stop the server, edit config, then start the server.

## Bug Reports

When reporting a bug, include:

- SharedHP version
- Paper version
- Java version
- `plugins/SharedHP/config.yml`
- Relevant part of `logs/latest.log`
- Steps to reproduce the issue

## Links

- GitHub Releases: See the Releases page of this repository.
- SpigotMC Resource: Coming soon
- Blog guide: Coming soon
- YouTube guide: Coming soon

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

SharedHP is released under the MIT License. See [LICENSE](LICENSE).
