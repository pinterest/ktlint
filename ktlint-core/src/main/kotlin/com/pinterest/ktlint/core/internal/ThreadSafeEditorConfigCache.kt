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

    /**
     * Gets the [editorConfigFile] from the cache. If not found, then the [editorConfigLoader] is used to retrieve the
     * '.editorconfig' properties. The result is stored in the cache.
     */
    override fun get(
        editorConfigFile: Resource,
        editorConfigLoader: EditorConfigLoader,
    ): EditorConfig {
        readWriteLock.read {
            return inMemoryMap[editorConfigFile]
                ?: readWriteLock.write {
                    val result = editorConfigLoader.load(editorConfigFile)
                    inMemoryMap[editorConfigFile] = result
                    result
                }
        }
    }

    fun clear() = readWriteLock.write {
        inMemoryMap.clear()
    }

    internal companion object {
        val threadSafeEditorConfigCache = ThreadSafeEditorConfigCache()
    }
}
