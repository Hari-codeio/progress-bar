package app.becoming.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    @TypeConverter
    fun statusToString(status: JourneyStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): JourneyStatus =
        JourneyStatus.entries.firstOrNull { it.name == value } ?: JourneyStatus.ACTIVE
}

/**
 * v1 (app 1.0.0) -> v2 (app 1.1.0): identity icon columns on the journeys table. Additive only;
 * existing rows get NULL, which means "suggest an icon from the skill and title".
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `journeys` ADD COLUMN `iconKey` TEXT")
        db.execSQL("ALTER TABLE `journeys` ADD COLUMN `iconPath` TEXT")
    }
}

@Database(
    entities = [JourneyEntity::class, VoteEntity::class, LevelRecordEntity::class],
    version = 2,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BecomingDatabase : RoomDatabase() {
    abstract fun journeyDao(): JourneyDao
    abstract fun voteDao(): VoteDao
    abstract fun levelRecordDao(): LevelRecordDao

    companion object {
        // Never fall back to a destructive migration: users' votes must survive every upgrade.
        fun build(context: Context): BecomingDatabase =
            Room.databaseBuilder(context.applicationContext, BecomingDatabase::class.java, "becoming.db")
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
