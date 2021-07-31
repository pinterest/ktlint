package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.psi.KtEnumEntry

public class EnumEntryNameCaseRule : Rule("enum-entry-name-case") {

    internal companion object {
        const val ERROR_MESSAGE = "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""
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
                ERROR_MESSAGE,
                false
            )
        }
    }
}
