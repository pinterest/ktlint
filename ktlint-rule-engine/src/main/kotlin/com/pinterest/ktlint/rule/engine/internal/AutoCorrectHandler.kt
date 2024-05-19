package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.LintError

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutoCorrectHandler {
    fun autoCorrect(lintError: LintError): Boolean
}

internal data object AutoCorrectDisabledHandler : AutoCorrectHandler {
    override fun autoCorrect(lintError: LintError) = false
}

internal data object AutoCorrectEnabledHandler : AutoCorrectHandler {
    override fun autoCorrect(lintError: LintError) = true
}

internal class LintErrorAutoCorrectHandler(
    private val callback: (LintError) -> Boolean,
) : AutoCorrectHandler {
    override fun autoCorrect(lintError: LintError) = callback(lintError)
}

internal class RangeAutoCorrectHandler(
    private val code: Code,
    private val autoCorrectOffsetRange: IntRange,
) : AutoCorrectHandler {
    override fun autoCorrect(lintError: LintError) = lintError.isInRange(code, autoCorrectOffsetRange)

    private fun LintError.isInRange(
        code: Code,
        intRange: IntRange,
    ): Boolean {
        val (startLine, startCol) = code.lineAndColumnFrom(intRange.first)
        val (endLine, endCol) = code.lineAndColumnFrom(intRange.last)
        return !(
            line < startLine ||
                (line == startLine && col < startCol) ||
                (line == endLine && col > endCol) ||
                line > endLine
        )
    }

    private fun Code.lineAndColumnFrom(offset: Int) =
        content
            .substring(0, offset)
            .let { text ->
                Pair(
                    text.count { it == '\n' } + 1,
                    text.length - text.lastIndexOf('\n'),
                )
            }
}
