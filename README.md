# Becoming

An identity-based habit tracker for Android, inspired by *Atomic Habits*:
**"Every action you take is a vote for the type of person you wish to become."** (James Clear)

You say who you want to become (for example "Badminton Player"), then tap **+** once a day when you
practise. Each tap is a *vote*. A home-screen widget shows your identity, your progress bar and the
− / + buttons, so you can vote without opening the app.

## How it works

1. **Setup.** Enter your name, what you want to master ("Badminton") and what to call yourself
   ("Badminton Player"). Choose whether the greeting uses your identity or your name.
   **Cast my first vote** creates the journey *and* casts today's vote, so you start at 1 of 75
   instead of 0 (endowed progress).
2. **Home and widget.** Tap **+** once per local calendar day. **−** undoes *today's* vote only;
   past days can't be changed. Missed days never take votes away. Home also shows a daily quote.
3. **Level complete.** When you reach a level's threshold, **+** is disabled (the widget gets a lime
   border and a **Level up** button) until you open the app and choose:
   - **Upskill**: the level is saved to your Legacy and a fresh 0% bar starts for the next level.
   - **Complete my journey**: the level is saved, the journey is closed, and you set up a new
     identity. Your Legacy keeps every journey.
   At Level 5 (day 365) only **Complete** is offered: "You are an Expert Badminton Player".

### Levels

| Level | Name      | Cumulative votes | Votes in level | Identity title (example)    |
|------:|-----------|-----------------:|---------------:|-----------------------------|
| 1     | Habit     | 75               | 75             | Badminton Player            |
| 2     | Committed | 150              | 75             | Committed Badminton Player  |
| 3     | Skilled   | 225              | 75             | Skilled Badminton Player    |
| 4     | Advanced  | 300              | 75             | Advanced Badminton Player   |
| 5     | Expert    | 365              | 65             | Expert Badminton Player     |

- Progress in a level = (votes − previous threshold) / (threshold − previous threshold) × 100,
  clamped to 0..100 and rounded to a whole percent.
- The article follows the first letter: "You are **a** Badminton Player", "You are **an** Expert
  Badminton Player".

### Progress label (goal gradient)

| Progress in level | Widget label     | Home label                                         |
|-------------------|------------------|----------------------------------------------------|
| below 50%         | `12 votes cast`  | `12 votes cast` (`1 vote cast` for one)             |
| 50% to 99%        | `20 to go`       | `20 to Committed` (on Level 5: `20 to day 365`)     |
| 100%              | `Level complete` | `Level complete`                                   |

The progress bar fill shifts from green (0%) through bright green (50%) to lime (100%).

### Daily message (first match wins)

| Condition                                                       | Message                                            |
|-----------------------------------------------------------------|----------------------------------------------------|
| Level complete, waiting for your choice                         | Level complete. Open to choose your next step.     |
| Missed exactly yesterday, not voted yet today                   | Missed yesterday. Never miss twice.                |
| Missed 2 or more days in a row (before or after voting today)   | Welcome back. Today's vote counts.                 |
| 30% to 50% into the level                                       | Results are building below the surface.            |
| Voted today                                                     | Vote cast. 1% better today.                        |
| Otherwise                                                       | Every action is a vote for who you're becoming.    |

The day you start a journey never counts as missed. After an upskill, missed days count from the
first day you could vote in the new level.

### Daily quote

A short James Clear (*Atomic Habits*) quote rotates once per local day
(`quoteFor(epochDay)` in `domain/Quotes.kt`, 17 quotes). It appears in a card on Home and under the
greeting on the widget.

### Colours

60-30-10, with light and dark variants: neutrals about 60%, violet about 30% (identity and actions:
**+**, Upskill, Level up, Cast my first vote, the current level, the "never miss twice" nudge) and
green to lime about 10% (progress fills only). Lime marks wins: completed levels, the celebration
badge, and the widget's level-complete border. **Complete my journey** and **−** are outlined neutral
buttons. No dynamic colour; system fonts.

## Open in Android Studio

Requirements: **Android Studio Ladybug (2024.2) or newer** and **JDK 17 or newer** (the JDK bundled
with Android Studio works). The Android SDK for API 35 is installed by Studio on first sync if missing.

1. **File > Open** and pick this folder (`progress-bar`).
2. Let Gradle sync. The wrapper downloads Gradle 8.9 on first use.
3. Select the `app` run configuration and a device or emulator (Android 8.0 / API 26 or newer).
4. Click **Run**.

Stack: AGP 8.7.3, Kotlin 2.0.21 (Compose compiler plugin), KSP 2.0.21-1.0.28, Compose BOM 2024.12.01
(Material 3), Navigation Compose 2.8.5, Glance 1.1.1, Room 2.6.1, DataStore 1.1.1, WorkManager 2.10.0,
Konfetti 2.0.4. compileSdk/targetSdk 35, minSdk 26, JVM target 17. Versions live in
`gradle/libs.versions.toml`.

## Add the widget

1. Long-press an empty spot on the home screen and choose **Widgets**.
2. Find **Becoming** and drag it to the home screen (default size 4×2; it can be resized).
3. Tap **+ Vote** on the widget to cast today's vote, or **−** to undo it. Tap the identity card to
   open the app. With no journey yet, the widget shows **Set up your identity**.

Widget layout, top to bottom:

- **Identity card**: icon tile, "Hey, <greeting>", and one line underneath, first match wins:
  "Level N complete · X votes" (lime) > "Missed yesterday. Never miss twice." (violet) >
  "Welcome back. Today's vote counts." > today's quote (muted, italic).
- **Progress**: the bar, then "Level 2 · 20 votes cast" on the left and "27%" on the right.
- **Buttons**: **−** (outlined; dimmed when there is nothing to undo) and **+ Vote**. After voting it
  reads **Voted today** (disabled). When a level is complete it becomes **Level up**, which opens the app.

The widget refreshes right after every vote (from the widget or the app) and just after local
midnight (a WorkManager job), so "Voted today" resets each day.

## Run the tests

The level, progress, label, colour, message and quote rules are plain Kotlin in
`app/src/main/java/app/becoming/domain/` (`HabitRules.kt`, `Quotes.kt`), with JUnit 4 tests in
`app/src/test/java/app/becoming/domain/HabitRulesTest.kt`.

```bash
./gradlew test          # macOS / Linux
gradlew.bat test        # Windows
```

Or right-click `HabitRulesTest` in Android Studio and choose **Run**.

## Quotes

The quotes in `domain/Quotes.kt` were supplied from memory and **must be checked word-for-word
against *Atomic Habits* before any Play Store release**. Shipping many quotes from one book in a
published app carries copyright risk, so keep the set small and always attributed to James Clear
(as it is now), or get permission.

## Data and backup

- Room database `becoming.db`: journeys, one vote row per journey per day, and saved level records.
- DataStore: only the greeting preference (identity or name).
- `android:allowBackup="true"`: Android Auto Backup restores your journeys when you set up a new phone
  from a Google backup.

## Project layout

```
app/src/main/java/app/becoming/
  BecomingApp.kt        Application + AppContainer (manual DI)
  MainActivity.kt
  domain/               Pure rules (levels, progress, labels, colours, messages, daily quote) and models
  data/                 Room entities/DAOs/database, DataStore settings, HabitRepository
  ui/                   NavHost root, view model, theme, Setup/Home/Celebration/Legacy screens
  widget/               Glance widget, vote/undo actions, receiver
  work/                 MidnightWorker (daily widget refresh)
```

## Versioning and releases

The version lives in `gradle.properties` (`VERSION_NAME`, for example `1.0.0`); `versionCode` is
derived from it (MAJOR×10000 + MINOR×100 + PATCH). Release builds are signed only when the
`BECOMING_KEYSTORE` (PKCS12 file path), `BECOMING_KEYSTORE_PASSWORD`, `BECOMING_KEY_ALIAS` and
`BECOMING_KEY_PASSWORD` environment variables are set, which is how the CI workflow in
`.github/workflows/android.yml` builds tagged releases. See `CHANGELOG.md` for the release steps.

## Build status

This project was written on a machine without a JDK or Android SDK, so it has **not been compiled
or run yet**. The first Gradle sync and `./gradlew test` in Android Studio are the first real build.
The Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) are the
official Gradle 8.9 files; the jar's SHA-256 matches the checksum Gradle publishes.
