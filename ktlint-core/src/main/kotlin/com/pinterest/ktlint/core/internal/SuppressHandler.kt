package com.pinterest.ktlint.core.internal

import org.jetbrains.kotlin.com.intellij.lang.ASTNode

internal class SuppressHandler(
    private val suppressionLocator: SuppressionLocator,
    private val autoCorrect: Boolean,
    private val emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
) {
    fun handle(
        node: ASTNode,
        ruleId: String,
        function: (Boolean, (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) -> Unit,
    ) {
        val suppress = suppressionLocator(
            node.startOffset,
            ruleId,
        )
        val autoCorrect = this.autoCorrect && !suppress
        val emit = if (suppress) {
            SUPPRESS_EMIT
        } else {
            this.emit
        }
        function(autoCorrect, emit)
    }

    private companion object {
        // Swallow violation so that it is not emitted
        val SUPPRESS_EMIT: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit = { _, _, _ -> }
    }
}
