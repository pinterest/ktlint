package com.pinterest.ktlint.rule.engine.internal

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Handler which determines whether autocorrect should be enabled or disabled for the given offset.
 */
internal sealed interface AutoCorrectHandler {
    fun autoCorrect(node: ASTNode): Boolean
}

internal data object AutoCorrectDisabledHandler : AutoCorrectHandler {
    override fun autoCorrect(node: ASTNode) = false
}

internal data object AutoCorrectEnabledHandler : AutoCorrectHandler {
    override fun autoCorrect(node: ASTNode) = true
}

internal class AutoCorrectOffsetRangeHandler(
    private val autoCorrectOffsetRange: IntRange,
) : AutoCorrectHandler {
    override fun autoCorrect(node: ASTNode) = node.startOffset in autoCorrectOffsetRange
}
