package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.psi.KtEnumEntry

class EnumEntryNameCaseRule : Rule("enum-entry-name-case") {

    companion object {
        const val ERROR_MESSAGE = "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""
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

        if (name.containsLowerCase()) {
            // In case of lower camel case like "enumName", or all lower case like "enumname"
            if (!name.startsWithUpperCase()) {
                emit(
                    node.startOffset,
                    ERROR_MESSAGE,
                    false
                )

                if (autoCorrect) correct(enumEntry, name)
            }

            // In case of lower case with underscore like "enum_name"
            else if (name.contains("_") && name.containsLowerCase()) {
                emit(
                    node.startOffset,
                    ERROR_MESSAGE,
                    false
                )

                if (autoCorrect) correct(enumEntry, name)
            }
        }
    }

    private fun correct(enumEntry: KtEnumEntry, originalName: String) {
        enumEntry.setName(originalName.toUpperCase())
    }

    private fun String.startsWithUpperCase(): Boolean {
        return this.isNotEmpty() && this[0].isUpperCase()
    }

    private fun String.containsLowerCase(): Boolean {
        return this.any { it.isLowerCase() }
    }
}
