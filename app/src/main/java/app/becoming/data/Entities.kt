package app.becoming.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import app.becoming.domain.JourneyInput
import app.becoming.domain.LevelSummary

enum class JourneyStatus { ACTIVE, COMPLETED }

@Entity(tableName = "journeys")
data class JourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val skill: String,
    val identityTitle: String,
    val startEpochDay: Long,
    /** First day votes can count toward [unlockedLevel] (journey start for level 1). */
    val levelStartEpochDay: Long,
    val unlockedLevel: Int = 1,
    val status: JourneyStatus = JourneyStatus.ACTIVE,
    val completedEpochDay: Long? = null,
) {
    fun toInput() = JourneyInput(id, name, skill, identityTitle, startEpochDay, levelStartEpochDay, unlockedLevel)
}

/** One vote per journey per local calendar day. */
@Entity(tableName = "votes", primaryKeys = ["journeyId", "epochDay"])
data class VoteEntity(
    val journeyId: Long,
    val epochDay: Long,
)

@Entity(tableName = "level_records", indices = [Index("journeyId")])
data class LevelRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journeyId: Long,
    val level: Int,
    val title: String,
    val startEpochDay: Long,
    val endEpochDay: Long,
    val votes: Int,
    val missedDays: Int,
)

fun LevelSummary.toEntity(journeyId: Long) = LevelRecordEntity(
    journeyId = journeyId,
    level = level,
    title = title,
    startEpochDay = startEpochDay,
    endEpochDay = endEpochDay,
    votes = votes,
    missedDays = missedDays,
)
