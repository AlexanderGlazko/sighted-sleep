# Sighted Sleep — Claude Code guide

Personal, fully offline Android sleep tracker. Single user, single device (Nothing Phone (3a) Pro, Android 17).

**The spec is `docs/SIGHTED_SLEEP_PROJECT.md`. Read the relevant sections before implementing anything.** Features are referenced by ID (e.g. F1.3). If a request conflicts with the spec, ask before deviating, and update the spec when a decision changes.

## Stack
- Kotlin, Jetpack Compose, Material 3 (dark theme default)
- Room (SQLite) for sessions/tags, DataStore for settings
- Jetpack Glance for the home-screen widget
- Custom Compose Canvas for all charts (`ui/pages/insights/Charts.kt`, actogram in `ui/pages/timeline`). No chart library: Vico 3.x needs Kotlin 2.4
- Debug builds have Settings → Developer → sample data (90 days, marked with comment `[sample]`; removal never touches real sleeps)
- AlarmManager + notifications for reminders
- java.time for all date/time logic
- Package: `com.sighted.sleep`

## Architecture
Single module, MVVM:
- `ui/` — Compose UI in layers that match the Figma components (see below); `AppNavigation.kt`, `Formatting.kt`, `ViewModelFactories.kt` stay at the root
- `data/` — Room entities, DAOs, repositories, DataStore
- `domain/` — pure Kotlin, no Android imports. Metrics engine lives in `domain/metrics`.

**UI layers (decided 2026-11-24):** `ui/kit` (Atomic Design: `foundations` · `atoms` · `molecules` · `organisms` · `templates`, no app copy or data), then Feature-Sliced layers `ui/entities` · `ui/features` · `ui/widgets` · `ui/pages` (screens + ViewModels). Imports only go downward. Figma `Rate sleep / Score scale` ↔ `ui/features/ratesleep/ScoreScale.kt`. The Glance widget stays in `widget/`. Full plan and package mapping: Obsidian vault `C:\Obsidian\Sighted Sleep`, note "Component structure plan".
- **In code so far:** `ui/kit/foundations` (theme), `ui/kit/atoms` (`ColorDot`, `LargeToggleButton`), `ui/entities/tag` (`tagColor`, `TagLabel`), `ui/features/ratesleep` (`CheckInForm`, `ScoreScale`), `ui/features/togglesleep` (`ToggleSleepButton`), `ui/widgets/lastsleep` (`LastSleepCard`), `ui/pages/{today,timeline,edit,insights,settings}` (History lives in `pages/timeline`). Today is fully split; charts, cards and settings sections of the other screens still sit in their page packages.

## Hard rules
- **Never add the `INTERNET` permission** or any networking library. The app is offline by design.
- Store timestamps as **UTC epoch millis + zoneId**. Never store local date-times as the source of truth.
- Time-of-day averages/spreads use **circular statistics** (spec §4).
- Midnight split and end-date attribution follow spec §3.3.
- Every function in `domain/metrics` must have **JUnit tests**, including edge cases: crossing midnight, DST changes, naps, empty periods.
- Room schema changes need a **migration**. Never use destructive migration fallback: user data must survive upgrades.
- Keep UI on Material 3 defaults until a design pass is requested.

## Workflow
- One feature (or a small group of feature IDs) per task, on its own branch (`phase-N/...` style), fast-forward merged into `main` after the user confirms; merged branches get deleted.
- After a change: build, run unit tests, install, then check on the phone and summarise what the user should check.
- Commit to Git after each working step with a clear message.

## Testing on the phone (the phone holds REAL data)
- Drive it over adb: `screencap` + `uiautomator dump` to read screens, `input tap/swipe` to act. Keep the screen awake while testing.
- **Read-only by default.** Never add, edit, delete or import on real data without asking. Leave edit screens with Back, not Save.
- Never answer system permission prompts (notifications etc.) for the user.
- Sample data (debug) can only be added to an empty log.
- Installing with `installDebug` is an in-place update and keeps data. Never uninstall.

## Status (2026-11-22)
- **All planned phases (0–5) are done and on `main`.** Features: logging + widget, check-in, tags, targets, work days, Timeline (actogram rows 15:00→15:00, list with tag filter), Insights (Day/Week/Month/Year, cards with Δ vs previous period, trend charts, quality by bedtime, tag impact), reminders, JSON/CSV export + JSON import. 70 unit tests.
- **Verified on phone:** everything except: JSON import (user skipped the test), reminders actually firing (user must enable them + allow notifications), widget Wake tap after the Phase 5 AppRequest refactor (same intent action, not re-tapped).
- **Next (optional):** design pass. The user designs screens in Figma; restyle theme (`ui/kit/foundations`), `ui/pages/insights/Charts.kt`, `ui/pages/timeline/TimelineScreen.kt`, widget (`widget/SleepWidget.kt`). Figma is already reorganised into the layers (2026-11-24); in code, pull kit, entity, feature and widget composables out of the pages screen by screen before or during restyling.
- **Docs:** screen and component documentation lives in the Obsidian vault `C:\Obsidian\Sighted Sleep` (canvas + one note per screen). Figma file key `wIPhet2cA6jXPMXPuCGg7T`, Design page node `11:74`.
- **Known trade-offs:** sleeps crossing 15:00 are split across two actogram rows (the user's real data has daytime sleep, e.g. 12:13→18:09); the Year chart's month labels skip some months.

## Commands (PowerShell, from project root)
Java isn't on PATH; use Android Studio's bundled JDK first in each shell:
`$env:JAVA_HOME = "C:\Program Files\Android Studio\jbr"`
SDK (incl. adb): `C:\Users\Alexa\AppData\Local\Android\Sdk` (`platform-tools\adb.exe`)

- Build debug APK: `.\gradlew assembleDebug`
- Install on connected phone: `.\gradlew installDebug`
- Unit tests: `.\gradlew testDebugUnitTest`
