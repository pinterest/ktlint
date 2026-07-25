package io.github.ktlint.core.rule.engine.internal

import io.github.ktlint.core.rule.engine.api.LintError
import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision.NO_AUTOCORRECT

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutocorrectHandler {
    fun autocorrectDecision(lintError: LintError): AutocorrectDecision
}

/**
 * Do not autocorrect any [LintError]s
 */
internal data object NoneAutocorrectHandler : AutocorrectHandler {
    override fun autocorrectDecision(lintError: LintError) = NO_AUTOCORRECT
}

internal class LintErrorAutocorrectHandler(
    private val callback: (LintError) -> AutocorrectDecision,
) : AutocorrectHandler {
    override fun autocorrectDecision(lintError: LintError) = callback(lintError)
}
