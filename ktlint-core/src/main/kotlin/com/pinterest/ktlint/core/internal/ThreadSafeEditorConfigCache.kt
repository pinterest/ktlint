package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.initKtLintKLogger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import mu.KotlinLogging
import org.ec4j.core.Cache
import org.ec4j.core.EditorConfigLoader
import org.ec4j.core.Resource
import org.ec4j.core.model.EditorConfig

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * In-memory [Cache] implementation that could be safely accessed from any [Thread].
 */
internal class ThreadSafeEditorConfigCache : Cache {
    private val readWriteLock = ReentrantReadWriteLock()
    private val inMemoryMap = HashMap<Resource, CacheValue>()

    /**
     * Gets the [resource] from the cache. If not found, then the [editorConfigLoader] is used to retrieve the
     * '.editorconfig' properties. The result is stored in the cache.
     */
    override fun get(
        resource: Resource,
        editorConfigLoader: EditorConfigLoader,
    ): EditorConfig {
        readWriteLock.read {
            val cachedEditConfig = inMemoryMap[resource]
                ?.also {
                    logger.trace { "Retrieving EditorConfig cache entry for path ${resource.path}" }
                }?.editConfig
            return cachedEditConfig
                ?: readWriteLock.write {
                    CacheValue(resource, editorConfigLoader)
                        .also { cacheValue ->
                            inMemoryMap[resource] = cacheValue
                            logger.trace { "Creating new EditorConfig cache entry for path ${resource.path}" }
                        }.editConfig
                }
        }
    }

    /**
     * Reloads an '.editorconfig' file given that it is currently loaded into the cache.
     */
    fun reloadIfExists(resource: Resource) {
        readWriteLock.read {
            inMemoryMap[resource]
                ?.let { cacheValue ->
                    readWriteLock.write {
                        cacheValue
                            .copy(editConfig = cacheValue.editorConfigLoader.load(resource))
                            .let { cacheValue -> inMemoryMap[resource] = cacheValue }
                            .also {
                                logger.trace { "Reload EditorConfig cache entry for path ${resource.path}" }
                            }
                    }
                }
        }
    }

    /**
     * Clears entire cache to free-up memory.
     */
    fun clear() = readWriteLock.write {
        inMemoryMap
            .also {
                logger.trace { "Removing ${it.size} entries from the EditorConfig cache" }
            }.clear()
    }

    private data class CacheValue(
        val editorConfigLoader: EditorConfigLoader,
        val editConfig: EditorConfig,
    ) {
        constructor(
            resource: Resource,
            editorConfigLoader: EditorConfigLoader,
        ) : this(
            editorConfigLoader = editorConfigLoader,
            editConfig = editorConfigLoader.load(resource),
        )
    }

    internal companion object {
        val threadSafeEditorConfigCache = ThreadSafeEditorConfigCache()
    }
}
