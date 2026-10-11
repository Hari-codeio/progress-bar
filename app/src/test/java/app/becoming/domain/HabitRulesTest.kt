package app.becoming.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitRulesTest {

    // ---- Levels and thresholds -------------------------------------------------------------

    @Test
    fun thresholdsSizesAndNames() {
        assertEquals(listOf(75, 150, 225, 300, 365), (1..5).map { Levels.threshold(it) })
        assertEquals(listOf(75, 75, 75, 75, 65), (1..5).map { Levels.levelSize(it) })
        assertEquals(listOf("Habit", "Committed", "Skilled", "Advanced", "Expert"), (1..5).map { Levels.name(it) })
        assertEquals(0, Levels.prevThreshold(1))
    }

    @Test
    fun boundary74_75_76() {
        assertEquals(1, Levels.levelForVotes(74))
        assertEquals(2, Levels.levelForVotes(75))
        assertEquals(2, Levels.levelForVotes(76))

        assertFalse(Levels.isComplete(74, 1))
        assertTrue(Levels.isComplete(75, 1))

        assertEquals(99, Levels.progressPercent(74, 1)) // 98.67
        assertEquals(100, Levels.progressPercent(75, 1))
        assertEquals(0, Levels.progressPercent(75, 2)) // fresh bar after upskill
        assertEquals(1, Levels.progressPercent(76, 2)) // 1.33
        assertEquals(1, Levels.votesInLevel(76, 2))
    }

    @Test
    fun level5StartsAt300() {
        assertEquals(4, Levels.levelForVotes(299))
        assertEquals(5, Levels.levelForVotes(300))
        assertEquals(300, Levels.prevThreshold(5))
        assertEquals(0, Levels.votesInLevel(300, 5))
        assertEquals(0, Levels.progressPercent(300, 5))
        assertEquals(98, Levels.progressPercent(364, 5)) // 64/65 = 98.46, never rounds up to 100 early
    }

    @Test
    fun journeyCapsAt365() {
        assertEquals(365, Levels.JOURNEY_CAP)
        assertEquals(5, Levels.levelForVotes(365))
        assertEquals(5, Levels.levelForVotes(1_000))
        assertTrue(Levels.isComplete(365, 5))
        assertEquals(100, Levels.progressPercent(365, 5))
        assertEquals(100, Levels.progressPercent(500, 5))
        assertEquals(65, Levels.votesInLevel(500, 5))
    }

    @Test
    fun progressRounding() {
        assertEquals(0, Levels.progressPercent(0, 1))
        assertEquals(1, Levels.progressPercent(1, 1)) // 1.33
        assertEquals(3, Levels.progressPercent(2, 1)) // 2.67
        assertEquals(49, Levels.progressPercent(37, 1)) // 49.33
        assertEquals(51, Levels.progressPercent(38, 1)) // 50.67
        assertEquals(13, percentOf(1, 8)) // 12.5 rounds half up
        assertEquals(17, percentOf(1, 6)) // 16.67
        assertEquals(0, Levels.progressPercent(10, 3)) // below the level's start clamps to 0
        assertEquals(37f / 75f, Levels.progressFraction(150 + 37, 3), 0.0001f)
    }

    // ---- Titles and articles ---------------------------------------------------------------

    @Test
    fun identityTitlesPerLevel() {
        assertEquals("Badminton Player", Levels.title(1, "Badminton Player"))
        assertEquals("Committed Badminton Player", Levels.title(2, "Badminton Player"))
        assertEquals("Skilled Badminton Player", Levels.title(3, "Badminton Player"))
        assertEquals("Advanced Badminton Player", Levels.title(4, "Badminton Player"))
        assertEquals("Expert Badminton Player", Levels.title(5, "Badminton Player"))
    }

    @Test
    fun articleByFirstLetter() {
        assertEquals("a", Levels.article("Badminton Player"))
        assertEquals("a", Levels.article("Committed Badminton Player"))
        assertEquals("a", Levels.article("Skilled Badminton Player"))
        assertEquals("an", Levels.article("Advanced Badminton Player"))
        assertEquals("an", Levels.article("Expert Badminton Player"))
        assertEquals("an", Levels.article("  oboist"))
        assertEquals("a", Levels.article(""))
        assertEquals("You are an Expert Badminton Player", Levels.youAre(Levels.title(5, "Badminton Player")))
        assertEquals("You are a Badminton Player", Levels.youAre(Levels.title(1, "Badminton Player")))
    }

    // ---- Goal-gradient labels --------------------------------------------------------------

    @Test
    fun progressLabelLooksBackThenAhead() {
        assertEquals("0 votes cast", progressLabel(0, 75))
        assertEquals("1 vote cast", progressLabel(1, 75))
        assertEquals("37 votes cast", progressLabel(37, 75)) // 49%
        assertEquals("37 to go", progressLabel(38, 75)) // 51%
        assertEquals("2 to go", progressLabel(73, 75))
        assertEquals("1 to go", progressLabel(74, 75))
        assertEquals("Level complete", progressLabel(75, 75))
        assertEquals("32 votes cast", progressLabel(32, 65)) // 49% on level 5
        assertEquals("32 to go", progressLabel(33, 65)) // 51% on level 5
    }

    @Test
    fun homeProgressLabelNamesTheDestination() {
        assertEquals("12 votes cast", homeProgressLabel(12, 75, 1))
        assertEquals("1 vote cast", homeProgressLabel(1, 75, 3))
        assertEquals("37 to Committed", homeProgressLabel(38, 75, 1))
        assertEquals("1 to Expert", homeProgressLabel(74, 75, 4))
        assertEquals("32 to day 365", homeProgressLabel(33, 65, 5))
        assertEquals("Level complete", homeProgressLabel(75, 75, 2))
        assertEquals("Level complete", homeProgressLabel(65, 65, 5))
    }

    // ---- Progress colour -------------------------------------------------------------------

    @Test
    fun progressColorHitsTheStops() {
        assertEquals(0xFF1EC07F.toInt(), progressColor(0, night = true))
        assertEquals(0xFF5FE04E.toInt(), progressColor(50, night = true))
        assertEquals(0xFFC2F542.toInt(), progressColor(100, night = true))
        assertEquals(0xFF0E9F62.toInt(), progressColor(0, night = false))
        assertEquals(0xFF2FA83A.toInt(), progressColor(50, night = false))
        assertEquals(0xFF6A9E12.toInt(), progressColor(100, night = false))
    }

    @Test
    fun progressColorInterpolatesLinearlyAndClamps() {
        // Halfway between #1EC07F and #5FE04E: (62.5, 208, 102.5) -> rounds to #3FD067.
        assertEquals(0xFF3FD067.toInt(), progressColor(25, night = true))
        // Halfway between #2FA83A and #6A9E12: (76.5, 163, 38) -> rounds to #4DA326.
        assertEquals(0xFF4DA326.toInt(), progressColor(75, night = false))
        assertEquals(progressColor(0, night = true), progressColor(-10, night = true))
        assertEquals(progressColor(100, night = false), progressColor(150, night = false))
        assertEquals(0xFF, progressColor(33, night = true) ushr 24) // always opaque
    }

    // ---- Daily quote -----------------------------------------------------------------------

    @Test
    fun quoteRotatesDailyWithFloorMod() {
        assertEquals(17, QUOTES.size)
        assertTrue(QUOTES.all { it.isNotBlank() })
        assertEquals(QUOTES.size, QUOTES.toSet().size)
        assertEquals("Every action you take is a vote for the type of person you wish to become.", quoteFor(0L))
        assertEquals("Habits do not restrict freedom. They create it.", quoteFor(16L))
        assertEquals(quoteFor(0L), quoteFor(17L))
        assertEquals(quoteFor(16L), quoteFor(-1L)) // floorMod: negative days still map into the list
        assertEquals("A habit must be established before it can be improved.", quoteFor(20_000L)) // 20000 mod 17 = 8
        assertTrue(quoteFor(20_000L) != quoteFor(20_001L))
    }

    // ---- Messages and greeting -------------------------------------------------------------

    @Test
    fun messagePriority() {
        assertEquals(Messages.LEVEL_COMPLETE, HabitRules.message(levelComplete = true, votedToday = false, missedStreak = 1, progressPercent = 40))
        assertEquals(Messages.NEVER_MISS_TWICE, HabitRules.message(levelComplete = false, votedToday = false, missedStreak = 1, progressPercent = 40))
        assertEquals(Messages.WELCOME_BACK, HabitRules.message(levelComplete = false, votedToday = false, missedStreak = 3, progressPercent = 40))
        assertEquals(Messages.WELCOME_BACK, HabitRules.message(levelComplete = false, votedToday = true, missedStreak = 2, progressPercent = 40))
        // Missed yesterday but already voted today: no nudge.
        assertEquals(Messages.BUILDING, HabitRules.message(levelComplete = false, votedToday = true, missedStreak = 1, progressPercent = 40))
        assertEquals(Messages.BUILDING, HabitRules.message(levelComplete = false, votedToday = false, missedStreak = 0, progressPercent = 30))
        assertEquals(Messages.BUILDING, HabitRules.message(levelComplete = false, votedToday = true, missedStreak = 0, progressPercent = 50))
        assertEquals(Messages.VOTE_CAST, HabitRules.message(levelComplete = false, votedToday = true, missedStreak = 0, progressPercent = 51))
        assertEquals(Messages.VOTE_CAST, HabitRules.message(levelComplete = false, votedToday = true, missedStreak = 0, progressPercent = 29))
        assertEquals(Messages.DEFAULT, HabitRules.message(levelComplete = false, votedToday = false, missedStreak = 0, progressPercent = 10))
    }

    @Test
    fun missedStreakRespectsJourneyAndLevelStart() {
        assertEquals(0, HabitRules.missedStreakBeforeToday(setOf(1000L, 1001L, 1002L), countFrom = 1001L, today = 1003L))
        assertEquals(1, HabitRules.missedStreakBeforeToday(setOf(1000L, 1001L), countFrom = 1001L, today = 1003L))
        assertEquals(2, HabitRules.missedStreakBeforeToday(setOf(1000L), countFrom = 1001L, today = 1003L))
        // Journey started yesterday: the start day is grace.
        assertEquals(0, HabitRules.missedStreakBeforeToday(emptySet(), HabitRules.missCountFrom(1, 1002L, 1002L), 1003L))
        // Journey started before yesterday: yesterday counts.
        assertEquals(1, HabitRules.missedStreakBeforeToday(setOf(1001L), HabitRules.missCountFrom(1, 1001L, 1001L), 1003L))
        // Level 2+ counts only from the level's first votable day.
        assertEquals(0, HabitRules.missedStreakBeforeToday(setOf(1000L), HabitRules.missCountFrom(2, 900L, 1003L), 1003L))
    }

    @Test
    fun greetingFollowsSetting() {
        assertEquals("Hey, Badminton Player", HabitRules.greeting(GreetBy.IDENTITY, "Sam", "Badminton Player"))
        assertEquals("Hey, Sam", HabitRules.greeting(GreetBy.NAME, "Sam", "Badminton Player"))
        assertEquals("Hey, Badminton Player", HabitRules.greeting(GreetBy.NAME, "  ", "Badminton Player"))
    }

    // ---- Level records ---------------------------------------------------------------------

    @Test
    fun levelSummaryCountsMissedDays() {
        val days = (100L..130L) + (132L..175L) // 75 votes with one gap (day 131)
        val s = HabitRules.levelSummary(1, "Badminton Player", 100L, days)
        assertEquals(75, s.votes)
        assertEquals(100L, s.startEpochDay)
        assertEquals(175L, s.endEpochDay)
        assertEquals(76, s.days)
        assertEquals(1, s.missedDays)
        assertEquals("Badminton Player", s.title)
    }

    @Test
    fun levelSummaryForLaterLevelUsesOnlyItsVotes() {
        val days = (0L until 150L).toList()
        val s = HabitRules.levelSummary(2, "Badminton Player", 75L, days)
        assertEquals(75, s.votes)
        assertEquals(75L, s.startEpochDay)
        assertEquals(149L, s.endEpochDay)
        assertEquals(0, s.missedDays)
        assertEquals("Committed Badminton Player", s.title)
    }

    // ---- Home state ------------------------------------------------------------------------

    private val today = 20_000L

    private fun journey(level: Int = 1, start: Long = today - 100, levelStart: Long = start) =
        JourneyInput(1L, "Sam", "Badminton", "Badminton Player", start, levelStart, level)

    @Test
    fun firstVoteOnSetupDay() {
        val s = HabitRules.homeState(journey(start = today), listOf(today), today, GreetBy.IDENTITY)
        assertEquals(1, s.votesInLevel)
        assertEquals(1, s.progressPercent)
        assertEquals("1 vote cast", s.progressLabel)
        assertEquals(Messages.VOTE_CAST, s.message)
        assertTrue(s.canUndo)
        assertFalse(s.canVote)
    }

    @Test
    fun seventyFourVotesStillVoting() {
        val days = ((today - 74) until today).toList() // 74 votes ending yesterday
        val s = HabitRules.homeState(journey(), days, today, GreetBy.IDENTITY)
        assertEquals(74, s.totalVotes)
        assertTrue(s.canVote)
        assertFalse(s.pendingLevelComplete)
        assertEquals(99, s.progressPercent)
        assertEquals("1 to go", s.progressLabel)
        assertEquals("1 to Committed", s.homeProgressLabel)
        assertEquals(Messages.DEFAULT, s.message)
        assertEquals("Hey, Badminton Player", s.greeting)
        assertEquals("Level 1 · Habit", s.levelLabel)
        assertEquals("Level 1 · 1 to go", s.widgetProgressLabel)
        assertEquals(quoteFor(today), s.quote)
        assertEquals(WidgetSubtitle(quoteFor(today), SubtitleKind.QUOTE), s.widgetSubtitle)
        assertNull(s.completedLevel)
    }

    @Test
    fun seventyFifthVoteCompletesLevelAndBlocksPlus() {
        val days = ((today - 74)..today).toList() // 75 votes including today
        val s = HabitRules.homeState(journey(start = today - 74), days, today, GreetBy.NAME)
        assertTrue(s.pendingLevelComplete)
        assertFalse(s.canVote)
        assertTrue(s.canUndo)
        assertEquals(100, s.progressPercent)
        assertEquals("Level complete", s.progressLabel)
        assertEquals(Messages.LEVEL_COMPLETE, s.message)
        assertFalse(s.isNudge)
        assertEquals(WidgetSubtitle("Level 1 complete · 75 votes", SubtitleKind.WIN), s.widgetSubtitle)
        assertEquals("Hey, Sam", s.greeting)
        val done = s.completedLevel
        assertNotNull(done)
        assertEquals(75, done!!.votes)
        assertEquals(75, done.days)
        assertEquals(0, done.missedDays)
    }

    @Test
    fun afterUpskillTodaysVoteIsSealed() {
        val days = ((today - 74)..today).toList()
        val s = HabitRules.homeState(journey(level = 2, start = today - 74, levelStart = today + 1), days, today, GreetBy.IDENTITY)
        assertFalse(s.pendingLevelComplete)
        assertTrue(s.votedToday)
        assertFalse(s.canVote)
        assertFalse(s.canUndo)
        assertEquals(0, s.progressPercent)
        assertEquals("0 votes cast", s.progressLabel)
        assertEquals("Level 2 · 0 votes cast", s.widgetProgressLabel)
        assertEquals(Messages.VOTE_CAST, s.message)
        assertEquals("Level 2 · Committed", s.levelLabel)
    }

    @Test
    fun missedYesterdayShowsNudge() {
        val days = ((today - 10)..(today - 2)).toList()
        val s = HabitRules.homeState(journey(start = today - 10), days, today, GreetBy.IDENTITY)
        assertEquals(Messages.NEVER_MISS_TWICE, s.message)
        assertTrue(s.isNudge)
        assertEquals(WidgetSubtitle(Messages.NEVER_MISS_TWICE, SubtitleKind.NUDGE), s.widgetSubtitle)
        assertTrue(s.canVote)
    }

    @Test
    fun welcomeBackAfterTwoMissedDays() {
        val days = ((today - 10)..(today - 3)).toList()
        val s = HabitRules.homeState(journey(start = today - 10), days, today, GreetBy.IDENTITY)
        assertEquals(Messages.WELCOME_BACK, s.message)
        assertEquals(WidgetSubtitle(Messages.WELCOME_BACK, SubtitleKind.WELCOME), s.widgetSubtitle)
    }

    @Test
    fun finalLevelCompletion() {
        val days = ((today - 364)..today).toList() // 365 votes
        val s = HabitRules.homeState(journey(level = 5, start = today - 364, levelStart = today - 64), days, today, GreetBy.IDENTITY)
        assertTrue(s.pendingLevelComplete)
        assertTrue(s.isFinalLevel)
        assertEquals("Expert Badminton Player", s.completedLevel!!.title)
        assertEquals(65, s.completedLevel!!.votes)
        assertEquals("You are an Expert Badminton Player", Levels.youAre(s.completedLevel!!.title))
    }
}
