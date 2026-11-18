# Mathi (formerly TnpscPrep)
The app is now called **Mathi** (Tamil: மதி). Launcher label/UI text/README use the new name; the applicationId and packages (`com.spcode.tnpscprep`), the DB file name (`tnpscprep.db`), the prefs name, the diary-export tag `"app":"TnpscPrep"` and the `Theme.TnpscPrep` style are INTENTIONALLY unchanged so updates install over existing copies and old exports still import.

## PURPOSE (clarified by the owner, overrides earlier guesses)
The app is for **REVISION + a progress TRACKER + real CURRENT AFFAIRS**. It is NOT primarily a question bank / mock-test app
(quizzes are optional, last). Offline is fine; from v1.1.0 ONE optional feature (Latest headlines) uses the internet (see 'Increment 9').
**Handover constraint: the owner will NOT maintain the app after development.** Therefore:
- Ship it self-contained and clearly documented (signed release APK, README with install steps in English + Tamil, this file).
- Anything time-sensitive (current affairs) must carry a visible "data up to <date>" label so it can never mislead silently.
- Let the user maintain her own content inside the app (own notes, own current-affairs entries) so it stays useful with no developer.
- Prefer evergreen features (syllabus, revision schedule, notes, tracker) over data that goes stale.

## Roadmap (current)
6. [DONE, 64 unit tests; Today screen + date picker seen on phone; due list verified by unit tests only] Revision engine: due-for-revision queue (3/7/21/60-day intervals by level), Today screen, exam date countdown, readiness %.
7. [DONE: notes+star (schema v2, MIGRATION_1_2 verified on phone over a v1 DB), progress by subject, starred list; 67 unit tests] Tracker views: readiness per exam/subject, stale topics, starred (weak) topics, per-topic own notes.
8. [DONE v1: 65 items, schema v3 + MIGRATION_2_3, browse/filter/bookmark/revise/own entries; 85 unit tests; NOT yet seen on screen (phone was locked)] Current affairs: real dated items from official/open sources (own words + source + verified flag), monthly browse,
   bookmarks/revised flag on the same revision queue, user-added entries.
9. [DONE: v1.0.0 signed APK, README EN+TA; on-phone test of the release build PENDING (needs uninstalling the debug build = data loss on the test phone)] Release: signed APK, README (EN+TA), handover notes, keystore backup instructions.

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

## Decisions added in increment 7
- Schema v2: `topic_note(topicId PK, note, starred, updatedAt)`, NO FK to `topic` (same rule as progress). `MIGRATION_1_2` in
  `data/db/Migrations.kt`; never use fallbackToDestructiveMigration. Every schema change = version bump + Migration + committed `app/schemas/<n>.json`.
- A topic_note row exists only while there is a note or a star (otherwise deleted).
- Star/note buttons use text glyphs (☆ ★ ✎), no icon library. Note dialog state is rememberSaveable.
- Today tab also shows progress by subject (for the target exam) and the starred (weak) list with note previews.
- Known gap: allowBackup=false and no export, so uninstalling loses progress/notes. Consider an export/import file in the release increment.

## Decisions added in increment 8 (current affairs)
- Pack = `assets/current_affairs.json` (version, dataUpTo, items). Tables: `current_affair` (re-seedable pack), `ca_user_item` (her entries),
  `ca_state` (bookmark + revision level for any item id; user ids are "user-<n>"; pack ids may not start with "user-"). No FKs; a pack update
  never touches her data. Schema v3, MIGRATION_2_3 generated from the exported 3.json.
- Every pack item: date, category (POLITY, TN, INDIA, WORLD, ECONOMY, SCIENCE, SPORTS, AWARDS), EN+TA title/summary (own words; Tamil is DRAFT),
  source name + https url, `verified` flag. ALL v1 items are `verified=false`: nobody has checked them against official sources.
- Sources used for v1: Wikipedia "2026 in India" (CC BY-SA; each bullet's cited news URL is the link), Wikipedia event articles, and a few
  search-result reports (Union Budget, Padma Awards, TN cabinet). PIB blocks automated fetches (403) so it was not used.
- Deliberately left out / toned down: a wiki line calling the Harish Rana ruling the "first" passive euthanasia case (doubtful, so only the ruling is stated).
  One TN item is explicitly marked REPORT / unconfirmed (5-year age relaxation, 3 Nov 2026): no official order was found; do not present as fact.
- Known gaps in v1: full Padma list, Nobel prizes other than Peace, state budgets, TN ministers list, Sept-Nov is thin (the source article had few entries).
- UI shows "Compiled up to <dataUpTo>" and a disclaimer so stale/unverified data can never mislead silently. Source links open the browser
  (no INTERNET permission needed). Own entries can be added/deleted in the app: this keeps the section alive with no developer.
- CA items you have revised (level >= 1) now appear in the Today due list on the same 3/7/21/60-day schedule (a bookmark alone is never due). `tools/` holds provenance scripts.

## Release v1.0.0 (handover)
- Signed with `~/.android-keys/tnpscprep-release.jks` (alias tnpscprep, PKCS12, valid ~30 years); passwords in git-ignored `keystore.properties`.
  Both MUST be backed up by the owner: losing them means no future update can install over existing copies. Debug builds work without them.
- `./gradlew assembleRelease` -> R8 minify + resource shrink; APK ~1.9 MB (debug ~13 MB), apksigner-verified (v2), no INTERNET permission,
  versionName 1.0.0 / versionCode 1. Deliverable copy: `release/TnpscPrep-1.0.0.apk` (+ .sha256), git-ignored.
- NOT yet verified: that the R8-shrunk release build runs correctly on a device (kotlinx.serialization / Room under R8). The debug build is what
  was tested. Debug and release have different signatures, so installing release over debug requires uninstalling debug (wipes its data).
- Known gaps for a next maintainer: no backup/export of progress (allowBackup=false), real tablet untested, Tamil strings + CA data need human review.

## Increment 9: Latest headlines (v1.1.0) - the ONLY feature that uses the internet
- Owner decision (17 Nov 2026): allow INTERNET for current affairs, scoped to this feature. Manifest has the INTERNET permission and a
  network security config with cleartext disabled. `SettingsStore.newsEnabled` (default ON) is the master switch: when off, NO requests are made.
- Why feeds: there is no free exam-curated current-affairs API; public news RSS gives headline+date+link only, and each feed holds just 1-7 days,
  so history accumulates on the phone (rolling ~6 months, `NewsPipeline.RETENTION_DAYS`=183) and the curated pack covers earlier months.
- Sources (assets/feeds.json; edit to change): The Hindu (National, TN, Economy, International, Science, Sport), Indian Express (India), BBC Tamil.
  Probed and rejected: PIB RSS (Hindi only, no dates), Dinamani (404), DD News (unreachable), AIR (redirect/no data), Google News RSS (ToS grey, 7-day window).
- Safety: HttpFeedSource = https only, host allow-list from feeds.json checked on every redirect hop, 10s/15s timeouts, 2 MB cap, generic User-Agent, no cookies.
  FeedParser rejects DOCTYPE/ENTITY (no XXE/billion-laughs), neutralises unknown entities, skips unexpected tags. Stored: title (tags stripped, <=220 chars),
  https link, date, source, category. NEVER article text. Links only open in the browser (no WebView).
- Relevance = keyword heuristic in `domain/NewsRelevance` (weights, +1 bonus for specialised feeds, deny-lists for entertainment/markets/street crime).
  Real-data check: ~143 of ~480 offered items kept; a few false positives remain. Specialised feeds' hint wins the category (except a clear TN mention).
- Refresh: on app start (TnpscApp) and when the Current affairs tab opens; skipped if <6h since last success; manual Refresh bypasses the 6h pause. Each feed is isolated.
  No WorkManager (ColorOS kills background work). Prune never deletes a headline she starred or revised (checks ca_state "news-<id>").
- Schema v4: `news_item` + MIGRATION_3_4. Star/revised for headlines reuse `ca_state` with ids "news-<hash16>"; revised headlines join the Today due list.
- Tests: FeedParser against REAL trimmed feed samples (app/src/test/resources/feeds), FeedPolicy (look-alike hosts), pipeline, relevance. kxml2 is a TEST-only
  dependency (Android has the pull parser built in).
- Not verified: the Latest headlines UI in Tamil; behaviour over many days (retention, feed breakage); release (R8) build on a device.

## Avatar reactions (increment 10, in progress)
- Owner's FINAL reaction set (domain/Mood): LAUGH, CHEER, PROUD, WORRIED, CRY, ANGRY, CELEBRATE. Rules in `domain/MoodEngine` (angry >= 7 days away, cry 3-6,
  worried 2 days / 8+ overdue / evening goal unmet, celebrate streak >= 14, laugh >= 7, cheer goal met + streak >= 3, proud default). Unit tested.
- Pictures are `mood_<name>.webp` in res/drawable-nodpi, GIT-IGNORED on purpose (a real person's likeness; the GitHub repo is PUBLIC). The app looks them up by
  name at runtime and shows an initial badge if one is missing, so a clean checkout still builds. ALL SEVEN pictures exist (laugh, cheer, proud, worried, cry, angry, celebrate). The tall ones (cry/angry/celebrate, 512 x ~660-700) are shown as their TOP square (ContentScale.Crop + TopCenter); `MoodAvatar.poseFor` holds per-picture effect positions (eyes, face centre, steam, sparkle) in fractions of that square: recalibrate there if a picture is replaced.
- Effects are drawn in code over the transparent picture (ui/mood/MoodAvatar): angry = pulsing RED face flush + steam; cry = BLUE tears + water puddle;
  celebrate/cheer/laugh = bounce + confetti; proud = sparkle; worried = sweat drop. Settings > Preview reactions shows all of them.
- Source art + prompts live in the git-ignored `design-src/` (portrait cut-out, sliced moods, prompt file).
- Open owner decisions: "login" = local profile (recommended) vs real accounts; make the GitHub repo private?
- Slicing lesson: split a sheet by the characters' OWN outlines (connected components on the whole sheet), never at fixed column lines: a raised fist crossed the line once and left a stray hand in a neighbour. Outer hair gaps are cleared; eyes/teeth are protected by only clearing enclosed white at the left/right edges.

## Local account + palette (increment 10 continued)
- Palette changed by the owner (dislikes blue): CRIMSON #C8102E primary, MAROON #800020 secondary, gold accent, warm off-white background; dark scheme is a wine-black.
  Blue survives ONLY in the crying tears/puddle (owner asked for blue water). Launcher background is maroon (#7A0019).
- "Login" = a LOCAL account (owner's decision): name + exam + optional exam date + daily goal + OPTIONAL 4-digit PIN. No server, no email, no password database.
  `ui/auth`: AuthGate decides NEEDS_ACCOUNT -> WelcomeFlow (3 steps), LOCKED -> LoginScreen (PIN pad), UNLOCKED -> the app (not even composed while locked).
  `domain/PinSecurity`: PIN = exactly 4 digits; salted PBKDF2-HMAC-SHA256 (120k iterations); constant-time verify; after 5 wrong PINs a wait that doubles (cap 15 min),
  persisted so restarting the app does not reset it. It is a PRIVACY lock, not bank-grade security (10,000 combinations). Re-locks after 5 minutes away
  (rotation/language-change restarts do not count) and on cold start.
- Forgot PIN = no recovery (nothing leaves the phone): the only way in is "Erase and start over" (database + settings wiped, packs re-seeded: `AppContainer.resetEverything`).
  Settings > Account: set / change / remove PIN, lock now, delete account and data.
- Existing installs (before accounts) see the Welcome flow once; their data is kept.
- Artwork for these screens is looked up by name at runtime, like the moods (git-ignored): `login_wave` (welcome/login hero) and `app_logo`. Stand-ins until supplied.
- Not done: fingerprint unlock (androidx.biometric could be added), multiple profiles.
- Assets delivered and wired: `login_wave` + `login_vanakkam` (welcome/login hero; Tamil UI shows Vanakkam, others the wave; GIT-IGNORED, a real person's likeness) and the
  `app_logo` brand mark (crimson/maroon/gold book + check + star; TRACKED in git, it is not personal). Launcher icon = adaptive: cream background + `ic_launcher_foreground`
  (logo at 58% of the 108dp canvas = inside the safe zone) + `ic_launcher_monochrome` for Android 13 themed icons. The logo's thin white separators are kept by an
  "opening" trick (only the wide outside area is made transparent). The logo sits on a white badge top-left of the crimson header (crimson-on-crimson would vanish).
- Verified on the phone before this: Welcome steps 1-2 (placeholder hero). NOT yet seen with the final pictures: the phone was locked.

## Increment A1: activity log (event sourcing)
- Schema v5: `activity_event(id, timestamp, localDate, type, refId, details JSON)` + indexes on localDate and type; `domain/ActivityType` (stable names; MOCK_TEST and
  HEALTH_LOG reserved). All day-level views (heatmap next, day sheet, streaks) are meant to be DERIVED from this table, not from stored daily totals.
- Events are written INSIDE the same Room transaction as the change they describe (topic level change/reset, note, focus session, revised/added current-affairs item),
  so an action and its event can never disagree. `localDate` is fixed at write time in the device zone. Builder: `data/ActivityEvents` (pure, tested).
- MIGRATION_4_5 creates the table and BACKFILLS: every study_session -> FOCUS_SESSION, every topic with level > 0 -> one TOPIC_PROGRESS at its last-touched time
  (older topic history was never stored). Backfilled rows have "backfilled":true.
- NOT yet verified on a device (adb became unauthorized); 157 unit tests pass. Next: heatmap UI + day bottom sheet with an optional daily note.

## Increment A2: activity heatmap
- Schema v6: `day_note(date PK, note, updatedAt)` + MIGRATION_5_6 (no FKs). `domain/Heatmap` (pure, tested): per-day minutes from FOCUS_SESSION events, level 0..4
  (1 = any activity under 1/3 of the daily goal, 4 = goal reached), Monday-first week grid for the last 26 weeks, no future days.
- Heatmap is DERIVED from `activity_event` (ActivityRepository), shown on the Today tab (`ui/today/HeatmapCard`). Tap a day -> pop-up with that day's events
  (backfilled rows show no clock time) + optional one-line note (max 200 chars; a dot-ringed cell has a note).
- Builds, unit tests pass. NOT seen on the phone: the app was at the PIN lock screen (owner's PIN is unknown to me).

## Increment B1: health maths (domain only so far)
- `domain/Health` (BMI + WHO bands, Mifflin-St Jeor BMR, TDEE, waist-hip ratio, water ~35 ml/kg), `Cycle` (estimated next period / ovulation / fertile window; average length from logged starts),
  `BreakNudge`. Pure + unit-tested (167 tests). ALL outputs are general ESTIMATES: the UI must say so. No UI, no storage yet.
- Next (B2): Health screen behind the PIN lock, entries stored as HEALTH_LOG activity events (private, deletable), break nudge in the timer.

## Increment B2: health toolkit UI
- Schema v7: `health_entry(id, date, kind, value, createdAt)` + MIGRATION_6_7. Kinds: WEIGHT_KG, WAIST_CM, HIP_CM, WATER_ML, PERIOD_START (`data/HealthRepository.HealthKind`).
  Profile (height, age, sex, activity, cycle length) lives in SettingsStore (`HealthProfile`), wiped by clearAll.
- Each entry also writes a HEALTH_LOG activity event holding ONLY the kind (never the value). Heatmap and day sheet queries EXCLUDE HEALTH_LOG so private data never shows there.
- UI: "Health (private)" card on the Today tab -> pop-up (`ui/today/HealthSheet`): profile, log, estimates (BMI, BMR/TDEE, WHR, water, next period / fertile window, all labelled estimates),
  history with per-entry delete, and "Delete all health data" (two taps; also removes HEALTH_LOG events). Behind the app's PIN lock like everything else.
- Timer: after 50+ focus minutes without starting a break, a soft hint is shown on the Timer tab (`TimerViewModel.breakNudge`). No notification (ColorOS kills background work).
- Built, 167 tests pass; NOT yet seen on the phone (PIN lock). Tamil strings are DRAFT.

## Increment S1: step counter (approximate, opt-in)
- `domain/StepCounter` (pure, 10 tests): TYPE_STEP_COUNTER is a running total since boot, read ONLY when the app comes to the front (one-shot listener, no service/job).
  Each read adds `reading - lastReading` to the current day. Reboot = reading < lastReading OR estimated boot time (now - elapsedRealtime) moved > 2 min; then the whole reading is new. Never negative.
  Next-day read credits the steps since the last open to the new day (approximate). Gap of more than 1 day: fresh baseline, nothing guessed.
- Schema v8: `step_day(date PK, baseline, lastReading, steps, bootMs, updatedAt)` + MIGRATION_7_8. `ActivityType.STEPS`: ONE event per day (refId = date), replaced on each read in the same transaction;
  excluded from the heatmap query (walking must not colour a study day) but shown in the day sheet.
- Feature hidden when `getDefaultSensor(TYPE_STEP_COUNTER)` is null. Manifest: ACTIVITY_RECOGNITION (runtime prompt only on API 29+, after a plain explanation pop-up; permanently denied -> "Open phone settings").
  UI: `ui/today/StepsCard` (today's steps + 7-day bars, labelled approximate). No Health Connect, no INTERNET use.
- NOT yet seen on the phone (PIN lock). 177 unit tests.

## Increment C: phone-usage view (opt-in)
- Android has no in-app prompt for this: she turns on "Usage access" for the app in system settings (`ACTION_USAGE_ACCESS_SETTINGS`); `UsageRepository.hasAccess()` checks AppOps. Manifest: PACKAGE_USAGE_STATS + a launcher-intent `<queries>` (app NAMES for apps with a home-screen icon only).
  We do NOT use QUERY_ALL_PACKAGES. Off by default (`SettingsStore.usageEnabled`).
- Read ONLY when the app comes to the front (and she opted in + access is on): `queryAndAggregateUsageStats` per calendar day for today + 6 days back (`domain/PhoneUsage`, tested). Apps without a launcher icon and the home launcher are excluded.
  Stored in schema v9 (`usage_day` top-10 apps per day, `usage_total` whole-day minutes; MIGRATION_8_9), pruned after 90 days. No activity_event, so it never colours the heatmap. On-device only, no export.
- UI `ui/today/UsageCard`: explanation pop-up -> system Usage access screen; today's total, top 5 apps, 7-day bars with her study minutes under each bar; "Turn off" and two-tap "Delete data". Labelled approximate.
- Needs the Usage access screen to exist on the phone (otherwise shows "not available"). 184 unit tests. NOT yet seen on the phone.

## Increment F1: daily food check-in (habit tracker, not a diet tool)
- No schema change: one `FOOD_CHECKIN` row per FOOD DAY in `activity_event` (refId = date, details {"value":"HEALTHY|MIXED|JUNK"}); logging again REPLACES it (`FoodRepository.set`, null = remove, used by Undo).
  Food day changes at 3 AM local (`FoodCheckin.foodDay`). `ActivityEvents.build` got a `localDateOverride`. FOOD_CHECKIN is excluded from the heatmap study levels (like STEPS/HEALTH_LOG) but shown as a small dot per day and in the day sheet.
- All rules are DERIVED in pure `domain/FoodCheckin` (tested): week = Mon-Sun, healthy-days count, streak (HEALTHY only; one grace day per week; unlogged today never breaks it),
  strict chain = consecutive calendar days logged JUNK (MIXED/HEALTHY/unlogged breaks it). Strict-mode nudge shows ONCE per chain: only the chain's START date is remembered (`SettingsStore.lastFoodAlertStart`).
  Strict mode toggle is in Settings, OFF by default. After 18:00 with nothing logged the card is highlighted (no notifications: ColorOS kills background work).
- UI `ui/today/FoodCard`: pick card -> result (spring + confetti + haptic for HEALTHY, 5 s Undo) -> compact chip; Mon-Sun strip (emoji + colour); "Healthy days this week X/7" + streak.
- EMOJI ASSETS: lookup order = own `food_*.webp` -> mood avatar stand-ins (healthy=cheer, mixed=proud, junk=worried, angry=angry; top square) -> placeholder vectors. The app uses `drawable-nodpi/food_healthy|mixed|junk|angry.webp` (192x192, transparent) when present, else the marked placeholder vectors `drawable/food_ph_*.xml`. `res/raw/keep.xml` protects name-looked-up pictures (mood_/login_/food_) from the release resource shrinker.
- 197 unit tests; NOT seen on the phone (PIN lock). Tamil copy is DRAFT.

## Increment D1: diary (plain text, offline, private)
- Schema v10: `diary_entry(id, localDate, text, moodEmojiId?, createdAt, updatedAt)` + index on localDate + MIGRATION_9_10. Several entries per day. `localDate` uses the shared 3 AM rollover (`domain/LogicalDay`, also used by the food check-in).
- Every save refreshes ONE `DIARY` activity event per entry (refId = entry id; holds only the character count, NEVER the text). DIARY is excluded from heatmap colour like STEPS/FOOD; the day sheet shows entries as their own "Diary" section (day story: diary, then sessions/food/steps, empty sections skipped) + a "Write about this day" button.
- Editor: `ui/diary/DiaryScreen` (full-screen Dialog over the app, opened from the Today "Diary" card or the day sheet). Autosave = the effect restarts on every keystroke + 800 ms delay; rules in pure `domain/Autosave` (blank NEW entry never stored; blank existing entry deleted only on leaving; flush on back and on ON_STOP). Saves are serialised by a Mutex in `DiaryViewModel` so two saves never create two rows.
  Mood row reuses the 7 mood avatars (`moodEmojiId` = Mood name). Prompt chip rotates daily (`DiaryPrompts`), inserts as the first line. 20,000 char cap, 34sp line height on 19sp text (Tamil not clipped).
- Timeline: lazy list grouped by month; only the first 200 chars of each entry are loaded (no Paging library). Search = `LIKE '%q%' ESCAPE '\'` (NFC-normalised, % _ \ escaped, 200 results). NOT FTS: FTS tokenises whole words and Tamil combining signs make that unreliable; substring match works on any script and a personal diary is small.
  Verified by a unit test that runs the real SQLite LIKE on Tamil text (test-only dependency `org.xerial:sqlite-jdbc`, never packaged in the app).
- Weekly review = counts only (entries this week, mood spread, study minutes): no interpretation or advice.
- Backup: `data/DiaryBackup` = minimal diary-only Export/Import through Android's file pickers (no permission): `{"app":"TnpscPrep","version":1,"diary":[...]}` (top-level sections so a full backup can add more later). Import is merge-only in ONE transaction: new entries added (identity = localDate + createdAt), a match is updated only if the file's copy is newer, NOTHING is deleted; any invalid entry rejects the whole file; 10 MB cap. The file is plain text (the UI warns).
  The FULL app backup (progress, notes, sessions, food, health ...) is still NOT built: next increment.
- 212 unit tests. NOT yet seen on the phone (PIN lock). Tamil copy is DRAFT.

## UI upgrade pass: Phase 1 (bugs) - done in code, awaiting phone review
- Phone usage: `domain/UsageForeground` computes per-app foreground time from usage EVENTS (resumed/paused/shutdown), clipped to the day (never `queryAndAggregateUsageStats`: it returns whole overlapping buckets = 54h days).
  Android sends "paused" AND "stopped" for one close: only "paused" counts; a BG with nothing open counts from the window start only for an app's FIRST event. Own app is excluded from the list. Tests assert no app/day exceeds 24 h.
- Bottom inset: `LocalBottomBarInset` is now the floating bar's MEASURED height + 16dp (fallback 108dp), so lists always scroll clear of it.
- News revision: state logic was correct (level 4 = "Revision 3" = all four dots); the phone held two items at level 4 from old test taps and affair cards had no way to undo. Added `AffairRepository.resetRevision` + a "Reset" button when level > 0. Test: no state row = level 0.
- "Preview reactions" only in debuggable builds; "Delete account and data" -> "Erase all my data" (EN + TA). Greetings capitalise the first letter (`properName`). ONE exam filter: SyllabusViewModel now reads/writes `SettingsStore.targetExam` like Today.
- Reminder: every install cold-starts the app, which re-locks it behind the PIN, so on-device checks need the owner to unlock after each install.

## Day-centred restructure: Phase 1 (navigation shell + Profile) - in code, awaiting phone review
- Decisions (owner OK'd): UI restructure only (no schema / activity_event / data-logic change); "My exams" becomes a multi-select preference (SharedPreferences, old value converted) in Phase 3; NO mood quick check-in for now (no storage: slot only);
  full app Backup is a separate step AFTER the restructure (only the diary can be exported today); read-only queries for Getting Started are allowed; phone usage lives in Profile > Insights; "Next best action" order = exam date -> due revision -> focus session today -> food (after 18:00) -> new topic.
- Bar: Today | Study | [Focus button] | Care | Journal. One NavHost with a NESTED graph per tab (`g_today`, `g_study`, `g_care`, `g_journal`) + full-screen routes `focus`, `profile`, `journal/editor?id&date`. Tab switch = `popUpTo(start){saveState}` + `restoreState`, so each tab keeps its sub-screen and scroll.
  Full-screen pages hide the bar (`NavPolicy.isFullScreen`, tested). `LocalBottomBarInset` = measured bar height + 16dp while the bar shows, else 16dp + system nav inset.
- Today: the avatar moved to the top right and opens Profile; the phone-usage card moved to Profile > Insights; the diary Dialog is gone (Journal tab + editor route; DiaryViewModel is Activity-scoped and holds the Undo/notice state).
- Study = segmented Syllabus | News (a Revise segment joins in Phase 3). Care = steps card + the health log as a page (HealthSheet with `dismiss = null`). Focus = TimerScreen(onClose) with ONE subject chip that opens a sheet and a marked empty `FocusMusicSlot()`.
- Profile = sections Profile (name, strict food mode), Appearance, Security (PIN), Language, Insights (usage), Data (headlines switch, Erase all my data). Settings tab and `nav_settings` are gone from the bar; dead strings are removed in Phase 5.

## Day-centred restructure: Phase 2 (Today rebuilt) - in code, awaiting phone review
- Today, top to bottom: greeting + avatar (opens Profile) -> **Next best action** card (`domain/TodayGuide.nextAction`, rule order: no exam date -> due revisions -> no focus session today -> food after 18:00 if unlogged -> new topic -> all caught up) ->
  **Getting started** checklist (exam date, first focus session, first food log, first diary entry; once complete the flag `gettingStartedDone` is stored and it never returns) -> **Today's plan** (`TodayPlan`: up to 3 due revisions + the next unread topic + the newest unrevised
  curated current-affairs item; "x of y done" = revisions/first-reads counted from today's activity events + what is still open; tapping a circle does the action) -> **Quick check-ins** (the existing food card with its week strip, + one-tap water = 250 ml health entry; marked SLOT for the future mood check-in) ->
  **tile row** (study ring, streak, steps) -> **12-week heatmap** with month and weekday labels (cells fill the card width, tap = day sheet).
- Removed from Today: readiness hero, exam chips (the one filter lives in Study until the global My exams in Phase 3), steps card (now in Care), Diary and Health cards (Journal / Care tabs), due list, progress by subject, starred list.
  The due / progress / starred lists moved UNCHANGED into **Study > Revise** (`ui/study/ReviseScreen`), so nothing is lost before Phase 3.
- Steps are read on app start by `StepsAutoRead()` (Today) now that the steps card is not on Today. TodayViewModel got `extras` (read-only derivations: any session ever, done-today count, next unread topic, newest unrevised affair): no schema/data-logic change.
- 238+ unit tests (TodayGuideTest: rule order, getting-started completion, plan caps).

## Day-centred restructure: Phase 3 (Study) - in code, awaiting phone review
- **My exams** is now one global MULTI-select (`domain/ExamSelection`, tested): stored in the same `SettingsStore.targetExam` text ("ALL", "G1", or "G1,G4"), so old single values need no conversion; read as a bitmask via `SettingsStore.examMask`. Empty/unknown = all exams (lists can never go empty because of a setting).
  Today, Revise and Syllabus all read it. Edit it in Profile (under the name) or from the chip at the top of Syllabus (`ui/syllabus/ExamPicker`). The per-screen G1/G2/G4 chips are gone; the Welcome flow still picks one value (multi-select there comes in Phase 5).
- Syllabus: search box (English or Tamil substring, `searchTopics`, tested), "Continue where you stopped" card (the most recently touched topic inside My exams; opens its subject), subject cards use a stacked per-level bar (`LevelBar`, replaces LinearProgressIndicator whose empty state showed a stray red dot at 0%).
- No schema / activity_event change.

## Day-centred restructure: Phase 4 (Care + Journal) - in code, awaiting phone review
- **Care** = three groups on one page: Food (new `FoodHistoryCard`: last-30-day counts + last 5 weeks as coloured dots, derived by pure `domain/FoodHistory`, tested), Steps (existing card), Daily log (the existing health log: water, body, cycle estimates).
- **Journal** = segmented Diary | Calendar. Calendar (`ui/diary/JournalCalendar`): one month at a time with prev/next back to the oldest loaded week (53 weeks from `TodayViewModel.yearHeat`, same level rules as the Today heatmap); colour = study, dot = food, ring = diary entry, today outlined. Tap a day = the existing day story sheet (diary -> activity, "Write about this day").
- `MusicLogSlot()` in the calendar page is the marked empty slot for the future music log. No schema / activity_event change.

## Day-centred restructure: Phase 5 (onboarding + cleanup) - in code, awaiting phone review
- Welcome flow step 2 now uses the same multi-select `ExamPicker` ("My exams"), a gentle non-blocking nudge under the optional exam-date button, and the daily goal; then the optional PIN; the app opens on Today, whose Getting started card (exam date, first focus, first food, first diary) continues the setup. Existing installs are unaffected (the flow only runs for a new account).
- Cleanup: the old single-value `ExamFilter` enum is removed (everything uses the `Exam` bitmask / `ExamSelection`); 17 unused strings (settings tab, readiness hero, old diary/health card text, nav_timer, nav_settings ...) deleted from English and Tamil.

## Polish pass 1: Today's plan, study tile, diary
- Today's plan card: header progress ring + "N left" (or "Plan complete"), each row has a kind icon + label (REVIEW / READ / NEWS), tinted row, 2-line ellipsis. Study tile is now a wide card: ring (minutes), "x of goal min" and a 7-day mini bar chart (from the heatmap data); streak and steps share a narrower tile.
- Diary: schema v11 = `diary_entry.pinned` (INTEGER NOT NULL DEFAULT 0) + MIGRATION_10_11 (a plain ADD COLUMN, existing rows stay unpinned), `app/schemas/.../11.json` committed. Pinned entries list first under "Pinned" (pin button on each card); bigger cards (48dp mood, full weekday date, 3-line preview); writing streak line in the weekly review (`DiaryStats.streak`: consecutive days ending today, an unwritten today does not break it, tested); word count under the editor.
- Diary Export/Import files do NOT carry the pinned flag yet (backup format v1 unchanged); importing never changes an existing entry's pin.
- Not done from the idea list: diary-only PIN lock, "this day last year", photos, tags.

## Evening Wrap-up: Phase 1 (data + recap engine) - done, no UI yet
- Schema v12 (`MIGRATION_11_12`, SQL kept in `Migration11To12.statements` so a JVM test can run it on real SQLite): new table `planned_task(id, localDate, title, refType TOPIC|CURRENT_AFFAIRS|CUSTOM, refId?, done, createdAt, sortOrder)` (tomorrow's top 3 are stored under tomorrow's logical date) and nullable `diary_entry.wrapup` (JSON of the wrap-up parts; non-null = this entry IS a wrap-up). Existing entries are untouched (wrapup = NULL).
- `domain/Recap`: `RecapBuilder.build(day, events, goal, topicNames, zone)` is pure and derived from `activity_event` (no stored recap): study minutes/sessions/longest, topics advanced (distinct, named, resets and backfilled rows ignored), affairs revised, food, steps (approximate), diary count; empty sections omitted; `DayRecap.isEmpty` -> friendly empty recap. Study events join a day by TIMESTAMP with the 3 AM rollover; food/diary/steps by their own `localDate`. There is no songs data, so no songs section.
- `domain/Wrapup`: `WrapupContent` (well, hard, intensity 1..5, tomorrow list), `Wrapup.render` (headings passed in by the UI, so Tamil/English both work; the diary text is rendered from the JSON so search works and re-opening reads the JSON, not the text), `WrapupSaver` (find-or-update by date = never a duplicate), `WrapupDrafts` (unfinished draft is JSON in SettingsStore `wrapupDraft`, one day only, never a diary entry), `PlanOrder.move`.
- Data: `PlannedTaskRepository` (replaceFor / setDone / move / delete), `DiaryRepository.saveWrapup / wrapupFor / observeWrapupDates`.
- Verified: 272 unit tests, 0 failures (migration test checks existing entries survive and that our SQL equals Room's exported 12.json); installed on the phone: DB opened at version 12 with the existing diary entry intact, `planned_task` present.

## Evening Wrap-up: Phase 2 (the flow) - in code, verified on the phone (EN + TA)
- Route `wrapup` (full screen, bar hidden: `NavPolicy`). `ui/wrapup/WrapupScreen`: six steps (recap with count-ups, mood + intensity with a soft mood tint, "what went well" and "what was hard" with quick-pick chips and serif free text, tomorrow's top 3 with suggestions / custom line / up-down-remove, finish). Skip on steps 2-5, Back, a leave dialog (save draft / discard / keep going), draft autosaved (debounced) in SettingsStore, spring page transitions, reduced-motion respected, light haptics. Diary font = built-in `FontFamily.Serif` (bundled fonts are a later pass).
- `WrapupViewModel` loads the recap (`RecapBuilder`) and today's saved wrap-up (edit) or the draft. Finishing writes the diary entry (`DiaryRepository.saveWrapup`, edits not duplicates) and replaces tomorrow's `planned_task` rows; if she chose and wrote nothing, nothing is saved. A draft remembers tomorrow's items as plain text (refs are lost until saved).
- Entry points: Today evening card (`WrapupCard.state`: from the hour set in Profile, default 18:00, and until the 3 AM rollover; reads "Wrapped up tonight" when done), Journal diary tab card. Today also shows a `PlannedTasksCard` ("Tomorrow's plan" read-only in the evening, "Your picks for today" checkable the next day).
- Next best action rule order now: exam date -> due revisions -> focus today -> food (after 18:00) -> first undone planned task -> new topic -> all caught up (a plan she made never overrides due revisions, focus or food). Tests in `TodayGuideTest`.
- Tamil copy is DRAFT like the rest.

## Today's plan fix (Part A of the one-Journal rework)
- Bug: the plan was recomputed on every refresh, so ticking a topic made the next unread topic appear (an endless list) and the ring counted every topic advanced today (106/108).
- Now: `PlanBuilder.build` (pure, tested) picks at most 3 lines from existing data (<= 2 due revisions, then the next unread topic, then a news item; more revisions fill gaps; never the same id twice). It runs ONCE per logical day and the result is STORED in `planned_task` (`PlannedTaskRepository.generateIfEmpty`: a no-op if the day already has rows, so last night's wrap-up plan wins). Generation waits ~0.9 s for the data flows to settle. Ref types: TOPIC (read), REVISION (new, plain text so no migration), CURRENT_AFFAIRS, CUSTOM.
- `PlanCard` reads the stored rows: ring = done of THESE lines only; ticking sets `done` and does the action (`readTopic` / `reviseAffair`); a line also shows done when the same id appears in today's events (`TodayExtras.doneRefs`), so work done in the Study tab ticks it. Stored titles keep the language they were made in.
- The separate "Tomorrow's plan"/"Your picks" card is gone; the wrap-up card shows tomorrow's lines as one text line. The Next-best-action planned rule now reads "Next in your plan" and its button runs the same tick action.
- Profile: the strict-mode note is back directly under its switch.
- Part A verified on the phone: ticking the READ line made the ring 0/2 -> 1/2, the line stayed (ticked, struck through) and nothing new appeared. Also: leaving the wrap-up only asks "Save draft?" when something changed since it was loaded (`WrapupState.dirty`); `values-v29/themes.xml` sets `forceDarkAllowed=false` so the phone's automatic night mode cannot repaint a Light-theme app.

## One Journal (B1): single screen + mood-emoji animations
- The Diary | Calendar switch is gone. ONE Journal page (LazyColumn): wrap-up card -> calendar card -> day story of the selected day -> search -> weekly review -> Pinned -> "All entries" by month -> export/import; a Write button (FAB). Searching shows only the results.
- `JournalCalendarCard`: a one-week strip that opens into the whole month (Week/Month toggle, ‹ › step a week or a month back to the oldest loaded week = 53 weeks). Each day: study-level tint, food dot, and the MOOD PICTURE of that day's diary entry (a ring if there is an entry without a mood); the selected day springs up and its mood picture pops. The day sheet is no longer used here.
- `DayStory` (selected day): chips (minutes studied, topics moved, news revised, food, steps; from `RecapBuilder` with no topic names, so `DayRecap.topicsAdvancedCount` was added), her entries for that day (tap to open; "Wrap-up" tag), "Write about this day", and a Start button for tonight's wrap-up.
- Diary previews carry an `isWrapup` flag (`(wrapup IS NOT NULL)`; no schema change) so cards can show a "Wrap-up" tag.
- Editor: two chips (Free write | Evening wrap-up: saves the text, then opens the guided wrap-up), the chosen mood plays its effect in the header (`MoodAvatar`), the page tints softly with the mood (`moodTint`), mood buttons spring when chosen. Timeline cards pop in one after another (`PopIn`), the pin wiggles when it changes. The wrap-up finish screen shows the chosen mood's own reaction (`MoodAvatar`) instead of the same thumbs-up.
- Not in B1 (by decision): the diary-only PIN. B2 (this-day-last-year, export/import with pins and wrap-ups, serif font) and B3 (photos, tags; needs schema v13) are still open.

## Journal B2: memories, backup v2
- "On this day" card (`DiaryMemories.onThisDay`, pure, tested): the entries written on today's date 1..10 years ago (nearest first, max 3, 29 Feb looks back to 28 Feb in normal years); shown on the Journal only when there is one. Not seen on the phone yet: the diary is only days old, so there is nothing to show (unit-tested only).
- Diary export/import is now file **version 2**: entries also carry `pinned` and `wrapup` (the wrap-up JSON, validated on import). Version 1 files still import (defaults: not pinned, not a wrap-up). Import is still merge-only and never deletes; a pin is only ever ADDED by import (`pinned = existing || file`), a wrap-up is only added/updated from a newer file copy.
- Decision: NO bundled font. The phone's built-in serif (`FontFamily.Serif` = Noto Serif on Android) already gives the diary look, and the only serif on this machine (DejaVu Serif) is plainer and +380 KB. A specific font (e.g. Lora) can be added later by dropping an OFL .ttf into res/font if the owner names one; Tamil always uses the phone's Tamil font.

## Check-ins and Care rework: C1 (mood check-in data) - done, no UI yet
- New `ActivityType.MOOD_CHECKIN`: ONE row per logical day (refId = date, `{"value":"<Mood>"}`), replaced when she changes it, exactly like FOOD_CHECKIN: NO schema change. Excluded from the heatmap study levels (`observeSince`) and shown in the day sheet as "Mood check-in".
- `MoodRepository` (observe / set(day, mood?)) + pure `domain/MoodCheckin` (`MoodCheckins.parse` latest-wins, `lastDays`, `counts` = how many days each mood over 30 days, most frequent first, counts only: no interpretation, no score). Tested.
- The mood chosen in the Evening wrap-up also becomes that day's mood check-in (one mood per day: the latest choice wins).
- Next: C2 the Today "How are you today?" card (mood / food / water tiles), C3 the Care restructure (hero, mood strip + counts, food, water, steps, collapsed private body section).

## Check-ins and Care rework: C2 (Today "How are you today?") - in code, awaiting phone review
- Today's old "Quick check-ins" (food card + week strip + water card) is replaced by ONE `CheckinCard`: three equal tiles, one tap each. **Mood** (today's mood picture plays its own effect via `MoodAvatar`; tap opens `MoodPickSheet` with the seven pictures, the latest pick wins, picking the chosen one again clears it), **Food** (today's food picture; tap opens `FoodPickSheet`; the tile is outlined in the evening while nothing is logged; HEALTHY gives a small confetti burst + haptic), **Water** (a ring against the target: 35 ml/kg of the latest logged weight, else 2000 ml; tap = +250 ml; a small minus removes today's last glass).
- `MoodViewModel` (Today-scoped) reads/writes `MoodRepository`; the mood day = the 3 AM logical day, like food. The food week strip and "healthy days this week" left Today and come back in Care (C3). `FoodSection` and its private pieces in FoodCard.kt, and `WaterRow`, are now unused and are removed during C3.
- Strict-mode food alert logic is unchanged.

## Check-ins and Care rework: C3 (Care restructure) - in code, awaiting phone review
- Care, top to bottom: **Hero** (crimson gradient like Next best step; today's mood picture big and animated via `MoodAvatar`, tap = mood sheet; glance tiles for food picture, water ml/target, steps) -> **Mood** (7-day strip of mood pictures, tap ANY day to set/clear it; 30-day counts per mood, counts only) -> **Food** (`FoodHistoryCard`: 30-day counts + 5-week grid now showing each day's food PICTURE; tap a day to set/clear) -> **Water** (ring, 7-day bars, +250 / +500 / Undo) -> **Steps** (existing card) -> **Body and cycle** (private; collapsed behind a lock icon, opens the existing `HealthSheet(showTitle = false)`). Cards enter with a staggered `PopIn`.
- Pure helpers `domain/CareStats` (water target = 35 ml/kg of the latest weight else 2000, per-day water sums, ring fraction), tested. The mood/food sheets are shared with Today (`MoodPickSheet`, `FoodPickSheet`).
- Removed dead code: `FoodSection` and its private pieces, `WaterRow`, 13 unused strings. No schema / activity_event change.

## Restructure of Profile / Study / Today: Phase 1 (Profile hub) - verified on the phone (EN + TA)
- Profile is a hub (`ui/settings/ProfileScreen`): top card (initial, name field, My exams) + six rows with one-line summaries, each opening one short page (Study: My exams + daily goal stepper; Evening and reminders; Food: strict mode; Look and language; Privacy and security: PIN, headlines, phone usage; Data and about: syllabus note, Erase all). Open page is `rememberSaveable`; Back goes page -> hub -> out. Exam date is still set from Today. No schema / permission change.
- Next: Phase 2 Study home page (continue card, progress, due revision card, news card, subject grid; Subject/Revise/News as drill-in pages), Phase 3 Today (Now / Plan / Check-in / Progress blocks).

## Restructure Phase 2 (Study home) - verified on the phone (EN + TA)
- Study tab is a HOME page (`ui/study/StudyScreen`): progress card for My exams (overall %, white started-topics bar, "x of y started", Continue button for the last touched topic), Revise tile ("n due today", highlighted) and News tile, a Search button, then a 2-column subject grid with level bars. Syllabus (subject detail / search), Revise and News open as pages with a Back arrow; page + subject are rememberSaveable; tablets keep list+detail inside the syllabus page.
- `SyllabusScreen(vm, twoPane, initialSubject, onHome)`: Back from a subject returns to the Study home when onHome is set. No schema / permission change.
- Next: Phase 3 Today (Now / Plan / Check-in / Progress blocks).

## Restructure Phase 3 (Today) - verified on the phone (EN + TA)
- Header: greeting + name, date line and an exam-countdown chip (plural `today_exam_days`; no exam date -> "Set exam date", tap opens the date sheet). The old tagline is removed.
- Order: [evening and not wrapped up: wrap-up card FIRST] -> Next best step -> slim Getting-started strip (4 dots + next undone step, replaces the big checklist card; `GettingStartedStrip`) -> Today's plan -> How are you today? -> numbers tiles -> heatmap -> [wrapped up: the "Wrapped up tonight" card at the bottom]. Logic, data and plan storage unchanged; no schema/permission change.
- The old `GettingStartedCard` is now unused (kept for now).

## Rename to Mathi + new icon (Phases 1-2)
- App name: Mathi / மதி (see the note at the top). Icon pipeline: `design/icon-source/mathi_symbol.png` (cropped from the owner's picture; the "second" image = gold symbol alone) -> `tools/trace_symbol.py` (white -> alpha, then REAL potrace via ctypes on libpotrace0, because the potrace CLI is not installed; path in `symbol_path.json`) -> `tools/make_icon_assets.py` writes the vector layers + previews (`design/icon-preview/`, git-ignored). Re-run both scripts to regenerate everything.
- Layers: adaptive icon foreground (gold gradient #E9CB82 -> #B8862B, 58% of 108dp), background (crimson #B0102C -> maroon #6E0A1E + radial vignette, no border), monochrome (themed icons), `ic_splash` (inside the 192dp safe circle), `app_logo` (tile on the lock/welcome badge), one-colour `ic_stat_wrapup` (notification). Old raster logo/foreground/monochrome webps are removed.
- Splash: AndroidX `core-splashscreen` 1.0.1 (new, build-time only dependency); `Theme.Mathi.Starting` (crimson) on MainActivity, `installSplashScreen()` before `super.onCreate`, post-splash theme `Theme.TnpscPrep`.
- The Oppo launcher draws icons small on its own dark plate; that is the launcher, not the icon.

## Full backup: Phase 1 (format + export engine) - in code, no UI yet
- File = the diary envelope at **version 3** with more top-level sections (`data/AppBackup.kt`, `FullBackupFile`): diary, progress (topic levels), notes (+stars), sessions, affairState, userAffairs, events (activity log), dayNotes, plans, health (private), steps, settings (chosen preferences only). Plain text, no password (owner's decision). The diary-only import still reads a full file (it ignores the other sections); old v1/v2 diary files parse as a full file with empty sections.
- NEVER exported: PIN hash/salt and wrong-PIN counters, syllabus, curated pack, cached headlines, phone-usage history, wrap-up draft. DIARY events are not carried (their refId is a local id); the diary import rebuilds them. Her own current-affairs entries have local ids ("user-<n>"), so their state is exported by `userCreatedAt` instead.
- Merge rules are pure functions in `AppBackup` (all tested; nothing is ever deleted): topic level = higher wins (a restore never lowers progress); notes/day notes/affair state/steps = later `updatedAt` wins; sessions/own affairs/plans/health = identity key, added only if missing; events = identity type+refId+timestamp, but FOOD/MOOD/STEPS keep ONE (the newer) per day. Parse is all-or-nothing (one bad row rejects the file), 40 MB cap.
- `BackupRepository.exportJson()` (read-only, wired in AppContainer). New DAO methods are read queries only: NO schema change, no new permission.
- Next: Phase 2 import transaction + Profile > Data and about UI (file pickers, last-backup date); Phase 3 gentle backup reminder.

## Full backup: Phase 2 (restore + UI) - verified on the phone (English)
- `BackupRepository.restore(text)`: parse (all-or-nothing) then ONE Room transaction applying the `AppBackup` merge rules table by table (diary via `DiaryRepository.mergeEntries`); a newer one-per-day food/mood/steps event replaces the older local row (the only removal). Her own affairs are inserted first so their state can be re-attached to the new local ids.
- Settings are applied AFTER the database part and only where the phone is still at the default (a value she changed is kept). The reminder switch is never restored (needs the notification permission + alarm).
- UI: Profile > Data and about > "Back up and restore" (`BackupSection`): CreateDocument/OpenDocument pickers (no permission), file `mathi-backup-<date>.json`, "Last backed up" date (`SettingsStore.lastBackupMs`, not part of the backup, cleared by Erase). Tamil strings `bk_*` are DRAFT and not yet seen on the phone.
- Phone check: backup saved (1.1 KB, no PIN/salt in it), restoring the same file = "Added 0, updated 0" (idempotent). NOT tested: restoring into a phone that has real data / after an Erase, the restore transaction has no JVM test (Room needs a device).

## Full backup: Phase 3 (gentle reminder) - in code, card NOT seen on the phone
- `domain/BackupNudge` (pure, 4 tests): shown on Today (`ui/today/BackupNudgeCard`, below Today's plan) when the reminder switch is on, 14+ days since the last backup (or, if she never backed up, since the app first looked: `SettingsStore.backupBaseMs`, so a new install is not nagged), and not snoozed. Buttons: Open Profile / Later (hides 7 days) / Don't remind me (switch off). Switch also in Profile > Data and about ("Remind me to back up", default ON). Card only: no notification, no new permission, no schema change.
- Not seen on screen: it needs 14 days (or edited prefs) to appear; only the rule is unit-tested. Tamil strings `bkn_*` are DRAFT.

## Floating bar hides on scroll
- `AppRoot`: a NestedScrollConnection on the root Box. Scrolling down (child consumed y < -6) hides the bar + its fade; any scroll up, reaching the end of a list, a tap on a 28dp strip at the bottom edge, or a route/tab change shows it. List padding (`LocalBottomBarInset`) is unchanged while hidden so nothing jumps. Still always hidden on Focus, Profile, Wrap-up and the diary editor. Seen on the phone on Today (hide, show on scroll up, fast flick); Study/Care/Journal not checked. One earlier capture right after launch still showed the bar after a fast swipe and I did not find why.

- Today's header avatar (top right, opens Profile) is now always the thumbs-up (PROUD) picture, like the Profile header; the initial/graduation-cap badge only shows if the picture is missing. Seen on the phone.

## Today: greeting font + red card restructure
- Greeting ("Good morning") uses bundled **Playfair Display Bold** (`res/font/playfair_display.ttf`, the OFL variable font pinned to weight 700, +300 KB; licence in `design/licenses/PlayfairDisplay-OFL.txt`). Only the greeting uses it; Tamil glyphs fall back to the phone's Tamil font (not yet seen in Tamil).
- Next best step card: small caps label, 2-line bold title, 2-line body, a quiet facts line (streak when > 0 + "x of goal min today", `na_ctx_today`), smaller button, 112dp picture in the bottom-right corner. Rule order and actions unchanged. Seen on the phone (English, set-exam-date state only; other states not seen).

## Home-screen widgets (Today + Plan)
- Plain RemoteViews (no Glance/library): `widget/MathiWidgets.kt` (`WidgetUpdater`, `TodayWidget` 2x2, `PlanWidget` 3-4x2), layouts `widget_today/plan`, `xml/widget_*_info`, manifest receivers. Study facts ONLY (exam countdown, minutes vs goal, streak, thumbs-up picture if present; plan lines): nothing from diary/health/mood, because a widget is visible without the PIN. Pure rules in `domain/WidgetRules` (tested).
- Refresh: system `updatePeriodMillis` 30 min (a floor) + `WidgetUpdater.refreshAll` in `MainActivity.onStop`. Follows the app's per-app language. Tap = open the app. No new permission, no schema change. Tamil strings `widget_*` are DRAFT and not yet seen.
- Profile > Look and language > "Home-screen widgets": two buttons call `AppWidgetManager.requestPinAppWidget` (Android's own confirm dialog); falls back to a how-to line. Both widgets seen working on the phone (Oppo launcher): Today showed "12 days to exam / 0 of 60 min today" + picture; Plan showed 2 lines "0 of 2 done".
- Not done: widget preview images in the picker (it shows the app icon), widget in dark mode / Tamil, quick check-in and Focus widgets.

## Performance pass 1 (measured with `dumpsys gfxinfo` on the 2020 Oppo)
- Finding: most of the lag is the DEBUG build. Same code as a shrunk build: tab-switch p99 frame 81-130 ms (debug) -> 24-57 ms; Today scroll p95/p99 23/69 ms -> 14/21 ms, janky frames 0.1%.
- New build type `perf` (`./gradlew assemblePerf`): release code (R8 + shrunk resources) signed with the DEBUG key so it installs over a debug build WITHOUT wiping data. For measuring/own phone only, never to give to anyone. Not debuggable (no run-as, no "Preview reactions").
- Mood pictures: pre-scaled top-square copies `mood_<name>_s.webp` (256 px, used <= 64 dp) and `_m.webp` (384 px, <= 128 dp); the original above that (git-ignored like all `mood_*`; if missing the original is used). `MoodBitmaps` (LruCache of decoded bitmaps, warmed in a background coroutine at app start) replaces `painterResource` in `MoodAvatar`/`DiaryMood`, because painterResource re-decoded the bitmap on the MAIN thread every time a screen entered. If a picture is replaced, regenerate the `_s`/`_m` copies from its top square.
- Release: `assembleRelease` gives versionName 1.2.0 / code 3 (signed with the owner keystore), copy in git-ignored `release/Mathi-1.2.0.apk`. NOT yet run on a phone (installing it over a debug/perf build needs an uninstall = data loss).
- Not done: the Food card's food pictures still use painterResource; Care has staggered pop-ins + several infinite animations; no baseline profile. Re-measure after any of those.

## Today's plan: manual tasks + mood by progress
- Plan card has "Add a task" (<= 80 chars, plan holds at most `PlanMood.MAX_LINES` = 6 lines); her lines are `planned_task.refType = MANUAL` (plain text, no schema change) with an X to delete. Auto lines still fill only up to 3 (`PlanBuilder.MAX`); MANUAL rows do not count as "the day already has a plan" in `generateIfEmpty`, and auto lines sort before hers. Profile > Study > "Make my plan automatically" (`SettingsStore.autoPlan`, default on) stops auto generation.
- `domain/PlanMood.of(done,total,hour)`: none ticked = PROUD (WORRIED after 18:00), some = CHEER, half+ = LAUGH, all = CELEBRATE; shown in the plan card header (static picture, no confetti) and in the Journal day story, which now lists that day's plan lines (done/open) with its mood. Backup already carries plans (refType MANUAL passes validation). Built + 3 new tests pass; NOT seen on the phone, Tamil strings DRAFT.
- UPDATE (owner): Today's plan is NO LONGER auto-generated; it holds only tasks she adds (MANUAL) or picked in the wrap-up. The "auto plan" switch and `PlanBuilder` use in Today were removed. An empty plan shows the CRY mood; delete uses a trash icon.
- UPDATE (owner): the floating bar is now HIDDEN by default on every tab page (`scrolledAway` starts true and resets to true on each route change); it shows on a scroll up or a tap on the 40dp bottom strip (a small handle marks it); reaching the end of a list no longer shows it. List bottom padding is small while hidden.
- Swipe left/right on a tab page moves to the next/previous tab (`AppRoot` pointerInput, Final pass: ignored when a child consumed the gesture, when under 90dp, or not mostly horizontal; order Today, Study, Care, Journal; no wrap). Not seen on the phone.
- CORRECTION (owner): "bottom bar" meant the PHONE's own navigation buttons. `MainActivity.hidePhoneNavButtons()` hides them (transient: a swipe up from the bottom edge shows them briefly; no system setting changed). The earlier "bar hidden by default" change in AppRoot was REVERTED: the floating bar is back to hide-on-scroll-down / show-on-scroll-up. Tab swipe made easier: 56dp and a 1.5:1 horizontal ratio (was 90dp, 2.5:1).
- The phone should run the `perf` build (`assemblePerf`), not debug: debug is much laggier (see Performance pass 1).
- Tab swipe is now a standard `Modifier.draggable(Horizontal)` on the AppRoot Box (the first raw-pointer version failed over clickable cards on the phone): 56dp drag, or 24dp + a quick flick. Perf build on the Oppo: Today scroll + tab swipes 900 frames, 0 janky, p99 17 ms.
