package com.pinterest.ktlint.api.consumer

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.EditorConfigDefaults
import com.pinterest.ktlint.rule.engine.api.EditorConfigOverride
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EXPERIMENTAL_RULES_EXECUTION_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.ec4j.toPropertyBuilderWithValue
import com.pinterest.ktlint.ruleset.standard.rules.FilenameRule
import com.pinterest.ktlint.ruleset.standard.rules.IndentationRule
import com.pinterest.ktlint.test.KtlintTestFileSystem
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.EditorConfig
import org.ec4j.core.model.Glob
import org.ec4j.core.model.Section
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * The KtLintRuleEngine is used by the Ktlint CLI and external API Consumers. Although most functionalities of the RuleEngine are already
 * tested via the Ktlint CLI Tests and normal unit tests in KtLint Core, some functionalities need additional testing from the perspective
 * of an API Consumer to ensure that the API is usable and stable across releases.
 */
class KtLintRuleEngineTest {
    private val ktlintTestFileSystem = KtlintTestFileSystem()

    @AfterEach
    fun tearDown() {
        ktlintTestFileSystem.close()
    }

    @Nested
    inner class `Lint with KtLintRuleEngine` {
        @Test
        fun `Given a file that does not contain an error`(
            @TempDir
            tempDir: Path,
        ) {
            val dir = ApiTestRunner(tempDir).prepareTestProject("no-code-style-error")

            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code = Code.fromFile(File("$dir/Main.kt")),
                callback = { lintErrors.add(it) },
            )

            assertThat(lintErrors).isEmpty()
        }

        @Test
        fun `Given a kotlin code snippet that does not contain an error`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code =
                    Code.fromSnippet(
                        """
                        fun main() {
                            println("Hello world!")
                        }
                        """.trimIndent(),
                    ),
                callback = { lintErrors.add(it) },
            )

            assertThat(lintErrors).isEmpty()
        }

        @Test
        fun `Given a kotlin script code snippet that does not contain an error`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code =
                    Code.fromSnippet(
                        """
                        plugins {
                            id("foo")
                            id("bar")
                        }
                        """.trimIndent(),
                        script = true,
                    ),
                callback = { lintErrors.add(it) },
            )

            assertThat(lintErrors).isEmpty()
        }

        @Test
        fun `Given a code snippet that violates a custom rule prefixed by a rule set id`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { NoVarRule() },
                        ),
                    editorConfigOverride =
                        EditorConfigOverride.from(
                            NoVarRule.NO_VAR_RULE_ID.createRuleExecutionEditorConfigProperty() to RuleExecution.enabled,
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
                callback = { lintErrors.add(it) },
            )

            assertThat(lintErrors).isNotEmpty
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
                callback = { lintErrors.add(it) },
            )

            assertThat(lintErrors).isEmpty()
        }
    }

    @Nested
    inner class `Format with KtLintRuleEngine` {
        @Test
        fun `Given a file that does not contain an error`(
            @TempDir
            tempDir: Path,
        ) {
            val dir = ApiTestRunner(tempDir).prepareTestProject("no-code-style-error")

            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val original = File("$dir/Main.kt").readText()

            val actual =
                ktLintRuleEngine.format(
                    code = Code.fromFile(File("$dir/Main.kt")),
                )

            assertThat(actual).isEqualTo(original)
        }

        @Test
        fun `Given a kotlin code snippet that does contain an indentation error`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val actual =
                ktLintRuleEngine.format(
                    code =
                        Code.fromSnippet(
                            """
                            fun main() {
                            println("Hello world!")
                            }
                            """.trimIndent(),
                        ),
                )

            assertThat(actual).isEqualTo(
                """
                fun main() {
                    println("Hello world!")
                }
                """.trimIndent(),
            )
        }

        @Test
        fun `Given a kotlin script code snippet that does contain an indentation error`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val actual =
                ktLintRuleEngine.format(
                    code =
                        Code.fromSnippet(
                            """
                            plugins {
                            id("foo")
                            id("bar")
                            }
                            """.trimIndent(),
                            script = true,
                        ),
                )

            assertThat(actual).isEqualTo(
                """
                plugins {
                    id("foo")
                    id("bar")
                }
                """.trimIndent(),
            )
        }

        @Test
        fun `Given a kotlin code snippet that does contain multiple indentation errors then only format the lint error at specific offset and message`() {
            val ktLintRuleEngine =
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { IndentationRule() },
                        ),
                    fileSystem = ktlintTestFileSystem.fileSystem,
                )

            val originalCode =
                """
                fun main() {
                println("Hello world!")
                println("Hello world!")
                println("Hello world!")
                }
                """.trimIndent()
            val actual =
                ktLintRuleEngine
                    .interactiveFormat(Code.fromSnippet(originalCode)) { lintError ->
                        lintError.line == 3 && lintError.detail == "Unexpected indentation (0) (should be 4)"
                    }

            assertThat(actual).isEqualTo(
                """
                fun main() {
                println("Hello world!")
                    println("Hello world!")
                println("Hello world!")
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `Given that all experimental rules are enabled`() {
        val ktLintEngine =
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider { NoVarRule() },
                    ),
                editorConfigDefaults =
                    EditorConfigDefaults(
                        EditorConfig
                            .builder()
                            .section(
                                Section
                                    .builder()
                                    .glob(Glob("*.{kt,kts}"))
                                    .properties(
                                        EXPERIMENTAL_RULES_EXECUTION_PROPERTY.toPropertyBuilderWithValue("enabled"),
                                    ),
                            ).build(),
                    ),
                fileSystem = ktlintTestFileSystem.fileSystem,
            )
        val errors = mutableListOf<LintError>()
        ktLintEngine.lint(
            code =
                Code.fromSnippet(
                    """
                    var foo = "foo"
                    """.trimIndent(),
                ),
            callback = errors::add,
        )

        val failedRules = errors.map { it.ruleId }
        check(failedRules.contains(NoVarRule.NO_VAR_RULE_ID))
    }

    private class NoVarRule :
        Rule(
            ruleId = NO_VAR_RULE_ID,
            about = About(),
        ),
        RuleAutocorrectApproveHandler,
        Rule.Experimental {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Boolean,
        ) {
            if (node.elementType == ElementType.VAR_KEYWORD) {
                emitAndApprove(node.startOffset, "Unexpected var, use val instead", false)
            }
        }

        companion object {
            val NO_VAR_RULE_ID = RuleId("test:no-var-rule")
        }
    }
}
