# Tally

Track what you eat, what you weigh and how you move. On your phone, nowhere else.

> **Status: pre-release (0.1.0, not yet tagged).** The food journal, weight tracking, barcode scan,
> backups and exports are implemented and covered by JVM/Robolectric tests. Sport tracking (GPS) is
> in development: its data layer exists, the tab is hidden. No store or F-Droid release yet.

> **Vibe coded.** Most of this project was written with AI coding assistants (Claude Code), guided by
> prompts rather than hand-written. Read the code before relying on it for anything sensitive.

## Why

Most nutrition apps want an account, your data and a subscription. Tally wants none of that: it's
free, open source, works offline and keeps everything on your device.

## What it does

| Feature | State |
|---|---|
| Food journal: meal slots, portions, macros, daily energy target | Done |
| Foods and simple recipes, archive instead of delete, undo on swipe | Done |
| Ciqual 2020 (Anses) food table embedded, searchable offline | Done |
| Open Food Facts search and barcode scan (CameraX + zxing-cpp) | Done, **off by default** (opt-in) |
| Weight history and chart (kg / lb / st) | Done |
| ZIP backup and restore (with undo), CSV exports, "Erase all my data" | Done |
| Appearance: dark OLED (default), light, system | Done |
| Sport: walk/run GPS recording, history, GPX export | In development (domain + database only, tab hidden) |
| Body measurements beyond weight, Bluetooth scales | Not started |

## Privacy in short

- Everything is stored in a local database. No account, no analytics, no crash reporting, no Google
  services. Android cloud backup is disabled.
- The only network access is Open Food Facts, and only after you turn it on. It receives the barcode
  or the search words, nothing from your journal.
- Permissions: `INTERNET`, `CAMERA` (asked at the first scan). The release manifest is checked
  against an allow-list in CI.
- Full policy: [PRIVACY.md](PRIVACY.md) (FR/EN).

## Under the hood

Kotlin, Jetpack Compose (Material 3), Hilt, Room, Coroutines/Flow, OkHttp, CameraX + zxing-cpp,
Vico. Multi-module: `:core` and `:domain` (pure JVM), `:data`, `:core-ui`, features `:nutrition`,
`:body`, `:activity`, and `:app`. Architecture rules (module graph, design tokens, strings parity,
licences, SPDX headers) are enforced by Gradle tasks. Design system: [docs/UI.md](docs/UI.md).
Decisions and audits: [docs/](docs/).

## Build

Requirements: JDK 17, Android SDK (platform 37, build-tools 37), Gradle wrapper (checked by
`distributionSha256Sum`). Dependencies are verified against `gradle/verification-metadata.xml`.

```sh
./gradlew assembleDebug
```

Release builds are minified (R8 + resource shrinking) and unsigned in this repository; no keystore
is committed.

## Tests and checks

The CI command (`.github/workflows/ci.yml`):

```sh
./gradlew checkArchitecture :app:checkPermissions :core:test :domain:test testDebugUnitTest \
  :app:lintRelease assembleDebug -Proborazzi.mode=verify
```

- `checkArchitecture`: module graph, no destructive Room migration, design tokens, FR/EN string
  parity, licences in `licenses.json`, SPDX headers.
- `:app:checkPermissions`: merged release manifest vs `config/permissions-allowlist.txt`.
- Unit tests: JVM for `:core`/`:domain`, Robolectric for `:data` (Room, migrations, backup,
  OFF client) and ViewModels. Screenshot tests (Roborazzi) are compared against
  `*/src/test/screenshots/`; re-record with `./gradlew testDebugUnitTest` (default mode) after a
  deliberate UI change and commit the PNGs.
- Dependencies: `osv-scanner scan source -L gradle/verification-metadata.xml --config=osv-scanner.toml`
  (also run in CI). After any dependency update, regenerate the checksums:
  `./gradlew --write-verification-metadata sha256 <CI tasks>`.

## Contributing

- Read `CLAUDE.md` (UI rules) and `docs/UI.md` before touching screens: tokens only, bottom sheets
  instead of dialogs, swipe + undo instead of confirmations, 48 dp targets, FR and EN strings.
- Every fix comes with a regression test (JVM or Robolectric, no instrumentation).
- Commits: Conventional Commits in English (`fix(data): …`, `test(app): …`).
- Issues and questions: <https://github.com/kerstz/poids/issues>.

## Data sources and licences

Open Food Facts (ODbL 1.0), Anses Ciqual 2020 (Licence Ouverte 1.0). See [NOTICE.md](NOTICE.md).

## License

[GPLv3 or later](LICENSE)
