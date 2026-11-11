# Changelog

All notable changes to Tally are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/). No release has been tagged yet.

## [Unreleased]

No build has been tagged or published so far (no Git tag, no GitHub release); 0.1.0 below is the
baseline the following changes are measured against.

### Added
- Appearance setting (dark OLED by default, light, or follow system) in Settings.
- One-tap opt-in to Open Food Facts from the "Online" tab and from an unknown barcode scan,
  with a reminder of what is sent.
- `CHANGELOG.md`, build/test/contributing sections in `README.md`.
- CI: `osv-scanner` job (actions pinned by SHA) with documented build-only exclusions in
  `osv-scanner.toml`; fastlane metadata length check.

### Changed
- **Open Food Facts lookups are now off by default** (opt-in). Nothing leaves the device until the
  option is enabled; see `PRIVACY.md`. Installs with no stored `off.enabled` setting lose online
  search and barcode lookup until the option is turned on in Settings or from the "Online" tab.
- Open Food Facts HTTP client: TLS-only connection specs, no HTTPS to HTTP redirects, response
  bodies capped at 2 MiB.
- Backup manifest `dbSchemaVersion` now follows the real Room schema version (4).
- Settings, food editor and recipe editor screens use `TallyTheme` tokens
  (`config/design-tokens-baseline.txt` is empty).
- `gradle/verification-metadata.xml` regenerated from the CI task set plus the release runtime
  classpath.

### Fixed
- Scanning an unknown barcode while Open Food Facts is disabled reported "lookup unavailable"
  instead of saying that online lookup is off.

### Security
- No known vulnerability in the shipped (release runtime) dependencies as of 2026-11-11.
  Known advisories affect Gradle build tooling only and are tracked in `osv-scanner.toml`.

## [0.1.0] - 2026-10-16

### Added
- Food journal with meal slots, macros and daily energy target.
- Foods with portions, simple recipes, Ciqual 2020 table embedded (offline search).
- Open Food Facts search and barcode scan (CameraX + zxing-cpp), local product cache.
- Weight tracking with chart.
- ZIP backup/restore with undo, CSV exports, "Erase all my data".
- Privacy screen, FR/EN strings, licences screen, F-Droid metadata.
- Sport domain and database tables (Room v4); the Activity tab is hidden until recording ships.
