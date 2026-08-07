package com.halovoid.lncrawler.data.db.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.halovoid.lncrawler.data.db.AppDatabase
import com.halovoid.lncrawler.data.db.entities.ExportRecordEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class ExportRecordDaoTest {
    private lateinit var exportRecordDao: ExportRecordDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        exportRecordDao = db.exportRecordDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeRootAndChildRequest() = runBlocking {
        // 1. Create a root request
        val rootRecord = ExportRecordEntity(
            novelUrl = "https://novelbin.com/novel-1",
            novelTitle = "Novel 1",
            status = "PENDING",
            destinationUri = null,
            errorLog = null,
            crawlerName = "NovelBins"
        )
        val rootId = exportRecordDao.insert(rootRecord).toInt()

        // Verify root record
        val rootFromDb = exportRecordDao.getRecordByIdOnce(rootId.toLong())
        assertEquals("Novel 1", rootFromDb?.novelTitle)
        assertNull(rootFromDb?.parentId)

        // 2. Create a child request linked to the root
        val childRecord = ExportRecordEntity(
            parentId = rootId,
            novelUrl = "https://novelbin.com/novel-1/chapter-1",
            novelTitle = "Chapter 1",
            status = "PENDING",
            destinationUri = null,
            errorLog = null,
            crawlerName = "NovelBins"
        )
        val childId = exportRecordDao.insert(childRecord).toInt()

        // Verify child record
        val childFromDb = exportRecordDao.getRecordByIdOnce(childId.toLong())
        assertEquals("Chapter 1", childFromDb?.novelTitle)
        assertEquals(rootId, childFromDb?.parentId)
    }
}
