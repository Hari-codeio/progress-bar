package app.becoming.data

import androidx.room.withTransaction
import app.becoming.domain.GreetBy
import app.becoming.domain.HabitRules
import app.becoming.domain.HomeState
import app.becoming.domain.Levels
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** A journey with everything the Legacy screen needs. */
data class JourneyHistory(
    val journey: JourneyEntity,
    val records: List<LevelRecordEntity>,
    val voteDays: Set<Long>,
)

class HabitRepository(
    private val db: BecomingDatabase,
    private val settings: SettingsStore,
) {
    private val journeys = db.journeyDao()
    private val votes = db.voteDao()
    private val levels = db.levelRecordDao()

    /** Local "today" as an epoch day. Re-read on resume, on widget render and at midnight. */
    private val today = MutableStateFlow(currentEpochDay())

    fun refreshToday() {
        today.value = currentEpochDay()
    }

    val greetBy: Flow<GreetBy> = settings.greetBy

    /** Null when there is no active journey (the app shows Setup). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val homeState: Flow<HomeState?> = journeys.observeActive()
        .flatMapLatest { journey ->
            if (journey == null) {
                flowOf<HomeState?>(null)
            } else {
                combine(votes.observeDays(journey.id), today, settings.greetBy) { days, day, greet ->
                    HabitRules.homeState(journey.toInput(), days, day, greet)
                }
            }
        }
        .distinctUntilChanged()

    /** Creates the journey and (endowed progress) casts today's first vote in the same transaction. */
    suspend fun startJourney(name: String, skill: String, identityTitle: String, castFirstVote: Boolean = true): Long =
        db.withTransaction {
            refreshToday()
            val day = today.value
            // Only one active journey at a time.
            journeys.getActive()?.let { journeys.update(it.copy(status = JourneyStatus.COMPLETED, completedEpochDay = day)) }
            val id = journeys.insert(
                JourneyEntity(
                    name = name.trim(),
                    skill = skill.trim(),
                    identityTitle = identityTitle.trim(),
                    startEpochDay = day,
                    levelStartEpochDay = day,
                ),
            )
            if (castFirstVote) votes.insert(VoteEntity(journeyId = id, epochDay = day))
            id
        }

    /** + : inserts today's vote if absent and the current level is not waiting for a choice. */
    suspend fun voteToday(): Boolean = db.withTransaction {
        refreshToday()
        val day = today.value
        val journey = journeys.getActive() ?: return@withTransaction false
        val days = votes.days(journey.id)
        if (day in days || Levels.isComplete(days.size, journey.unlockedLevel)) return@withTransaction false
        votes.insert(VoteEntity(journey.id, day)) != -1L
    }

    /** − : removes today's vote only, and only if it belongs to the current level. */
    suspend fun undoToday(): Boolean = db.withTransaction {
        refreshToday()
        val day = today.value
        val journey = journeys.getActive() ?: return@withTransaction false
        val days = votes.days(journey.id)
        if (day !in days || days.size <= Levels.prevThreshold(journey.unlockedLevel)) return@withTransaction false
        votes.delete(journey.id, day) > 0
    }

    /** Saves the finished level and opens the next one with a fresh 0% bar. */
    suspend fun upskill(): Boolean = db.withTransaction {
        refreshToday()
        val day = today.value
        val journey = journeys.getActive() ?: return@withTransaction false
        val days = votes.days(journey.id)
        val level = journey.unlockedLevel
        if (level >= Levels.MAX_LEVEL || !Levels.isComplete(days.size, level)) return@withTransaction false
        val summary = HabitRules.levelSummary(level, journey.identityTitle, journey.levelStartEpochDay, days)
        levels.insert(summary.toEntity(journey.id))
        journeys.update(
            journey.copy(
                unlockedLevel = level + 1,
                // Today's vote (if any) belongs to the finished level, so the new level starts tomorrow.
                levelStartEpochDay = maxOf(day, summary.endEpochDay + 1),
            ),
        )
        true
    }

    /** Saves the finished level and closes the journey; the app then shows Setup. */
    suspend fun completeJourney(): Boolean = db.withTransaction {
        refreshToday()
        val day = today.value
        val journey = journeys.getActive() ?: return@withTransaction false
        val days = votes.days(journey.id)
        if (!Levels.isComplete(days.size, journey.unlockedLevel)) return@withTransaction false
        val summary = HabitRules.levelSummary(journey.unlockedLevel, journey.identityTitle, journey.levelStartEpochDay, days)
        levels.insert(summary.toEntity(journey.id))
        journeys.update(journey.copy(status = JourneyStatus.COMPLETED, completedEpochDay = day))
        true
    }

    suspend fun setGreetBy(value: GreetBy) = settings.setGreetBy(value)

    /** All journeys, newest first, live. */
    fun observeHistory(): Flow<List<JourneyHistory>> =
        combine(journeys.observeAll(), levels.observeAll(), votes.observeAll()) { all, records, allVotes ->
            val recordsByJourney = records.groupBy { it.journeyId }
            val daysByJourney = allVotes.groupBy({ it.journeyId }, { it.epochDay })
            all.map { j ->
                JourneyHistory(j, recordsByJourney[j.id].orEmpty(), daysByJourney[j.id].orEmpty().toSet())
            }
        }

    suspend fun history(): List<JourneyHistory> = observeHistory().first()

    private fun currentEpochDay(): Long = LocalDate.now().toEpochDay()
}
