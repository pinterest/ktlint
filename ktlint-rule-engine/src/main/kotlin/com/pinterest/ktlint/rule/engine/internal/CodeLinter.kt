package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.internal.RuleExecutionContext.Companion.createRuleExecutionContext
import io.github.oshai.kotlinlogging.KotlinLogging

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

internal class CodeLinter(
    val ktLintRuleEngine: KtLintRuleEngine,
) {
    fun lint(
        code: Code,
        callback: (LintError) -> Unit = { },
    ) {
        LOGGER.debug { "Starting with linting file '${code.fileNameOrStdin()}'" }
        executeLint(code)
            .sortedWith(lintErrorLineAndColumnComparator { it })
            .forEach { e -> callback(e) }
        LOGGER.debug { "Finished with linting file '${code.fileNameOrStdin()}'" }
    }

    private fun executeLint(code: Code): MutableList<LintError> =
        with(createRuleExecutionContext(ktLintRuleEngine, code)) {
            val errors = mutableListOf<LintError>()
            VisitorProvider(ruleProviders)
                .rules
                .forEach { rule -> executeRule(rule, code).let { errors.addAll(it) } }
            errors
        }

    private fun RuleExecutionContext.executeRule(
        rule: Rule,
        code: Code,
    ): List<LintError> {
        val errors = mutableListOf<LintError>()
        executeRule(rule, AutoCorrectDisabledHandler) { offset, errorMessage, canBeAutoCorrected ->
            val (line, col) = this.positionInTextLocator(offset)
            LintError(line, col, rule.ruleId, errorMessage, canBeAutoCorrected)
                .let { lintError ->
                    errors.add(lintError)
                    // In trace mode report the violation immediately. The order in which violations are actually found might be
                    // different from the order in which they are reported. For debugging purposes it can be helpful to know the
                    // exact order in which violations are being solved.
                    LOGGER.trace { "Lint violation: ${lintError.logMessage(code)}" }
                }
            // No need to ask for approval in lint mode
            false
        }
        return errors
    }
}
