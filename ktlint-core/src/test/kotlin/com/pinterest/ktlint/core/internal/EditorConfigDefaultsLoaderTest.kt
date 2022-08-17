package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import com.pinterest.ktlint.core.api.EditorConfigDefaults
import com.pinterest.ktlint.core.api.EditorConfigDefaults.Companion.emptyEditorConfigDefaults
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.EditorConfig
import org.ec4j.core.model.Glob
import org.ec4j.core.model.Property
import org.ec4j.core.model.Section
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class EditorConfigDefaultsLoaderTest {
    private val fileSystemMock = Jimfs.newFileSystem(Configuration.forCurrentPlatform())
    private val editorConfigDefaultsLoader = EditorConfigDefaultsLoader()

    @AfterEach
    internal fun tearDown() {
        fileSystemMock.close()
    }

    @Test
    fun `Given a null path then return empty editor config default`() {
        val actual = editorConfigDefaultsLoader.load(null)

        assertThat(actual).isEqualTo(emptyEditorConfigDefaults)
    }

    @Test
    fun `Given an empty path then return empty editor config default`() {
        val actual = editorConfigDefaultsLoader.load(
            fileSystemMock.normalizedPath(""),
        )

        assertThat(actual).isEqualTo(emptyEditorConfigDefaults)
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // Filename can not start or end with space
    fun `Given a blank path then return empty editor config default`() {
        val actual = editorConfigDefaultsLoader.load(
            fileSystemMock.normalizedPath("  "),
        )

        assertThat(actual).isEqualTo(emptyEditorConfigDefaults)
    }

    @Test
    fun `Given an non existing path then return empty editor config default`() {
        val actual = editorConfigDefaultsLoader.load(
            fileSystemMock.normalizedPath("/path/to/non/existing/file.kt"),
        )

        assertThat(actual).isEqualTo(emptyEditorConfigDefaults)
    }

    @ParameterizedTest(name = "Filename: {0}")
    @ValueSource(
        strings = [
            ".editorconfig",
            "some-alternative-file-name",
        ],
    )
    fun `Given an existing editor config file then load all settings from it`(
        fileName: String,
    ) {
        val existingEditorConfigFileName = "/some/path/to/existing/$fileName"
        fileSystemMock.writeEditorConfigFile(
            existingEditorConfigFileName,
            SOME_EDITOR_CONFIG.toString(),
        )

        val actual = editorConfigDefaultsLoader.load(
            fileSystemMock.normalizedPath(existingEditorConfigFileName),
        )

        assertThat(actual).isEqualTo(
            EditorConfigDefaults(SOME_EDITOR_CONFIG),
        )
    }

    @Test
    fun `Given an existing directory containing an editor config file then load all settings from it`() {
        val existingDirectory = "/some/path/to/existing/directory"
        fileSystemMock.writeEditorConfigFile(
            existingDirectory.plus("/.editorconfig"),
            SOME_EDITOR_CONFIG.toString(),
        )

        val actual = editorConfigDefaultsLoader.load(
            fileSystemMock.normalizedPath(existingDirectory),
        )

        assertThat(actual).isEqualTo(
            EditorConfigDefaults(SOME_EDITOR_CONFIG),
        )
    }

    private fun FileSystem.writeEditorConfigFile(
        filePath: String,
        content: String,
    ) {
        val path = normalizedPath(filePath)
        require(!path.isDirectory())

        Files.createDirectories(path.parent)
        Files.write(path, content.toByteArray())
    }

    private fun FileSystem.normalizedPath(path: String): Path {
        val root = rootDirectories.joinToString(separator = "/")
        return getPath("$root$path")
    }

    private companion object {
        val SOME_EDITOR_CONFIG: EditorConfig = EditorConfig
            .builder()
            .section(
                Section
                    .builder()
                    .glob(Glob("*.kt"))
                    .properties(
                        Property
                            .builder()
                            .name("some-property")
                            .value("some-property-value"),
                    ),
            )
            .build()
    }
}
