package com.halovoid.lncrawler.data.export

import com.halovoid.lncrawler.domain.models.Novel
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exporter class responsible for generating EPUB files in the Data layer.
 * Uses [ZipOutputStream] to package HTML and XML content according to the EPUB 2.0 specification.
 */
class EpubExporter {
    /**
     * Exports a novel to an EPUB format.
     * 
     * @param novel The [Novel] metadata to include.
     * @param chapters List of chapter titles and their sanitized HTML content.
     * @param outputStream The stream to write the generated EPUB data to (typically SAF-based).
     */
    fun export(novel: Novel, chapters: List<Pair<String, String>>, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zip ->
            // 1. Mimetype - MUST be first and uncompressed
            val mimeBytes = "application/epub+zip".toByteArray(Charsets.UTF_8)
            val mimeEntry = ZipEntry("mimetype").apply {
                method = ZipEntry.STORED
                size = mimeBytes.size.toLong()
                compressedSize = mimeBytes.size.toLong()
                crc = CRC32().apply { update(mimeBytes) }.value
            }
            zip.putNextEntry(mimeEntry)
            zip.write(mimeBytes)
            zip.closeEntry()

            // 2. Container.xml
            writeZipEntry(zip, "META-INF/container.xml", """
                <?xml version="1.0" encoding="UTF-8"?>
                <container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                    <rootfiles>
                        <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
                    </rootfiles>
                </container>
            """.trimIndent())

            // 3. Intro page (Synopsis)
            addIntroPage(zip, novel)

            // 4. Chapters
            chapters.forEachIndexed { index, (title, content) ->
                addChapterPage(zip, index + 1, title, content)
            }

            // 5. TOC.ncx
            addTocNcx(zip, novel, chapters)

            // 6. Styles
            writeZipEntry(zip, "OEBPS/style.css", """
                body { font-family: serif; line-height: 1.5; margin: 5%; }
                h1, h2 { text-align: center; }
                .synopsis { font-style: italic; margin-top: 2em; }
                p { margin-bottom: 1em; text-indent: 0; }
            """.trimIndent())

            // 7. Content.opf
            addContentOpf(zip, novel, chapters)
        }
    }

    /** Helper to write a string content to a new zip entry. */
    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    /** Adds the intro/synopsis page to the EPUB. */
    private fun addIntroPage(zip: ZipOutputStream, novel: Novel) {
        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <title>${escapeXml(novel.title)}</title>
                <link rel="stylesheet" type="text/css" href="style.css"/>
            </head>
            <body>
                <h1>${escapeXml(novel.title)}</h1>
                <p><b>Author:</b> ${escapeXml(novel.author ?: "Unknown")}</p>
                <div class="synopsis">
                    <h2>Synopsis</h2>
                    <p>${escapeXml(novel.description ?: "No description available.")}</p>
                </div>
            </body>
            </html>
        """.trimIndent()
        writeZipEntry(zip, "OEBPS/intro.xhtml", content)
    }

    /** Adds a single chapter's HTML page to the EPUB. */
    private fun addChapterPage(zip: ZipOutputStream, index: Int, title: String, content: String) {
        val xhtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <title>${escapeXml(title)}</title>
                <link rel="stylesheet" type="text/css" href="style.css"/>
            </head>
            <body>
                <h2>${escapeXml(title)}</h2>
                $content
            </body>
            </html>
        """.trimIndent()
        writeZipEntry(zip, "OEBPS/chapter_$index.xhtml", xhtml)
    }

    /** Generates the Table of Contents (ncx) file. */
    private fun addTocNcx(zip: ZipOutputStream, novel: Novel, chapters: List<Pair<String, String>>) {
        val navPoints = StringBuilder()
        navPoints.append("""
            <navPoint id="intro" playOrder="1">
                <navLabel><text>Introduction</text></navLabel>
                <content src="intro.xhtml"/>
            </navPoint>
        """.trimIndent())
        
        chapters.forEachIndexed { index, (title, _) ->
            navPoints.append("""
                <navPoint id="chapter_${index + 1}" playOrder="${index + 2}">
                    <navLabel><text>${escapeXml(title)}</text></navLabel>
                    <content src="chapter_${index + 1}.xhtml"/>
                </navPoint>
            """.trimIndent())
        }

        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1">
                <head>
                    <meta name="dtb:uid" content="${escapeXml(novel.url)}"/>
                    <meta name="dtb:depth" content="1"/>
                    <meta name="dtb:totalPageCount" content="0"/>
                    <meta name="dtb:maxPageNumber" content="0"/>
                </head>
                <docTitle><text>${escapeXml(novel.title)}</text></docTitle>
                <navMap>
                    $navPoints
                </navMap>
            </ncx>
        """.trimIndent()
        writeZipEntry(zip, "OEBPS/toc.ncx", content)
    }

    /** Generates the content.opf manifest file. */
    private fun addContentOpf(zip: ZipOutputStream, novel: Novel, chapters: List<Pair<String, String>>) {
        val manifest = StringBuilder()
        val spine = StringBuilder()

        manifest.append("""<item id="style" href="style.css" media-type="text/css"/>""").append("\n")
        manifest.append("""<item id="intro" href="intro.xhtml" media-type="application/xhtml+xml"/>""").append("\n")
        spine.append("""<itemref idref="intro"/>""").append("\n")

        chapters.forEachIndexed { index, _ ->
            manifest.append("""<item id="chapter_${index + 1}" href="chapter_${index + 1}.xhtml" media-type="application/xhtml+xml"/>""").append("\n")
            spine.append("""<itemref idref="chapter_${index + 1}"/>""").append("\n")
        }

        val content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <package xmlns="http://www.idpf.org/2007/opf" unique-identifier="BookID" version="2.0">
                <metadata xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:opf="http://www.idpf.org/2007/opf">
                    <dc:title>${escapeXml(novel.title)}</dc:title>
                    <dc:creator opf:role="aut">${escapeXml(novel.author ?: "Unknown")}</dc:creator>
                    <dc:language>en</dc:language>
                    <dc:identifier id="BookID" opf:scheme="URI">${escapeXml(novel.url)}</dc:identifier>
                    <dc:description>${escapeXml(novel.description ?: "")}</dc:description>
                </metadata>
                <manifest>
                    <item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/>
                    $manifest
                </manifest>
                <spine toc="ncx">
                    $spine
                </spine>
            </package>
        """.trimIndent()
        writeZipEntry(zip, "OEBPS/content.opf", content)
    }

    /** Escapes special characters for XML compliance. */
    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
