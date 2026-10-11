package app.becoming.domain

import kotlin.math.roundToInt

/** How the greeting addresses the user. */
enum class GreetBy { IDENTITY, NAME }

/** Level ladder: cumulative vote thresholds, names and identity titles. Pure Kotlin. */
object Levels {
    const val MAX_LEVEL = 5
    val THRESHOLDS = listOf(75, 150, 225, 300, 365)
    val NAMES = listOf("Habit", "Committed", "Skilled", "Advanced", "Expert")
    val JOURNEY_CAP: Int get() = THRESHOLDS.last()

    private fun index(level: Int) = level.coerceIn(1, MAX_LEVEL) - 1

    /** Cumulative votes needed to complete [level]. */
    fun threshold(level: Int): Int = THRESHOLDS[index(level)]

    /** Cumulative votes at which [level] begins. */
    fun prevThreshold(level: Int): Int = if (level <= 1) 0 else THRESHOLDS[index(level) - 1]

    /** Votes contained in [level] (75 for L1..L4, 65 for L5). */
    fun levelSize(level: Int): Int = threshold(level) - prevThreshold(level)

    fun name(level: Int): String = NAMES[index(level)]

    /** The level a vote total is working towards, ignoring the user's upskill choice. */
    fun levelForVotes(votes: Int): Int =
        (1..MAX_LEVEL).firstOrNull { votes < threshold(it) } ?: MAX_LEVEL

    fun votesInLevel(totalVotes: Int, level: Int): Int =
        (totalVotes - prevThreshold(level)).coerceIn(0, levelSize(level))

    fun isComplete(totalVotes: Int, level: Int): Boolean = totalVotes >= threshold(level)

    /** (votes - prev) / (threshold - prev), clamped to 0..1. */
    fun progressFraction(totalVotes: Int, level: Int): Float =
        votesInLevel(totalVotes, level).toFloat() / levelSize(level)

    /** Progress inside [level] as a rounded whole percent, 0..100. */
    fun progressPercent(totalVotes: Int, level: Int): Int =
        percentOf(votesInLevel(totalVotes, level), levelSize(level))

    /** L1 = identity title; L2..L5 = "<LevelName> <identity title>". */
    fun title(level: Int, identityTitle: String): String {
        val identity = identityTitle.trim()
        return if (level <= 1) identity else "${name(level)} $identity"
    }

    /** "a" or "an" chosen by whether the first letter is a vowel. */
    fun article(phrase: String): String {
        val first = phrase.trimStart().firstOrNull()?.lowercaseChar()
        return if (first != null && first in "aeiou") "an" else "a"
    }

    /** "You are an Expert Badminton Player". */
    fun youAre(title: String): String = "You are ${article(title)} $title"
}

/** Rounded whole percent of [votesInLevel] / [levelSize], clamped 0..100. */
fun percentOf(votesInLevel: Int, levelSize: Int): Int {
    if (levelSize <= 0) return 0
    val clamped = votesInLevel.coerceIn(0, levelSize)
    return (clamped * 100.0 / levelSize).roundToInt().coerceIn(0, 100)
}

/**
 * Goal-gradient framing. Below 50%: "<n> votes cast" (look back at what you did).
 * From 50%: "<remaining> to go" (look ahead at how little is left). 100%: "Level complete".
 */
fun progressLabel(votesInLevel: Int, levelSize: Int): String {
    val votes = votesInLevel.coerceIn(0, levelSize.coerceAtLeast(0))
    val remaining = levelSize - votes
    val pct = percentOf(votes, levelSize)
    return when {
        pct >= 100 || remaining <= 0 -> "Level complete"
        pct < 50 -> if (votes == 1) "1 vote cast" else "$votes votes cast"
        else -> if (remaining == 1) "1 to go" else "$remaining to go"
    }
}

/**
 * In-app variant of [progressLabel]: from 50% it names the destination,
 * "<remaining> to <NextLevelName>", or "<remaining> to day 365" on the final level.
 */
fun homeProgressLabel(votesInLevel: Int, levelSize: Int, level: Int): String {
    val votes = votesInLevel.coerceIn(0, levelSize.coerceAtLeast(0))
    val remaining = levelSize - votes
    val pct = percentOf(votes, levelSize)
    if (pct < 50 || pct >= 100 || remaining <= 0) return progressLabel(votesInLevel, levelSize)
    val target = if (level >= Levels.MAX_LEVEL) "day ${Levels.JOURNEY_CAP}" else Levels.name(level + 1)
    return "$remaining to $target"
}

private val NIGHT_STOPS = intArrayOf(0xFF1EC07F.toInt(), 0xFF5FE04E.toInt(), 0xFFC2F542.toInt())
private val DAY_STOPS = intArrayOf(0xFF0E9F62.toInt(), 0xFF2FA83A.toInt(), 0xFF6A9E12.toInt())

/**
 * Progress fill colour (opaque ARGB) for a level percent: green at 0%, bright green at 50%,
 * lime at 100%, linearly interpolated in RGB between the stops. Used only for progress fills.
 */
fun progressColor(pct: Int, night: Boolean): Int {
    val stops = if (night) NIGHT_STOPS else DAY_STOPS
    val p = pct.coerceIn(0, 100)
    return if (p <= 50) lerpArgb(stops[0], stops[1], p / 50f) else lerpArgb(stops[1], stops[2], (p - 50) / 50f)
}

private fun lerpArgb(from: Int, to: Int, t: Float): Int {
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}

object Messages {
    const val LEVEL_COMPLETE = "Level complete. Open to choose your next step."
    const val NEVER_MISS_TWICE = "Missed yesterday. Never miss twice."
    const val WELCOME_BACK = "Welcome back. Today's vote counts."
    const val BUILDING = "Results are building below the surface."
    const val VOTE_CAST = "Vote cast. 1% better today."
    const val DEFAULT = "Every action is a vote for who you're becoming."
}

object HabitRules {

    fun greeting(greetBy: GreetBy, name: String, identityTitle: String): String {
        val who = if (greetBy == GreetBy.NAME && name.isNotBlank()) name.trim() else identityTitle.trim()
        return "Hey, $who"
    }

    /**
     * First day that can count as "missed" for message purposes. On level 1 the journey's start
     * day is grace (the rule "journey started before yesterday"); on later levels it is the first
     * day the user could vote after upskilling.
     */
    fun missCountFrom(level: Int, journeyStartEpochDay: Long, levelStartEpochDay: Long): Long =
        if (level <= 1) journeyStartEpochDay + 1 else levelStartEpochDay

    /** Consecutive un-voted days ending yesterday, never counting days before [countFrom]. */
    fun missedStreakBeforeToday(voteDays: Set<Long>, countFrom: Long, today: Long): Int {
        var day = today - 1
        var streak = 0
        while (day >= countFrom && day !in voteDays) {
            streak++
            day--
        }
        return streak
    }

    /**
     * Motivational line, first match wins:
     * level complete > missed exactly yesterday (not voted yet today) > missed 2+ days in a row
     * > 30..50% into the level > voted today > default.
     */
    fun message(levelComplete: Boolean, votedToday: Boolean, missedStreak: Int, progressPercent: Int): String =
        when {
            levelComplete -> Messages.LEVEL_COMPLETE
            !votedToday && missedStreak == 1 -> Messages.NEVER_MISS_TWICE
            missedStreak >= 2 -> Messages.WELCOME_BACK
            progressPercent in 30..50 -> Messages.BUILDING
            votedToday -> Messages.VOTE_CAST
            else -> Messages.DEFAULT
        }

    /** Stats for [level] computed from all of the journey's vote days. */
    fun levelSummary(level: Int, identityTitle: String, levelStartEpochDay: Long, voteDays: Collection<Long>): LevelSummary {
        val inLevel = voteDays.sorted().drop(Levels.prevThreshold(level)).take(Levels.levelSize(level))
        val start = minOf(levelStartEpochDay, inLevel.firstOrNull() ?: levelStartEpochDay)
        val end = inLevel.lastOrNull() ?: start
        // Same grace as messages: an un-voted journey start day is not a missed day.
        val countFrom = if (level <= 1 && start !in inLevel) start + 1 else start
        val missed = ((end - countFrom + 1) - inLevel.size).coerceAtLeast(0L).toInt()
        return LevelSummary(level, Levels.title(level, identityTitle), start, end, inLevel.size, missed)
    }

    fun homeState(journey: JourneyInput, voteDays: Collection<Long>, today: Long, greetBy: GreetBy): HomeState {
        val days = voteDays.toHashSet()
        val level = journey.unlockedLevel.coerceIn(1, Levels.MAX_LEVEL)
        val total = days.size
        val votedToday = today in days
        val complete = Levels.isComplete(total, level)
        val percent = Levels.progressPercent(total, level)
        val streak = missedStreakBeforeToday(
            days, missCountFrom(level, journey.startEpochDay, journey.levelStartEpochDay), today,
        )
        return HomeState(
            journeyId = journey.id,
            name = journey.name,
            skill = journey.skill,
            identityTitle = journey.identityTitle,
            greetBy = greetBy,
            greeting = greeting(greetBy, journey.name, journey.identityTitle),
            totalVotes = total,
            votedToday = votedToday,
            level = level,
            levelName = Levels.name(level),
            progress = Levels.progressFraction(total, level),
            progressPercent = percent,
            votesInLevel = Levels.votesInLevel(total, level),
            levelSize = Levels.levelSize(level),
            message = message(complete, votedToday, streak, percent),
            pendingLevelComplete = complete,
            canVote = !votedToday && !complete,
            // Only today's vote, and only if it belongs to the current level (past levels are sealed).
            canUndo = votedToday && total > Levels.prevThreshold(level),
            isFinalLevel = level >= Levels.MAX_LEVEL,
            completedLevel = if (complete) levelSummary(level, journey.identityTitle, journey.levelStartEpochDay, days) else null,
            quote = quoteFor(today),
        )
    }
}
