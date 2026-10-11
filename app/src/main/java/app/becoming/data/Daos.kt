package app.becoming.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JourneyDao {
    @Query("SELECT * FROM journeys WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<JourneyEntity?>

    @Query("SELECT * FROM journeys WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): JourneyEntity?

    @Query("SELECT * FROM journeys ORDER BY id DESC")
    fun observeAll(): Flow<List<JourneyEntity>>

    @Insert
    suspend fun insert(journey: JourneyEntity): Long

    @Update
    suspend fun update(journey: JourneyEntity)
}

@Dao
interface VoteDao {
    @Query("SELECT epochDay FROM votes WHERE journeyId = :journeyId ORDER BY epochDay")
    fun observeDays(journeyId: Long): Flow<List<Long>>

    @Query("SELECT epochDay FROM votes WHERE journeyId = :journeyId ORDER BY epochDay")
    suspend fun days(journeyId: Long): List<Long>

    @Query("SELECT * FROM votes ORDER BY epochDay")
    fun observeAll(): Flow<List<VoteEntity>>

    /** Returns the new rowId, or -1 when today's vote already exists. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vote: VoteEntity): Long

    @Query("DELETE FROM votes WHERE journeyId = :journeyId AND epochDay = :epochDay")
    suspend fun delete(journeyId: Long, epochDay: Long): Int
}

@Dao
interface LevelRecordDao {
    @Insert
    suspend fun insert(record: LevelRecordEntity): Long

    @Query("SELECT * FROM level_records ORDER BY journeyId, level")
    fun observeAll(): Flow<List<LevelRecordEntity>>

    /** Rewrites a saved badge title (used when the identity title is edited). Dates and votes stay. */
    @Query("UPDATE level_records SET title = :title WHERE journeyId = :journeyId AND level = :level")
    suspend fun updateTitle(journeyId: Long, level: Int, title: String): Int
}
