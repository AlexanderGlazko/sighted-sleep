# Sighted Sleep — Claude Code guide

Personal, fully offline Android sleep tracker. Single user, single device (Nothing Phone (3a) Pro, Android 17).

**The spec is `docs/SIGHTED_SLEEP_PROJECT.md`. Read the relevant sections before implementing anything.** Features are referenced by ID (e.g. F1.3). If a request conflicts with the spec, ask before deviating, and update the spec when a decision changes.

## Stack
- Kotlin, Jetpack Compose, Material 3 (dark theme default)
- Room (SQLite) for sessions/tags, DataStore for settings
- Jetpack Glance for the home-screen widget
- Vico for trend charts; custom Compose Canvas for the actogram
- AlarmManager + notifications for reminders
- java.time for all date/time logic
- Package: `com.sighted.sleep`

## Architecture
Single module, MVVM:
- `ui/` — Compose screens + ViewModels (one package per screen)
- `data/` — Room entities, DAOs, repositories, DataStore
- `domain/` — pure Kotlin, no Android imports. Metrics engine lives in `domain/metrics`.

## Hard rules
- **Never add the `INTERNET` permission** or any networking library. The app is offline by design.
- Store timestamps as **UTC epoch millis + zoneId**. Never store local date-times as the source of truth.
- Time-of-day averages/spreads use **circular statistics** (spec §4).
- Midnight split and end-date attribution follow spec §3.3.
- Every function in `domain/metrics` must have **JUnit tests**, including edge cases: crossing midnight, DST changes, naps, empty periods.
- Room schema changes need a **migration**. Never use destructive migration fallback: user data must survive upgrades.
- Keep UI on Material 3 defaults until a design pass is requested.

## Workflow
- One feature (or a small group of feature IDs) per task.
- After a change: build, run unit tests, then summarise what to check on the phone.
- Commit to Git after each working step with a clear message.

## Commands (PowerShell, from project root)
Java isn't on PATH; use Android Studio's bundled JDK first in each shell:
`$env:JAVA_HOME = "C:\Program Files\Android Studio\jbr"`
SDK (incl. adb): `C:\Users\Alexa\AppData\Local\Android\Sdk` (`platform-tools\adb.exe`)

- Build debug APK: `.\gradlew assembleDebug`
- Install on connected phone: `.\gradlew installDebug`
- Unit tests: `.\gradlew testDebugUnitTest`
