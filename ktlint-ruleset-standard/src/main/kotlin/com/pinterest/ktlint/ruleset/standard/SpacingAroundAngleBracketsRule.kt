package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VAL_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.VAR_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

public class SpacingAroundAngleBracketsRule : Rule("spacing-around-angle-brackets") {
    private fun String.trimBeforeLastLine() = this.substring(this.lastIndexOf('\n'))

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
            val openingBracket = node.firstChildNode
            if (openingBracket != null) {
                // Check for rogue spacing before an opening bracket, e.g. Map <String, Int>
                val beforeLeftAngle = openingBracket.prevLeaf()
                if (beforeLeftAngle?.elementType == WHITE_SPACE) {
                    // Ignore when the whitespace is preceded by certain keywords, e.g. fun <T> func(arg: T) {}
                    if (!typesOkWithPrecedingWhitespace.contains(beforeLeftAngle.prevLeaf()?.elementType)) {
                        emit(beforeLeftAngle.startOffset, "Unexpected spacing before \"<\"", true)
                        if (autoCorrect) {
                            beforeLeftAngle.treeParent.removeChild(beforeLeftAngle)
                        }
                    }
                }

                // Check for rogue spacing after an opening bracket
                val afterLeftAngle = openingBracket.nextLeaf()
                if (afterLeftAngle?.elementType == WHITE_SPACE) {
                    if (afterLeftAngle.isWhiteSpaceWithoutNewline()) {
                        // when spacing does not include any new lines, e.g. Map< String, Int>
                        emit(afterLeftAngle.startOffset, "Unexpected spacing after \"<\"", true)
                        if (autoCorrect) {
                            afterLeftAngle.treeParent.removeChild(afterLeftAngle)
                        }
                    } else {
                        // when spacing contains at least one new line, e.g.
                        // SomeGenericType<[whitespace]
                        //
                        //      String, Int, String>
                        // gets converted to
                        // SomeGenericType<
                        //      String, Int, String>
                        val newLineWithIndent = afterLeftAngle.text.trimBeforeLastLine()
                        if (autoCorrect) {
                            (afterLeftAngle as LeafElement).rawReplaceWithText(newLineWithIndent)
                        }
                    }
                }
            }

            val closingBracket = node.lastChildNode
            if (closingBracket != null) {
                val beforeRightAngle = closingBracket.prevLeaf()
                // Check for rogue spacing before a closing bracket
                if (beforeRightAngle?.elementType == WHITE_SPACE) {
                    if (beforeRightAngle.isWhiteSpaceWithoutNewline()) {
                        // when spacing does not include any new lines, e.g. Map<String, Int >
                        emit(beforeRightAngle.startOffset, "Unexpected spacing before \">\"", true)
                        if (autoCorrect) {
                            beforeRightAngle.treeParent.removeChild(beforeRightAngle)
                        }
                    } else {
                        // when spacing contains at least one new line, e.g.
                        // SomeGenericType<String, Int, String[whitespace]
                        //
                        //      >
                        // gets converted to
                        // SomeGenericType<String, Int, String
                        //      >
                        val newLineWithIndent = beforeRightAngle.text.trimBeforeLastLine()
                        if (autoCorrect) {
                            (beforeRightAngle as LeafElement).rawReplaceWithText(newLineWithIndent)
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val typesOkWithPrecedingWhitespace = setOf(VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD)
    }
}
