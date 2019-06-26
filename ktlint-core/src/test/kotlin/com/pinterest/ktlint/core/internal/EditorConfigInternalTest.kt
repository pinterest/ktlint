package com.pinterest.ktlint.core.internal

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs
import java.nio.file.Files
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class EditorConfigInternalTest {

    @Test
    fun testParentDirectoryFallback() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        Files.createDirectories(fs.getPath("/projects/project-1/project-1-subdirectory"))
        for (
            cfg in arrayOf(
                """
                [*]
                indent_size = 2
                """,
                """
                root = true
                [*]
                indent_size = 2
                """,
                """
                [*]
                indent_size = 4
                [*.{kt,kts}]
                indent_size = 2
                """,
                """
                [*.{kt,kts}]
                indent_size = 4
                [*]
                indent_size = 2
                """
            )
        ) {
            Files.write(fs.getPath("/projects/project-1/.editorconfig"), cfg.trimIndent().toByteArray())
            val editorConfig = EditorConfigInternal.of(fs.getPath("/projects/project-1/project-1-subdirectory"))
            assertThat(editorConfig?.parent).isNull()
            assertThat(editorConfig?.toMap())
                .overridingErrorMessage("Expected \n%s\nto yield indent_size = 2", cfg.trimIndent())
                .isEqualTo(mapOf("indent_size" to "2"))
        }
    }

    @Test
    fun testRootTermination() {
        val fs = Jimfs.newFileSystem(Configuration.unix())
        Files.createDirectories(fs.getPath("/projects/project-1/project-1-subdirectory"))
        Files.write(
            fs.getPath("/projects/.editorconfig"),
            """
            root = true
            [*]
            end_of_line = lf
            """.trimIndent().toByteArray()
        )
        Files.write(
            fs.getPath("/projects/project-1/.editorconfig"),
            """
            root = true
            [*.{kt,kts}]
            indent_size = 4
            indent_style = space
            """.trimIndent().toByteArray()
        )
        Files.write(
            fs.getPath("/projects/project-1/project-1-subdirectory/.editorconfig"),
            """
            [*]
            indent_size = 2
            """.trimIndent().toByteArray()
        )
        EditorConfigInternal.of(fs.getPath("/projects/project-1/project-1-subdirectory")).let { editorConfig ->
            assertThat(editorConfig?.parent).isNotNull()
            assertThat(editorConfig?.parent?.parent).isNull()
            assertThat(editorConfig?.toMap()).isEqualTo(
                mapOf(
                    "indent_size" to "2",
                    "indent_style" to "space"
                )
            )
        }
        EditorConfigInternal.of(fs.getPath("/projects/project-1")).let { editorConfig ->
            assertThat(editorConfig?.parent).isNull()
            assertThat(editorConfig?.toMap()).isEqualTo(
                mapOf(
                    "indent_size" to "4",
                    "indent_style" to "space"
                )
            )
        }
        EditorConfigInternal.of(fs.getPath("/projects")).let { editorConfig ->
            assertThat(editorConfig?.parent).isNull()
            assertThat(editorConfig?.toMap()).isEqualTo(
                mapOf(
                    "end_of_line" to "lf"
                )
            )
        }
    }

    @Test
    fun testSectionParsing() {
        assertThat(EditorConfigInternal.parseSection("*")).isEqualTo(listOf("*"))
        assertThat(EditorConfigInternal.parseSection("*.{js,py}")).isEqualTo(listOf("*.js", "*.py"))
        assertThat(EditorConfigInternal.parseSection("*.py")).isEqualTo(listOf("*.py"))
        assertThat(EditorConfigInternal.parseSection("Makefile")).isEqualTo(listOf("Makefile"))
        assertThat(EditorConfigInternal.parseSection("lib/**.js")).isEqualTo(listOf("lib/**.js"))
        assertThat(EditorConfigInternal.parseSection("{package.json,.travis.yml}"))
            .isEqualTo(listOf("package.json", ".travis.yml"))
    }

    @Test
    fun testMalformedSectionParsing() {
        assertThat(EditorConfigInternal.parseSection("")).isEqualTo(listOf(""))
        assertThat(EditorConfigInternal.parseSection(",*")).isEqualTo(listOf("", "*"))
        assertThat(EditorConfigInternal.parseSection("*,")).isEqualTo(listOf("*", ""))
        assertThat(EditorConfigInternal.parseSection("*.{js,py")).isEqualTo(listOf("*.js", "*.py"))
        assertThat(EditorConfigInternal.parseSection("*.{js,{py")).isEqualTo(listOf("*.js", "*.{py"))
        assertThat(EditorConfigInternal.parseSection("*.py}")).isEqualTo(listOf("*.py}"))
    }
}
