package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_LIST
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.internal.VisitorProvider
import com.pinterest.ktlint.core.internal.initPsiFileFactory
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test

class VisitorProviderTest {
    @Test
    fun `A normal rule visits all nodes`() {
        val actual = testVisitorProvider(
            NormalRule(NORMAL_RULE)
        )

        assertThat(actual).containsExactly(
            Visit(NORMAL_RULE, FILE),
            Visit(NORMAL_RULE, PACKAGE_DIRECTIVE),
            Visit(NORMAL_RULE, IMPORT_LIST)
        )
    }

    @Test
    fun `A root only rule only visits the FILE node only`() {
        val actual = testVisitorProvider(
            RootNodeOnlyRule(ROOT_NODE_ONLY_RULE)
        )

        assertThat(actual).containsExactly(
            Visit(ROOT_NODE_ONLY_RULE)
        )
    }

    @Test
    fun `A run as late as possible rule visits all nodes`() {
        val actual = testVisitorProvider(
            RunAsLateAsPossibleRule(RUN_AS_LATE_AS_POSSIBLE_RULE)
        )

        assertThat(actual).containsExactly(
            Visit(RUN_AS_LATE_AS_POSSIBLE_RULE, FILE),
            Visit(RUN_AS_LATE_AS_POSSIBLE_RULE, PACKAGE_DIRECTIVE),
            Visit(RUN_AS_LATE_AS_POSSIBLE_RULE, IMPORT_LIST)
        )
    }

    @Test
    fun `A run as late as possible on root node only rule visits the root node only`() {
        val actual = testVisitorProvider(
            RunAsLateAsPossibleOnRootNodeOnlyRule(RUN_AS_LATE_AS_POSSIBLE_RULE)
        )

        assertThat(actual).containsExactly(
            Visit(RUN_AS_LATE_AS_POSSIBLE_RULE, FILE)
        )
    }

    @Test
    fun `Disabled rules in any type of rule set are not executed`() {
        val actual = testVisitorProvider(
            RuleSet(
                STANDARD,
                NormalRule(RULE_A),
                NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET)
            ),
            RuleSet(
                EXPERIMENTAL,
                NormalRule(RULE_B),
                NormalRule(SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET)
            ),
            RuleSet(
                CUSTOM_RULE_SET_A,
                NormalRule(RULE_C),
                NormalRule(SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A)
            )
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(EXPERIMENTAL, RULE_B),
            Visit(CUSTOM_RULE_SET_A, RULE_C)
        )
    }

    @Test
    fun `When no enabled rules are found for the root node, the visit function on the root node is not executed`() {
        val actual = testVisitorProvider(
            RuleSet(
                STANDARD,
                NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET)
            )
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `When no runnable rules are found for the root node, the visit function on the root node is not executed`() {
        val actual = testVisitorProvider(
            NormalRule(SOME_DISABLED_RULE_IN_STANDARD_RULE_SET),
            object : R(
                id = RULE_A,
                visitorModifier = VisitorModifier.RunAfterRule(
                    ruleId = SOME_DISABLED_RULE_IN_STANDARD_RULE_SET,
                    runOnlyWhenOtherRuleIsEnabled = true
                )
            ) {}
        )

        assertThat(actual).isNull()
    }

    @Test
    fun `Visits all rules on a node concurrently before proceeding to the next node`() {
        val actual = testVisitorProvider(
            NormalRule(RULE_A),
            NormalRule(RULE_B),
            concurrent = true
        )

        assertThat(actual).containsExactly(
            Visit(RULE_A, FILE),
            Visit(RULE_B, FILE),
            Visit(RULE_A, PACKAGE_DIRECTIVE),
            Visit(RULE_B, PACKAGE_DIRECTIVE),
            Visit(RULE_A, IMPORT_LIST),
            Visit(RULE_B, IMPORT_LIST)
        )
    }

    /**
     * Create a visitor provider for a given list of rules in the same rule set (STANDARD). It returns a list of visits
     * that the provider made after it was invoked. The tests of the visitor provider should only focus on whether the
     * visit provider has invoked the correct rules in the correct order. Note that the testProvider does not invoke the
     * real visit method of the rule.
     */
    private fun testVisitorProvider(
        vararg rules: Rule,
        concurrent: Boolean? = null
    ): MutableList<Visit>? {
        return testVisitorProvider(
            RuleSet(
                STANDARD,
                *rules
            ),
            concurrent = concurrent
        )
    }

    /**
     * Create a visitor provider for a given list of rule sets. It returns a list of visits that the provider made
     * after it was invoked. The tests of the visitor provider should only focus on whether the visit provider has
     * invoked the correct rules in the correct order. Note that the testProvider does not invoke the real visit method
     * of the rule.
     */
    private fun testVisitorProvider(
        vararg ruleSets: RuleSet,
        concurrent: Boolean? = null
    ): MutableList<Visit>? {
        val ruleSetList = ruleSets.toList()
        return VisitorProvider(
            ruleSets = ruleSetList,
            // Enable debug mode as it is helpful when a test fails
            debug = true,
            // Creates a new VisitorProviderFactory for each unit test to prevent that tests for the exact same set of
            // ruleIds are influencing each other.
            recreateRuleSorter = true
        ).run {
            var visits: MutableList<Visit>? = null
            visitor(ruleSetList, SOME_ROOT_AST_NODE, concurrent ?: false)
                .invoke { node, _, fqRuleId ->
                    if (visits == null) {
                        visits = mutableListOf()
                    }
                    visits?.add(Visit(fqRuleId, node.elementType))
                }
            visits
        }
    }

    /**
     * When visiting a node with a normal rule, this results in multiple visits. In most tests this would bloat the
     * assertion needlessly.
     */
    private fun List<Visit>?.filterFileNodes(): List<Visit>? =
        this?.filter { it.elementType == FILE }

    private companion object {
        const val STANDARD = "standard"
        const val EXPERIMENTAL = "experimental"
        const val CUSTOM_RULE_SET_A = "custom-rule-set-a"
        val SOME_ROOT_AST_NODE = initRootAstNode()
        const val NORMAL_RULE = "normal-rule"
        const val ROOT_NODE_ONLY_RULE = "root-node-only-rule"
        const val RUN_AS_LATE_AS_POSSIBLE_RULE = "run-as-late-as-possible-rule"
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val SOME_DISABLED_RULE_IN_STANDARD_RULE_SET = "some-disabled-rule-in-standard-rule-set"
        const val SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET = "some-disabled-rule-in-experimental-rule-set"
        const val SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A = "some-disabled-rule-custom-rule-set-a"

        fun initRootAstNode(): ASTNode {
            initPsiFileFactory(false).apply {
                val psiFile = createFileFromText(
                    "unit-test.kt",
                    KotlinLanguage.INSTANCE,
                    // An empty file results in three ASTNodes which are all empty:
                    //   - kotlin.FILE (root node)
                    //       - PACKAGE_DIRECTIVE
                    //       - IMPORT_LIST
                    ""
                ) as KtFile
                return psiFile.node.apply {
                    putUserData(
                        KtLint.DISABLED_RULES,
                        setOf(
                            SOME_DISABLED_RULE_IN_STANDARD_RULE_SET,
                            "$EXPERIMENTAL:$SOME_DISABLED_RULE_IN_EXPERIMENTAL_RULE_SET",
                            "$CUSTOM_RULE_SET_A:$SOME_DISABLED_RULE_IN_CUSTOM_RULE_SET_A"
                        )
                    )
                }
            }
        }
    }

    open class NormalRule(id: String) : R(id)

    class RootNodeOnlyRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunOnRootNodeOnly
        )
    )

    class RunAsLateAsPossibleRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible
        )
    )

    class RunAsLateAsPossibleOnRootNodeOnlyRule(id: String) : R(
        id = id,
        visitorModifiers = setOf(
            VisitorModifier.RunOnRootNodeOnly,
            VisitorModifier.RunAsLateAsPossible
        )
    )

    open class R(
        id: String,
        visitorModifiers: Set<VisitorModifier> = emptySet()
    ) : Rule(id, visitorModifiers) {
        constructor(id: String, visitorModifier: VisitorModifier) : this(id, setOf(visitorModifier))

        override fun visit(
            node: ASTNode,
            autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
        ) {
            throw UnsupportedOperationException(
                "Rule should never be really invoked because that is not the aim of this unit test."
            )
        }
    }

    private data class Visit(
        val shortenedQualifiedRuleId: String,
        val elementType: IElementType = FILE
    ) {
        constructor(
            ruleSetId: String,
            ruleId: String,
            elementType: IElementType = FILE
        ) : this(
            shortenedQualifiedRuleId = "$ruleSetId:$ruleId",
            elementType = elementType
        ) {
            require(!ruleSetId.contains(':')) {
                "rule set id may not contain the ':' character"
            }
            require(!ruleId.contains(':')) {
                "rule id may not contain the ':' character"
            }
        }

        init {
            require(!shortenedQualifiedRuleId.startsWith(STANDARD)) {
                "the shortened qualified rule id may not start with '$STANDARD'"
            }
        }
    }
}
