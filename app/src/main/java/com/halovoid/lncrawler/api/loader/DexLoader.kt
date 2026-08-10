package com.halovoid.lncrawler.api.loader

import android.content.Context
import dalvik.system.DexClassLoader
import java.io.File
import java.io.InputStream

class DexLoader(
    private val context: Context
) {
    fun load(
        dexFile: File,
        className: String
    ): Class<*> {
        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            null,
            null,
            context.classLoader
        )

        return classLoader.loadClass(className)
    }

    // TODO: This Function will later be updated to download the dex file and save it
    fun copyDexFromExternalPath(externalPath: String): File {
        val sourceFile = File(externalPath)
        if (!sourceFile.exists()) throw Exception("Source file not found at $externalPath")

        val dexDir = File(context.filesDir, "dex_cache")
        dexDir.mkdirs()

        val targetFile = File(dexDir, "test.dex")

        if (targetFile.exists()) {
            targetFile.setWritable(true)
            targetFile.delete()
        }

        sourceFile.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        targetFile.setReadOnly()

        return targetFile
    }

    fun copyDexFromInputStream(stream: InputStream): File {
        val dexDir = File(context.filesDir, "dex_cache")
        dexDir.mkdirs()

        val targetFile = File(dexDir, "test.dex")

        if (targetFile.exists()) {
            targetFile.setWritable(true)
            targetFile.delete()
        }

        stream.use { input ->
            targetFile.outputStream().use { output ->
                stream.copyTo(output)
            }
        }

        targetFile.setReadOnly()
        return targetFile
    }
}