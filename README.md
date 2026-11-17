# Mathi (v1.1.0)

An Android app for revising and tracking TNPSC **Group 1, Group 2/2A and Group 4** preliminary exams.
No ads, no analytics, no account. Everything is stored on the phone. **It works fully offline;** the only thing that uses the internet is the optional *Latest headlines* feature (see *Privacy and internet use*), which can be switched off.

**English** · [தமிழ்](#தமிழ்)

## What it does
| Tab | Purpose |
|---|---|
| **Today** | Exam countdown, syllabus readiness, today's study minutes and streak, topics **due for revision**, progress by subject, starred (weak) topics |
| **Syllabus** | The real syllabus (266 topics) for G1 / G2 / G4. Tap a topic to move it up a revision level (Not started, First read, Revision 1-3), long-press to reset, ☆ to star it, ✎ to write your own note |
| **Current affairs** | **Curated:** 65 dated 2026 items with source links. **Latest headlines:** auto-fetched, exam-filtered headlines kept for ~6 months. Star, mark revised, add your own entries |
| **Timer** | Pomodoro focus timer (25/5 by default), logs completed sessions toward a daily goal and streak |
| **Settings** | English / Tamil / system language; the on/off switch for headline fetching |

**Revision schedule:** a topic (or current-affairs item) you have studied comes back in the *Due for revision* list after 3 days (First read), 7 (Revision 1), 21 (Revision 2) and 60 (Revision 3). Press **Revised** to move it up.

## Install on a phone (Android 8.0 or newer)
1. Send `Mathi-1.1.0.apk` to the phone (WhatsApp, Drive, USB cable...).
2. Open the file. If Android asks, allow **"Install unknown apps"** for the app you opened it from.
3. Tap **Install**.
4. **To update later, install the newer APK over the old one. Do NOT uninstall first: uninstalling deletes all progress and notes** (there is no backup/export yet).

## Please read: what the data is, and is not
- **Syllabus:** built from the official TNPSC documents (Group 1 prelims dated 16 Dec 2024; Group 2/2A and Group 4/VAO revised 27 Jan 2022). Topics were split from the official bullet points by us. Always check the current official notification at tnpsc.gov.in; TNPSC may have revised it.
- **Latest headlines:** fetched automatically from public news feeds (The Hindu, The Indian Express, BBC Tamil). A keyword filter picks exam-looking headlines, so it will miss some and let a few irrelevant ones through. Only headline, date and link are kept; we have not checked them. The list builds up as the app is used: it shows what was fetched since install (up to ~6 months), so it is short at first.
- **Curated current affairs:** compiled on **17 Nov 2026** from public sources (mainly Wikipedia's "2026 in India" and the news reports it cites). Written in our own words, **not independently verified, and far from complete.** Check anything important against an official source. One item (a 5-year age relaxation for job aspirants) is only a news report: confirm it in an official order before relying on it.
- **Tamil text** (topic names are from the official Tamil text where it was readable; current-affairs titles/summaries and many UI strings are **draft translations**) should be reviewed by a native speaker.
- Not included: question bank / mock tests, notifications, cloud sync, Mains.

## Privacy and internet use
- The app asks for **one** permission, INTERNET, used only by *Latest headlines*. It is **https only**, limited to the hosts listed in `app/src/main/assets/feeds.json`, and runs at most once every 6 hours (plus the Refresh button).
- It sends **no personal information** (no account, no identifiers, no analytics). The request is an ordinary feed download with a generic client name.
- Turn it off in **Settings** and the app makes **no network requests at all**.
- Stored from each headline: title, date, source and link (never article text). Items older than ~6 months are deleted unless you starred or revised them. Links open in your browser.

## Known limitations
- The timer runs only while the app is open and **pauses when you leave the app** (phones kill background apps). The screen stays on during a session.
- No backup/export: progress lives only on the phone.
- Tested on one phone (Oppo/ColorOS) and in landscape; not tested on a real tablet.
- Headline sources can change or disappear (two we tried were already broken). If a source fails the app keeps working and says how many sources could not be reached; to add or replace a source edit `feeds.json`.

---

## For whoever maintains this next
**Stack:** Kotlin, Jetpack Compose (Material 3), Room (KSP), ViewModel + StateFlow, Navigation Compose. Manual dependency container, no DI framework.
Read **`CLAUDE.md`**: it records the purpose, every architecture decision and why.

### Build and test
```bash
./gradlew testDebugUnitTest      # JVM unit tests (130)
./gradlew assembleDebug          # debug APK: app/build/outputs/apk/debug/
./gradlew assembleRelease        # signed release APK (needs keystore.properties, see below)
adb install -r app/build/outputs/apk/release/app-release.apk
```
Needs JDK 17+ (built with 21) and the Android SDK (compileSdk 37). Debug builds work without the signing key.

### Signing key (IMPORTANT)
Android only accepts an update if it is signed with the **same key** as the installed app. The release key is:
- `~/.android-keys/tnpscprep-release.jks` (outside the project) and
- `keystore.properties` in the project root (passwords; git-ignored, never commit it).

**Back both files up somewhere safe (a password manager or an offline drive). If they are lost, nobody can ever publish an update that installs over existing copies of the app.**
Certificate SHA-256: `36:01:5F:D4:64:A5:DB:C0:23:57:5E:16:BD:C0:DA:0E:C1:C7:B6:18:83:B0:9E:66:3D:F7:2A:A2:FE:54:BC:DB`

### Updating content (no code changes needed)
The two JSON files in `app/src/main/assets/` are the source of truth. Edit, **bump `version`**, run the tests, rebuild:
- `syllabus.json`: subjects → units → topics. **Never renumber or reuse a topic id** (progress is keyed by it); only add or rename. A new version re-seeds the syllabus and never touches progress, notes or bookmarks.
- `current_affairs.json`: copy an existing item, set `dataUpTo`, bump `version`. The test suite rejects bad dates, categories, duplicate ids and non-https links.

`tools/` records how the files were first generated (provenance only).

### Changing the database
Every schema change needs: a version bump in `AppDatabase`, a `Migration` in `data/db/Migrations.kt`, and the new `app/schemas/<n>.json` committed. Never use destructive migration: it would wipe users' progress.

---

## தமிழ்

**மதி** - TNPSC தொகுதி 1, தொகுதி 2/2A, தொகுதி 4 முதல்நிலைத் தேர்வுகளைத் திருப்பிப் படிக்கவும் முன்னேற்றத்தைக் கண்காணிக்கவும் உதவும் ஆண்ட்ராய்டு செயலி. விளம்பரம் இல்லை; கணக்கு தேவையில்லை; எல்லாத் தகவலும் உங்கள் போனிலேயே இருக்கும். **இணையம் இல்லாமலும் முழுமையாகச் செயல்படும்;** இணையத்தைப் பயன்படுத்துவது விருப்பத்தின்பேரில் இயங்கும் *சமீபத்திய தலைப்புச் செய்திகள்* வசதி மட்டுமே (அதை அணைக்கலாம்).

### செயலியில் உள்ளவை
- **இன்று:** தேர்வுக்கு எத்தனை நாள், பாடத்திட்டத் தயார்நிலை, இன்றைய படிப்பு நேரம், தொடர் நாள்கள், **இன்று திருப்ப வேண்டிய தலைப்புகள்**.
- **பாடத்திட்டம்:** உண்மையான பாடத்திட்டம் (266 தலைப்புகள்). தலைப்பைத் தட்டினால் அடுத்த நிலை (தொடங்கவில்லை → முதல் வாசிப்பு → திருப்புதல் 1-3); நீண்ட நேரம் அழுத்தினால் மீட்டமைப்பு; ☆ குறியிடலாம்; ✎ சொந்தக் குறிப்பு எழுதலாம்.
- **நடப்பு நிகழ்வுகள்:** *தொகுக்கப்பட்டவை* - 2026-இன் 65 தேதியிட்ட குறிப்புகள், மூல இணைப்புகளுடன்; *சமீபத்திய தலைப்புச் செய்திகள்* - தேர்வுக்குப் பொருத்தமானவை என வடிகட்டப்பட்டு, தானாகப் பெறப்படுபவை (சுமார் 6 மாதங்கள் வைக்கப்படும்). உங்கள் சொந்தப் பதிவுகளையும் சேர்க்கலாம்.
- **டைமர்:** 25/5 நிமிட கவன டைமர்; முடிந்த அமர்வுகள் தினசரி இலக்குக்குக் கணக்கிடப்படும்.
- **அமைப்புகள்:** ஆங்கிலம் / தமிழ் மொழித் தேர்வு.

**திருப்புதல் அட்டவணை:** படித்த தலைப்பு 3 நாள் (முதல் வாசிப்பு), 7 (திருப்புதல் 1), 21 (திருப்புதல் 2), 60 (திருப்புதல் 3) நாள்களுக்குப் பிறகு "திருப்ப வேண்டியவை" பட்டியலில் மீண்டும் வரும். **திருப்பினேன்** பொத்தானை அழுத்தினால் அடுத்த நிலைக்குச் செல்லும்.

### போனில் நிறுவுவது எப்படி (ஆண்ட்ராய்டு 8.0 அல்லது புதியது)
1. `Mathi-1.1.0.apk` கோப்பை போனுக்கு அனுப்புங்கள் (WhatsApp, Drive, USB வழியாக).
2. கோப்பைத் திறக்கவும். கேட்டால் **"தெரியாத செயலிகளை நிறுவ அனுமதி"** வழங்கவும்.
3. **நிறுவு** பொத்தானை அழுத்தவும்.
4. **புதிய பதிப்பு வந்தால் பழையதின் மேலேயே நிறுவவும். முதலில் நீக்கிவிடாதீர்கள்: நீக்கினால் உங்கள் முன்னேற்றமும் குறிப்புகளும் அழிந்துவிடும்** (தற்போது காப்புப் பிரதி வசதி இல்லை).

### தயவுசெய்து கவனிக்கவும்
- **பாடத்திட்டம்:** TNPSC-யின் அதிகாரப்பூர்வ ஆவணங்களிலிருந்து உருவாக்கப்பட்டது (தொகுதி 1 முதல்நிலை - 16 டிச 2024; தொகுதி 2/2A, 4 - திருத்தம் 27 ஜன 2022). தலைப்புகளைப் பிரித்தது நாங்களே. tnpsc.gov.in-இல் தற்போதைய அறிவிப்பைச் சரிபார்க்கவும்.
- **நடப்பு நிகழ்வுகள்:** 2026 நவம்பர் 17 அன்று பொதுத் தகவல் மூலங்களிலிருந்து (முக்கியமாக விக்கிப்பீடியா "2026 in India" மற்றும் அது சுட்டும் செய்திகள்) எங்கள் சொந்த வார்த்தைகளில் தொகுக்கப்பட்டது. **தனியாக உறுதிசெய்யப்படவில்லை; முழுமையானதும் அல்ல.** முக்கியமானவற்றை அதிகாரப்பூர்வ மூலத்தில் சரிபார்க்கவும். (வேலை நாடுவோருக்கு 5 ஆண்டு வயது தளர்வு என்ற குறிப்பு செய்தி மட்டுமே; அரசாணையில் உறுதிசெய்த பின்பே நம்பவும்.)
- **தமிழ் உரை:** நடப்பு நிகழ்வுகளின் தமிழ் மொழிபெயர்ப்பு போன்றவை வரைவு மட்டுமே; தமிழ் அறிந்தவர் சரிபார்க்க வேண்டும்.
- இதில் இல்லாதவை: வினா வங்கி / மாதிரித் தேர்வு, அறிவிப்புகள், கிளவுடு ஒத்திசைவு, முதன்மைத் தேர்வு.

### இணையப் பயன்பாடும் தனியுரிமையும்
- செயலி **ஒரே ஒரு** அனுமதியைக் கேட்கிறது: இணையம் (INTERNET); அது *சமீபத்திய தலைப்புச் செய்திகள்* வசதிக்கு மட்டுமே. https மட்டும்; `feeds.json`-இல் உள்ள தளங்களுக்கு மட்டும்; 6 மணி நேரத்துக்கு ஒருமுறைக்கு மேல் இல்லை (புதுப்பி பொத்தானைத் தவிர).
- **தனிப்பட்ட தகவல் எதுவும் அனுப்பப்படாது.** அமைப்புகளில் இதை அணைத்தால் செயலி **எந்த இணையக் கோரிக்கையும்** அனுப்பாது.
- ஒவ்வொரு செய்தியிலும் தலைப்பு, தேதி, மூலம், இணைப்பு மட்டுமே சேமிக்கப்படும். 6 மாதத்துக்கு மேற்பட்டவை (நீங்கள் குறியிட்டவை/திருப்பியவை தவிர) நீக்கப்படும்.

### வரம்புகள்
- டைமர் செயலி திறந்திருக்கும்போது மட்டுமே இயங்கும்; செயலியை விட்டு வெளியேறினால் **இடைநிறுத்தப்படும்**.
- காப்புப் பிரதி வசதி இல்லை; தகவல் போனில் மட்டுமே இருக்கும்.
- ஒரு போனில் (Oppo) மட்டுமே சோதிக்கப்பட்டது; டேப்லெட்டில் சோதிக்கப்படவில்லை.
