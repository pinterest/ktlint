package com.pinterest.ktlint.rule.engine.internal.rules

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty

internal val INTERNAL_RULE_ABOUT =
    Rule.About(
        maintainer = "KtLint",
        repositoryUrl = "https://github.com/pinterest/ktlint",
        issueTrackerUrl = "https://github.com/pinterest/ktlint/issues",
    )

/**
 * Internal rules can only be declared and instantiated in the 'ktlint-rule-engine'.
 */
public open class InternalRule internal constructor(
    id: String,
    override val visitorModifiers: Set<VisitorModifier> = emptySet(),
    override val usesEditorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) : Rule(
        ruleId = RuleId("internal:$id"),
        visitorModifiers = visitorModifiers,
        usesEditorConfigProperties = usesEditorConfigProperties,
        about = INTERNAL_RULE_ABOUT,
    ),
    RuleAutocorrectApproveHandler
