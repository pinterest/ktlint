package com.pinterest.ktlint.api.consumer

import com.pinterest.ktlint.api.consumer.KtLintRuleEngineTest.RuleWithAutocorrectApproveHandler.Companion.RULE_WITH_AUTOCORRECT_APPROVE_HANDLER
import com.pinterest.ktlint.api.consumer.KtLintRuleEngineTest.RuleWithoutAutocorrectApproveHandler.Companion.RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.ALLOW_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.NO_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.ruleset.standard.rules.FilenameRule
import com.pinterest.ktlint.ruleset.standard.rules.INDENTATION_RULE_ID
import com.pinterest.ktlint.ruleset.standard.rules.IndentationRule
import com.pinterest.ktlint.test.KtlintTestFileSystem
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileWriter
import java.nio.file.Path

/**
 * The KtLintRuleEngine is used by the Ktlint CLI and external API Consumers. Although most functionalities of the RuleEngine are already
 * tested via the Ktlint CLI Tests and normal unit tests in KtLint Core, some functionalities need additional testing from the perspective
 * of an API Consumer to ensure that the API is usable and stable across releases.
 */
class KtLintRuleEngineTest {
    private val ktlintTestFileSystem = KtlintTestFileSystem()
    private val ktLintRuleEngine =
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    RuleProvider { IndentationRule() },
                    RuleProvider { RuleWithAutocorrectApproveHandler() },
                    RuleProvider { RuleWithoutAutocorrectApproveHandler() },
                ),
            editorConfigOverride =
                EditorConfigOverride.from(
                    RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                    RULE_WITH_AUTOCORRECT_APPROVE_HANDLER.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
                ),
            fileSystem = ktlintTestFileSystem.fileSystem,
        )

    @AfterEach
    fun tearDown() {
        ktlintTestFileSystem.close()
    }

    @Nested
    inner class `Lint with KtLintRuleEngine` {
        @Test
        fun `Given a file containing errors found by standard and custom rules`(
            @TempDir
            tempDir: Path,
        ) {
            val filePath = "$tempDir/Code.kt"
            FileWriter(filePath).use {
                it.write(
                    """
                    fun bar() {
                        // foo
                        // bar
                        }
                    """.trimIndent(),
                )
            }

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code = Code.fromFile(File(filePath)),
            ) { lintErrors.add(it) }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
            )
        }

        @Test
        fun `Given a kotlin code snippet containing errors found by standard and custom rules`() {
            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code =
                    Code.fromSnippet(
                        """
                        fun bar() {
                            // foo
                            // bar
                            }
                        """.trimIndent(),
                    ),
            ) { lintErrors.add(it) }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
            )
        }

        @Test
        fun `Given a kotlin script code snippet containing errors found by standard and custom rules`() {
            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code =
                    Code.fromSnippet(
                        """
                        plugins {
                            // foo
                            // bar
                            }
                        """.trimIndent(),
                        script = true,
                    ),
            ) { lintErrors.add(it) }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
            )
        }

        @Test
        fun `Given a code snippet then the file name rule may not result in a Lint violation`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { FilenameRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )
            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code =
                    Code.fromSnippet(
                        """
                        var foo = "foo"
                        """.trimIndent(),
                    ),
            ) { lintErrors.add(it) }

            assertThat(lintErrors).isEmpty()
        }
    }

    @Nested
    inner class `Format (legacy) with KtLintRuleEngine` {
        @Test
        fun `Given a file that does not contain an error`(
            @TempDir
            tempDir: Path,
        ) {
            val filePath = "$tempDir/Code.kt"
            FileWriter(filePath).use {
                it.write(
                    """
                    fun bar() {
                        // foo
                        // bar
                        }
                    """.trimIndent(),
                )
            }

            val lintErrors = mutableListOf<LintError>()
            val actual =
                ktLintRuleEngine.format(
                    code = Code.fromFile(File(filePath)),
                ) { lintError, _ -> lintErrors.add(lintError) }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
            )
            assertThat(actual).isEqualTo(
                """
                fun bar() {
                    // FOO
                    // BAR
                }
                """.trimIndent(),
            )
        }

        @Test
        fun `Given a kotlin code snippet that does contain an indentation error`() {
            val lintErrors = mutableListOf<LintError>()
            val actual =
                ktLintRuleEngine.format(
                    code =
                        Code.fromSnippet(
                            """
                            fun bar() {
                                // foo
                                // bar
                                }
                            """.trimIndent(),
                        ),
                ) { lintError, _ -> lintErrors.add(lintError) }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
            )
            assertThat(actual).isEqualTo(
                """
                fun bar() {
                    // FOO
                    // BAR
                }
                """.trimIndent(),
            )
        }

        @Test
        fun `Given a kotlin script code snippet that does contain an indentation error`() {
            val lintErrors = mutableListOf<LintError>()
            val actual =
                ktLintRuleEngine.format(
                    code =
                        Code.fromSnippet(
                            """
                            plugins {
                                // foo
                                // bar
                                }
                            """.trimIndent(),
                            script = true,
                        ),
                ) { lintError, _ -> lintErrors.add(lintError) }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
            )
            assertThat(actual).isEqualTo(
                """
                plugins {
                    // FOO
                    // BAR
                }
                """.trimIndent(),
            )
        }
    }

    @Nested
    inner class `Format with KtLintRuleEngine` {
        @Nested
        inner class `Given a file that does not contain an error` {
            @Test
            fun `Given defaultAutocorrect is not set`(
                @TempDir
                tempDir: Path,
            ) {
                val filePath = "$tempDir/Code.kt"
                FileWriter(filePath).use {
                    it.write(
                        """
                        fun bar() {
                            // foo
                            // bar
                            }
                        """.trimIndent(),
                    )
                }

                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code = Code.fromFile(File(filePath)),
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    """
                    fun bar() {
                        // FOO
                        // BAR
                    }
                    """.trimIndent(),
                )
            }

            @Test
            fun `Given defaultAutocorrect is enabled`(
                @TempDir
                tempDir: Path,
            ) {
                val filePath = "$tempDir/Code.kt"
                FileWriter(filePath).use {
                    it.write(
                        """
                        fun bar() {
                            // foo
                            // bar
                            }
                        """.trimIndent(),
                    )
                }

                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code = Code.fromFile(File(filePath)),
                        defaultAutocorrect = true,
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    """
                    fun bar() {
                        // FOO
                        // BAR
                    }
                    """.trimIndent(),
                )
            }

            @Test
            fun `Given defaultAutocorrect is disabled`(
                @TempDir
                tempDir: Path,
            ) {
                val filePath = "$tempDir/Code.kt"
                FileWriter(filePath).use {
                    it.write(
                        """
                        fun bar() {
                            // foo
                            // bar
                            }
                        """.trimIndent(),
                    )
                }

                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code = Code.fromFile(File(filePath)),
                        defaultAutocorrect = false,
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    // Note that "foo" is not transformed to "FOO" as the defaultAutocorrect for rules without AutocorrectApproveHandler is
                    // not set
                    """
                    fun bar() {
                        // foo
                        // BAR
                    }
                    """.trimIndent(),
                )
            }
        }

        @Nested
        inner class `Given a kotlin code snippet that does contain an indentation error` {
            @Test
            fun `Given defaultAutocorrect is not set`() {
                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code =
                            Code.fromSnippet(
                                """
                                fun bar() {
                                    // foo
                                    // bar
                                    }
                                """.trimIndent(),
                            ),
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    """
                    fun bar() {
                        // FOO
                        // BAR
                    }
                    """.trimIndent(),
                )
            }

            @Test
            fun `Given defaultAutocorrect is enabled`() {
                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code =
                            Code.fromSnippet(
                                """
                                fun bar() {
                                    // foo
                                    // bar
                                    }
                                """.trimIndent(),
                            ),
                        defaultAutocorrect = true,
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    """
                    fun bar() {
                        // FOO
                        // BAR
                    }
                    """.trimIndent(),
                )
            }

            @Test
            fun `Given defaultAutocorrect is disabled`() {
                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code =
                            Code.fromSnippet(
                                """
                                fun bar() {
                                    // foo
                                    // bar
                                    }
                                """.trimIndent(),
                            ),
                        defaultAutocorrect = false,
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    // Note that "foo" is not transformed to "FOO" as the defaultAutocorrect for rules without AutocorrectApproveHandler is
                    // not set
                    """
                    fun bar() {
                        // foo
                        // BAR
                    }
                    """.trimIndent(),
                )
            }
        }

        @Nested
        inner class `Given a kotlin script code snippet that does contain an indentation error` {
            @Test
            fun `Given defaultAutocorrect is not set`() {
                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code =
                            Code.fromSnippet(
                                """
                                plugins {
                                    // foo
                                    // bar
                                    }
                                """.trimIndent(),
                                script = true,
                            ),
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    """
                    plugins {
                        // FOO
                        // BAR
                    }
                    """.trimIndent(),
                )
            }

            @Test
            fun `Given defaultAutocorrect is enabled`() {
                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code =
                            Code.fromSnippet(
                                """
                                plugins {
                                    // foo
                                    // bar
                                    }
                                """.trimIndent(),
                                script = true,
                            ),
                        defaultAutocorrect = true,
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    """
                    plugins {
                        // FOO
                        // BAR
                    }
                    """.trimIndent(),
                )
            }

            @Test
            fun `Given defaultAutocorrect is disabled`() {
                val lintErrors = mutableListOf<LintError>()
                val actual =
                    ktLintRuleEngine.format(
                        code =
                            Code.fromSnippet(
                                """
                                plugins {
                                    // foo
                                    // bar
                                    }
                                """.trimIndent(),
                                script = true,
                            ),
                        defaultAutocorrect = false,
                    ) { lintError ->
                        lintErrors.add(lintError)
                        ALLOW_AUTOCORRECT
                    }

                assertThat(lintErrors).containsExactlyInAnyOrder(
                    LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
                    LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                    LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
                )
                assertThat(actual).isEqualTo(
                    // Note that "foo" is not transformed to "FOO" as the defaultAutocorrect for rules without AutocorrectApproveHandler is
                    // not set
                    """
                    plugins {
                        // foo
                        // BAR
                    }
                    """.trimIndent(),
                )
            }
        }

        @Test
        fun `Given a kotlin code snippet that does contain multiple errors then only format the lint error at specific offset and message`() {
            val lintErrors = mutableListOf<LintError>()
            val actual =
                ktLintRuleEngine
                    .format(
                        code =
                            Code.fromSnippet(
                                """
                                // bar
                                // bar
                                // bar
                                """.trimIndent(),
                            ),
                    ) { lintError ->
                        lintErrors.add(lintError)
                        if (lintError.line == 2 &&
                            lintError.col == 1 &&
                            lintError.ruleId == RULE_WITH_AUTOCORRECT_APPROVE_HANDLER &&
                            lintError.detail == "Bar comment with autocorrect approve handler"
                        ) {
                            ALLOW_AUTOCORRECT
                        } else {
                            NO_AUTOCORRECT
                        }
                    }

            assertThat(lintErrors).containsExactlyInAnyOrder(
                LintError(1, 1, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(2, 1, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
                LintError(3, 1, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
            )
            assertThat(actual).isEqualTo(
                """
                // bar
                // BAR
                // bar
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Given that all experimental rules are enabled`() {
        val ktLintRuleEngine =
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider { IndentationRule() },
                        RuleProvider { RuleWithAutocorrectApproveHandler() },
                        RuleProvider { RuleWithoutAutocorrectApproveHandler() },
                    ),
                editorConfigOverride =
                    EditorConfigOverride.from(
                        EXPERIMENTAL_RULES_EXECUTION_PROPERTY to RuleExecution.enabled,
                    ),
                fileSystem = ktlintTestFileSystem.fileSystem,
            )

        val lintErrors = mutableListOf<LintError>()
        ktLintRuleEngine.lint(
            code =
                Code.fromSnippet(
                    """
                    fun bar() {
                        // foo
                        // bar
                        }
                    """.trimIndent(),
                ),
            callback = lintErrors::add,
        )

        assertThat(lintErrors).containsExactlyInAnyOrder(
            LintError(2, 5, RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER, "Foo comment without autocorrect approve handler", true),
            LintError(3, 5, RULE_WITH_AUTOCORRECT_APPROVE_HANDLER, "Bar comment with autocorrect approve handler", true),
            LintError(4, 1, INDENTATION_RULE_ID, "Unexpected indentation (4) (should be 0)", true),
        )
    }

    private class RuleWithoutAutocorrectApproveHandler :
        Rule(
            ruleId = RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER,
            about = About(),
        ),
        Rule.Experimental {
        @Deprecated("Marked for removal in Ktlint 2.0. Please implement interface RuleAutocorrectApproveHandler.")
        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ) {
            if (node.elementType == ElementType.EOL_COMMENT && node.text == "// foo") {
                emit(node.startOffset, "Foo comment without autocorrect approve handler", true)
                if (autoCorrect) {
                    node
                        .safeAs<LeafElement>()
                        ?.rawReplaceWithText("// FOO")
                }
            }
        }

        companion object {
            val RULE_WITHOUT_AUTOCORRECT_APPROVE_HANDLER = RuleId("custom:rule-without-autocorrect-approval-handler")
        }
    }

    private class RuleWithAutocorrectApproveHandler :
        Rule(
            ruleId = RULE_WITH_AUTOCORRECT_APPROVE_HANDLER,
            about = About(),
        ),
        RuleAutocorrectApproveHandler,
        Rule.Experimental {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
        ) {
            if (node.elementType == ElementType.EOL_COMMENT && node.text == "// bar") {
                emitAndApprove(node.startOffset, "Bar comment with autocorrect approve handler", true)
                    .ifAutocorrectAllowed {
                        node
                            .safeAs<LeafElement>()
                            ?.rawReplaceWithText("// BAR")
                    }
            }
        }

        companion object {
            val RULE_WITH_AUTOCORRECT_APPROVE_HANDLER = RuleId("custom:rule-with-autocorrect-approval-handler")
        }
    }
}
