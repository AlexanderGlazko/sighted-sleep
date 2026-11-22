# Sighted Sleep — Project Document

| | |
|---|---|
| **Product** | Sighted Sleep — personal sleep schedule tracker |
| **Platform** | Android (native), single device, fully offline |
| **Target device** | Nothing Phone (3a) Pro · Nothing OS 5.0 · Android 17 |
| **Owner / Builder** | Solo — product designer, vibecoding with Claude Code |
| **Time budget** | 3–4 hours / week, no hard deadline |
| **Document status** | v1.0 — approved scope for MVP |
| **Date** | 2026-11-21 |

---

## 1. Vision & Problem

### Problem
My sleep schedule is irregular and undisciplined. Bedtimes and wake times drift, and I don't have objective data on *how much* they shift, *when*, *why*, or how it affects how rested I feel.

### Vision
A private app on my phone that makes logging sleep effortless (one tap) and turns that log into clear answers:
- **How is my schedule shifting** over days, weeks, months and years?
- **What triggers** the shifts (caffeine, screens, stress, social events…)?
- **When is my best time to sleep** — which bedtime windows give me the best quality?

### Goals
| # | Goal | Measured by |
|---|---|---|
| G1 | Logging takes < 5 seconds per sleep | One-tap widget; check-in sheet ≤ 3 interactions |
| G2 | See schedule drift at a glance | Sleep timeline + midpoint trend available for any period |
| G3 | Identify likely triggers | Tag impact view with ≥ 3 sessions per tag |
| G4 | Find my optimal sleep window | Quality-by-bedtime view |
| G5 | Build discipline toward a target schedule | Deviation from target + sleep debt + consistency score |

### Non-goals
- Automatic sleep detection (sensors, wearables, Health Connect)
- Cloud sync, accounts, multiple users
- Publishing to Google Play
- Medical diagnosis or advice

### Principles
1. **Logging must be effortless** — if it's hard, I won't log, and the analytics are worthless.
2. **Private by design** — no internet permission at all. Data never leaves the phone except via manual export.
3. **Raw data is sacred** — store exact timestamps; derive everything else. Calculations can change; history can't be recovered.
4. **Correlation, not causation** — insights are presented as patterns, not verdicts.

---

## 2. User & Context

- **Single user:** me.
- **Work schedule:** regular Mon–Fri work week; Sat–Sun free.
- **Usage moments:**
  - *Night, half-asleep, in the dark* → tap widget "Sleep". Needs dark UI, big target, zero thinking.
  - *Morning, groggy* → tap "Wake", rate 1–10, tick a few tags. Under 10 seconds.
  - *Weekly / monthly, relaxed* → explore timeline, trends, insights.

---

## 3. Core Concepts & Rules

### 3.1 Sleep session
A continuous period of sleep with an exact **start** and **end** timestamp. A day can contain several sessions (main sleep + naps).

### 3.2 Main sleep vs nap
- For each calendar day, the **longest session ending on that day = main sleep**; all others are **naps**.
- The user can manually override a session's type (main ↔ nap).
- Pattern metrics (midpoint, consistency, social jetlag, quality-by-timing) use **main sleep only**. Totals (duration, sleep debt) include naps.

### 3.3 Calendar day
- Days are regular calendar days: **00:00–24:00 local time**.
- **Daily totals** ("hours slept on Tuesday") split a session at midnight. Example: 23:00 Mon → 07:00 Tue = 1h on Monday + 7h on Tuesday.
- **Session-level metrics** (quality score, midpoint, tags, deviation) are attributed to the **day the session ended** (the morning you woke up and rated it).

### 3.4 Work days vs free days
- Default: Mon–Fri = work days, Sat–Sun = free days (configurable in Settings).
- A session's day type is the day type of its **end date**. So Friday night → Saturday morning sleep counts as a *free-day* sleep (it's the night before a free day).
- The **"Day off"** tag on a session overrides it to a free-day sleep (holidays, vacation).

### 3.5 Target schedule
- One target for every day: **target bedtime** and **target wake time**.
- **Target duration** = wake − bedtime (calculated, across midnight).

### 3.6 Session rules
- Only **one active (unfinished) session** at a time.
- Sessions **cannot overlap**; editing that would create an overlap shows an error.
- Sessions shorter than **5 minutes** prompt "Discard this session?" (accidental taps).
- An active session older than **14 hours** triggers a "Still sleeping?" notification with *Stop now* / *Edit end time* actions.
- Timestamps are stored in **UTC + the time zone ID** at start, so travel and daylight-saving changes don't corrupt history.

---

## 4. Metrics Specification

All time-of-day averages use **circular statistics** (so the mean of 23:30 and 00:30 is 00:00, not 12:00). Convert minutes-of-day to an angle on a 24h circle, average the unit vectors, convert back.

| Metric | Definition | Uses |
|---|---|---|
| **Duration** | `end − start` | All sessions |
| **Daily total** | Sum of session portions within the calendar day (split at midnight) | All sessions |
| **Bedtime / Wake time** | Clock time of `start` / `end` | Main sleep |
| **Sleep midpoint** | Clock time of `start + duration / 2`. Best single indicator of circadian drift: rising = drifting later, flat = stable, zig-zag = chaotic | Main sleep |
| **Bedtime / wake deviation** | Signed circular difference from target (e.g. "+47 min late") | Main sleep |
| **Sleep debt** | Rolling 7-day sum of `(target duration − daily total)`. Surplus offsets deficit, but debt never goes below 0 | All sessions |
| **Consistency score (0–100)** | `100 − (avgSD × 100 / 120)`, clamped to 0–100, where `avgSD` = mean of circular standard deviation (minutes) of bedtime and wake time in the period. 0 min SD → 100; ≥ 120 min SD → 0. *Constant is tunable.* | Main sleep, period ≥ 3 sessions |
| **Social jetlag** | `|circular mean midpoint (free-day sleeps) − circular mean midpoint (work-day sleeps)|` in hours. > 1h is considered high | Main sleep, period needs both day types |
| **Quality by timing** | Average quality score grouped by main-sleep bedtime in **30-min buckets**. Buckets with < 3 sessions shown greyed as "not enough data" | Rated main sleeps |
| **Tag impact** | For each tag: avg quality, avg duration, avg midpoint shift **with vs. without** the tag. Shown only when both groups have ≥ 3 sessions | Rated sessions |
| **Period comparison** | Any metric above for the current period vs. the previous one (this week vs last week, etc.) with Δ | — |

### Periods
| Period | Range | Trend granularity |
|---|---|---|
| Day | Single calendar day | Session detail |
| Week | Mon–Sun | Daily points |
| Month | Calendar month | Daily points |
| Year | Calendar year | Weekly averages (option: monthly) |

---

## 5. Features

Priority: **M** = Must (MVP) · **S** = Should · **C** = Could (later, if desired)

### F1. Sleep logging
| ID | Feature | Pri |
|---|---|---|
| F1.1 | Big **Sleep / Wake** toggle on the Today screen; shows live "Sleeping since 23:42 · 6h 12m" | M |
| F1.2 | **Home-screen widget** (Glance): one tap toggles sleep/wake; displays current state. Tapping *Wake* opens the morning check-in | M |
| F1.3 | **Add session manually** (start/end date-time pickers) for forgotten logs | M |
| F1.4 | **Edit / delete** any session, with overlap validation | M |
| F1.5 | Auto main/nap classification + manual override | M |
| F1.6 | Accidental-tap guard (< 5 min) and "Still sleeping?" (> 14h) safeguard | M |
| F1.7 | Quick Settings tile for sleep/wake | C |

### F2. Morning check-in
| ID | Feature | Pri |
|---|---|---|
| F2.1 | Bottom sheet after Wake: **quality score 1–10**, **tags** (chips), **optional comment** | M |
| F2.2 | Skippable; unrated sessions show a badge and can be rated later | M |

### F3. Tags
| ID | Feature | Pri |
|---|---|---|
| F3.1 | Preset tags: Caffeine late, Alcohol, Late meal, Screens in bed, Late work, Exercise, Stress, Illness, Travel, Social event, **Day off** | M |
| F3.2 | Custom tags: create, rename, change colour, archive (archived tags keep their history). Names are unique ignoring case. A custom tag that no sleep uses can be deleted; preset or used tags can only be archived | M |
| F3.3 | Tags attach to **sleep sessions** (many-to-many) | M |

### F4. Targets
| ID | Feature | Pri |
|---|---|---|
| F4.1 | Set target bedtime + wake time; target duration derived | M |
| F4.2 | Deviation shown on each session and on Today | M |
| F4.3 | Target lines drawn on timeline and trend charts | S |

### F5. Analytics
| ID | Feature | Pri |
|---|---|---|
| F5.1 | **Today**: last night's sleep, score, deviation, 7-day sleep debt, active-session state | M |
| F5.2 | **Sleep timeline (actogram)**: one row per day, sleep drawn as bars across 00:00–24:00, naps lighter, target band shaded. Scrollable through history. *Key view for shifts.* | M |
| F5.3 | **Trends** with Day / Week / Month / Year selector: duration, bedtime, wake, midpoint, quality | M |
| F5.4 | Consistency score and social jetlag cards per period | M |
| F5.5 | Quality by timing chart | S |
| F5.6 | Tag impact list | S |
| F5.7 | Period comparison (Δ vs previous period) | S |
| F5.8 | Session list / history with filter by tag | S |

### F6. Reminders (each toggle independent, all off by default)
| ID | Feature | Pri |
|---|---|---|
| F6.1 | Bedtime reminder N minutes before target bedtime (default 30) | S |
| F6.2 | Morning check-in reminder if the last session is unrated (N minutes after wake, default 60) | S |
| F6.3 | Reminders reschedule after reboot / time change | S |

### F7. Settings & data
| ID | Feature | Pri |
|---|---|---|
| F7.1 | Targets, work days, reminders, tag manager | M |
| F7.2 | **Export** full data to JSON (with schema version) + sessions to CSV, via system file picker | M |
| F7.3 | **Import** from JSON (replace all, with confirmation) | M |
| F7.4 | Dark theme by default; light theme option | M |
| F7.5 | **Debug only:** generate 90 days of realistic sample data (to develop and test analytics) | M (dev) |

---

## 6. Screens & Navigation

Bottom navigation with 4 destinations, Material 3 defaults first, refined later in Figma.

> **Build order note (Phase 1):** the app starts with **Today** and **History** tabs. History is the session list (F5.8), pulled forward because Phase 1 needs a place to review and edit sessions. Timeline, Insights and Settings tabs are added in their phases; whether History stays a separate tab or moves inside Timeline is decided in Phase 3.

```
┌ Today ──────────┬ Timeline ───────┬ Insights ───────┬ Settings ───────┐
│ Sleep/Wake btn  │ Actogram        │ Period selector │ Targets         │
│ Last night card │ (scroll days)   │ Trends charts   │ Work days       │
│ Debt / deviation│ Tap row → day   │ Consistency     │ Reminders       │
│ Unrated badge   │ detail          │ Social jetlag   │ Tags manager    │
│ + Add manually  │                 │ Quality/timing  │ Export / Import │
│                 │                 │ Tag impact      │ Theme           │
└─────────────────┴─────────────────┴─────────────────┴─────────────────┘
Overlays: Morning check-in sheet · Session edit screen · Day detail
Widget: Sleep/Wake toggle (resizable, min 2×1)
```

---

## 7. Data Model

### Room entities
```kotlin
SleepSession(
  id: Long,                 // PK, auto
  startUtc: Long,           // epoch millis
  endUtc: Long?,            // null = active session
  zoneId: String,           // e.g. "Europe/Kyiv", captured at start
  typeOverride: String?,    // null = auto; "MAIN" | "NAP"
  quality: Int?,            // 1..10, null = unrated
  comment: String?,
  createdAt: Long,
  updatedAt: Long
)

Tag(
  id: Long,
  name: String,             // unique
  colorHex: String,
  isPreset: Boolean,
  isArchived: Boolean,
  sortOrder: Int
)

SessionTag(sessionId: Long, tagId: Long)   // composite PK, cascade delete
```

### DataStore (settings)
`targetBedtime (LocalTime)`, `targetWake (LocalTime)`, `workDays (Set<DayOfWeek>)`, `bedtimeReminderEnabled`, `bedtimeReminderOffsetMin`, `morningReminderEnabled`, `morningReminderDelayMin`, `theme`.

### Export format (JSON)
```json
{ "app": "Sighted Sleep", "schemaVersion": 1, "exportedAt": "…",
  "settings": { … }, "tags": [ … ], "sessions": [ … ], "sessionTags": [ … ] }
```

---

## 8. Technology Stack

| Layer | Choice | Rationale |
|---|---|---|
| Language | **Kotlin** | Native Android standard |
| UI | **Jetpack Compose + Material 3** | Declarative (familiar from frontend); M3 defaults now, custom design later |
| Widget | **Jetpack Glance** | Compose-style widgets; widget is a must-have, which is why native beats React Native/Flutter |
| Database | **Room (SQLite)** | Local, typed queries, migrations |
| Settings | **DataStore (Preferences)** | Lightweight key-value |
| Date/time | **java.time** | Time-zone and DST-safe |
| Charts | **Vico** (trends) + **custom Compose Canvas** (actogram, quality-by-timing) | Vico for standard charts; the actogram needs custom drawing |
| Reminders | **AlarmManager** (exact alarms) + **NotificationManager**; BootReceiver to reschedule | Works fully offline at exact times |
| File I/O | **Storage Access Framework** | Export/import without storage permissions |
| Architecture | Single module, **MVVM**: `ui/` · `data/` · `domain/` (pure-Kotlin metrics engine) | Simple enough to vibecode, testable |
| Testing | **JUnit** unit tests for the metrics engine | Verifies the maths independently of the UI — essential when AI writes the code |
| Tooling | **Android Studio** (SDK, emulator, install) + **Claude Code** (writing code) + **Git** (safety net / rollback) | |

**SDK levels:** `minSdk 34`, `targetSdk / compileSdk` = latest stable (Android 17). Personal device only, so there's no need to support old versions.

**Permissions:** `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`. **No `INTERNET` permission.**

---

## 9. Realization Plan

Estimated at 3–4 h/week. Each phase ends with a build installed on the phone.

### Phase 0 — Setup *(~1 week)*
- Install Android Studio; enable Developer options + USB debugging on the Nothing Phone.
- Create the project folder, `git init`, place this document in it, run `/init` in Claude Code to create `CLAUDE.md`.
- Have Claude Code scaffold an empty Compose app named *Sighted Sleep* (package e.g. `com.sighted.sleep`).
- ✅ **Done when:** "Hello Sighted Sleep" runs on your phone from Android Studio.

### Phase 1 — Core logging *(~1–2 weeks)* → **first usable version**
- Room database (entities in §7), repository, preset tags seeded on first launch.
- Today screen with Sleep/Wake toggle and live timer.
- Session list; add / edit / delete with validation (§3.6).
- Main/nap auto-classification.
- ✅ **Done when:** you can log real sleep every night. **Start logging from here on.**

### Phase 2 — Widget & check-in *(~1–2 weeks)*
- Glance widget with state + one-tap toggle.
- Morning check-in sheet: score, tags, comment; unrated badge.
- Tag manager (custom tags, colours, archive).
- Targets in Settings; deviation on sessions.
- ✅ **Done when:** a full night + morning takes two taps from the home screen plus a rating.

### Phase 3 — Metrics engine & core analytics *(~2–3 weeks)*
- `domain/metrics` in pure Kotlin + unit tests: midnight split, circular mean/SD, midpoint, sleep debt, consistency, social jetlag.
- Debug sample-data generator (F7.5).
- Timeline (actogram) screen.
- Insights: period selector, trend charts, consistency + social jetlag cards; Today stats.
- ✅ **Done when:** all metric tests pass and the actogram shows 90 days of sample data correctly.

### Phase 4 — Insights *(~1–2 weeks)*
- Quality by timing, tag impact, period comparison, history filter by tag.
- ✅ **Done when:** you can answer "Which bedtime gives me the best score?" and "What does late caffeine do to me?" from the app.

### Phase 5 — Reminders & data safety *(~1 week)*
- Bedtime + morning reminders with toggles, boot rescheduling, "Still sleeping?" notification.
- Export JSON/CSV, Import JSON.
- ✅ **Done when:** an export → uninstall → reinstall → import round-trip restores everything.

### Phase 6 — Design polish *(ongoing, optional)*
- Design the key screens in Figma; Claude Code reads them via the Figma connection and restyles the Compose UI.

**Total:** ~2–3 weeks to the first usable version, ~2–3 months to the full scope.

### Vibecoding workflow tips
- **One feature per Claude Code session**, using the feature IDs from this doc (e.g. *"Implement F1.3 and F1.4 per SIGHTED_SLEEP_PROJECT.md"*).
- **Commit to Git after every working step**, so a bad change is one command away from being undone.
- Ask Claude Code to **write tests first** for anything in §4.
- **Export your real data before installing a new build** once Phase 5 exists. Until then, avoid uninstalling (upgrading a build in place keeps the data).

---

## 10. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| **Nothing OS battery optimisation** delays alarms or widget updates | Reminders arrive late or never | Exempt Sighted Sleep from battery optimisation; use exact alarms; test on the device early in Phase 5 |
| **Data loss** (uninstall, reset, bad migration) | Lose history | Export/Import (F7.2–7.3); Room migrations, never destructive; export before risky installs |
| **Forgetting to log** | Gaps make the analytics misleading | One-tap widget, manual add, morning reminder, "Still sleeping?" guard |
| **Wrong calculations** (midnight, DST, circular maths) | Misleading insights | Pure metrics engine + unit tests + sample data |
| **Too little data for insights** | Tag impact or quality views are empty or noisy | Minimum sample thresholds (≥ 3); start logging after Phase 1 |
| **Scope creep / motivation loss** | Project stalls | Strict MVP; usable app after Phase 1; each phase ships something you use |

---

## 11. Success Criteria (MVP complete)
- [ ] I've logged ≥ 30 consecutive days with ≤ 3 missing nights.
- [ ] Logging a night + morning takes ≤ 10 seconds total.
- [ ] I can see my midpoint drift on the Timeline and Trends for any week, month or year.
- [ ] I know my consistency score, social jetlag and sleep debt.
- [ ] I can name my best bedtime window and my 2 most harmful tags.
- [ ] Export → reinstall → import restores all data.

---

## 12. Decisions Log
| Decision | Choice |
|---|---|
| Platform | Android only, native Kotlin |
| Data input | Manual logging + one-tap widget; no auto detection, ever |
| Quality | Single subjective score 1–10 |
| Triggers | Preset + custom tags and an optional comment, attached to sleep sessions |
| Day definition | Calendar day 00:00–24:00; totals split at midnight; session metrics go to the end date |
| Multiple sessions | Allowed; longest = main sleep, others = naps, overridable |
| Work week | Mon–Fri work, Sat–Sun free; "Day off" tag overrides |
| Target | One target bedtime + wake time for all days |
| Reminders | Optional, individually toggleable |
| Backup | JSON/CSV export + JSON import; no cloud |
| Connectivity | Fully offline, no INTERNET permission |
| Design | Material 3 defaults first, refine in Figma later |

## 13. Glossary
- **Actogram / sleep timeline** — chart with one row per day and sleep drawn as bars across the 24h clock; shifts appear as diagonal drift.
- **Sleep midpoint** — clock time halfway through the main sleep; proxy for body-clock timing.
- **Social jetlag** — difference between free-day and work-day sleep midpoints.
- **Circular mean / SD** — averaging and spread for clock times that wrap around midnight.
- **Sleep debt** — accumulated shortfall vs. target duration over the last 7 days.
