package com.halovoid.lncrawler.data.db.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportRecordEntityTest {

    @Test
    fun createRootRequest() {
        val record = ExportRecordEntity(
            novelUrl = "url",
            novelTitle = "title",
            status = "PENDING",
            destinationUri = null,
            errorLog = null,
            crawlerName = "crawler"
        )
        assertNull(record.parentId)
    }

    @Test
    fun createChildRequest() {
        val parentId = 123
        val record = ExportRecordEntity(
            parentId = parentId,
            novelUrl = "url",
            novelTitle = "title",
            status = "PENDING",
            destinationUri = null,
            errorLog = null,
            crawlerName = "crawler"
        )
        assertEquals(parentId, record.parentId)
    }
}
