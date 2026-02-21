package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty

internal val STANDARD_RULE_ABOUT =
    RuleV2.About(
        maintainer = "KtLint",
        repositoryUrl = "https://github.com/pinterest/ktlint",
        issueTrackerUrl = "https://github.com/pinterest/ktlint/issues",
    )

/**
 * Standard rules can only be declared and instantiated in the 'ktlint-ruleset-standard'. Custom rule set providers or API consumers have to
 * extend the [RuleV2] class to define a custom rule.
 */
public open class StandardRule internal constructor(
    id: String,
    override val visitorModifiers: Set<VisitorModifier> = emptySet(),
    override val usesEditorConfigProperties: Set<EditorConfigProperty<*>> = emptySet(),
) : RuleV2(
        ruleId = RuleId("${RuleSetId.STANDARD.value}:$id"),
        visitorModifiers = visitorModifiers,
        usesEditorConfigProperties = usesEditorConfigProperties,
        about = STANDARD_RULE_ABOUT,
    )
