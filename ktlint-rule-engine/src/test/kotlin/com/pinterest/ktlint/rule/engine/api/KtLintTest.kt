package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.AUTOCORRECT_ERROR_RULE_ID
import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED
import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED
import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.STRING_VALUE_AFTER_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.STRING_VALUE_NOT_TO_BE_CORRECTED
import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.STRING_VALUE_TO_BE_AUTOCORRECTED
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.AFTER_CHILDREN
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.AFTER_LAST
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.BEFORE_CHILDREN
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.BEFORE_FIRST
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.VisitNodeType.CHILD
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.VisitNodeType.ROOT
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.END_OF_LINE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isRoot20
import com.pinterest.ktlint.rule.engine.core.api.replaceTextWith
import org.assertj.core.api.Assertions.assertThat
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KtLintTest {
    /**
     * API Consumers directly use the ktlint-rule-engine module. Tests in this module should guarantee that the API is kept stable.
     */
    @Nested
    inner class `Given an API consumer` {
        @Nested
        inner class `Given that lint is invoked via the KtLintRuleEngine` {
            @Test
            fun `Given a non empty rule providers and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider {
                                DummyRule { node ->
                                    if (node.isRoot20) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            },
                        ),
                ).lint(Code.fromSnippet("fun main() {}"))
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "$STRING_VALUE_NOT_TO_BE_CORRECTED"
                    val bar = "$STRING_VALUE_TO_BE_AUTOCORRECTED"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider { AutoCorrectErrorRule() },
                        ),
                ).lint(Code.fromSnippet(code)) { e ->
                    callbacks.add(
                        CallbackResult(
                            line = e.line,
                            col = e.col,
                            ruleId = e.ruleId,
                            detail = e.detail,
                            canBeAutoCorrected = e.canBeAutoCorrected,
                            corrected = false,
                        ),
                    )
                }
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = AUTOCORRECT_ERROR_RULE_ID,
                        detail = ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = AUTOCORRECT_ERROR_RULE_ID,
                        detail = ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                        canBeAutoCorrected = true,
                        corrected = false,
                    ),
                )
            }
        }

        @Nested
        inner class `Given that format is invoked via the KtLintRuleEngine` {
            @Test
            fun `Given a non empty rule providers and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLintRuleEngine(
                    ruleProviders =
                        setOf(
                            RuleProvider {
                                DummyRule { node ->
                                    if (node.isRoot20) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            },
                        ),
                ).format(Code.fromSnippet("fun main() {}")) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "$STRING_VALUE_NOT_TO_BE_CORRECTED"
                    val bar = "$STRING_VALUE_TO_BE_AUTOCORRECTED"
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo = "$STRING_VALUE_NOT_TO_BE_CORRECTED"
                    val bar = "$STRING_VALUE_AFTER_AUTOCORRECT"
                    """.trimIndent()
                val callbacks = mutableSetOf<CallbackResult>()
                val actualFormattedCode =
                    KtLintRuleEngine(
                        ruleProviders =
                            setOf(
                                RuleProvider { AutoCorrectErrorRule() },
                            ),
                    ).format(Code.fromSnippet(code)) { e ->
                        callbacks.add(
                            CallbackResult(
                                line = e.line,
                                col = e.col,
                                ruleId = e.ruleId,
                                detail = e.detail,
                                canBeAutoCorrected = e.canBeAutoCorrected,
                                corrected = e.canBeAutoCorrected,
                            ),
                        )
                        if (e.canBeAutoCorrected) {
                            AutocorrectDecision.ALLOW_AUTOCORRECT
                        } else {
                            AutocorrectDecision.NO_AUTOCORRECT
                        }
                    }
                assertThat(actualFormattedCode).isEqualTo(formattedCode)
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = AUTOCORRECT_ERROR_RULE_ID,
                        detail = ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = AUTOCORRECT_ERROR_RULE_ID,
                        detail = ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                        canBeAutoCorrected = true,
                        corrected = true,
                    ),
                )
            }
        }
    }

    @Test
    fun `Given a normal rule then execute on root node and child nodes`() {
        val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    RuleProvider {
                        SimpleTestRule(
                            ruleExecutionCalls = ruleExecutionCalls,
                            ruleId = SimpleTestRule.RULE_ID_A,
                            visitorModifiers = setOf(),
                        )
                    },
                    RuleProvider {
                        SimpleTestRule(
                            ruleExecutionCalls = ruleExecutionCalls,
                            ruleId = SimpleTestRule.RULE_ID_B,
                            visitorModifiers = setOf(RunAsLateAsPossible),
                        )
                    },
                ),
        ).lint(EMPTY_CODE_SNIPPET)
        assertThat(ruleExecutionCalls).containsExactly(
            // File a
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, BEFORE_FIRST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, BEFORE_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, BEFORE_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, AFTER_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, AFTER_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_A, AFTER_LAST),
            // File b
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_FIRST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_LAST),
        )
    }

    @Test
    fun `Given multiple rules which have to run in a certain order`() {
        val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    RuleProvider {
                        SimpleTestRule(
                            ruleExecutionCalls = ruleExecutionCalls,
                            ruleId = SimpleTestRule.RULE_ID_D,
                            visitorModifiers = setOf(RunAsLateAsPossible),
                        )
                    },
                    RuleProvider {
                        SimpleTestRule(
                            ruleExecutionCalls = ruleExecutionCalls,
                            ruleId = SimpleTestRule.RULE_ID_B,
                        )
                    },
                    RuleProvider {
                        SimpleTestRule(
                            ruleExecutionCalls = ruleExecutionCalls,
                            ruleId = SimpleTestRule.RULE_ID_C,
                        )
                    },
                ),
        ).lint(EMPTY_CODE_SNIPPET)

        assertThat(ruleExecutionCalls).containsExactly(
            // File b
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_FIRST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, BEFORE_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_B, AFTER_LAST),
            // File c
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, BEFORE_FIRST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, BEFORE_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, BEFORE_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, AFTER_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, AFTER_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_C, AFTER_LAST),
            // File d
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, BEFORE_FIRST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, BEFORE_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, BEFORE_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, AFTER_CHILDREN, CHILD, IMPORT_LIST),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, AFTER_CHILDREN, ROOT, FILE),
            RuleExecutionCall(SimpleTestRule.RULE_ID_D, AFTER_LAST),
        )
    }

    @Test
    fun testFormatUnicodeBom() {
        val code =
            getResourceAsText("spec/format-unicode-bom.kt.spec")
                // Standardize code to use LF as line separator regardless of OS
                .replace("\r\n", "\n")
                .replace("\r", "\n")

        val actual =
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider { DummyRule() },
                    ),
                editorConfigOverride =
                    EditorConfigOverride.from(
                        // The code sample use LF as line separator, so ensure that formatted code uses that as well, as otherwise the test
                        // breaks on Windows OS
                        END_OF_LINE_PROPERTY to PropertyType.EndOfLineValue.lf,
                    ),
            ).format(Code.fromSnippet(code)) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }

        assertThat(actual).isEqualTo(code)
    }

    @Nested
    inner class `Given that the traversal is stopped` {
        @Test
        fun `Given that the traversal is stopped in the beforeFirstNode hook then do no traverse the AST but do call the afterLastNode hook`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeFirstNode = true,
                            )
                        },
                    ),
            ).format(Code.fromSnippet("class Foo")) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }

            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_FIRST),
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_LAST),
            )
        }

        @Test
        fun `Given that the traversal is stopped in the beforeVisitChildNodes when encountering the class with name Foo then classes InsideFoo and AfterFoo are not traversed`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            val code =
                """
                class FooBar {
                    class Foo {
                        class InsideFoo // Won't be visited as traversal is stopped when entering class Foo
                    }

                    class AfterFoo // Won't be visited as traversal is stopped when entering class Foo
                }
                """.trimIndent()
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeVisitChildNodes = { node ->
                                    // Stop when Class Foo has been entered
                                    node.elementType == CLASS && node.findChildByType(IDENTIFIER)?.text == "Foo"
                                },
                            )
                        },
                    ),
            ).format(Code.fromSnippet(code)) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }

            assertThat(ruleExecutionCalls)
                .filteredOn { it.elementType == null || it.classIdentifier != null }
                .containsExactly(
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_FIRST),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_LAST),
                )
        }

        @Test
        fun `Given that the traversal is stopped in the afterVisitChildNodes when encountering the class with name Foo then class AfterFoo is not traversed`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            val code =
                """
                class FooBar {
                    class Foo {
                        class InsideFoo // Is visited as traversal is stopped when after leaving class Foo
                    }

                    class AfterFoo // Won't be visited as traversal is stopped when entering class Foo
                }
                """.trimIndent()
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInAfterVisitChildNodes = { node ->
                                    // Stop when Class Foo has been visited
                                    node.elementType == CLASS && node.findChildByType(IDENTIFIER)?.text == "Foo"
                                },
                            )
                        },
                    ),
            ).format(Code.fromSnippet(code)) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }

            assertThat(ruleExecutionCalls)
                .filteredOn { it.elementType == null || it.classIdentifier != null }
                .containsExactly(
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_FIRST),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_CHILDREN, CHILD, CLASS, "InsideFoo"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_CHILDREN, CHILD, CLASS, "InsideFoo"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_LAST),
                )
        }

        @Test
        fun `Given that the traversal is stopped in the afterLastNode hook then do nothing special as traversal is already stopped`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeFirstNode = true,
                            )
                        },
                    ),
            ).format(Code.fromSnippet("class Foo")) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }

            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_FIRST),
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_LAST),
            )
        }
    }

    @Test
    fun `Given that format is started using the ruleProviders parameter then NO exception is thrown`() {
        /*
         * Formatting some code with the [WithStateRule] does not result in a [KtLintRuleException] because [KtLintRuleEngine.format] is
         * able to request a new instance of the rule whenever the instance has been used before to traverse the AST.
         */
        KtLintRuleEngine(
            ruleProviders =
                setOf(
                    RuleProvider { WithStateRule() },
                ),
        ).format(EMPTY_CODE_SNIPPET) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }
    }

    @Test
    fun `Issue 1623 - Given a file with multiple top-level declarations then a file suppression annotation should be applied on each top level declaration`() {
        val code =
            """
            @file:Suppress("ktlint:${AUTOCORRECT_ERROR_RULE_ID.value}")
            val foo = "$STRING_VALUE_TO_BE_AUTOCORRECTED" // Won't be auto corrected due to suppress annotation
            val bar = "$STRING_VALUE_TO_BE_AUTOCORRECTED" // Won't be auto corrected due to suppress annotation
            """.trimIndent()
        val actualFormattedCode =
            KtLintRuleEngine(
                ruleProviders =
                    setOf(
                        RuleProvider { AutoCorrectErrorRule() },
                    ),
            ).format(Code.fromSnippet(code)) { _ -> AutocorrectDecision.ALLOW_AUTOCORRECT }
        assertThat(actualFormattedCode).isEqualTo(code)
    }

    private companion object {
        val EMPTY_CODE_SNIPPET =
            Code.fromSnippet(
                // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                "",
            )
    }
}

/**
 * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
 */
private open class DummyRule(
    val block: (node: ASTNode) -> Unit = {},
) : Rule(
        ruleId = RuleId("test:dummy"),
        about = About(),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        block(node)
    }
}

/**
 * A dummy rule for testing
 */
private class AutoCorrectErrorRule :
    Rule(
        ruleId = AUTOCORRECT_ERROR_RULE_ID,
        about = About(),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == REGULAR_STRING_PART) {
            when (node.text) {
                STRING_VALUE_TO_BE_AUTOCORRECTED -> {
                    emit(node.startOffset, ERROR_MESSAGE_CAN_BE_AUTOCORRECTED, true)
                        .ifAutocorrectAllowed { node.replaceTextWith(STRING_VALUE_AFTER_AUTOCORRECT) }
                }

                STRING_VALUE_NOT_TO_BE_CORRECTED -> {
                    emit(node.startOffset, ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED, false)
                }
            }
        }
    }

    companion object {
        val AUTOCORRECT_ERROR_RULE_ID = RuleId("test:auto-correct")
        const val STRING_VALUE_TO_BE_AUTOCORRECTED = "string-value-to-be-autocorrected"
        const val STRING_VALUE_NOT_TO_BE_CORRECTED = "string-value-not-to-be-corrected"
        const val STRING_VALUE_AFTER_AUTOCORRECT = "string-value-after-autocorrect"
        const val ERROR_MESSAGE_CAN_BE_AUTOCORRECTED = "This string value is not allowed and can be autocorrected"
        const val ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED =
            "This string value is not allowed but can not be autocorrected"
    }
}

/**
 * Rule in style starting from ktlint 0.47.x in which a rule can can override method [Rule.beforeFirstNode],
 * [Rule.beforeVisitChildNodes], [Rule.afterVisitChildNodes] and [Rule.afterLastNode]. For each invocation to
 * this method a [RuleExecutionCall] is added to the list of previously calls made.
 */
private class SimpleTestRule(
    private val ruleExecutionCalls: MutableList<RuleExecutionCall>,
    ruleId: RuleId,
    visitorModifiers: Set<VisitorModifier> = emptySet(),
    private val stopTraversalInBeforeFirstNode: Boolean = false,
    private val stopTraversalInBeforeVisitChildNodes: (ASTNode) -> Boolean = { false },
    private val stopTraversalInAfterVisitChildNodes: (ASTNode) -> Boolean = { false },
    private val stopTraversalInAfterLastNode: Boolean = false,
) : Rule(
        ruleId = ruleId,
        about = About(),
        visitorModifiers,
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeFirstNode(editorConfig: EditorConfig) {
        ruleExecutionCalls.add(RuleExecutionCall(ruleId, BEFORE_FIRST))
        if (stopTraversalInBeforeFirstNode) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        ruleExecutionCalls.add(node.toRuleExecutionCall(ruleId, BEFORE_CHILDREN))
        if (stopTraversalInBeforeVisitChildNodes(node)) {
            stopTraversalOfAST()
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        ruleExecutionCalls.add(node.toRuleExecutionCall(ruleId, AFTER_CHILDREN))
        if (stopTraversalInAfterVisitChildNodes(node)) {
            stopTraversalOfAST()
        }
    }

    override fun afterLastNode() {
        ruleExecutionCalls.add(RuleExecutionCall(ruleId, AFTER_LAST))
        if (stopTraversalInAfterLastNode) {
            stopTraversalOfAST()
        }
    }

    private fun ASTNode.toRuleExecutionCall(
        ruleId: RuleId,
        ruleMethod: RuleExecutionCall.RuleMethod,
    ) = RuleExecutionCall(
        ruleId,
        ruleMethod,
        visitNodeType,
        elementType,
        if (elementType == CLASS) {
            findChildByType(IDENTIFIER)?.text
        } else {
            null
        },
    )

    companion object {
        val RULE_ID_A = RuleId("simple-test:a")
        val RULE_ID_B = RuleId("simple-test:b")
        val RULE_ID_C = RuleId("simple-test:c")
        val RULE_ID_D = RuleId("simple-test:d")
        val RULE_ID_STOP_TRAVERSAL = RuleId("simple-test:stop-traversal")
    }
}

private data class RuleExecutionCall(
    val ruleId: RuleId,
    val ruleMethod: RuleMethod,
    val visitNodeType: VisitNodeType? = null,
    val elementType: IElementType? = null,
    val classIdentifier: String? = null,
) {
    enum class RuleMethod { BEFORE_FIRST, BEFORE_CHILDREN, AFTER_CHILDREN, AFTER_LAST }

    enum class VisitNodeType { ROOT, CHILD }
}

private val ASTNode.visitNodeType: RuleExecutionCall.VisitNodeType
    get() =
        if (isRoot20) {
            ROOT
        } else {
            CHILD
        }

private fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()

private data class CallbackResult(
    val line: Int,
    val col: Int,
    val ruleId: RuleId,
    val detail: String,
    val canBeAutoCorrected: Boolean,
    val corrected: Boolean,
)

/**
 * This rule throws an exception when it is visited more than once.
 */
private class WithStateRule :
    Rule(
        ruleId = RuleId("test:with-state"),
        about = About(),
    ),
    RuleAutocorrectApproveHandler {
    private var hasNotBeenVisitedYet = true

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        check(hasNotBeenVisitedYet) {
            "Rule has been visited before"
        }
        hasNotBeenVisitedYet = false
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(node.startOffset, "Fake violation which can be autocorrected", true)
    }
}
