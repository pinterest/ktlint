package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Provides a [Rule] instance. Important: to ensure that a [Rule] can keep internal state and that processing of files is thread-safe,
 * a *new* instance should be provided on each call of the [provider] function.
 */
@Suppress("DEPRECATION")
@Deprecated(
    message = "Provides backwards compatibility of custom ruleset JARs created for Ktlint 1.x.",
    replaceWith = ReplaceWith("RuleV2InstanceProvider", "com.pinterest.ktlint.core.RuleV2InstanceProvider"),
    level = DeprecationLevel.WARNING,
)
public class RuleProvider private constructor(
    /**
     * Lambda which creates a new instance of the rule.
     */
    private val provider: () -> Rule,
    /**
     * The rule id of the [RuleV2] created by the provider.
     */
    public override val ruleId: RuleId,
) : RuleInstanceProvider(ruleId) {
    /**
     * Creates a new [RuleV2] instance.
     */
    public override fun createNewRuleInstance(): RuleV2 =
        provider()
            .also {
                require(it is RuleAutocorrectApproveHandler) {
                    "Ktlint 2.x does not support rules that have not correctly implemented the RuleAutocorrectApproveHandler. Use a new " +
                        "version of the ruleset. or contact the maintainer of this ruleset to upgrade it."
                }
            }.let { rule ->
                object :
                    RuleV2(
                        ruleId = ruleId,
                        about =
                            About(
                                maintainer = rule.about.maintainer,
                                repositoryUrl = rule.about.repositoryUrl,
                                issueTrackerUrl = rule.about.issueTrackerUrl,
                            ),
                        usesEditorConfigProperties = rule.usesEditorConfigProperties,
                    ) {
                    override fun beforeFirstNode(editorConfig: EditorConfig) {
                        rule.beforeFirstNode(editorConfig)
                    }

                    override fun beforeVisitChildNodes(
                        node: ASTNode,
                        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
                    ) {
                        (rule as RuleAutocorrectApproveHandler).beforeVisitChildNodes(node, emit)
                    }

                    override fun afterVisitChildNodes(
                        node: ASTNode,
                        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
                    ) {
                        (rule as RuleAutocorrectApproveHandler).afterVisitChildNodes(node, emit)
                    }

                    override fun afterLastNode() {
                        rule.afterLastNode()
                    }
                }
            }

    /**
     * Lambda which creates a new instance of the [Rule]. Important: to ensure that a [Rule] can keep internal state and that processing of
     * files is thread-safe, a *new* instance should be provided on each call of the [provider] function.
     */
    public companion object {
        // Note that the KDOC is placed on the companion object to make it actually visually when the RuleProvider identifier is being
        // hovered in IntelliJ IDEA
        public operator fun invoke(provider: () -> Rule): RuleProvider = RuleProvider(provider = provider, ruleId = provider().ruleId)
    }
}
