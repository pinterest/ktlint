package com.pinterest.ktlint.rule.engine.internal

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutoCorrectHandler {
    fun autoCorrect(offset: Int): Boolean
}

internal data object AutoCorrectDisabledHandler : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = false
}

internal data object AutoCorrectEnabledHandler : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = true
}

internal class AutoCorrectOffsetRangeHandler(
    private val autoCorrectOffsetRange: IntRange,
) : AutoCorrectHandler {
    override fun autoCorrect(offset: Int) = offset in autoCorrectOffsetRange
}
