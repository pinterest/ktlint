package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.initKtLintKLogger
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.io.path.isDirectory
import kotlin.system.measureTimeMillis
import mu.KotlinLogging
import org.ec4j.core.Resource
import org.ec4j.core.ResourcePropertiesService
import org.ec4j.core.model.Version
import org.jetbrains.kotlin.konan.file.File

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

internal class EditorConfigFinder {
    /**
     * Finds all relevant ".editorconfig" files for the given path.
     */
    fun findEditorConfigs(path: Path): List<Path> {
        readWriteLock.read {
            val cacheValue = inMemoryMap[path]
                ?.also {
                    logger.info { "Retrieving EditorConfig cache entry for path $path" }
                }
            return cacheValue
                ?: readWriteLock.write {
                    cacheEditorConfigs(path)
                        .also { cacheValue ->
                            logger.info { "Creating cache entry for path $path with value $cacheValue" }
                        }
                }
        }
    }

    private fun cacheEditorConfigs(path: Path): List<Path> {
        val result = mutableListOf<Path>()
        val normalizedPath = path.normalize().toAbsolutePath()
        if (path.isDirectory()) {
            result += findEditorConfigsInSubDirectories(normalizedPath)
        }
        result += findEditorConfigsInParentDirectories(normalizedPath)

        val editorConfigPaths =
            result
                .map {
                    // Resolve against original path as the drive letter seems to get lost on WindowsOs
                    path.resolve(it)
                }.toList()
        inMemoryMap[path] = editorConfigPaths

        return editorConfigPaths
    }

    private fun findEditorConfigsInSubDirectories(path: Path): List<Path> {
        val result = mutableListOf<Path>()
        var visitedDirectoryCount = 0

        measureTimeMillis {
            Files.walkFileTree(
                path,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(
                        filePath: Path,
                        fileAttrs: BasicFileAttributes,
                    ): FileVisitResult {
                        if (filePath.File().name == ".editorconfig") {
                            logger.trace { "- File: $filePath: add to list of accessed files" }
                            result.add(filePath)
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun preVisitDirectory(
                        dirPath: Path,
                        dirAttr: BasicFileAttributes,
                    ): FileVisitResult {
                        visitedDirectoryCount++
                        return if (Files.isHidden(dirPath)) {
                            logger.trace { "- Dir: $dirPath: Ignore" }
                            FileVisitResult.SKIP_SUBTREE
                        } else {
                            logger.trace { "- Dir: $dirPath: Traverse" }
                            FileVisitResult.CONTINUE
                        }
                    }
                },
            )
        }.also { duration ->
            // TODO: Remove (or reduce loglevel to debug/trace) before release 0.48
            logger.info {
                "Scanning file system to find all '.editorconfig' files in directory '$path' scanned $visitedDirectoryCount directories in $duration ms"
            }
        }

        return result.toList()
    }

    private fun findEditorConfigsInParentDirectories(path: Path): List<Path> {
        // The logic to load parental ".editorconfig" files resides in the ec4j library. This library however uses a
        // cache provided by KtLint. As of this the list of parental ".editorconfig" files can be extracted from the
        // cache.
        createLoaderService().queryProperties(path.resource())
        return editorConfigCache.getPaths()
    }

    private fun Path?.resource() =
        Resource.Resources.ofPath(this, StandardCharsets.UTF_8)

    private fun createLoaderService() =
        ResourcePropertiesService.builder()
            .cache(editorConfigCache)
            .loader(org.ec4j.core.EditorConfigLoader.of(Version.CURRENT))
            .build()

    private companion object {
        // Do not reuse the generic threadSafeEditorConfigCache to prevent that results are incorrect due to other
        // calls to KtLint that result in changing the cache
        val editorConfigCache = ThreadSafeEditorConfigCache()

        private val readWriteLock = ReentrantReadWriteLock()
        private val inMemoryMap = HashMap<Path, List<Path>>()
    }
}
