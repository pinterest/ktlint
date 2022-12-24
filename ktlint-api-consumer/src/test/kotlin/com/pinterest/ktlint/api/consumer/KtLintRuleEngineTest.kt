package com.pinterest.ktlint.api.consumer

import com.pinterest.ktlint.core.Code
import com.pinterest.ktlint.core.KtLintRuleEngine
import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.ruleset.standard.IndentationRule
import java.io.File
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class KtLintRuleEngineTest {
    @Nested
    inner class `Lint with KtLintRuleEngine` {
        @Test
        fun `Given a file that does not contain an error`(
            @TempDir
            tempDir: Path,
        ) {
            val dir = ApiTestRunner(tempDir).prepareTestProject("no-code-style-error")

            val ktLintRuleEngine = KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { IndentationRule() },
                ),
            )

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code = Code.CodeFile(File("$dir/Main.kt")),
                callback = { lintErrors.add(it) },
            )

            assertThat(lintErrors).isEmpty()
        }

        @Test
        fun `Given a kotlin code snippet that does not contain an error`() {
            val ktLintRuleEngine = KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { IndentationRule() },
                ),
            )

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code = Code.CodeSnippet(
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
        fun `Givens a kotlin script code snippet that does not contain an error`() {
            val ktLintRuleEngine = KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { IndentationRule() },
                ),
            )

            val lintErrors = mutableListOf<LintError>()
            ktLintRuleEngine.lint(
                code = Code.CodeSnippet(
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
    }

    @Nested
    inner class `Format with KtLintRuleEngine` {
        @Test
        fun `Given a file that does not contain an error`(
            @TempDir
            tempDir: Path,
        ) {
            val dir = ApiTestRunner(tempDir).prepareTestProject("no-code-style-error")

            val ktLintRuleEngine = KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { IndentationRule() },
                ),
            )

            val original = File("$dir/Main.kt").readText()

            val actual = ktLintRuleEngine.format(
                code = Code.CodeFile(File("$dir/Main.kt")),
            )

            assertThat(actual).isEqualTo(original)
        }

        @Test
        fun `Given a kotlin code snippet that does contain an indentation error`() {
            val ktLintRuleEngine = KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { IndentationRule() },
                ),
            )

            val actual = ktLintRuleEngine.format(
                code = Code.CodeSnippet(
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
            val ktLintRuleEngine = KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { IndentationRule() },
                ),
            )

            val actual = ktLintRuleEngine.format(
                code = Code.CodeSnippet(
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
    }
}
