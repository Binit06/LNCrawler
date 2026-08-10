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
}