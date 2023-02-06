package com.pinterest.ktlint.rule.engine.api

import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.rule.engine.api.AutoCorrectErrorRule.Companion.STRING_VALUE_AFTER_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.api.DummyRuleWithCustomEditorConfigProperty.Companion.SOME_CUSTOM_RULE_PROPERTY_NAME
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.AFTER_CHILDREN
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.AFTER_LAST
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.BEFORE_CHILDREN
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.BEFORE_FIRST
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.RuleMethod.VISIT
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.VisitNodeType.CHILD
import com.pinterest.ktlint.rule.engine.api.RuleExecutionCall.VisitNodeType.ROOT
import com.pinterest.ktlint.ruleset.core.api.ElementType.CLASS
import com.pinterest.ktlint.ruleset.core.api.ElementType.FILE
import com.pinterest.ktlint.ruleset.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.ruleset.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.ruleset.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.ruleset.core.api.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.ruleset.core.api.Rule
import com.pinterest.ktlint.ruleset.core.api.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.ruleset.core.api.RuleId
import com.pinterest.ktlint.ruleset.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.ruleset.core.api.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KtLintTest {
    /**
     * API Consumers directly use the ktlint-core module. Tests in this module should guarantee that the API is kept
     * stable.
     */
    @Nested
    inner class `Given an API consumer` {
        @Nested
        inner class `Given that lint is invoked via the KtLintRuleEngine` {
            @Test
            fun `Given a non empty rule providers and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider {
                            DummyRule { node ->
                                if (node.isRoot()) {
                                    numberOfRootNodesVisited++
                                }
                            }
                        },
                    ),
                ).lint("fun main() {}")
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider { AutoCorrectErrorRule() },
                    ),
                ).lint(code) { e ->
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
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
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
                    ruleProviders = setOf(
                        RuleProvider {
                            DummyRule { node ->
                                if (node.isRoot()) {
                                    numberOfRootNodesVisited++
                                }
                            }
                        },
                    ),
                ).format("fun main() {}")
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "$STRING_VALUE_AFTER_AUTOCORRECT"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                val actualFormattedCode = KtLintRuleEngine(
                    ruleProviders = setOf(
                        RuleProvider { AutoCorrectErrorRule() },
                    ),
                ).format(code) { e, corrected ->
                    callbacks.add(
                        CallbackResult(
                            line = e.line,
                            col = e.col,
                            ruleId = e.ruleId,
                            detail = e.detail,
                            canBeAutoCorrected = e.canBeAutoCorrected,
                            corrected = corrected,
                        ),
                    )
                }
                assertThat(actualFormattedCode).isEqualTo(formattedCode)
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
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
            ruleProviders = setOf(
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
        ).lint(
            // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
            code = "",
        )
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
            ruleProviders = setOf(
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
        ).lint(
            // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
            code = "",
        )
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
        val code = getResourceAsText("spec/format-unicode-bom.kt.spec")

        val actual = KtLintRuleEngine(
            ruleProviders = setOf(
                RuleProvider { DummyRule() },
            ),
        ).format(code)

        assertThat(actual).isEqualTo(code)
    }

    @Nested
    inner class `Given that the traversal is stopped` {
        @Test
        fun `Given that the traversal is stopped in the beforeFirstNode hook then do no traverse the AST but do call the afterLastNode hook`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider {
                        SimpleTestRule(
                            ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                            ruleExecutionCalls = ruleExecutionCalls,
                            stopTraversalInBeforeFirstNode = true,
                        )
                    },
                ),
            ).format("class Foo")

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
                ruleProviders = setOf(
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
            ).format(code)

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
                ruleProviders = setOf(
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
            ).format(code)

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
                ruleProviders = setOf(
                    RuleProvider {
                        SimpleTestRule(
                            ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                            ruleExecutionCalls = ruleExecutionCalls,
                            stopTraversalInBeforeFirstNode = true,
                        )
                    },
                ),
            ).format("class Foo")

            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_FIRST),
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_LAST),
            )
        }
    }

    @Test
    fun `Given that format is started using the ruleProviders parameter then NO exception is thrown`() {
        /**
         * Formatting some code with the [WithStateRule] using the [KtLint.ExperimentalParams.ruleProviders] parameter
         * does not result in a [KtLintRuleException] because [KtLint.format] now is able to request a new instance of
         * the rule whenever the instance has been used before to traverse the AST.
         */
        KtLintRuleEngine(
            ruleProviders = setOf(
                RuleProvider { WithStateRule() },
            ),
        ).format(code = "")
    }

    @Test
    fun `Issue 1623 - Given a file with multiple top-level declarations then a file suppression annotation should be applied on each top level declaration`() {
        val code =
            """
            @file:Suppress("ktlint:${AutoCorrectErrorRule.RULE_ID}")
            val foo = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}" // Won't be auto corrected due to suppress annotation
            val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}" // Won't be auto corrected due to suppress annotation
            """.trimIndent()
        val actualFormattedCode =
            KtLintRuleEngine(
                ruleProviders = setOf(
                    RuleProvider { AutoCorrectErrorRule() },
                ),
            ).format(code)
        assertThat(actualFormattedCode).isEqualTo(code)
    }
}

@Deprecated("Marked for removal in KtLint 0.49 when KtLint is removed")
class KtLintLegacyTest {
    /**
     * API Consumers directly use the ktlint-core module. Tests in this module should guarantee that the API is kept
     * stable.
     */
    @Nested
    inner class ApiConsumer {
        @Nested
        inner class LintViaExperimentalParams {
            @Test
            fun `Given a non empty rule providers and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleProviders = setOf(
                            RuleProvider {
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            },
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        debug = false,
                    ),
                )
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a non empty rule providers and userData that contains one default editor config property then throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider { DummyRule() },
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file.",
                    )
            }

            @Test
            fun `Given a non empty rule providers and userData that contains multiple default editor config properties then throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider { DummyRule() },
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4",
                            ),
                            cb = { _, _ -> },
                            script = false,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file.",
                    )
            }

            @Test
            fun `Given a non empty rule providers and userData that refers to a custom Rule property then do throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider { DummyRuleWithCustomEditorConfigProperty() },
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY_NAME to "false"),
                            cb = { _, _ -> },
                            script = false,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY_NAME]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file.",
                    )
            }

            @Test
            fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        text = code,
                        ruleProviders = setOf(
                            RuleProvider { AutoCorrectErrorRule() },
                        ),
                        userData = emptyMap(),
                        cb = { e, corrected ->
                            callbacks.add(
                                CallbackResult(
                                    line = e.line,
                                    col = e.col,
                                    ruleId = e.ruleId,
                                    detail = e.detail,
                                    canBeAutoCorrected = e.canBeAutoCorrected,
                                    corrected = corrected,
                                ),
                            )
                        },
                        script = false,
                        debug = false,
                    ),
                )
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                        canBeAutoCorrected = true,
                        corrected = false,
                    ),
                )
            }
        }

        @Nested
        inner class `Format called with rule providers` {
            @Test
            fun `Given a non empty rule providers and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLint.format(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleProviders = setOf(
                            RuleProvider {
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            },
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        debug = false,
                    ),
                )
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a non empty rule providers and userData that contains one default editor config property then throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider { DummyRule() },
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file.",
                    )
            }

            @Test
            fun `Given a non empty rule providers and userData that contains multiple default editor config properties then throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider { DummyRule() },
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4",
                            ),
                            cb = { _, _ -> },
                            script = false,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file.",
                    )
            }

            @Test
            fun `Given a non empty rule providers and userData that refers to a custom Rule property then do throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider { DummyRuleWithCustomEditorConfigProperty() },
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY_NAME to "false"),
                            cb = { _, _ -> },
                            script = false,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY_NAME]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file.",
                    )
            }

            @Test
            fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "$STRING_VALUE_AFTER_AUTOCORRECT"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                val actualFormattedCode = KtLint.format(
                    KtLint.ExperimentalParams(
                        text = code,
                        ruleProviders = setOf(
                            RuleProvider { AutoCorrectErrorRule() },
                        ),
                        userData = emptyMap(),
                        cb = { e, corrected ->
                            callbacks.add(
                                CallbackResult(
                                    line = e.line,
                                    col = e.col,
                                    ruleId = e.ruleId,
                                    detail = e.detail,
                                    canBeAutoCorrected = e.canBeAutoCorrected,
                                    corrected = corrected,
                                ),
                            )
                        },
                        script = false,
                        debug = false,
                    ),
                )
                assertThat(actualFormattedCode).isEqualTo(formattedCode)
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = AutoCorrectErrorRule.RULE_ID,
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
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
        KtLint.lint(
            KtLint.ExperimentalParams(
                // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                text = "",
                ruleProviders = setOf(
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
                cb = { _, _ -> },
            ),
        )
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
        KtLint.lint(
            KtLint.ExperimentalParams(
                // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                text = "",
                ruleProviders = setOf(
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
                cb = { _, _ -> },
            ),
        )
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
        val code = getResourceAsText("spec/format-unicode-bom.kt.spec")

        val actual = KtLint.format(
            KtLint.ExperimentalParams(
                text = code,
                ruleProviders = setOf(
                    RuleProvider { DummyRule() },
                ),
                cb = { _, _ -> },
            ),
        )

        assertThat(actual).isEqualTo(code)
    }

    @Nested
    inner class StopTraversal {
        @Test
        fun `Given that the traversal is stopped in the beforeFirstNode hook then do no traverse the AST but do call the afterLastNode hook`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = "class Foo",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeFirstNode = true,
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )

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
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleProviders = setOf(
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
                    cb = { _, _ -> },
                ),
            )

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
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleProviders = setOf(
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
                    cb = { _, _ -> },
                ),
            )

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
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = "class Foo",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleId = SimpleTestRule.RULE_ID_STOP_TRAVERSAL,
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeFirstNode = true,
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )

            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, BEFORE_FIRST),
                RuleExecutionCall(SimpleTestRule.RULE_ID_STOP_TRAVERSAL, AFTER_LAST),
            )
        }
    }

    @Test
    fun `Given that format is started using the ruleProviders parameter then NO exception is thrown`() {
        /**
         * Formatting some code with the [WithStateRule] using the [KtLint.ExperimentalParams.ruleProviders] parameter
         * does not result in a [KtLintRuleException] because [KtLint.format] now is able to request a new instance of
         * the rule whenever the instance has been used before to traverse the AST.
         */
        KtLint.format(
            KtLint.ExperimentalParams(
                text = "",
                ruleProviders = setOf(
                    RuleProvider { WithStateRule() },
                ),
                cb = { _, _ -> },
            ),
        )
    }

    @Test
    fun `Issue 1623 - Given a file with multiple top-level declarations then a file suppression annotation should be applied on each top level declaration`() {
        val code =
            """
            @file:Suppress("ktlint:${AutoCorrectErrorRule.RULE_ID}")
            val foo = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}" // Won't be auto corrected due to suppress annotation
            val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}" // Won't be auto corrected due to suppress annotation
            """.trimIndent()
        val actualFormattedCode = KtLint.format(
            KtLint.ExperimentalParams(
                text = code,
                ruleProviders = setOf(
                    RuleProvider { AutoCorrectErrorRule() },
                ),
                userData = emptyMap(),
                cb = { _, _ -> },
                script = false,
                debug = false,
            ),
        )
        assertThat(actualFormattedCode).isEqualTo(code)
    }
}

private class DummyRuleWithCustomEditorConfigProperty :
    Rule(RuleId("test:dummy-rule-with-custom-editor-config-property")),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<EditorConfigProperty<*>> =
        listOf(SOME_CUSTOM_RULE_PROPERTY)

    companion object {
        const val SOME_CUSTOM_RULE_PROPERTY_NAME = "some-custom-rule-property"

        val SOME_CUSTOM_RULE_PROPERTY: EditorConfigProperty<Boolean> =
            EditorConfigProperty(
                name = SOME_CUSTOM_RULE_PROPERTY_NAME,
                type = PropertyType.LowerCasingPropertyType(
                    SOME_CUSTOM_RULE_PROPERTY_NAME,
                    "some-custom-rule-property-description",
                    PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    setOf(true.toString(), false.toString()),
                ),
                defaultValue = false,
            )
    }
}

/**
 * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
 */
private open class DummyRule(
    val block: (node: ASTNode) -> Unit = {},
) : Rule(RuleId("test:dummy")) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        block(node)
    }
}

/**
 * A dummy rule for testing
 */
private class AutoCorrectErrorRule : Rule(RuleId("test:auto-correct")) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == REGULAR_STRING_PART) {
            when (node.text) {
                STRING_VALUE_TO_BE_AUTOCORRECTED -> {
                    emit(node.startOffset, ERROR_MESSAGE_CAN_BE_AUTOCORRECTED, true)
                    if (autoCorrect) {
                        (node as LeafElement).replaceWithText(STRING_VALUE_AFTER_AUTOCORRECT)
                    }
                }
                STRING_VALUE_NOT_TO_BE_CORRECTED ->
                    emit(node.startOffset, ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED, false)
            }
        }
    }

    companion object {
        const val RULE_ID = "test:auto-correct"
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
) : Rule(ruleId, visitorModifiers) {
    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        ruleExecutionCalls.add(RuleExecutionCall(ruleId, BEFORE_FIRST))
        if (stopTraversalInBeforeFirstNode) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        ruleExecutionCalls.add(node.toRuleExecutionCall(ruleId, BEFORE_CHILDREN))
        if (stopTraversalInBeforeVisitChildNodes(node)) {
            stopTraversalOfAST()
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
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
    enum class RuleMethod { BEFORE_FIRST, BEFORE_CHILDREN, VISIT, AFTER_CHILDREN, AFTER_LAST }
    enum class VisitNodeType { ROOT, CHILD }
}

private val ASTNode.visitNodeType: RuleExecutionCall.VisitNodeType
    get() =
        if (isRoot()) {
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
    val ruleId: String,
    val detail: String,
    val canBeAutoCorrected: Boolean,
    val corrected: Boolean,
)

/**
 * This rule throws an exception when it is visited more than once.
 */
private class WithStateRule : Rule(RuleId("test:with-state")) {
    private var hasNotBeenVisitedYet = true

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        check(hasNotBeenVisitedYet) {
            "Rule has been visited before"
        }
        hasNotBeenVisitedYet = false
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(node.startOffset, "Fake violation which can be autocorrected", true)
    }
}
