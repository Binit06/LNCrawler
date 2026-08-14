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

    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // No changes between 6 and 7
        }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DELETE FROM requests WHERE type = 'EXPORT';")
        }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Recreate requests table to ensure Foreign Key is correctly set on novelUrl
            // while preserving parentNovel column.
            
            // 1. Create new table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `requests_new` (
                    `id` TEXT NOT NULL, 
                    `name` TEXT NOT NULL, 
                    `parentNovel` TEXT, 
                    `dependsOn` TEXT, 
                    `url` TEXT, 
                    `novelUrl` TEXT NOT NULL, 
                    `priority` INTEGER NOT NULL, 
                    `type` TEXT NOT NULL, 
                    `createdAt` INTEGER NOT NULL, 
                    `updatedAt` INTEGER NOT NULL, 
                    `completedAt` INTEGER, 
                    `progressTotal` INTEGER NOT NULL, 
                    `progressSuccess` INTEGER NOT NULL, 
                    `progressFailed` INTEGER NOT NULL, 
                    `progressCancelled` INTEGER NOT NULL, 
                    `status` TEXT NOT NULL, 
                    `metadata` TEXT, 
                    `error` TEXT, 
                    PRIMARY KEY(`id`), 
                    FOREIGN KEY(`novelUrl`) REFERENCES `novels`(`url`) ON UPDATE NO ACTION ON DELETE CASCADE , 
                    FOREIGN KEY(`dependsOn`) REFERENCES `requests`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL 
                )
            """.trimIndent())

            // 2. Copy data
            db.execSQL("""
                INSERT INTO requests_new (
                    id, name, parentNovel, dependsOn, url, novelUrl, priority, type, 
                    createdAt, updatedAt, completedAt, progressTotal, progressSuccess, 
                    progressFailed, progressCancelled, status, metadata, error
                )
                SELECT 
                    id, name, parentNovel, dependsOn, url, novelUrl, priority, type, 
                    createdAt, updatedAt, completedAt, progressTotal, progressSuccess, 
                    progressFailed, progressCancelled, status, metadata, error
                FROM requests
            """.trimIndent())

            // 3. Drop old table
            db.execSQL("DROP TABLE requests")

            // 4. Rename new table
            db.execSQL("ALTER TABLE requests_new RENAME TO requests")

            // 5. Recreate indices
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_parentNovel` ON `requests` (`parentNovel`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_dependsOn` ON `requests` (`dependsOn`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_status` ON `requests` (`status`)")
        }
    }
}
