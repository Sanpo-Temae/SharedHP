# Changelog

All notable changes to SharedHP are documented in this file.

The format is based on Keep a Changelog, and this project uses semantic versioning while it is public.

## [Unreleased]

### Changed

- Made the GitHub repository README Japanese-first for Japanese server owners.
- Added an English README as `README_en.md`.
- Added GitHub Issues template and GitHub Actions build workflow.
- Added release and SpigotMC listing draft documents under `docs/`.

## [1.0.0] - 2026-04-28

### Changed

- Promoted the 0.1.7 build to the first stable public release.

## [0.1.7] - 2026-04-28

### Changed

- Updated plugin author and license copyright holder to Sanpo Temae.

## [0.1.6] - 2026-04-28

### Added

- Added `/sharedhp heal <percent>` to change healing efficiency from in-game or console.
- Added Japanese README documentation.

### Changed

- Updated the default boss bar title format so it can show the current healing percentage.

## [0.1.5] - 2026-04-28

### Changed

- Clarified that SharedHP is a Paper-only plugin.
- Added README documentation for GitHub Releases and SpigotMC listings.
- Added MIT License.
- Added commented default `config.yml`.
- Improved `plugin.yml` command usage, alias, description, and permissions.
- Split viewing and management permissions into `sharedhp.view` and `sharedhp.admin`.
- Prevented accidental `/sharedhp start` while SharedHP is already active.
- Added server logs for important management operations.
- Synced `plugin.yml` version from Gradle `project.version`.
- Moved release jar output to `build/release` for GitHub Releases upload.

### Fixed

- Removed a participant's damage ranking data when they are removed.

## [0.1.4] - 2026-04-28

### Added

- Added per-participant damage tracking while SharedHP is active.
- Added `/sharedhp damage` to show damage ranking to online participants.
- Added `/sharedhp damage reset` to reset damage ranking manually.

### Changed

- `/sharedhp start` and `/sharedhp reset` reset damage ranking.
- `/sharedhp stop` keeps damage ranking in memory.

## [0.1.3] - 2026-04-28

### Changed

- Removed fixed participant count and four-player start requirement.
- `/sharedhp start` now works when at least one registered participant is online.
- Damage feedback sound and hurt animation are shown to other participants for all damage types.
- Knockback is applied to other participants only when the damage has an attacker/source.
- Absorption cleanup after eating is limited to golden apples and enchanted golden apples.
- Removed `required-player-count` from the default config.

## [0.1.2] - 2026-04-28

### Changed

- Stopped cancelling damage events so the damaged player gets vanilla hurt sound, red flash, and knockback.
- Shared HP synchronization now runs one tick after vanilla damage handling.
- Other participants receive synthetic hurt sound, hurt animation, and knockback.

## [0.1.1] - 2026-04-28

### Fixed

- Added shared knockback feedback for entity attacks while keeping shared damage at one hit's final damage value.

## [0.1.0] - 2026-04-28

### Added

- Initial Paper plugin project.
- Added `/sharedhp add`, `/sharedhp remove`, `/sharedhp list`, `/sharedhp status`, `/sharedhp start`, `/sharedhp stop`, `/sharedhp reset`, and `/sharedhp set`.
- Added internal shared HP pool.
- Added shared damage processing.
- Added 25% healing processing.
- Added boss bar display.
- Added absorption heart cleanup.
