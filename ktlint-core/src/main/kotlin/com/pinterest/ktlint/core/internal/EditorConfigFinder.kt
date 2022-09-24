package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.core.internal.ThreadSafeEditorConfigCache.Companion.threadSafeEditorConfigCache
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.isDirectory
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
        val result = mutableListOf<Path>()
        val normalizedPath = path.normalize().toAbsolutePath()
        if (path.isDirectory()) {
            result += findEditorConfigsInSubDirectories(normalizedPath)
        }
        result += findEditorConfigsInParentDirectories(normalizedPath)
        return result.toList()
    }

    private fun findEditorConfigsInSubDirectories(path: Path): List<Path> {
        val result = mutableListOf<Path>()

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

        return result.toList()
    }

    private fun findEditorConfigsInParentDirectories(path: Path): List<Path> {
        // The logic to load parental ".editorconfig" files resides in the ec4j library. This library however uses a
        // cache provided by KtLint. As of this the list of parental ".editorconfig" files can be extracted from the
        // cache.
        createLoaderService().queryProperties(path.resource())
        return threadSafeEditorConfigCache.getPaths()
    }

    private fun Path?.resource() =
        Resource.Resources.ofPath(this, StandardCharsets.UTF_8)

    private fun createLoaderService() =
        ResourcePropertiesService.builder()
            .cache(threadSafeEditorConfigCache)
            .loader(org.ec4j.core.EditorConfigLoader.of(Version.CURRENT))
            .build()
}
