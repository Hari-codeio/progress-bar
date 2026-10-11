package app.becoming.domain

/** The parts of a journey the rules need (decoupled from Room). */
data class JourneyInput(
    val id: Long,
    val name: String,
    val skill: String,
    val identityTitle: String,
    val startEpochDay: Long,
    val levelStartEpochDay: Long,
    val unlockedLevel: Int,
    /** Preset chosen by hand, or null for automatic. */
    val iconKey: String? = null,
    /** Photo path (relative to filesDir), or null. */
    val iconPath: String? = null,
)

/** A finished level: what gets saved as a LevelRecord and shown on the celebration screen. */
data class LevelSummary(
    val level: Int,
    val title: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val votes: Int,
    val missedDays: Int,
) {
    val days: Int get() = (endEpochDay - startEpochDay + 1).toInt().coerceAtLeast(0)
}

/** Everything the Home screen and the widget render. */
data class HomeState(
    val journeyId: Long,
    val name: String,
    val skill: String,
    val identityTitle: String,
    val greetBy: GreetBy,
    val greeting: String,
    val totalVotes: Int,
    val votedToday: Boolean,
    val level: Int,
    val levelName: String,
    val progress: Float,
    val progressPercent: Int,
    val votesInLevel: Int,
    val levelSize: Int,
    val message: String,
    val pendingLevelComplete: Boolean,
    val canVote: Boolean,
    val canUndo: Boolean,
    val isFinalLevel: Boolean,
    /** Non-null while [pendingLevelComplete]: stats for the level just finished. */
    val completedLevel: LevelSummary?,
    /** Today's quote (rotates daily). */
    val quote: String,
    /** Stored preset key (null = automatic); see [displayIconKey]. */
    val iconKey: String? = null,
    /** Stored photo path (relative to filesDir), or null. A photo wins over any preset. */
    val iconPath: String? = null,
) {
    /** The preset to draw when there is no photo; null means the default identity mark. */
    val displayIconKey: String? get() = resolveIconKey(iconKey, skill, identityTitle)

    val levelLabel: String get() = "Level $level · $levelName"

    /** Widget label: "12 votes cast" / "20 to go" / "Level complete". */
    val progressLabel: String get() = app.becoming.domain.progressLabel(votesInLevel, levelSize)

    /** Widget progress row, left side: "Level 2 · 20 votes cast". */
    val widgetProgressLabel: String get() = "Level $level · $progressLabel"

    /** Home label: "12 votes cast" / "20 to Committed" / "5 to day 365" / "Level complete". */
    val homeProgressLabel: String get() = app.becoming.domain.homeProgressLabel(votesInLevel, levelSize, level)

    /** The gentle "never miss twice" nudge (shown in violet). */
    val isNudge: Boolean get() = message == Messages.NEVER_MISS_TWICE

    /**
     * The line under the widget greeting, first match wins: level complete > never miss twice >
     * welcome back > today's quote.
     */
    val widgetSubtitle: WidgetSubtitle
        get() = when {
            pendingLevelComplete -> WidgetSubtitle("Level $level complete · $totalVotes votes", SubtitleKind.WIN)
            message == Messages.NEVER_MISS_TWICE -> WidgetSubtitle(message, SubtitleKind.NUDGE)
            message == Messages.WELCOME_BACK -> WidgetSubtitle(message, SubtitleKind.WELCOME)
            else -> WidgetSubtitle(quote, SubtitleKind.QUOTE)
        }
}

enum class SubtitleKind { WIN, NUDGE, WELCOME, QUOTE }

data class WidgetSubtitle(val text: String, val kind: SubtitleKind)
