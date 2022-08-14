package com.pinterest.ktlint.core.internal

import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import org.ec4j.core.Cache
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource
import org.ec4j.core.model.EditorConfig

/**
 * In-memory [Cache] implementation that could be safely accessed from any [Thread].
 */
internal class ThreadSafeEditorConfigCache : Cache {
    private val readWriteLock = ReentrantReadWriteLock()
    private val inMemoryMap = HashMap<Resource, EditorConfig>()

    override fun get(
        editorConfigFile: Resource,
        loader: EditorConfigLoader,
    ): EditorConfig {
        readWriteLock.read {
            return inMemoryMap[editorConfigFile]
                ?: readWriteLock.write {
                    val result = loader.load(editorConfigFile)
                    inMemoryMap[editorConfigFile] = result
                    result
                }
        }
    }

    fun clear() = readWriteLock.write {
        inMemoryMap.clear()
    }
}
