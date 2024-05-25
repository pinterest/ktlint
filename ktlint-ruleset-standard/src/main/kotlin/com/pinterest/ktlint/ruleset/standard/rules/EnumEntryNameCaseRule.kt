package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.psi.KtEnumEntry

/**
 * https://kotlinlang.org/docs/coding-conventions.html#property-names
 */
@SinceKtlint("0.36", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class EnumEntryNameCaseRule : StandardRule("enum-entry-name-case") {
    internal companion object {
        val ENUM_ENTRY_IDENTIFIER_REGEX = "[A-Z]([A-Za-z\\d]*|[A-Z_\\d]*)".regExIgnoringDiacriticsAndStrokesOnLetters()
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node !is CompositeElement) {
            return
        }
        val enumEntry = node.psi as? KtEnumEntry ?: return
        val name = enumEntry.name ?: return

        if (!name.matches(ENUM_ENTRY_IDENTIFIER_REGEX)) {
            emit(
                node.startOffset,
                "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\"",
                false,
            )
        }
    }
}

public val ENUM_ENTRY_NAME_CASE_RULE_ID: RuleId = EnumEntryNameCaseRule().ruleId
