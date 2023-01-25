package com.pinterest.ktlint.ruleset.core.api

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.ruleset.core.api.ElementType.CLASS
import com.pinterest.ktlint.ruleset.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.ruleset.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.ruleset.core.api.ElementType.FUN
import com.pinterest.ktlint.ruleset.core.api.ElementType.LPAR
import com.pinterest.ktlint.ruleset.core.api.ElementType.RPAR
import com.pinterest.ktlint.ruleset.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.ruleset.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.ruleset.core.api.ElementType.WHITE_SPACE
import org.assertj.core.api.Assertions
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.reflect.KFunction1

class ASTNodeExtensionTest {
    @Test
    fun `Given an enum class body then get all leaves in the open range of the class body`() {
        val code =
            """
            enum class Shape {
                FOO, FOOBAR, BAR
            }
            """.trimIndent()
        val enumClassBody = code.transformAst(::toEnumClassBodySequence)

        val actual =
            leavesInOpenRange(enumClassBody.first(), enumClassBody.last())
                .map { it.text }
                .toList()

        Assertions.assertThat(actual).containsExactly(
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
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR
                }
                """.trimIndent()
            val enumEntries =
                code
                    .transformAst(::toEnumClassBodySequence)
                    .filter { it.elementType == ENUM_ENTRY }

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes not starting or ending with a whitespace leaf containing a newline but containing another whitespace leaf containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO,
                    FOOBAR,
                    BAR
                }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape { FOO, FOOBAR, BAR
                } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes not containing a whitespace with a newline but having a block comment in between which does contain a newline`() {
            val code =
                """
                enum class Shape { FOO, /*
                newline in a block comment is ignored as it is not part of a whitespace leaf
                */ FOOBAR, BAR }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }
    }

    @Nested
    inner class HasWhiteSpaceWithNewLineInClosedRange {
        @Test
        fun `Given an enum class with no whitespace leaf containing a newline between the first and last enum entry`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR
                }
                """.trimIndent()
            val enumEntries =
                code
                    .transformAst(::toEnumClassBodySequence)
                    .filter { it.elementType == ENUM_ENTRY }

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes not starting or ending with a whitespace leaf containing a newline but containing another whitespace leaf containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO,
                    FOOBAR,
                    BAR
                }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape { FOO, FOOBAR, BAR
                } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes not containing a whitespace with a newline but having a block comment in between which does contain a newline`() {
            val code =
                """
                enum class Shape { FOO, /*
                newline in a block comment is ignored as it is not part of a whitespace leaf
                */ FOOBAR, BAR }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noWhiteSpaceWithNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }
    }

    @Nested
    inner class HasNewLineInClosedRange {
        @Test
        fun `Given an enum class with no whitespace leaf containing a newline between the first and last enum entry`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR
                }
                """.trimIndent()
            val enumEntries =
                code
                    .transformAst(::toEnumClassBodySequence)
                    .filter { it.elementType == ENUM_ENTRY }

            val actual = hasNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes not starting or ending with a whitespace leaf containing a newline but containing another whitespace leaf containing a newline`() {
            val code = """
                enum class Shape {
                    FOO,
                    FOOBAR,
                    BAR
                }
            """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape { FOO, FOOBAR, BAR
                } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes not containing a newline inside a block comment in between`() {
            val code =
                """
                enum class Shape { FOO, /*
                newline in a block comment is ignored as it is not part of a whitespace leaf
                */ FOOBAR, BAR }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isTrue
        }
    }

    @Nested
    inner class NoNewLineInClosedRange {
        @Test
        fun `Given an enum class with no whitespace leaf containing a newline between the first and last enum entry`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR
                }
                """.trimIndent()
            val enumEntries =
                code
                    .transformAst(::toEnumClassBodySequence)
                    .filter { it.elementType == ENUM_ENTRY }

            val actual = noNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            Assertions.assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO, FOOBAR, BAR } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes not starting or ending with a whitespace leaf containing a newline but containing another whitespace leaf containing a newline`() {
            val code =
                """
                enum class Shape {
                    FOO,
                    FOOBAR,
                    BAR
                }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape { FOO, FOOBAR, BAR
                } // Malformed on purpose for test
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes not containing a newline inside a block comment in between`() {
            val code =
                """
                enum class Shape { FOO, /*
                newline in a block comment is ignored as it is not part of a whitespace leaf
                */ FOOBAR, BAR }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = noNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            Assertions.assertThat(actual).isFalse
        }
    }

    @Nested
    inner class UpsertWhitespaceBeforeMe {
        @Test
        fun `Given a whitespace node and upsert a whitespace before the node (RPAR) then replace the current whitespace element`() {
            val code =
                """
                fun foo( ) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(
                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(WHITE_SPACE)
                            ?.upsertWhitespaceBeforeMe("\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (RPAR) which is preceded by a non-whitespace leaf element (LPAR) and upsert a whitespace before the node (RPAR) then create a new whitespace element before the node (RPAR)`() {
            val code =
                """
                fun foo() = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(

                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        nextLeaf { it.elementType == RPAR }
                            ?.upsertWhitespaceBeforeMe("\n\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (RPAR) which is preceded by a whitespace leaf element and upsert a whitespace before the node (RPAR) then replace the whitespace element before the node (RPAR)`() {
            val code =
                """
                fun foo( ) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(

                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        nextLeaf { it.elementType == RPAR }
                            ?.upsertWhitespaceBeforeMe("\n\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (VALUE_PARAMETER) which is preceded by a non-whitespace leaf element (LPAR) and upsert a whitespace before the node (VALUE_PARAMETER) then create a new whitespace element before the node (VALUE_PARAMETER)`() {
            val code =
                """
                fun foo(string: String) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(
                    string: String) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(VALUE_PARAMETER)
                            ?.upsertWhitespaceBeforeMe("\n    ")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (FUN bar) which is preceded by a composite element (FUN foo) and upsert a whitespace before the node (FUN bar) then create a new whitespace element before the node (FUN bar)`() {
            val code =
                """
                fun foo() = "foo"
                fun bar() = "bar"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() = "foo"

                fun bar() = "bar"
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        children()
                            .last { it.elementType == FUN }
                            .upsertWhitespaceBeforeMe("\n\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }
    }

    @Nested
    inner class UpsertWhitespaceAfterMe {
        @Test
        fun `Given a node (LPAR) which is followed by a non-whitespace leaf element (RPAR) and upsert a whitespace after the node (LPAR) then create a new whitespace element after the node (LPAR)`() {
            val code =
                """
                fun foo() = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(

                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        nextLeaf { it.elementType == LPAR }
                            ?.upsertWhitespaceAfterMe("\n\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (LPAR) which is followed by a whitespace leaf element and upsert a whitespace after the node (LPAR) then replace the whitespace element after the node (LPAR)`() {
            val code =
                """
                fun foo( ) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(

                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        nextLeaf { it.elementType == LPAR }
                            ?.upsertWhitespaceAfterMe("\n\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (VALUE_PARAMETER) which is followed by a non-whitespace leaf element (RPAR) and upsert a whitespace after the node (VALUE_PARAMETER) then create a new whitespace element after the node (VALUE_PARAMETER)`() {
            val code =
                """
                fun foo(string: String) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(string: String
                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(VALUE_PARAMETER)
                            ?.upsertWhitespaceAfterMe("\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given a node (FUN foo) which is followed by a composite element (FUN bar) and upsert a whitespace after the node (FUN foo) then create a new whitespace element after the node (FUN foo)`() {
            val code =
                """
                fun foo() = "foo"
                fun bar() = "bar"
                """.trimIndent()
            val formattedCode =
                """
                fun foo() = "foo"

                fun bar() = "bar"
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        children()
                            .first { it.elementType == FUN }
                            .upsertWhitespaceAfterMe("\n\n")
                    }.text

            Assertions.assertThat(actual).isEqualTo(formattedCode)
        }
    }

    private inline fun String.transformAst(block: FileASTNode.() -> Unit): FileASTNode =
        transformCodeToAST(this)
            .apply(block)

    private fun String.transformAst(kFunction1: KFunction1<FileASTNode, Sequence<ASTNode>>) = kFunction1(transformCodeToAST(this))

    private fun transformCodeToAST(code: String) =
        KtLintRuleEngine(
            ruleProviders = setOf(
                RuleProvider { DummyRule() },
            ),
        ).transformToAst(
            Code.CodeSnippet(code),
        )

    private fun toEnumClassBodySequence(fileASTNode: FileASTNode) =
        fileASTNode
            .findChildByType(CLASS)
            ?.findChildByType(CLASS_BODY)
            ?.children()
            .orEmpty()

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
}
