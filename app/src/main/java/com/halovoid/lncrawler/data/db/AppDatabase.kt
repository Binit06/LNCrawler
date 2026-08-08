package com.halovoid.lncrawler.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.halovoid.lncrawler.data.db.dao.ArtifactDao
import com.halovoid.lncrawler.data.db.dao.ChapterDao
import com.halovoid.lncrawler.data.db.dao.NovelDao
import com.halovoid.lncrawler.data.db.dao.RequestDao
import com.halovoid.lncrawler.data.db.dao.VolumeDao
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity
import com.halovoid.lncrawler.data.db.entities.RequestEntity
import com.halovoid.lncrawler.data.db.entities.VolumeEntity
import com.halovoid.lncrawler.data.db.entities.ArtifactEntity


/**
 * Main Room database for the application.
 * Part of the Data layer, responsible for local persistence.
 */
@Database(
    entities = [NovelEntity::class, ChapterEntity::class, VolumeEntity::class, RequestEntity::class, ArtifactEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun novelDao(): NovelDao
    abstract fun chapterDao(): ChapterDao
    abstract fun volumeDao(): VolumeDao
    abstract fun requestDao(): RequestDao
    abstract fun artifactDao(): ArtifactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 4 to 5: Add parentId to export_history table.
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the nullable parentId column
                db.execSQL("ALTER TABLE export_history ADD COLUMN parentId INTEGER DEFAULT NULL")
                // Create index for the new foreign key column
                db.execSQL("CREATE INDEX IF NOT EXISTS index_export_history_parentId ON export_history (parentId)")
            }
        }

        /**
         * Returns the singleton instance of the [AppDatabase].
         * @param context The application context.
         * @return The database instance.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lncrawler_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
