package com.halovoid.lncrawler.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Update the 'requests' table
            db.execSQL("ALTER TABLE requests ADD COLUMN progressSuccess INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE requests ADD COLUMN progressFailed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE requests ADD COLUMN progressCancelled INTEGER NOT NULL DEFAULT 0")

            // 2. Drop and Recreate since SQLite sometimes does not support Rename WELL
            db.execSQL("UPDATE requests SET progressSuccess = progressCurrent")
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM requests WHERE type = 'EXPORT';")
        }
    }
}