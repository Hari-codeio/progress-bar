# Changelog

All notable changes to Becoming are listed here. Versions follow [Semantic Versioning](https://semver.org): MAJOR.MINOR.PATCH.

How to release: bump `VERSION_NAME` in `gradle.properties`, add a section below, commit, then tag `vX.Y.Z` and push the tag. CI builds the signed APK and publishes the GitHub Release. `versionCode` is derived from `VERSION_NAME` (MAJOR×10000 + MINOR×100 + PATCH), so it always increases.

## [1.0.0] - 2026-10-11

First release.

- Setup: your name, what you want to master, the identity you are becoming, and greet-by toggle (identity or name). Setup casts your first vote.
- One vote per day with **+**; **−** undoes today's vote only. Missed days never remove votes.
- Five levels to 365 votes: Habit (75), Committed (150), Skilled (225), Advanced (300), Expert (365). Each level has its own 0–100% bar.
- Level complete: celebration screen with Upskill or Complete my journey. Completed levels are kept as Legacy history with a 12-week practice calendar.
- Home-screen widget: identity greeting, daily Atomic Habits quote, progress bar, − / + buttons. Refreshes at midnight.
- Progress bar shifts spring green → lime as you near the goal; labels switch from "votes cast" to "to go" at 50%.
- Messages: "Never miss twice", "Results are building below the surface", and more.
- Dark and light themes (black, violet, lime).
