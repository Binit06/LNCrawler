package com.halovoid.lncrawler.data.export

import com.halovoid.lncrawler.data.crawler.core.Crawler
import com.halovoid.lncrawler.domain.models.Novel
import kotlinx.coroutines.*
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Duration.Companion.seconds

/**
 * Enhanced EPUB exporter logic in the Data layer.
 * 
 * This class handles the entire process of generating an EPUB:
 * 1. Fetching chapter content from the source using the provided [Crawler].
 * 2. Respecting the crawler's rate-limiting and batching settings.
 * 3. Packaging everything into a standard EPUB structure.
 */
class EpubExporter {

    private val STYLE_FILE_NAME = "style.css"
    private val COVER_IMAGE_NAME = "cover.jpg"
    private val PROJECT_URL = "https://github.com/halovoid/LNCrawler"

    /**
     * Executes the full export process: fetches chapters via [crawler] and writes the EPUB to [outputStream].
     */
    suspend fun export(
        novel: Novel,
        crawler: Crawler,
        outputStream: OutputStream,
        onProgress: (Int, Int, String) -> Unit
    ) = coroutineScope {
        val chaptersWithContent = mutableListOf<Pair<String, String>>()
        val rateLimit = crawler.requestRateLimit
        val batchSize = crawler.chapterBatchSize ?: 1

        val totalChapters = novel.chapters.size
        val chapterChunks = novel.chapters.chunked(batchSize)

        chapterChunks.forEachIndexed { chunkIndex, chunk ->
            ensureActive()
            val deferreds = chunk.map { chapter ->
                async(Dispatchers.IO) {
                    var content = crawler.getChapterContent(chapter.url)
                    if (content.isEmpty()) {
                        content = "<p><i>[Error: Failed to fetch chapter content]</i></p>"
                    }
                    chapter.title to content
                }
            }

            chaptersWithContent.addAll(deferreds.awaitAll())
            onProgress(chaptersWithContent.size, totalChapters, "Fetching chapters...")

            if (chunkIndex != chapterChunks.lastIndex) {
                delay(rateLimit.seconds)
            }
        }

        onProgress(totalChapters, totalChapters, "Generating EPUB structure...")

        withContext(Dispatchers.IO) {
            ZipOutputStream(outputStream).use { zip ->
                writeMimetype(zip)
                writeContainerXml(zip)
                writeStyles(zip)

                val coverBytes = novel.coverUrl?.let { crawler.downloadImage(it) }
                if (coverBytes != null) {
                    writeZipEntry(zip, "OEBPS/$COVER_IMAGE_NAME", coverBytes)
                    writeFrontPage(zip)
                }

                writeIntroPage(zip, novel)

                chaptersWithContent.forEachIndexed { index, (title, content) ->
                    writeChapterPage(zip, index + 1, title, content)
                }

                writeTocNcx(zip, novel, chaptersWithContent)
                writeContentOpf(zip, novel, chaptersWithContent, hasCover = coverBytes != null)
            }
        }
    }

    private fun writeMimetype(zip: ZipOutputStream) {
        val mimeBytes = "application/epub+zip".toByteArray(Charsets.UTF_8)
        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimeBytes.size.toLong()
            compressedSize = mimeBytes.size.toLong()
            crc = CRC32().apply { update(mimeBytes) }.value
        }
        zip.putNextEntry(entry)
        zip.write(mimeBytes)
        zip.closeEntry()
    }

    private fun writeContainerXml(zip: ZipOutputStream) {
        val content = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
            |    <rootfiles>
            |        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
            |    </rootfiles>
            |</container>
        """.trimMargin()
        writeZipEntry(zip, "META-INF/container.xml", content.toByteArray(Charsets.UTF_8))
    }

    private fun writeStyles(zip: ZipOutputStream) {
        val content = """
            |body { font-family: serif; line-height: 1.5; margin: 5%; }
            |h1, h2, h3 { text-align: center; }
            |.synopsis { font-style: italic; margin-top: 2em; border-top: 1px dotted #ccc; padding-top: 1em; }
            |.footer { margin-top: 2em; font-size: 0.8em; color: #666; text-align: center; border-top: 1px solid #eee; padding-top: 1em; }
            |#cover img { max-width: 100%; height: auto; display: block; margin: 0 auto; }
            |p { margin-bottom: 1em; text-indent: 0; }
        """.trimMargin()
        writeZipEntry(zip, "OEBPS/$STYLE_FILE_NAME", content.toByteArray(Charsets.UTF_8))
    }

    private fun writeFrontPage(zip: ZipOutputStream) {
        val content = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<html xmlns="http://www.w3.org/1999/xhtml">
            |<head>
            |    <title>Front Page</title>
            |    <link rel="stylesheet" type="text/css" href="$STYLE_FILE_NAME"/>
            |</head>
            |<body>
            |    <div id="cover">
            |        <img src="$COVER_IMAGE_NAME" alt="cover" />
            |    </div>
            |</body>
            |</html>
        """.trimMargin()
        writeZipEntry(zip, "OEBPS/front.xhtml", content.toByteArray(Charsets.UTF_8))
    }

    private fun writeIntroPage(zip: ZipOutputStream, novel: Novel) {
        val content = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<html xmlns="http://www.w3.org/1999/xhtml">
            |<head>
            |    <title>Intro Page</title>
            |    <link rel="stylesheet" type="text/css" href="$STYLE_FILE_NAME"/>
            |</head>
            |<body>
            |    <div id="intro">
            |        <h1>${escapeXml(novel.title)}</h1>
            |        <h3>${escapeXml(novel.author ?: "Unknown Author")}</h3>
            |        <div class="synopsis">
            |            ${novel.description ?: "No synopsis available."}
            |        </div>
            |        <div class="footer">
            |            <b>Source:</b> <a href="${novel.url}">${novel.url}</a>
            |            <br/>
            |            <i>Generated by <b><a href="$PROJECT_URL">LNCrawler</a></b></i>
            |        </div>
            |    </div>
            |</body>
            |</html>
        """.trimMargin()
        writeZipEntry(zip, "OEBPS/intro.xhtml", content.toByteArray(Charsets.UTF_8))
    }

    private fun writeChapterPage(zip: ZipOutputStream, index: Int, title: String, content: String) {
        val xhtml = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<html xmlns="http://www.w3.org/1999/xhtml">
            |<head>
            |    <title>${escapeXml(title)}</title>
            |    <link rel="stylesheet" type="text/css" href="$STYLE_FILE_NAME"/>
            |</head>
            |<body>
            |    <div id="chapter">
            |        <h4 style="opacity: 0.5">#$index</h4>
            |        <h1>${escapeXml(title)}</h1>
            |        $content
            |    </div>
            |</body>
            |</html>
        """.trimMargin()
        writeZipEntry(zip, "OEBPS/chapter_$index.xhtml", xhtml.toByteArray(Charsets.UTF_8))
    }

    private fun writeTocNcx(zip: ZipOutputStream, novel: Novel, chapters: List<Pair<String, String>>) {
        val navPoints = StringBuilder()
        navPoints.append("""
            |<navPoint id="intro" playOrder="1">
            |    <navLabel><text>Introduction</text></navLabel>
            |    <content src="intro.xhtml"/>
            </navPoint>
        """.trimMargin())
        
        chapters.forEachIndexed { index, (title, _) ->
            navPoints.append("""
                |<navPoint id="chapter_${index + 1}" playOrder="${index + 2}">
                |    <navLabel><text>${escapeXml(title)}</text></navLabel>
                |    <content src="chapter_${index + 1}.xhtml"/>
                |</navPoint>
            """.trimMargin())
        }

        val content = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
            |    <head>
            |        <meta name="dtb:uid" content="${escapeXml(novel.url)}"/>
            |        <meta name="dtb:depth" content="1"/>
            |    </head>
            |    <docTitle><text>${escapeXml(novel.title)}</text></docTitle>
            |    <navMap>
            |        $navPoints
            |    </navMap>
            |</ncx>
        """.trimMargin()
        writeZipEntry(zip, "OEBPS/toc.ncx", content.toByteArray(Charsets.UTF_8))
    }

    private fun writeContentOpf(zip: ZipOutputStream, novel: Novel, chapters: List<Pair<String, String>>, hasCover: Boolean) {
        val manifest = StringBuilder()
        val spine = StringBuilder()

        manifest.append("""<item id="style" href="$STYLE_FILE_NAME" media-type="text/css"/>""").append("\n")
        if (hasCover) {
            manifest.append("""<item id="cover-image" href="$COVER_IMAGE_NAME" media-type="image/jpeg"/>""").append("\n")
            manifest.append("""<item id="front" href="front.xhtml" media-type="application/xhtml+xml"/>""").append("\n")
            spine.append("""<itemref idref="front"/>""").append("\n")
        }
        manifest.append("""<item id="intro" href="intro.xhtml" media-type="application/xhtml+xml"/>""").append("\n")
        spine.append("""<itemref idref="intro"/>""").append("\n")

        chapters.forEachIndexed { index, _ ->
            manifest.append("""<item id="chapter_${index + 1}" href="chapter_${index + 1}.xhtml" media-type="application/xhtml+xml"/>""").append("\n")
            spine.append("""<itemref idref="chapter_${index + 1}"/>""").append("\n")
        }

        val content = """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
            |    <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
            |        <dc:title>${escapeXml(novel.title)}</dc:title>
            |        <dc:creator opf:role="aut">${escapeXml(novel.author ?: "Unknown Author")}</dc:creator>
            |        <dc:language>en</dc:language>
            |        <dc:identifier id="BookID" opf:scheme="URI">${escapeXml(novel.url)}</dc:identifier>
            |        <dc:description>${escapeXml(novel.description ?: "")}</dc:description>
            |        ${if (hasCover) "<meta name=\"cover\" content=\"cover-image\"/>" else ""}
            |    </metadata>
            |    <manifest>
            |        <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
            |        $manifest
            |    </manifest>
            |    <spine toc="ncx">
            |        $spine
            |   </spine>
            |</package>
        """.trimMargin()
        writeZipEntry(zip, "OEBPS/content.opf", content.toByteArray(Charsets.UTF_8))
    }

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: ByteArray) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content)
        zip.closeEntry()
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
