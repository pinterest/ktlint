package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.parent
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc

public class NoTrailingSpacesRule : Rule("no-trailing-spaces") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isPartOfKDoc()) {
            if (node.elementType == WHITE_SPACE && node.hasTrailingSpacesBeforeNewline()) {
                val offsetOfSpaceBeforeNewlineInText = node.text.indexOf(" \n")
                val offsetOfFirstSpaceBeforeNewlineInText =
                    node
                        .text
                        .take(offsetOfSpaceBeforeNewlineInText)
                        .dropLastWhile { it == ' ' }
                        .length
                emit(node.startOffset + offsetOfFirstSpaceBeforeNewlineInText, "Trailing space(s)", true)
                if (autoCorrect) {
                    node.removeTrailingSpacesBeforeNewline()
                }
            }
        } else if (node.elementType == WHITE_SPACE || node.isPartOfComment()) {
            val lines = node.text.split("\n")
            var violated = false
            var violationOffset = node.startOffset

            val modifiedLines =
                lines
                    .mapIndexed { index, line ->
                        val modifiedLine = when {
                            node.elementType != EOL_COMMENT && index == lines.size - 1 && node.nextLeaf() != null ->
                                // Do not change the last line as it contains the indentation of the next element except
                                // when it is an EOL comment which may also not contain trailing spaces
                                line
                            line.hasTrailingSpace() -> {
                                val modifiedLine = line.trimEnd()
                                val firstTrailingSpaceOffset = violationOffset + modifiedLine.length
                                emit(firstTrailingSpaceOffset, "Trailing space(s)", true)
                                violated = true
                                modifiedLine
                            }
                            else -> line
                        }
                        violationOffset += line.length + 1
                        modifiedLine
                    }
            if (violated && autoCorrect) {
                (node as LeafPsiElement).rawReplaceWithText(modifiedLines.joinToString(separator = "\n"))
            }
        }
    }

    private fun ASTNode.isPartOfKDoc() = parent({ it.psi is KDoc }, strict = false) != null

    private fun ASTNode.hasTrailingSpacesBeforeNewline() =
        text.contains(SPACE_OR_TAB_BEFORE_NEWLINE_REGEX)

    private fun ASTNode.removeTrailingSpacesBeforeNewline() {
        val newText = text.replace(
            regex = SPACE_OR_TAB_BEFORE_NEWLINE_REGEX,
            replacement = "\n"
        )
        (this as LeafPsiElement).replaceWithText(newText)
    }

    private fun String.hasTrailingSpace() =
        takeLast(1) == " "

    private companion object {
        val SPACE_OR_TAB_BEFORE_NEWLINE_REGEX = Regex(" +\\n")
    }
}
