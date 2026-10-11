# Changelog

All notable changes to Becoming are listed here. Versions follow [Semantic Versioning](https://semver.org): MAJOR.MINOR.PATCH.

How to release: bump `VERSION_NAME` in `gradle.properties`, add a section below, commit, then tag `vX.Y.Z` and push the tag. CI builds the signed APK and publishes the GitHub Release. `versionCode` is derived from `VERSION_NAME` (MAJOR×10000 + MINOR×100 + PATCH), so it always increases.

## [1.1.0] - 2026-10-12

### Added

- **Edit identity.** A pencil button on Home opens your identity: change your name, what you are mastering, what you call yourself, the icon and the greeting. Votes, levels and dates stay exactly as they are. Each field is checked (not empty; at most 30, 30 and 32 characters).
- **Identity icons and photo upload.** Pick one of 22 icons (gym, runner, badminton, cricket, football, tennis, swimming, cycling, yoga, meditation, developer, reader, writer, musician, singer, artist, photographer, chef, student, language learner, entrepreneur, speaker) or upload a photo from your gallery (no permissions needed). While you type, the icon is suggested from what you are mastering. Existing journeys get a suggested icon automatically. The icon appears on the widget, Home, the celebration badge and Legacy.
- **Dark mode switch.** A sun/moon button on Home switches between light and dark. The choice is remembered and the widget follows it; until you use the switch, both follow the system.

### Changed

- Past level titles follow identity edits: fixing "Badminton Playr" also fixes the saved "Committed Badminton Playr" badges.

## [1.0.0] - 2026-10-12

First release.

- Setup: your name, what you want to master, the identity you are becoming, and greet-by toggle (identity or name). Setup casts your first vote.
- One vote per day with **+**; **−** undoes today's vote only. Missed days never remove votes.
- Five levels to 365 votes: Habit (75), Committed (150), Skilled (225), Advanced (300), Expert (365). Each level has its own 0–100% bar.
- Level complete: celebration screen with Upskill or Complete my journey. Completed levels are kept as Legacy history with a 12-week practice calendar.
- Home-screen widget: identity greeting, daily Atomic Habits quote, progress bar, − / + buttons. Refreshes at midnight.
- Progress bar shifts spring green → lime as you near the goal; labels switch from "votes cast" to "to go" at 50%.
- Messages: "Never miss twice", "Results are building below the surface", and more.
- Dark and light themes (black, violet, lime).
