package com.halovoid.lncrawler.data.artifact

import com.halovoid.lncrawler.data.scheduler.RequestMetadata
import com.halovoid.lncrawler.domain.models.Chapter
import com.halovoid.lncrawler.domain.models.Novel
import com.halovoid.lncrawler.domain.models.Volume
import java.io.File

interface ArtifactGenerator {
    val format: String // which format of Artifact is to be generated

    suspend fun generate(
        novel: Novel,
        volumes: List<Volume>,
        chapters: List<Chapter>,
        metadata: RequestMetadata
    ): File // Returns the temporary file in cache
}