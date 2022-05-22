package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_LIST
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.internal.initPsiFileFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
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
    fun `Multiple normal rules in the same rule set are run in alphabetical order`() {
        val actual = testVisitorProvider(
            NormalRule(RULE_B),
            NormalRule(RULE_A)
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(RULE_B)
        )
    }

    @Test
    fun `Multiple normal rules in different rule sets are run in alphabetical order but grouped in order standard, experimental and custom`() {
        val customRuleSetA = "custom-rule-set-a"
        val customRuleSetB = "custom-rule-set-b"
        val actual = testVisitorProvider(
            RuleSet(
                EXPERIMENTAL,
                NormalRule(RULE_B),
                NormalRule(RULE_A)
            ),
            RuleSet(
                customRuleSetA,
                NormalRule(RULE_B),
                NormalRule(RULE_A)
            ),
            RuleSet(
                STANDARD,
                NormalRule(RULE_B),
                NormalRule(RULE_A)
            ),
            RuleSet(
                customRuleSetB,
                NormalRule(RULE_B),
                NormalRule(RULE_A)
            )
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(RULE_B),
            Visit(EXPERIMENTAL, RULE_A),
            Visit(EXPERIMENTAL, RULE_B),
            // Rules from custom rule sets are all grouped together
            Visit(customRuleSetA, RULE_A),
            Visit(customRuleSetB, RULE_A),
            Visit(customRuleSetA, RULE_B),
            Visit(customRuleSetB, RULE_B)
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
    fun `Root only rule is run before non-root-only rule`() {
        val actual = testVisitorProvider(
            RootNodeOnlyRule(ROOT_NODE_ONLY_RULE),
            NormalRule(NORMAL_RULE)
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(ROOT_NODE_ONLY_RULE),
            Visit(NORMAL_RULE)
        )
    }

    @Test
    fun `Multiple root only rules in the same rule set are run in alphabetical order`() {
        val customRuleSetA = "custom-rule-set-a"
        val customRuleSetB = "custom-rule-set-b"
        val actual = testVisitorProvider(
            RuleSet(
                EXPERIMENTAL,
                RootNodeOnlyRule(RULE_B),
                RootNodeOnlyRule(RULE_A)
            ),
            RuleSet(
                customRuleSetA,
                RootNodeOnlyRule(RULE_B),
                RootNodeOnlyRule(RULE_A)
            ),
            RuleSet(
                STANDARD,
                RootNodeOnlyRule(RULE_B),
                RootNodeOnlyRule(RULE_A)
            ),
            RuleSet(
                customRuleSetB,
                RootNodeOnlyRule(RULE_B),
                RootNodeOnlyRule(RULE_A)
            )
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(RULE_B),
            Visit(EXPERIMENTAL, RULE_A),
            Visit(EXPERIMENTAL, RULE_B),
            // Rules from custom rule sets are all grouped together
            Visit(customRuleSetA, RULE_A),
            Visit(customRuleSetB, RULE_A),
            Visit(customRuleSetA, RULE_B),
            Visit(customRuleSetB, RULE_B)
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
    fun `A run as late as possible rule runs after the rules not marked to run as late as possible`() {
        val actual = testVisitorProvider(
            NormalRule(RULE_C),
            RunAsLateAsPossibleRule(RULE_A),
            NormalRule(RULE_B)
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_B),
            Visit(RULE_C),
            Visit(RULE_A)
        )
    }

    @Test
    fun `Multiple run as late as possible rules in the same rule set are sorted alphabetically`() {
        val customRuleSetA = "custom-rule-set-a"
        val customRuleSetB = "custom-rule-set-b"
        val actual = testVisitorProvider(
            RuleSet(
                EXPERIMENTAL,
                RunAsLateAsPossibleRule(RULE_B),
                RunAsLateAsPossibleRule(RULE_A)
            ),
            RuleSet(
                customRuleSetA,
                RunAsLateAsPossibleRule(RULE_B),
                RunAsLateAsPossibleRule(RULE_A)
            ),
            RuleSet(
                STANDARD,
                RunAsLateAsPossibleRule(RULE_B),
                RunAsLateAsPossibleRule(RULE_A)
            ),
            RuleSet(
                customRuleSetB,
                RunAsLateAsPossibleRule(RULE_B),
                RunAsLateAsPossibleRule(RULE_A)
            )
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(RULE_B),
            Visit(EXPERIMENTAL, RULE_A),
            Visit(EXPERIMENTAL, RULE_B),
            // Rules from custom rule sets are all grouped together
            Visit(customRuleSetA, RULE_A),
            Visit(customRuleSetB, RULE_A),
            Visit(customRuleSetA, RULE_B),
            Visit(customRuleSetB, RULE_B)
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
    fun `A run as late as possible rule on root node only runs after the rules not marked to run as late as possible`() {
        val actual = testVisitorProvider(
            NormalRule(RULE_C),
            RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_A),
            NormalRule(RULE_B)
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_B),
            Visit(RULE_C),
            Visit(RULE_A)
        )
    }

    @Test
    fun `Multiple run as late as possible on root node only rules in the same rule set are sorted alphabetically`() {
        val customRuleSetA = "custom-rule-set-a"
        val customRuleSetB = "custom-rule-set-b"
        val actual = testVisitorProvider(
            RuleSet(
                EXPERIMENTAL,
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_B),
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_A)
            ),
            RuleSet(
                customRuleSetA,
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_B),
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_A)
            ),
            RuleSet(
                STANDARD,
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_B),
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_A)
            ),
            RuleSet(
                customRuleSetB,
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_B),
                RunAsLateAsPossibleOnRootNodeOnlyRule(RULE_A)
            )
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A),
            Visit(RULE_B),
            Visit(EXPERIMENTAL, RULE_A),
            Visit(EXPERIMENTAL, RULE_B),
            // Rules from custom rule sets are all grouped together
            Visit(customRuleSetA, RULE_A),
            Visit(customRuleSetB, RULE_A),
            Visit(customRuleSetA, RULE_B),
            Visit(customRuleSetB, RULE_B)
        )
    }

    @Test
    fun `A rule annotated with run after rule can not refer to itself`() {
        assertThatIllegalStateException().isThrownBy {
            testVisitorProvider(
                RuleSet(
                    CUSTOM_RULE_SET_A,
                    object : R(
                        id = RULE_A,
                        visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_A")
                    ) {}
                )
            )
        }.withMessage(
            "Rule with id '$CUSTOM_RULE_SET_A:$RULE_A' has a visitor modifier of type 'RunAfterRule' but it is not " +
                "referring to another rule but to the rule itself. A rule can not run after itself. This should be " +
                "fixed by the maintainer of the rule."
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the same rule set runs after that rule and override the alphabetical sort order`() {
        val actual = testVisitorProvider(
            object : R(
                id = RULE_A,
                visitorModifier = VisitorModifier.RunAfterRule(RULE_C)
            ) {},
            NormalRule(RULE_B),
            object : R(
                id = RULE_D,
                visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
            ) {},
            object : R(
                id = RULE_C,
                visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
            ) {}
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_B),
            Visit(RULE_C),
            Visit(RULE_A),
            // Although RULE_D like RULE_C depends on RULE_B it still comes after RULE_A because that rules according to
            // the default sort order comes before rule D
            Visit(RULE_D)
        )
    }

    @Test
    fun `A rule annotated with run after rule for a rule in the different rule set runs after that rule and override the alphabetical sort order`() {
        val actual = testVisitorProvider(
            RuleSet(
                STANDARD,
                NormalRule(RULE_B),
                object : R(
                    id = RULE_D,
                    visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                ) {},
                object : R(
                    id = RULE_C,
                    visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                ) {}
            ),
            RuleSet(
                EXPERIMENTAL,
                object : R(
                    id = RULE_A,
                    visitorModifier = VisitorModifier.RunAfterRule(RULE_C)
                ) {}
            )
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_B),
            Visit(RULE_C),
            Visit(RULE_D),
            Visit(EXPERIMENTAL, RULE_A)
        )
    }

    @Test
    fun `A rule annotated with run after rule which has to be loaded throws an exception in case isUnitTestContext is disabled`() {
        assertThatIllegalStateException().isThrownBy {
            testVisitorProvider(
                object : R(
                    id = RULE_A,
                    visitorModifier = VisitorModifier.RunAfterRule(
                        ruleId = "not-loaded-rule",
                        loadOnlyWhenOtherRuleIsLoaded = true
                    )
                ) {},
                isUnitTestContext = false
            )
        }.withMessage("No runnable rules found. Please ensure that at least one is enabled.")
    }

    @Test
    fun `A rule annotated with run after rule of a rule which has to be loaded will still be ignored in case isUnitTestContext is enabled`() {
        val actual = testVisitorProvider(
            object : R(
                id = RULE_A,
                visitorModifier = VisitorModifier.RunAfterRule("not-loaded-rule")
            ) {},
            isUnitTestContext = true
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A)
        )
    }

    @Test
    fun `A rule annotated with run after rule of a rule which does not have to be loaded will be ignored in case isUnitTestContext is disabled`() {
        val actual = testVisitorProvider(
            object : R(
                id = RULE_A,
                visitorModifier = VisitorModifier.RunAfterRule("not-loaded-rule")
            ) {},
            isUnitTestContext = false
        ).filterFileNodes()

        assertThat(actual).containsExactly(
            Visit(RULE_A)
        )
    }

    @Test
    fun `Rules annotated with run after rule but cyclic depend on each others, no custom rule sets involved, throws an exception`() {
        assertThatIllegalStateException().isThrownBy {
            testVisitorProvider(
                RuleSet(
                    STANDARD,
                    object : R(
                        id = RULE_A,
                        visitorModifier = VisitorModifier.RunAfterRule(RULE_B)
                    ) {},
                    object : R(
                        id = RULE_B,
                        visitorModifier = VisitorModifier.RunAfterRule("$EXPERIMENTAL:$RULE_C")
                    ) {}
                ),
                RuleSet(
                    EXPERIMENTAL,
                    object : R(
                        id = RULE_C,
                        visitorModifier = VisitorModifier.RunAfterRule(RULE_A)
                    ) {}
                )
            )
        }.withMessage(
            """
            Found cyclic dependencies between rules that should run after another rule:
              - Rule with id '$STANDARD:$RULE_A' should run after rule with id '$STANDARD:$RULE_B'
              - Rule with id '$STANDARD:$RULE_B' should run after rule with id '$EXPERIMENTAL:$RULE_C'
              - Rule with id '$EXPERIMENTAL:$RULE_C' should run after rule with id '$STANDARD:$RULE_A'
            """.trimIndent()
        )
    }

    @Test
    fun `Rules annotated with run after rule but cyclic depend on each others, custom rule sets involved, throws an exception`() {
        assertThatIllegalStateException().isThrownBy {
            testVisitorProvider(
                RuleSet(
                    STANDARD,
                    object : R(
                        id = RULE_C,
                        visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_B:$RULE_B")
                    ) {}
                ),
                RuleSet(
                    CUSTOM_RULE_SET_B,
                    object : R(
                        id = RULE_B,
                        visitorModifier = VisitorModifier.RunAfterRule("$CUSTOM_RULE_SET_A:$RULE_A")
                    ) {}
                ),
                RuleSet(
                    CUSTOM_RULE_SET_A,
                    object : R(
                        id = RULE_A,
                        visitorModifier = VisitorModifier.RunAfterRule("$STANDARD:$RULE_C")
                    ) {}
                )
            )
        }.withMessage(
            """
            Found cyclic dependencies between rules that should run after another rule. Please contact the maintainer(s) of the custom rule set(s) [$CUSTOM_RULE_SET_A, $CUSTOM_RULE_SET_B] before creating an issue in the KtLint project. Dependencies:
              - Rule with id '$STANDARD:$RULE_C' should run after rule with id '$CUSTOM_RULE_SET_B:$RULE_B'
              - Rule with id '$CUSTOM_RULE_SET_A:$RULE_A' should run after rule with id '$STANDARD:$RULE_C'
              - Rule with id '$CUSTOM_RULE_SET_B:$RULE_B' should run after rule with id '$CUSTOM_RULE_SET_A:$RULE_A'
            """.trimIndent()
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
        isUnitTestContext: Boolean? = null,
        concurrent: Boolean? = null
    ): MutableList<Visit>? {
        return testVisitorProvider(
            RuleSet(
                STANDARD,
                *rules
            ),
            isUnitTestContext = isUnitTestContext,
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
        isUnitTestContext: Boolean? = null,
        concurrent: Boolean? = null
    ): MutableList<Visit>? {
        val ruleSetList = ruleSets.toList()
        return VisitorProvider(
            ruleSets = ruleSetList,
            // Enable debug mode as it is helpful when a test fails
            debug = true
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
        const val CUSTOM_RULE_SET_B = "custom-rule-set-b"
        val SOME_ROOT_AST_NODE = initRootAstNode()
        const val NORMAL_RULE = "normal-rule"
        const val ROOT_NODE_ONLY_RULE = "root-node-only-rule"
        const val RUN_AS_LATE_AS_POSSIBLE_RULE = "run-as-late-as-possible-rule"
        const val RULE_A = "rule-a"
        const val RULE_B = "rule-b"
        const val RULE_C = "rule-c"
        const val RULE_D = "rule-d"
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
