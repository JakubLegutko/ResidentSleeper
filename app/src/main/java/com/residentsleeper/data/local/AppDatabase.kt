package com.residentsleeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.residentsleeper.data.model.AppNotification
import com.residentsleeper.data.model.BabyEvent
import com.residentsleeper.data.model.BabyProfile

@Database(
    entities = [BabyEvent::class, BabyProfile::class, AppNotification::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun babyEventDao(): BabyEventDao
    abstract fun babyProfileDao(): BabyProfileDao
    abstract fun appNotificationDao(): AppNotificationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE baby_profile ADD COLUMN dayStartHour INTEGER NOT NULL DEFAULT 7")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE baby_profile ADD COLUMN maxRecommendedNightFeeds INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE baby_profile ADD COLUMN notifyForOptionalNightFeeds INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE baby_profile ADD COLUMN gender TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE baby_profile ADD COLUMN use12HourFormat INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notifications` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_timestamp` ON `notifications` (`timestamp`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE baby_profile ADD COLUMN customFeedingIntervalMinutes INTEGER DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "residentsleeper.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
