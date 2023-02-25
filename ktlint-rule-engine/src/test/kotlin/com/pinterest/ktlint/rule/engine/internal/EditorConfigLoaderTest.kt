package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INSERT_FINAL_NEWLINE_PROPERTY
import com.pinterest.ktlint.test.KtlintTestFileSystem
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Suppress("EditorConfigKeyCorrectness")
internal class EditorConfigLoaderTest {
    private val ktlintTestFileSystem = KtlintTestFileSystem()

    @AfterEach
    fun tearDown() {
        ktlintTestFileSystem.close()
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            //language=EditorConfig
            """
            [*]
            indent_size = 2
            """,
            //language=EditorConfig
            """
            root = true
            [*]
            indent_size = 2
            """,
            // Note, when multiple globs matches with the file being scanned then the value from the last glob wins
            //language=EditorConfig
            """
            [*]
            indent_size = 4
            [*.{kt,kts}]
            indent_size = 2
            """,
            // Note, when multiple globs matches with the file being scanned then the value from the last glob wins
            //language=EditorConfig
            """
            [*.{kt,kts}]
            indent_size = 4
            [*]
            indent_size = 2
            """,
        ],
    )
    fun `Given an editorconfig file in the project root and the file to be linted is in a subdirectory not containing an editorconfig file`(
        editorconfigContent: String,
    ) {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(editorconfigContent.trimIndent())
        }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("some-subdirectory/test.kt"))
            .run {
                assertThat(convertToPropertyValues()).contains("indent_size = 2")
            }
    }

    @Test
    fun `Given editorconfig files at different levels in the project hierarchy and an intermediate editorconfig file contains the root is true property then stop reading the parent editorconfig`() {
        val someRelativeProjectDirectory = "some-project-directory"
        val someRelativeProjectSubDirectory = "$someRelativeProjectDirectory/some-project-sub-directory"

        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                root = true
                [*]
                end_of_line = lf
                some_property_1 = some_value_1
                """.trimIndent(),
            )

            writeEditorConfigFile(
                someRelativeProjectDirectory,
                //language=EditorConfig
                """
                root = true
                [*.{kt,kts}]
                indent_size = 4
                indent_style = space
                some_property_2 = some_value_2
                """.trimIndent(),
            )

            writeEditorConfigFile(
                someRelativeProjectSubDirectory,
                //language=EditorConfig
                """
                [*]
                indent_size = 2
                some_property_3 = some_value_3
                """.trimIndent(),
            )
        }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("$someRelativeProjectSubDirectory/test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains(
                        "some_property_2 = some_value_2",
                        "some_property_3 = some_value_3",
                    ).doesNotContain(
                        // The '.editorconfig' file in the root directory is skipped because the property "root = true" is
                        // found
                        "some_property_1 = some_value_1",
                    )
            }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("$someRelativeProjectDirectory/test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains("some_property_2 = some_value_2")
                    .doesNotContain(
                        // The '.editorconfig' file in the root directory is skipped because the property "root = true" is
                        // found
                        "some_property_1 = some_value_1",
                    )
            }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains("some_property_1 = some_value_1")
            }
    }

    @Test
    fun `Should parse properties with and without spaces before or after the =`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                some_property_1 = some_value_1
                some_property_2= some_value_2
                some_property_3 =some_value_3
                some_property_4=some_value_4
                """.trimIndent(),
            )
        }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains(
                        "some_property_1 = some_value_1",
                        "some_property_2 = some_value_2",
                        "some_property_3 = some_value_3",
                        "some_property_4 = some_value_4",
                    )
            }
    }

    @Test
    fun `Should parse unset values`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                some_property_1 = unset
                """.trimIndent(),
            )
        }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains("some_property_1 = unset")
            }
    }

    @Test
    fun `Given a property with a comma separated list of values, with or without spaces around the comma the return the value inclusive those spaces`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                some_property_1=some_value_1,some_value_2
                some_property_2=some_value_1 ,some_value_2
                some_property_3=some_value_1, some_value_2
                some_property_4=some_value_1 , some_value_2
                """.trimIndent(),
            )
        }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains(
                        "some_property_1 = some_value_1,some_value_2",
                        "some_property_2 = some_value_1 ,some_value_2",
                        "some_property_3 = some_value_1, some_value_2",
                        "some_property_4 = some_value_1 , some_value_2",
                    )
            }
    }

    @Test
    fun `Given some override properties and a null file path then return only the default ktlint properties and the override properties`() {
        val editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "true")
        createEditorConfigLoader(editorConfigOverride = editorConfigOverride)
            .load(null)
            .run {
                assertThat(convertToPropertyValues())
                    .containsExactlyInAnyOrder(
                        "insert_final_newline = true",
                        "end_of_line = lf",
                    )
            }
    }

    @Test
    fun `Given some override properties and a file with a non supported file extension then return only the default ktlint properties and the override properties`() {
        val editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "true")
        createEditorConfigLoader(editorConfigOverride = editorConfigOverride)
            .load(ktlintTestFileSystem.resolve("test.java"))
            .run {
                assertThat(convertToPropertyValues())
                    .containsExactlyInAnyOrder(
                        "insert_final_newline = true",
                        "end_of_line = lf",
                    )
            }
    }

    @Test
    fun `Should return properties for stdin from editorconfig file in current directory and override properties`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                some_property_1 = some_value_1
                """.trimIndent(),
            )
        }

        val editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to true)
        createEditorConfigLoader(editorConfigOverride = editorConfigOverride)
            .load(ktlintTestFileSystem.resolve(".kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .containsExactlyInAnyOrder(
                        "some_property_1 = some_value_1",
                        "insert_final_newline = true",
                        "end_of_line = lf",
                    )
            }
    }

    @Test
    fun `Given a project with editorconfig properties (root=true) and override properties then ignore properties from root dir but apply the override properties`() {
        val someRelativeProjectDirectory = "some-project-directory"

        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                root = true
                [*]
                some_property_1 = some_value_1
                """.trimIndent(),
            )

            writeEditorConfigFile(
                someRelativeProjectDirectory,
                //language=EditorConfig
                """
                root = true
                [*.{kt,kts}]
                some_property_1 = some_value_2
                some_property_2 = some_value_2
                indent_size = 4
                """.trimIndent(),
            )
        }

        val editorConfigOverride = EditorConfigOverride.from(INDENT_SIZE_PROPERTY to 2)
        createEditorConfigLoader(editorConfigOverride = editorConfigOverride)
            .load(ktlintTestFileSystem.resolve("$someRelativeProjectDirectory/test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains(
                        "some_property_1 = some_value_2",
                        "some_property_2 = some_value_2",
                        "indent_size = 2",
                    )
            }
    }

    @Test
    fun `Should load properties from override on stdin input`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                root = true
                [*]
                end_of_line = lf
                """.trimIndent(),
            )
        }

        val editorConfigOverride = EditorConfigOverride.from(INDENT_SIZE_PROPERTY to 2)
        createEditorConfigLoader(editorConfigOverride = editorConfigOverride)
            .load(null)
            .run {
                assertThat(convertToPropertyValues())
                    .containsExactlyInAnyOrder(
                        "end_of_line = lf",
                        "indent_size = 2",
                        "tab_width = 2",
                    )
            }
    }

    @Test
    fun `Should support editorconfig globs when loading properties for file specified under such glob`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                some_property_1 = some_value_1
                some_property_2 = some_value_2

                [api/*.{kt,kts}]
                some_property_1 = some_value_2
                some_property_3 = some_value_3
                """.trimIndent(),
            )
        }

        createEditorConfigLoader()
            .load(ktlintTestFileSystem.resolve("api/test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains(
                        "some_property_1 = some_value_2",
                        "some_property_2 = some_value_2",
                        "some_property_3 = some_value_3",
                    )
            }
    }

    @Test
    fun `Should add property from override`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                some_property_1 = some_value_1
                """.trimIndent(),
            )
        }

        val editorConfigDefaults = EditorConfigDefaults.EMPTY_EDITOR_CONFIG_DEFAULTS
        val editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "true")
        createEditorConfigLoader(editorConfigDefaults, editorConfigOverride)
            .load(ktlintTestFileSystem.resolve("test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains(
                        "some_property_1 = some_value_1",
                        "insert_final_newline = true",
                    )
            }
    }

    @Test
    fun `Should replace property from override`() {
        ktlintTestFileSystem.apply {
            writeRootEditorConfigFile(
                //language=EditorConfig
                """
                [*.{kt,kts}]
                insert_final_newline = true
                """.trimIndent(),
            )
        }

        val editorConfigDefaults = EditorConfigDefaults.EMPTY_EDITOR_CONFIG_DEFAULTS
        val editorConfigOverride = EditorConfigOverride.from(INSERT_FINAL_NEWLINE_PROPERTY to "false")
        createEditorConfigLoader(editorConfigDefaults, editorConfigOverride)
            .load(ktlintTestFileSystem.resolve("test.kt"))
            .run {
                assertThat(convertToPropertyValues())
                    .contains("insert_final_newline = false")
            }
    }

    private fun createEditorConfigLoader(
        editorConfigDefaults: EditorConfigDefaults = EditorConfigDefaults.EMPTY_EDITOR_CONFIG_DEFAULTS,
        editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE,
    ) = EditorConfigLoader(
        fileSystem = ktlintTestFileSystem.fileSystem,
        editorConfigLoaderEc4j = EditorConfigLoaderEc4j(emptySet()),
        editorConfigDefaults = editorConfigDefaults,
        editorConfigOverride = editorConfigOverride,
    )

    private fun EditorConfig.convertToPropertyValues(): List<String> =
        map {
            val value = if (it.isUnset) {
                "unset"
            } else {
                it.sourceValue
            }
            "${it.name} = $value"
        }.toList()
}
