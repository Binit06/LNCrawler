package com.halovoid.lncrawler.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.halovoid.lncrawler.data.db.dao.NovelDao
import com.halovoid.lncrawler.data.db.entities.ChapterEntity
import com.halovoid.lncrawler.data.db.entities.NovelEntity

/**
 * Main Room database for the application.
 * Part of the Data layer, responsible for local persistence.
 */
@Database(entities = [NovelEntity::class, ChapterEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    /** Provides access to [NovelDao]. */
    abstract fun novelDao(): NovelDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
