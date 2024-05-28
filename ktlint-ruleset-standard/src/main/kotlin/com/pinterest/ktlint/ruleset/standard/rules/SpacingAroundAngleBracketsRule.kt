package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAL_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

@SinceKtlint("0.30", STABLE)
public class SpacingAroundAngleBracketsRule : StandardRule("spacing-around-angle-brackets") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType != TYPE_PARAMETER_LIST && node.elementType != TYPE_ARGUMENT_LIST) {
            return
        }

        val openingBracket = node.firstChildNode
        if (openingBracket != null) {
            // Check for rogue spacing before an opening bracket, e.g. Map <String, Int>
            val beforeLeftAngle = openingBracket.prevLeaf()
            if (beforeLeftAngle?.elementType == WHITE_SPACE) {
                // Ignore when the whitespace is preceded by certain keywords, e.g. fun <T> func(arg: T) {}
                if (!ELEMENT_TYPES_ALLOWING_PRECEDING_WHITESPACE.contains(beforeLeftAngle.prevLeaf()?.elementType)) {
                    emit(beforeLeftAngle.startOffset, "Unexpected spacing before \"<\"", true)
                        .ifAutocorrectAllowed {
                            beforeLeftAngle.treeParent.removeChild(beforeLeftAngle)
                        }
                }
            }

            // Check for rogue spacing after an opening bracket
            val afterLeftAngle = openingBracket.nextLeaf()
            if (afterLeftAngle?.elementType == WHITE_SPACE) {
                if (afterLeftAngle.isWhiteSpaceWithoutNewline()) {
                    emit(afterLeftAngle.startOffset, "Unexpected spacing after \"<\"", true)
                        .ifAutocorrectAllowed {
                            // when spacing does not include any new lines, e.g. Map< String, Int>
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
                    if (newLineWithIndent != afterLeftAngle.text) {
                        emit(afterLeftAngle.startOffset, "Single newline expected after \"<\"", true)
                            .ifAutocorrectAllowed {
                                (afterLeftAngle as LeafElement).rawReplaceWithText(newLineWithIndent)
                            }
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
                    emit(beforeRightAngle.startOffset, "Unexpected spacing before \">\"", true)
                        .ifAutocorrectAllowed {
                            // when spacing does not include any new lines, e.g. Map<String, Int >
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
                    if (newLineWithIndent != beforeRightAngle.text) {
                        emit(beforeRightAngle.startOffset, "Single newline expected before \">\"", true)
                            .ifAutocorrectAllowed {
                                (beforeRightAngle as LeafElement).rawReplaceWithText(newLineWithIndent)
                            }
                    }
                }
            }
        }
    }

    private fun String.trimBeforeLastLine() = this.substring(this.lastIndexOf('\n'))

    private companion object {
        val ELEMENT_TYPES_ALLOWING_PRECEDING_WHITESPACE = setOf(VAL_KEYWORD, VAR_KEYWORD, FUN_KEYWORD)
    }
}

public val SPACING_AROUND_ANGLE_BRACKETS_RULE_ID: RuleId = SpacingAroundAngleBracketsRule().ruleId
