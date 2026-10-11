package app.becoming.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun statusToString(status: JourneyStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): JourneyStatus =
        JourneyStatus.entries.firstOrNull { it.name == value } ?: JourneyStatus.ACTIVE
}

@Database(
    entities = [JourneyEntity::class, VoteEntity::class, LevelRecordEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BecomingDatabase : RoomDatabase() {
    abstract fun journeyDao(): JourneyDao
    abstract fun voteDao(): VoteDao
    abstract fun levelRecordDao(): LevelRecordDao

    companion object {
        fun build(context: Context): BecomingDatabase =
            Room.databaseBuilder(context.applicationContext, BecomingDatabase::class.java, "becoming.db").build()
    }
}
