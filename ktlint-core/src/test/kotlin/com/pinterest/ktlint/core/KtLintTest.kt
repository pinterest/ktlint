package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.DummyRuleWithCustomEditorConfigProperty.Companion.SOME_CUSTOM_RULE_PROPERTY
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunOnRootNodeOnly
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.AFTER_CHILDREN
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.AFTER_LAST
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.BEFORE_CHILDREN
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.BEFORE_FIRST
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.VISIT
import com.pinterest.ktlint.core.RuleExecutionCall.VisitNodeType.CHILD
import com.pinterest.ktlint.core.RuleExecutionCall.VisitNodeType.ROOT
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_LIST
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.DisplayName
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
    }

    @DisplayName("Calls to rules defined in ktlint 0.46.x or before")
    @Nested
    inner class RuleExecutionCallsLegacy {
        @Test
        fun `Given a normal rule then execute on root node and child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleSets = listOf(
                        RuleSet(
                            "standard",
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf()
                            ),
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunAsLateAsPossible)
                            )
                        )
                    ),
                    cb = { _, _ -> }
                )
            )
            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(VISIT, ROOT, "a", FILE),
                RuleExecutionCall(VISIT, CHILD, "a", PACKAGE_DIRECTIVE),
                RuleExecutionCall(VISIT, CHILD, "a", IMPORT_LIST),
                RuleExecutionCall(VISIT, ROOT, "b", FILE),
                RuleExecutionCall(VISIT, CHILD, "b", PACKAGE_DIRECTIVE),
                RuleExecutionCall(VISIT, CHILD, "b", IMPORT_LIST)
            )
        }

        @Test
        fun `Given a run-on-root-node-only rule then execute on root node but not on child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    text = "fun main() {}",
                    ruleSets = listOf(
                        RuleSet(
                            "standard",
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(RunOnRootNodeOnly)
                            ),
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunOnRootNodeOnly, RunAsLateAsPossible)
                            )
                        )
                    ),
                    cb = { _, _ -> }
                )
            )
            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(VISIT, ROOT, "a", FILE),
                RuleExecutionCall(VISIT, ROOT, "b", FILE)
            )
        }

        @Test
        fun `Given multiple rules which have to run in a certain order`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleSets = listOf(
                        RuleSet(
                            "standard",
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "e",
                                visitorModifiers = setOf(RunAsLateAsPossible)
                            ),
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "d",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                    RunAsLateAsPossible
                                )
                            ),
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b"
                            ),
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly
                                )
                            ),
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "c"
                            )
                        )
                    ),
                    cb = { _, _ -> }
                )
            )
            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall(VISIT, ROOT, "a", FILE),
                RuleExecutionCall(VISIT, ROOT, "b", FILE),
                RuleExecutionCall(VISIT, CHILD, "b", PACKAGE_DIRECTIVE),
                RuleExecutionCall(VISIT, CHILD, "b", IMPORT_LIST),
                RuleExecutionCall(VISIT, ROOT, "c", FILE),
                RuleExecutionCall(VISIT, CHILD, "c", PACKAGE_DIRECTIVE),
                RuleExecutionCall(VISIT, CHILD, "c", IMPORT_LIST),
                RuleExecutionCall(VISIT, ROOT, "d", FILE),
                RuleExecutionCall(VISIT, ROOT, "e", FILE),
                RuleExecutionCall(VISIT, CHILD, "e", PACKAGE_DIRECTIVE),
                RuleExecutionCall(VISIT, CHILD, "e", IMPORT_LIST)
            )
        }
    }

    @DisplayName("Calls to rules defined in ktlint 0.47 and after")
    @Nested
    inner class RuleExecutionCalls {
        @Test
        fun `Given a normal rule then execute on root node and child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleSets = listOf(
                        RuleSet(
                            "standard",
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf()
                            ),
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunAsLateAsPossible)
                            )
                        )
                    ),
                    cb = { _, _ -> }
                )
            )
            assertThat(ruleExecutionCalls).containsExactly(
                // File a
                RuleExecutionCall(BEFORE_FIRST, null, "a", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "a", FILE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "a", PACKAGE_DIRECTIVE),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "a", PACKAGE_DIRECTIVE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "a", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "a", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "a", FILE),
                RuleExecutionCall(AFTER_LAST, null, "a", null),
                // File b
                RuleExecutionCall(BEFORE_FIRST, null, "b", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "b", FILE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "b", PACKAGE_DIRECTIVE),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "b", PACKAGE_DIRECTIVE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "b", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "b", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "b", FILE),
                RuleExecutionCall(AFTER_LAST, null, "b", null)
            )
        }

        @Test
        fun `Given a run-on-root-node-only rule then execute on root node but not on child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    text = "fun main() {}",
                    ruleSets = listOf(
                        RuleSet(
                            "standard",
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(RunOnRootNodeOnly)
                            ),
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunOnRootNodeOnly, RunAsLateAsPossible)
                            )
                        )
                    ),
                    cb = { _, _ -> }
                )
            )
            assertThat(ruleExecutionCalls).containsExactly(
                // File a
                RuleExecutionCall(BEFORE_FIRST, null, "a", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "a", FILE),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "a", FILE),
                RuleExecutionCall(AFTER_LAST, null, "a", null),
                // File b
                RuleExecutionCall(BEFORE_FIRST, null, "b", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "b", FILE),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "b", FILE),
                RuleExecutionCall(AFTER_LAST, null, "b", null)
            )
        }

        @Test
        fun `Given multiple rules which have to run in a certain order`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleSets = listOf(
                        RuleSet(
                            "standard",
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "e",
                                visitorModifiers = setOf(RunAsLateAsPossible)
                            ),
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "d",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                    RunAsLateAsPossible
                                )
                            ),
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b"
                            ),
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly
                                )
                            ),
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "c"
                            )
                        )
                    ),
                    cb = { _, _ -> }
                )
            )
            assertThat(ruleExecutionCalls).containsExactly(
                // File a (root only)
                RuleExecutionCall(BEFORE_FIRST, null, "a", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "a", FILE),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "a", FILE),
                RuleExecutionCall(AFTER_LAST, null, "a", null),
                // File b
                RuleExecutionCall(BEFORE_FIRST, null, "b", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "b", FILE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "b", PACKAGE_DIRECTIVE),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "b", PACKAGE_DIRECTIVE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "b", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "b", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "b", FILE),
                RuleExecutionCall(AFTER_LAST, null, "b", null),
                // File c
                RuleExecutionCall(BEFORE_FIRST, null, "c", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "c", FILE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "c", PACKAGE_DIRECTIVE),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "c", PACKAGE_DIRECTIVE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "c", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "c", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "c", FILE),
                RuleExecutionCall(AFTER_LAST, null, "c", null),
                // File d (root only)
                RuleExecutionCall(BEFORE_FIRST, null, "d", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "d", FILE),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "d", FILE),
                RuleExecutionCall(AFTER_LAST, null, "d", null),
                // File e
                RuleExecutionCall(BEFORE_FIRST, null, "e", null),
                RuleExecutionCall(BEFORE_CHILDREN, ROOT, "e", FILE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "e", PACKAGE_DIRECTIVE),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "e", PACKAGE_DIRECTIVE),
                RuleExecutionCall(BEFORE_CHILDREN, CHILD, "e", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, CHILD, "e", IMPORT_LIST),
                RuleExecutionCall(AFTER_CHILDREN, ROOT, "e", FILE),
                RuleExecutionCall(AFTER_LAST, null, "e", null)
            )
        }
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
 * Rule in style up to ktlint 0.46.x in which a rule only has to override method [Rule.visit]. For each invocation to
 * this method a [RuleExecutionCall] is added to the list of previously calls made.
 */
private class SimpleTestRuleLegacy(
    private val ruleExecutionCalls: MutableList<RuleExecutionCall>,
    id: String,
    visitorModifiers: Set<VisitorModifier> = emptySet()
) : Rule(id, visitorModifiers) {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        ruleExecutionCalls.add(RuleExecutionCall(VISIT, node.visitNodeType, id, node.elementType))
    }
}

/**
 * Rule in style starting from ktlint 0.47.x in which a rule can can override method [Rule.beforeFirstNode],
 * [Rule.beforeVisitChildNodes], [Rule.afterVisitChildNodes] and [Rule.afterLastNode]. For each invocation to
 * this method a [RuleExecutionCall] is added to the list of previously calls made.
 */
private class SimpleTestRule(
    private val ruleExecutionCalls: MutableList<RuleExecutionCall>,
    id: String,
    visitorModifiers: Set<VisitorModifier> = emptySet()
) : Rule(id, visitorModifiers) {
    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        ruleExecutionCalls.add(RuleExecutionCall(BEFORE_FIRST, null, id, null))
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        ruleExecutionCalls.add(RuleExecutionCall(BEFORE_CHILDREN, node.visitNodeType, id, node.elementType))
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        ruleExecutionCalls.add(RuleExecutionCall(VISIT, node.visitNodeType, id, node.elementType))
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        ruleExecutionCalls.add(RuleExecutionCall(AFTER_CHILDREN, node.visitNodeType, id, node.elementType))
    }

    override fun afterLastNode() {
        ruleExecutionCalls.add(RuleExecutionCall(AFTER_LAST, null, id, null))
    }
}

private data class RuleExecutionCall(
    val ruleMethod: RuleMethod,
    val visitNodeType: VisitNodeType?,
    val id: String,
    val file: IElementType?
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
