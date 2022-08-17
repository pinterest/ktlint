package com.pinterest.ktlint.core.ast

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.CLASS_BODY
import com.pinterest.ktlint.core.ast.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.core.internal.prepareCodeForLinting
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class PackageKtTest {
    @Test
    fun `Given an enum class body then get all leaves in the open range of the class body`() {
        val enumClassBody =
            transformCodeToAST(
                """
                enum class Shape {
                    FOO, FOOBAR, BAR
                }
                """.trimIndent(),
            ).toEnumClassBodySequence()

        val actual =
            leavesInOpenRange(enumClassBody.first(), enumClassBody.last())
                .map { it.text }
                .toList()

        assertThat(actual).containsExactly(
            // LBRACE is omitted from class body as it is an open range
            "\n    ",
            "FOO",
            ",",
            " ",
            "FOOBAR",
            ",",
            " ",
            "BAR",
            "\n",
            // RBRACE is omitted from class body as it is an open range
        )
    }

    @Nested
    inner class NoWhiteSpaceWithNewLineInClosedRange {
        @Test
        fun `Given an enum class with no whitespace leaf containing a newline between the first and last enum entry`() {
            val enumEntries =
                transformCodeToAST(
                    """
                    enum class Shape {
                        FOO, FOOBAR, BAR
                    }
                    """.trimIndent(),
                ).toEnumClassBodySequence()
                    .filter { it.elementType == ENUM_ENTRY }

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape {
                        FOO, FOOBAR, BAR } // Malformed on purpose for test
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes not starting or ending with a whitespace leaf containing a newline but containing another whitespace leaf containing a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape {
                        FOO,
                        FOOBAR,
                        BAR
                    }
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape { FOO, FOOBAR, BAR
                    } // Malformed on purpose for test
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes not containing a whitespace with a newline but having a block comment in between which does contain a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape { FOO, /*
                    newline in a block comment is ignored as it is not part of a whitespace leaf
                    */ FOOBAR, BAR }
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
        }
    }

    @Nested
    inner class HasWhiteSpaceWithNewLineInClosedRange {
        @Test
        fun `Given an enum class with no whitespace leaf containing a newline between the first and last enum entry`() {
            val enumEntries =
                transformCodeToAST(
                    """
                    enum class Shape {
                        FOO, FOOBAR, BAR
                    }
                    """.trimIndent(),
                ).toEnumClassBodySequence()
                    .filter { it.elementType == ENUM_ENTRY }

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape {
                        FOO, FOOBAR, BAR } // Malformed on purpose for test
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes not starting or ending with a whitespace leaf containing a newline but containing another whitespace leaf containing a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape {
                        FOO,
                        FOOBAR,
                        BAR
                    }
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape { FOO, FOOBAR, BAR
                    } // Malformed on purpose for test
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes not containing a whitespace with a newline but having a block comment in between which does contain a newline`() {
            val enumClassBody =
                transformCodeToAST(
                    """
                    enum class Shape { FOO, /*
                    newline in a block comment is ignored as it is not part of a whitespace leaf
                    */ FOOBAR, BAR }
                    """.trimIndent(),
                ).toEnumClassBodySequence()

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
        }
    }

    @Test
    fun `Given a range of nodes not containing a whitespace with a newline but having a block comment in between which does contain a newline`() {
        val enumClassBody =
            transformCodeToAST(
                """
                enum class Shape { FOO, /*
                newline in a block comment should be ignored as it is not part of a whitespace leaf
                */ FOOBAR, BAR }
                """.trimIndent(),
            ).toEnumClassBodySequence()

        val actual = containsLineBreakInRange(enumClassBody.first().psi, enumClassBody.last().psi)

        // This method should have returned false instead of true. As of that the method is deprecated.
        assertThat(actual).isTrue
    }

    private fun transformCodeToAST(code: String) =
        prepareCodeForLinting(
            KtLint.ExperimentalParams(
                text =
                code,
                ruleProviders = setOf(
                    RuleProvider { DummyRule() },
                ),
                cb = { _, _ -> },
            ),
        ).rootNode

    private fun FileASTNode.toEnumClassBodySequence() =
        findChildByType(CLASS)
            ?.findChildByType(CLASS_BODY)
            ?.children()
            .orEmpty()
}

/**
 * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
 */
private open class DummyRule(
    val block: (node: ASTNode) -> Unit = {},
) : Rule(DUMMY_RULE_ID) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        block(node)
    }

    companion object {
        const val DUMMY_RULE_ID = "dummy-rule"
    }
}
