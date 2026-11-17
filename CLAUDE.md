# TnpscPrep

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
