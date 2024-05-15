package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

internal class SuppressHandler(
    private val suppressionLocator: SuppressionLocator,
    private val autoCorrectHandler: AutoCorrectHandler,
    private val emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Boolean,
) {
    fun handle(
        node: ASTNode,
        ruleId: RuleId,
        function: (Boolean, (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) -> Unit,
    ) {
        val suppress =
            suppressionLocator(
                node.startOffset,
                ruleId,
            )
        val autoCorrect = this.autoCorrectHandler.autoCorrect(node.startOffset) && !suppress
        val emit =
            if (suppress) {
                SUPPRESS_EMIT
            } else {
                this.emitAndApprove
            }
        function(autoCorrect, emit.onlyEmit())
    }

    fun handle(
        node: ASTNode,
        ruleId: RuleId,
        function: (
            // emitAndApprove
            (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Boolean,
        ) -> Unit,
    ) {
        val suppress = suppressionLocator(node.startOffset, ruleId)
        if (suppress) {
            function(SUPPRESS_EMIT)
        } else {
            function(emitAndApprove)
        }
    }

    // Simplify the emitAndApprove to an emit only lambda which can be used in the legacy (deprecated) functions
    @Deprecated(message = "Remove in Ktlint 2.0")
    private fun ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Boolean).onlyEmit() =
        {
                offset: Int,
                errorMessage: String,
                canBeAutoCorrected: Boolean,
            ->
            this(offset, errorMessage, canBeAutoCorrected)
            Unit
        }

    private companion object {
        // Swallow violation so that it is not emitted
        val SUPPRESS_EMIT: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Boolean = { _, _, _ -> false }
    }
}
