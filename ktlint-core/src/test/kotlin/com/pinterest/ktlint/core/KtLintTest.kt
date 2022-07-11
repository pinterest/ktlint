package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.AutoCorrectErrorRule.Companion.STRING_VALUE_AFTER_AUTOCORRECT
import com.pinterest.ktlint.core.DummyRuleWithCustomEditorConfigProperty.Companion.SOME_CUSTOM_RULE_PROPERTY
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunOnRootNodeOnly
import com.pinterest.ktlint.core.VisitedNode.VisitNodeType.CHILD
import com.pinterest.ktlint.core.VisitedNode.VisitNodeType.ROOT
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KtLintTest {
    /**
     * API Consumers directly use the ktlint-core module. Tests in this module should guarantee that the API is kept
     * stable.
     */
    @Nested
    inner class ApiConsumer {
        @Nested
        inner class LintViaExperimentalParams {
            @Test
            fun `Given that an empty ruleSet is provided than throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("No runnable rules found. Please ensure that at least one is enabled.")
            }

            @Test
            fun `Given a non empty ruleset and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            )
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a non empty ruleset and userData that contains one default editor config property then throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that contains multiple default editor config properties then throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4"
                            ),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that refers to a custom Rule property then do throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRuleWithCustomEditorConfigProperty())
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        text = code,
                        ruleSets = listOf(
                            RuleSet("standard", AutoCorrectErrorRule())
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
                                    corrected = corrected
                                )
                            )
                        },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = "auto-correct",
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = "auto-correct",
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                        canBeAutoCorrected = true,
                        corrected = false
                    )
                )
            }
        }

        @Nested
        inner class FormatViaExperimentalParams {
            @Test
            fun `Given that an empty ruleSet is provided than throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("No runnable rules found. Please ensure that at least one is enabled.")
            }

            @Test
            fun `Given a non empty ruleset and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLint.format(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            )
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a non empty ruleset and userData that contains one default editor config property then throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that contains multiple default editor config properties then throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4"
                            ),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that refers to a custom Rule property then do throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRuleWithCustomEditorConfigProperty())
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }
        }

        @Test
        fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
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
                    ruleSets = listOf(
                        RuleSet("standard", AutoCorrectErrorRule())
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
                                corrected = corrected
                            )
                        )
                    },
                    script = false,
                    editorConfigPath = null,
                    debug = false
                )
            )
            assertThat(actualFormattedCode).isEqualTo(formattedCode)
            assertThat(callbacks).containsExactly(
                CallbackResult(
                    line = 1,
                    col = 12,
                    ruleId = "auto-correct",
                    detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                    canBeAutoCorrected = false,
                    corrected = false
                ),
                CallbackResult(
                    line = 2,
                    col = 12,
                    ruleId = "auto-correct",
                    detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                    canBeAutoCorrected = true,
                    corrected = true
                )
            )
        }
    }

    @Test
    fun `Given a normal rule then execute on root node and child nodes`() {
        val visitedNodes = mutableListOf<VisitedNode>()
        KtLint.lint(
            KtLint.ExperimentalParams(
                text = "fun main() {}",
                ruleSets = listOf(
                    RuleSet(
                        "standard",
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "a",
                            visitorModifiers = setOf()
                        ),
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "b",
                            visitorModifiers = setOf(RunAsLateAsPossible)
                        )
                    )
                ),
                cb = { _, _ -> }
            )
        )
        assertThat(visitedNodes).containsExactly(
            VisitedNode(ROOT, "a"),
            VisitedNode(CHILD, "a"),
            VisitedNode(ROOT, "b"),
            VisitedNode(CHILD, "b")
        )
    }

    @Test
    fun `Given a run-on-root-node-only rule then execute on root node but not on child nodes`() {
        val visitedNodes = mutableListOf<VisitedNode>()
        KtLint.lint(
            KtLint.ExperimentalParams(
                text = "fun main() {}",
                ruleSets = listOf(
                    RuleSet(
                        "standard",
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "a",
                            visitorModifiers = setOf(RunOnRootNodeOnly)
                        ),
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "b",
                            visitorModifiers = setOf(RunOnRootNodeOnly, RunAsLateAsPossible)
                        )
                    )
                ),
                cb = { _, _ -> }
            )
        )
        assertThat(visitedNodes).containsExactly(
            VisitedNode(ROOT, "a"),
            VisitedNode(ROOT, "b")
        )
    }

    @Test
    fun `Given multiple rules which have to run in a certain order`() {
        val visitedNodes = mutableListOf<VisitedNode>()
        KtLint.lint(
            KtLint.ExperimentalParams(
                text = "fun main() {}",
                ruleSets = listOf(
                    RuleSet(
                        "standard",
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "e",
                            visitorModifiers = setOf(RunAsLateAsPossible)
                        ),
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "d",
                            visitorModifiers = setOf(
                                RunOnRootNodeOnly,
                                RunAsLateAsPossible
                            )
                        ),
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "b"
                        ),
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "a",
                            visitorModifiers = setOf(
                                RunOnRootNodeOnly
                            )
                        ),
                        SimpleTestRule(
                            visitedNodes = visitedNodes,
                            id = "c"
                        )
                    )
                ),
                cb = { _, _ -> }
            )
        )
        assertThat(visitedNodes).containsExactly(
            VisitedNode(ROOT, "a"),
            VisitedNode(ROOT, "b"),
            VisitedNode(CHILD, "b"),
            VisitedNode(ROOT, "c"),
            VisitedNode(CHILD, "c"),
            VisitedNode(ROOT, "d"),
            VisitedNode(ROOT, "e"),
            VisitedNode(CHILD, "e")
        )
    }

    @Test
    fun testFormatUnicodeBom() {
        val code = getResourceAsText("spec/format-unicode-bom.kt.spec")

        val actual = KtLint.format(
            KtLint.ExperimentalParams(
                text = code,
                ruleSets = listOf(
                    RuleSet("standard", DummyRule())
                ),
                cb = { _, _ -> }
            )
        )

        assertThat(actual).isEqualTo(code)
    }
}

private class DummyRuleWithCustomEditorConfigProperty :
    Rule("dummy-rule-with-custom-editor-config-property"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(someCustomRuleProperty)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
    }

    companion object {
        const val SOME_CUSTOM_RULE_PROPERTY = "some-custom-rule-property"

        val someCustomRuleProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    SOME_CUSTOM_RULE_PROPERTY,
                    "some-custom-rule-property-description",
                    PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    setOf(true.toString(), false.toString())
                ),
                defaultValue = false
            )
    }
}

/**
 * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
 */
private open class DummyRule(
    val block: (node: ASTNode) -> Unit = {}
) : Rule("dummy-rule") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        block(node)
    }
}

/**
 * A dummy rule for testing
 */
private class AutoCorrectErrorRule : Rule("auto-correct") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
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
        const val STRING_VALUE_TO_BE_AUTOCORRECTED = "string-value-to-be-autocorrected"
        const val STRING_VALUE_NOT_TO_BE_CORRECTED = "string-value-not-to-be-corrected"
        const val STRING_VALUE_AFTER_AUTOCORRECT = "string-value-after-autocorrect"
        const val ERROR_MESSAGE_CAN_BE_AUTOCORRECTED = "This string value is not allowed and can be autocorrected"
        const val ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED = "This string value is not allowed but can not be autocorrected"
    }
}

/**
 * Collects a maximum of two placeholders for a code sample. The first placeholder represent the root node of the code
 * sample and is returned as "root:<id>". The placeholder represents *all* other nodes of the code sample and is
 * represented as "<id>".
 */
private open class SimpleTestRule(
    private val visitedNodes: MutableList<VisitedNode>,
    id: String,
    visitorModifiers: Set<VisitorModifier> = emptySet()
) : Rule(id, visitorModifiers) {
    private var done = false
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            visitedNodes.add(VisitedNode(ROOT, id))
        } else if (!done) {
            visitedNodes.add(VisitedNode(CHILD, id))
            done = true
        }
    }
}

private data class VisitedNode(val visitNodeType: VisitNodeType, val id: String) {
    enum class VisitNodeType { ROOT, CHILD }
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
    val corrected: Boolean
)
