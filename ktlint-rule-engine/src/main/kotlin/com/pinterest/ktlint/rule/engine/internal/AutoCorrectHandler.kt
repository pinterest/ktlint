package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.api.LintError

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutoCorrectHandler {
    fun autoCorrect(offset: Int): Boolean

    fun autoCorrect(lintError: LintError): Boolean
}

internal data object AutoCorrectDisabledHandler : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = false

    override fun autoCorrect(lintError: LintError) = false
}

internal data object AutoCorrectEnabledHandler : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = true

    override fun autoCorrect(lintError: LintError) = true
}

@Deprecated(message = "Replace with LintErrorAutocorrectHandler")
internal class AutoCorrectOffsetRangeHandler(
    private val autoCorrectOffsetRange: IntRange,
) : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = offset in autoCorrectOffsetRange

    override fun autoCorrect(lintError: LintError) = throw IllegalStateException()
}

internal class LintErrorAutoCorrectHandler(
    private val callback: (LintError) -> Boolean,
) : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = true

    override fun autoCorrect(lintError: LintError) = callback(lintError)
}
