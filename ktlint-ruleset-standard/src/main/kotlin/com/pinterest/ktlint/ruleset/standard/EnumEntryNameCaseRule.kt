package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.psi.KtEnumEntry

/**
 * https://kotlinlang.org/docs/coding-conventions.html#property-names
 */
public class EnumEntryNameCaseRule : Rule("enum-entry-name-case") {
    internal companion object {
        val regex = Regex("[A-Z]([A-Za-z\\d]*|[A-Z_\\d]*)")
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node !is CompositeElement) {
            return
        }
        val enumEntry = node.psi as? KtEnumEntry ?: return
        val name = enumEntry.name ?: return

        if (!name.matches(regex)) {
            emit(
                node.startOffset,
                "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\"",
                false
            )
        }
    }
}
