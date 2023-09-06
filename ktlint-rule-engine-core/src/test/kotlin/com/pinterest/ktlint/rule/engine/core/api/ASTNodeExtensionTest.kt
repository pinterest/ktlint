package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.psi.psiUtil.leaves
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

    @Test
    fun `Given an enum class body then get all leaves in the closed range of the class body`() {
        val code =
            """
            enum class Shape {
                FOO, FOOBAR, BAR
            }
            """.trimIndent()
        val enumClassBody = code.transformAst(::toEnumClassBodySequence)

        val actual =
            leavesInClosedRange(enumClassBody.first(), enumClassBody.last())
                .map { it.text }
                .toList()

        assertThat(actual).containsExactly(
            "{",
            "\n    ",
            "FOO",
            ",",
            " ",
            "FOOBAR",
            ",",
            " ",
            "BAR",
            "\n",
            "}",
        )
    }

    @Nested
    inner class NoNewLineInOpenRange {
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

            val actual = noNewLineInOpenRange(enumEntries.first(), enumEntries.last())

            assertThat(actual).isTrue
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

            assertThat(actual).isFalse
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

            assertThat(actual).isFalse
        }
    }

    @Nested
    inner class HasNewLineInClosedRange {
        @Test
        fun `Given an enum class with no whitespace leaf containing a newline between the first and last enum entry`() {
            val code =
                """
                enum class Shape { FOO, FOOBAR, BAR }
                """.trimIndent()
            val enumEntries = code.transformAst(::toEnumClassBodySequence)

            val actual = hasNewLineInClosedRange(enumEntries.first(), enumEntries.last())

            assertThat(actual).isFalse
        }

        @Test
        fun `Given a range of nodes starting with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape
                    { FOO, FOOBAR, BAR } // Malformed on purpose for test
                """.trimIndent()
            val enumClass = code.transformAst(::toEnumClassSequence)

            val actual =
                hasNewLineInClosedRange(
                    enumClass.first { it.isWhiteSpaceWithNewline() },
                    enumClass.last(),
                )

            assertThat(actual).isTrue
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

            val actual = hasNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes ending with a whitespace leaf containing a newline but other whitespace leaves not containing a newline`() {
            val code =
                """
                enum class Shape { FOO, FOOBAR, BAR
                } // Malformed on purpose for test
                """.trimIndent()
            val enumBodyClass = code.transformAst(::toEnumClassBodySequence)

            val actual =
                hasNewLineInClosedRange(
                    enumBodyClass.first(),
                    enumBodyClass.last { it.isWhiteSpaceWithNewline() },
                )

            assertThat(actual).isTrue
        }

        @Test
        fun `Given a range of nodes containing a newline inside a block comment in between`() {
            val code =
                """
                enum class Shape { FOO, /*
                newline in a block comment is ignored as it is not part of a whitespace leaf
                */ FOOBAR, BAR }
                """.trimIndent()
            val enumClassBody = code.transformAst(::toEnumClassBodySequence)

            val actual = hasNewLineInClosedRange(enumClassBody.first(), enumClassBody.last())

            assertThat(actual).isTrue
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

            assertThat(actual).isTrue
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

            assertThat(actual).isFalse
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

            assertThat(actual).isFalse
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

            assertThat(actual).isFalse
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

            assertThat(actual).isFalse
        }
    }

    @Nested
    inner class UpsertWhitespaceBeforeMe {
        @Test
        fun `Given an upsert of a whitespace based before another whitespace node then replace the existing whitespace element`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace before a non-whitespace node (RPAR) which is preceded by a whitespace leaf element (LPAR) then then replace the whitespace element`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace before a non-whitespace node (RPAR) which is preceded by another non-whitespace leaf element (LPAR) then create a new whitespace element`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace before a composite node (ANNOTATION_ENTRY) which is the first child of another composite element then create a new whitespace before (VALUE_PARAMETER)`() {
            val code =
                """
                fun foo(@Bar string: String) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(
                    @Bar string: String) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(VALUE_PARAMETER)
                            ?.findChildByType(MODIFIER_LIST)
                            ?.findChildByType(ANNOTATION_ENTRY)
                            ?.upsertWhitespaceBeforeMe("\n    ")
                    }.text

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace before a composite node (VALUE_PARAMETER) which is preceded by a non-whitespace leaf element (LPAR) then create a new whitespace element before the node (VALUE_PARAMETER)`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace before a composite node (ANNOTATION_ENTRY) which is preceded by another composite node (ANNOTATION_ENTRY) then create a new whitespace between the ANNOTATION_ENTRIES`() {
            val code =
                """
                fun foo(@Bar@Foo string: String) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(@Bar @Foo string: String) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(VALUE_PARAMETER)
                            ?.findChildByType(MODIFIER_LIST)
                            ?.findChildByType(ANNOTATION_ENTRY)
                            ?.nextSibling()
                            ?.upsertWhitespaceBeforeMe(" ")
                    }.text

            assertThat(actual).isEqualTo(formattedCode)
        }
    }

    @Nested
    inner class UpsertWhitespaceAfterMe {
        @Test
        fun `Given an upsert of a whitespace based after another whitespace node then replace the existing whitespace element`() {
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
                            ?.upsertWhitespaceAfterMe("\n")
                    }.text

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace after a non-whitespace node (LPAR) which is followed by a whitespace leaf element (RPAR) then then replace the whitespace element`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace after a non-whitespace node (LPAR) which is followed by another non-whitespace leaf element (RPAR) then create a new whitespace element`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace after a composite node (ANNOTATION_ENTRY) which is the last child of another composite element then create a new whitespace after (VALUE_PARAMETER)`() {
            val code =
                """
                fun foo(@Bar string: String) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(@Bar string: String
                ) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(VALUE_PARAMETER)
                            ?.findChildByType(TYPE_REFERENCE)
                            ?.upsertWhitespaceAfterMe("\n")
                    }.text

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace after a composite node (VALUE_PARAMETER) which is followed by a non-whitespace leaf element (RPAR) then create a new whitespace element after the node (VALUE_PARAMETER)`() {
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

            assertThat(actual).isEqualTo(formattedCode)
        }

        @Test
        fun `Given an upsert of a whitespace after a composite node (ANNOTATION_ENTRY) which is followed by another composite node (ANNOTATION_ENTRY) then create a new whitespace between the ANNOTATION_ENTRIES`() {
            val code =
                """
                fun foo(@Bar@Foo string: String) = 42
                """.trimIndent()
            val formattedCode =
                """
                fun foo(@Bar @Foo string: String) = 42
                """.trimIndent()

            val actual =
                code
                    .transformAst {
                        findChildByType(FUN)
                            ?.findChildByType(VALUE_PARAMETER_LIST)
                            ?.findChildByType(VALUE_PARAMETER)
                            ?.findChildByType(MODIFIER_LIST)
                            ?.findChildByType(ANNOTATION_ENTRY)
                            ?.upsertWhitespaceAfterMe(" ")
                    }.text

            assertThat(actual).isEqualTo(formattedCode)
        }
    }

    @Test
    fun `Given some identifiers at different indentation levels`() {
        val code =
            """
            class Foo1 {
                val foo2 = "foo2"

                fun foo3() {
                    val foo4 = "foo4"
                }
            }
            """.trimIndent()

        val actual =
            transformCodeToAST(code)
                .firstChildLeafOrSelf()
                .leaves()
                .filter { it.elementType == IDENTIFIER }
                .map { it.text to it.indent() }
                .toMap()

        assertThat(actual).contains(
            entry("Foo1", "\n"),
            entry("foo2", "\n    "),
            entry("foo3", "\n    "),
            entry("foo4", "\n        "),
        )
    }

    @Test
    fun `Given some line containing identifiers at different indentation levels then check that all leaves on those line are found`() {
        val code =
            """
            class Foo1 {
                val foo2 = "foo2"

                fun foo3() {
                    val foo4 = "foo4"
                }
            }
            """.trimIndent()

        val actual =
            transformCodeToAST(code)
                .firstChildLeafOrSelf()
                .leaves()
                .filter { it.elementType == IDENTIFIER }
                .map { identifier ->
                    identifier
                        .leavesOnLine()
                        .joinToString(separator = "") { it.text }
                }.toList()

        assertThat(actual).contains(
            "class Foo1 {",
            "\n    val foo2 = \"foo2\"",
            "\n\n    fun foo3() {",
            "\n        val foo4 = \"foo4\"",
        )
    }

    @Test
    fun `Given some lines containing identifiers at different indentation levels then get line length exclusive the leading newline characters`() {
        val code =
            """
            class Foo1 {
                val foo2 = "foo2"

                fun foo3() {
                    val foo4 = "foo4"
                }
            }
            """.trimIndent()

        val actual =
            transformCodeToAST(code)
                .firstChildLeafOrSelf()
                .leaves()
                .filter { it.elementType == IDENTIFIER }
                .map { identifier -> identifier.lineLengthWithoutNewlinePrefix() }
                .toList()

        assertThat(actual).contains(
            "class Foo1 {".length,
            "    val foo2 = \"foo2\"".length,
            "    fun foo3() {".length,
            "        val foo4 = \"foo4\"".length,
        )
    }

    @Test
    fun `Given some lines containing identifiers at different indentation levels then get line length exclusive the leading newline characters until and including the identifier`() {
        val code =
            """
            class Foo1 {
                val foo2 = "foo2"

                fun foo3() {
                    val foo4 = "foo4"
                }
            }
            """.trimIndent()

        val actual =
            transformCodeToAST(code)
                .firstChildLeafOrSelf()
                .leaves()
                .filter { it.elementType == IDENTIFIER }
                .map { identifier ->
                    identifier
                        .leavesOnLine()
                        .takeWhile { it.prevLeaf() != identifier }
                        .lineLengthWithoutNewlinePrefix()
                }.toList()

        assertThat(actual).contains(
            "class Foo1".length,
            "    val foo2".length,
            "    fun foo3".length,
            "        val foo4".length,
        )
    }

    private inline fun String.transformAst(block: FileASTNode.() -> Unit): FileASTNode =
        transformCodeToAST(this)
            .apply(block)

    private fun String.transformAst(kFunction1: KFunction1<FileASTNode, Sequence<ASTNode>>) = kFunction1(transformCodeToAST(this))

    private fun transformCodeToAST(code: String) =
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    RuleProvider { DummyRule() },
                ),
        ).transformToAst(
            Code.fromSnippet(code),
        )

    private fun toEnumClassBodySequence(fileASTNode: FileASTNode) =
        fileASTNode
            .findChildByType(CLASS)
            ?.findChildByType(CLASS_BODY)
            ?.children()
            .orEmpty()

    private fun toEnumClassSequence(fileASTNode: FileASTNode) =
        fileASTNode
            .findChildByType(CLASS)
            ?.children()
            .orEmpty()

    /**
     * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
     */
    private open class DummyRule(
        val block: (node: ASTNode) -> Unit = {},
    ) : Rule(
            ruleId = RuleId("test:dummy-rule"),
            about = About(),
        ) {
        override fun beforeVisitChildNodes(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ) {
            block(node)
        }
    }
}
