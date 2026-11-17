# TnpscPrep

## PURPOSE (clarified by the owner, overrides earlier guesses)
The app is for **REVISION + a progress TRACKER + real CURRENT AFFAIRS**. It is NOT primarily a question bank / mock-test app
(quizzes are optional, last). Offline is fine: no INTERNET permission.
**Handover constraint: the owner will NOT maintain the app after development.** Therefore:
- Ship it self-contained and clearly documented (signed release APK, README with install steps in English + Tamil, this file).
- Anything time-sensitive (current affairs) must carry a visible "data up to <date>" label so it can never mislead silently.
- Let the user maintain her own content inside the app (own notes, own current-affairs entries) so it stays useful with no developer.
- Prefer evergreen features (syllabus, revision schedule, notes, tracker) over data that goes stale.

## Roadmap (current)
6. [DONE, 64 unit tests; Today screen + date picker seen on phone; due list verified by unit tests only] Revision engine: due-for-revision queue (3/7/21/60-day intervals by level), Today screen, exam date countdown, readiness %.
7. Tracker views: readiness per exam/subject, stale topics, starred (weak) topics, per-topic own notes.
8. Current affairs: real dated items from official/open sources (own words + source + verified flag), monthly browse,
   bookmarks/revised flag on the same revision queue, user-added entries.
9. Release: signed APK, README (EN+TA), handover notes, keystore backup instructions.

Lightweight, fully offline Android app for a friend preparing for TNPSC Group 1, 2 and 4 PRELIMS.
Package: `com.spcode.tnpscprep`. minSdk 26, targetSdk 36, compileSdk 37 (Compose 1.12 libs require compileSdk 37; targetSdk stays 36 to avoid new-runtime-behaviour changes).

## Working rules
- Before installing anything or making big changes: state the plan and WAIT for the user's OK.
- The user runs hands-on steps (installs, downloads) themselves; give exact commands.
- The user is a Java/Spring Boot dev, new to Kotlin/Android: briefly explain the WHY of each architectural decision.
- Machine is low on RAM. CLI-only workflow: Gradle wrapper + adb, edited in VS Code/vim. No Android Studio, no emulator.
- Small increments. After each: run `./gradlew assembleDebug`, show the result, then move on.
- Free and offline. NO backend, NO `INTERNET` permission, no analytics, no ads. Never add a dependency that needs network at runtime.
- Keep this file updated as decisions change.

## Stack
Kotlin, Jetpack Compose (Material 3), single Activity (`AppCompatActivity`, for per-app locales),
Room + KSP, ViewModel + StateFlow, Navigation Compose, kotlinx-serialization-json (seed file),
Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`). No DI framework, no heavy libs.
Targets: 2020 Oppo phone (ColorOS, modest RAM) and an unknown tablet: keep the app small and fast.

## Architecture
- `data/` Room entities/DAOs, JSON seeder, small prefs store (SharedPreferences, not DataStore: few keys).
- `domain/` only pure Kotlin logic with no Android imports (`RevisionLevel`, `StreakCalculator`) so it is JVM-unit-testable.
- `ui/` Compose screens + ViewModels (StateFlow), theme, navigation.
- Manual DI: `AppContainer` built in `Application`, ViewModels get dependencies via constructor/factory.
  (Hilt adds build time/RAM for a handful of objects; constructor injection keeps migration easy.)

## Data decisions
- Tables: `subject`, `unit`, `topic`, `topic_progress`, `study_session`, `app_meta`.
- A topic shared across exams is stored ONCE; `exams` is an int bitmask (G1=1, G2=2, G4=4) via TypeConverter.
  Filter: `exams & :mask != 0`.
- `topic_progress` is SEPARATE from `topic`, keyed by stable string `topicId`, with NO foreign key
  (a cascade/replace on re-seed must never wipe progress). Revision level 0..4, `lastUpdated` epoch millis.
- `syllabus.json` has a `version`. Seeder re-seeds only if JSON version > `app_meta.syllabus_version`,
  in one transaction, upserting syllabus rows and deleting removed ones. NEVER touches `topic_progress`.
- Topic IDs come from the JSON and are stable; never auto-generated.
- Streak is computed from `SUM(minutes) GROUP BY date` vs the daily goal (default 60 min), not stored.
- Syllabus content is a SMALL, clearly-labelled `[SAMPLE]` set. Never invent the official syllabus;
  the user will convert the official TNPSC PDFs later.

## Features (increment 1)
1. Syllabus checklist: subject -> unit -> topic; tap advances level, long-press resets; filter G1/G2/G4/All; percent per subject.
2. Focus timer: Pomodoro 25/5 adjustable, optional subject tag, session log, today's minutes, daily streak.
   v1 timer runs ONLY while the app is open (screen kept on via FLAG_KEEP_SCREEN_ON); no foreground service
   (ColorOS kills background apps). Say so in the UI copy. Count down against `SystemClock.elapsedRealtime()`.
3. Language: English + Tamil (`values/`, `values-ta/`), in-app toggle via `AppCompatDelegate.setApplicationLocales`
   + `res/xml/locales_config.xml`. Tamil: use system Noto Sans Tamil fallback; line height >= 1.5x to avoid clipping.
4. Layout: WindowSizeClass; single pane on phones, list+detail on medium/expanded. Light/dark follows system.

## Tests
Unit tests for revision-level logic, streak calculation, and the seeder's "don't wipe progress" behavior.

## Out of scope
Mains/answer-writing, notifications/reminders, cloud sync, health log, Play Store release, foreground service.

## Environment
JDK 21, SDK at `~/Android/Sdk` (platforms 36, 37.0; build-tools 35, 36). `cmdline-tools` to be installed.
Low-RAM `gradle.properties`: `-Xmx1536m`, kotlin daemon `-Xmx1g`, caching on, parallel off.
Deploy: `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Increment plan / status
1. [x] Skeleton builds (assembleDebug OK, 12 MB debug APK); phone install pending
2. [x] Room schema, seeder, tests (JVM unit tests pass; real-Room instrumented test not written)
3. [x] Syllabus screen + adaptive two-pane (builds, 33 unit tests pass; NOT yet seen on a real device)
4. [x] Timer + session log (48 unit tests; countdown + pause-on-leave verified on phone; a full completed session not yet watched)
5. [x] Language toggle (Settings tab) + Tamil typography (53 unit tests; verified on phone: Tamil renders, persists, timer survives switch, landscape two-pane OK; real tablet not tested)

## Decisions added in increment 3
- Navigation Compose is NOT used yet: with one screen, master/detail is plain state (`rememberSaveable` selected subject +
  `BackHandler`). Introduce NavHost when the timer/settings screens are added (increment 4/5).
- Two-pane when `WindowWidthSizeClass != Compact` (>= 600dp). `calculateWindowSizeClass` needs
  `@OptIn(androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi)`.
- Advance reads the current level inside a DB transaction (never trusts the level the UI showed).
- Percent per subject is computed only from topics matching the selected exam filter.

## Decisions added in increment 4
- Navigation Compose now used: two top-level tabs (Syllabus, Timer) via PrimaryTabRow + NavHost. Both ViewModels are created at
  Activity scope in MainActivity (NOT inside the NavHost) so the timer keeps running while viewing the syllabus tab.
- Timer = pure `PomodoroLogic` state machine driven by an injected monotonic clock (counts against an end time, no drift).
  When a phase ends it moves to the other phase STOPPED (never auto-starts). Only a finished FOCUS phase is logged.
- v1 rule: leaving the app (ON_STOP, except configuration changes) PAUSES the timer and shows a notice, so away-time never counts.
  Screen kept on via `view.keepScreenOn` while running. No foreground service / notifications yet.
- Settings (focus/break/daily goal) in `SettingsStore` (SharedPreferences) with bounds in `TimerLimits`; edited via steppers on the Timer tab.
- Session `date` is the local completion date stored as ISO text; today's total and streak are derived from `SUM(minutes) GROUP BY date`.

## Decisions added in increment 5
- Language: `AppCompatDelegate.setApplicationLocales` (AppCompat per-app locales) + `res/xml/locales_config.xml` + the
  `AppLocalesMetadataHolderService` manifest entry (persists the choice on API < 33). Options: System default / English / Tamil.
  Tradeoff: needs AppCompatActivity + appcompat lib; recreates the Activity on change (ViewModels, so the timer, survive).
  Alternative (hand-rolled Configuration wrapper) is fragile across OS versions.
- `androidResources.localeFilters = [en, ta]` drops library translations from the APK.
- Tamil: no bundled font (system Noto Sans Tamil fallback renders fine on the Oppo). `tnpscTypography(tamil=true)` enforces
  line height >= 1.6x font size with `LineHeightStyle(Center, Trim.None)` so glyphs aren't clipped. If a device shows tofu boxes, bundle Noto Sans Tamil.
- Settings tab holds only language for now; daily goal / durations live on the Timer tab.

## Content roadmap (agreed): syllabus -> question bank -> practice/mock -> review -> content pipeline -> pack import
Step 1 DONE: real syllabus (`syllabus.json` version 2, 12 subjects, 266 topics: G1 142 / G2 260 / G4 174; G2 counts both
the General Tamil and General English papers because a candidate chooses one). Sources (official, tnpsc.gov.in/static_pdf/syllabus/):
G1 prelims code 497 dated 16.12.2024; G2/2A and G4/VAO scheme+syllabus revised 27.01.2022 (check for newer revisions).
Prelims patterns seen in those documents: G1 = 175 GS (degree) + 25 aptitude (SSLC) = 200 Qs; G2 = General Tamil OR English 100
+ GS 75 + aptitude 25 = 200 Qs / 300 marks / 3h; G4 = General Tamil 100 + GS 75 + aptitude 25 = 200 Qs. G1 unit question counts:
Sci 10, Geo 10, Hist+NM 25, Polity 40, Economy+TN dev admin 50, TN history 40 (those are G1 only). Verify against the live notification.
Content notes: the Tamil half of these PDFs extracts as garbled text (legacy font); Tamil names were read from page images.
Tamil names marked as DRAFT (own wording, no readable official text): "Modes of agitation of Tamil Nadu and movements" (nm.u1.t06),
"Social reformers, social reform movements and social transformation of Tamil Nadu" (tn-hist.u3.t07). Native-speaker review still needed.
Topic IDs are stable: never renumber, only append. Bump `version` on every change. Re-seed removed the old [SAMPLE] rows
(their progress rows are harmless orphans).
Copyright stance: syllabus = official public document; do NOT copy coaching-site content; previous papers only from tnpsc.gov.in with
source/year labels (reuse terms unconfirmed); TN State Board textbooks are free to download but redistribution terms unconfirmed -> use as
reference, write own notes with page refs.
