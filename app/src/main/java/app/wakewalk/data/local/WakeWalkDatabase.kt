package app.wakewalk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import app.wakewalk.data.local.converter.Converters
import app.wakewalk.data.local.dao.ActiveSessionDao
import app.wakewalk.data.local.dao.AlarmDao
import app.wakewalk.data.local.dao.AlarmHistoryDao
import app.wakewalk.data.local.entity.ActiveSessionEntity
import app.wakewalk.data.local.entity.AlarmEntity
import app.wakewalk.data.local.entity.AlarmHistoryEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlarmEntity::class,
        ActiveSessionEntity::class,
        AlarmHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WakeWalkDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun alarmHistoryDao(): AlarmHistoryDao

    /**
     * Atomically records the completed/dismissed alarm history record and clears
     * the singleton active session, ensuring zero session leaks or duplicate records.
     */
    suspend fun completeActiveSession(history: AlarmHistoryEntity) {
        withTransaction {
            alarmHistoryDao().insertHistory(history)
            activeSessionDao().clearActiveSession()
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN qrCodePayload TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE alarms ADD COLUMN qrCodeLabel TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN lowerVolumeWhileWalking INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}
