package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.LintError
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.ALLOW_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision.NO_AUTOCORRECT
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.END_OF_LINE_PROPERTY
import io.github.oshai.kotlinlogging.KotlinLogging
import org.ec4j.core.model.PropertyType

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

internal class CodeFormatter(
    val ktLintRuleEngine: KtLintRuleEngine,
) {
    fun format(
        code: Code,
        autocorrectHandler: AutocorrectHandler,
        callback: (LintError, Boolean) -> Unit = { _, _ -> },
        maxFormatRunsPerFile: Int,
    ): String {
        LOGGER.debug { "Starting with processing file '${code.fileNameOrStdin()}'" }

        val (formattedCode, errors) = format(code, autocorrectHandler, maxFormatRunsPerFile)

        errors
            .sortedWith(lintErrorLineAndColumnComparator { it.first })
            .forEach { (e, corrected) -> callback(e, corrected) }

        return (code.utf8Bom() + formattedCode).also {
            LOGGER.debug { "Finished with processing file '${code.fileNameOrStdin()}'" }
        }
    }

    private fun Code.utf8Bom() =
        if (content.startsWith(KtLintRuleEngine.UTF8_BOM)) {
            KtLintRuleEngine.UTF8_BOM
        } else {
            ""
        }

    private fun format(
        code: Code,
        autocorrectHandler: AutocorrectHandler,
        maxFormatRunsPerFile: Int,
    ): Pair<String, Set<Pair<LintError, Boolean>>> {
        with(RuleExecutionContext.createRuleExecutionContext(ktLintRuleEngine, code)) {
            val errors = mutableSetOf<Pair<LintError, Boolean>>()
            var formatRunCount = 0
            var mutated: Boolean
            do {
                mutated =
                    format(autocorrectHandler, code).let { ruleErrors ->
                        errors.addAll(ruleErrors)
                        ruleErrors.any { it.first.canBeAutoCorrected }
                    }
                formatRunCount++
            } while (mutated && formatRunCount < maxFormatRunsPerFile)
            if (mutated && formatRunCount == maxFormatRunsPerFile) {
                // It is unknown if the last format run introduces new lint violations which can be autocorrected. So run lint once more
                // so that the user can be informed about this correctly.
                lintAfterFormat().also {
                    LOGGER.warn {
                        "Format was not able to resolve all violations which (theoretically) can be autocorrected in file " +
                            "${code.filePathOrStdin()} in $maxFormatRunsPerFile consecutive runs of format."
                    }
                }
            }
            val lineSeparator = code.determineLineSeparator(editorConfig[END_OF_LINE_PROPERTY])
            return Pair(formattedCode(lineSeparator), errors)
        }
    }

    private fun RuleExecutionContext.formattedCode(lineSeparator: String) = rootNode.text.replace("\n", lineSeparator)

    private fun RuleExecutionContext.format(
        autocorrectHandler: AutocorrectHandler,
        code: Code,
    ): Set<Pair<LintError, Boolean>> {
        val errors: MutableSet<Pair<LintError, Boolean>> = mutableSetOf()
        VisitorProvider(ruleProviders)
            .rules
            .forEach { rule ->
                executeRule(rule, autocorrectHandler, code).let { ruleErrors -> errors.addAll(ruleErrors) }
            }
        return errors
    }

    private fun RuleExecutionContext.lintAfterFormat(): Boolean {
        var hasErrorsWhichCanBeAutocorrected = false
        VisitorProvider(ruleProviders)
            .rules
            .forEach { rule ->
                if (!hasErrorsWhichCanBeAutocorrected) {
                    executeRule(rule, NoneAutocorrectHandler) { _, _, canBeAutoCorrected ->
                        if (canBeAutoCorrected) {
                            hasErrorsWhichCanBeAutocorrected = true
                        }
                        // No need to ask for approval in lint mode
                        NO_AUTOCORRECT
                    }
                }
            }
        return hasErrorsWhichCanBeAutocorrected
    }

    private fun RuleExecutionContext.executeRule(
        rule: Rule,
        autocorrectHandler: AutocorrectHandler,
        code: Code,
    ): Set<Pair<LintError, Boolean>> {
        val errors = mutableSetOf<Pair<LintError, Boolean>>()
        executeRule(rule, autocorrectHandler) { offset, errorMessage, canBeAutoCorrected ->
            val (line, col) = positionInTextLocator(offset)
            val lintError = LintError(line, col, rule.ruleId, errorMessage, canBeAutoCorrected)
            // In trace mode report the violation immediately. The order in which violations are actually found might be
            // different from the order in which they are reported. For debugging purposes it can be helpful to know the
            // exact order in which violations are being solved.
            LOGGER.trace { "Format violation: ${lintError.logMessage(code)}" }

            // Always request the autocorrectDecision, even in case it is already known that the LintError can not be autocorrected. In
            // this way the API Consumer can still use data from the LintError for other purposes.
            autocorrectHandler
                .autocorrectDecision(lintError)
                .also { autocorrectDecision ->
                    // Ignore decision of the API Consumer in case the error can not be autocorrected
                    val autocorrect = autocorrectDecision == ALLOW_AUTOCORRECT && canBeAutoCorrected
                    if (autocorrect) {
                        /*
                         * Rebuild the suppression locator after each change in the AST as the offsets of the suppression hints might
                         * have changed.
                         */
                        rebuildSuppressionLocator()
                    }
                    errors.add(
                        Pair(
                            lintError,
                            // It is assumed that a rule that asks for autocorrect approval, also does correct the error.
                            autocorrect,
                        ),
                    )
                }
        }
        return errors
    }

    private fun Code.determineLineSeparator(eolEditorConfigProperty: PropertyType.EndOfLineValue): String =
        when {
            eolEditorConfigProperty == PropertyType.EndOfLineValue.crlf ||
                eolEditorConfigProperty != PropertyType.EndOfLineValue.lf &&
                doesNotContain('\r') ->
                "\r\n".also { LOGGER.debug { "line separator: ${eolEditorConfigProperty.name} --> CRLF" } }

            else -> "\n".also { LOGGER.debug { "line separator: ${eolEditorConfigProperty.name} --> LF" } }
        }

    private fun Code.doesNotContain(char: Char) = content.lastIndexOf(char) != -1

    private fun <T> lintErrorLineAndColumnComparator(transformer: (T) -> LintError) =
        compareBy<T> { transformer(it).line }
            .then(compareBy { transformer(it).col })

    private fun LintError.logMessage(code: Code) =
        "${code.fileNameOrStdin()}:$line:$col: $detail ($ruleId)" +
            if (canBeAutoCorrected) {
                ""
            } else {
                " [cannot be autocorrected]"
            }
}
