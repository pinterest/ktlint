package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.EOL_COMMENT
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_END
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_LEADING_ASTERISK
import io.github.ktlint.core.rule.engine.core.api.ElementType.KDOC_TEXT
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.STABLE
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isCode
import io.github.ktlint.core.rule.engine.core.api.isPartOf
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpace
import io.github.ktlint.core.rule.engine.core.api.nextLeaf
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.1", STABLE)
public class NoTrailingSpacesRule : StandardRule("no-trailing-spaces") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isPartOfKDoc()) {
            if (node.isWhiteSpace && node.hasTrailingSpacesBeforeNewline()) {
                val offsetOfSpaceBeforeNewlineInText = node.text.indexOf(" \n")
                val offsetOfFirstSpaceBeforeNewlineInText =
                    node
                        .text
                        .take(offsetOfSpaceBeforeNewlineInText)
                        .dropLastWhile { it == ' ' }
                        .length
                emit(node.startOffset + offsetOfFirstSpaceBeforeNewlineInText, "Trailing space(s)", true)
                    .ifAutocorrectAllowed { node.removeTrailingSpacesBeforeNewline() }
            }

            if (node.elementType == KDOC_TEXT &&
                node.text.endsWith(" ") &&
                (
                    node.nextLeaf
                        ?.takeIf { it.nextLeaf?.elementType == KDOC_LEADING_ASTERISK || it.nextLeaf?.elementType == KDOC_END }
                        ?.text
                        ?.startsWith("\n") == true
                )
            ) {
                node
                    .text
                    .trimEnd()
                    .let { trimmedText ->
                        emit(node.startOffset + trimmedText.length, "Trailing space(s)", true)
                            .ifAutocorrectAllowed { node.replaceTextWith(trimmedText) }
                    }
            }
        } else if (!node.isCode) {
            val lines = node.text.split("\n")
            var autocorrect = false
            var violationOffset = node.startOffset

            val modifiedLines =
                lines
                    .mapIndexed { index, line ->
                        val modifiedLine =
                            when {
                                node.elementType != EOL_COMMENT && index == lines.size - 1 && node.nextLeaf != null -> {
                                    // Do not change the last line as it contains the indentation of the next element except
                                    // when it is an EOL comment which may also not contain trailing spaces
                                    line
                                }

                                line.hasTrailingSpace() -> {
                                    val modifiedLine = line.trimEnd()
                                    val firstTrailingSpaceOffset = violationOffset + modifiedLine.length
                                    emit(firstTrailingSpaceOffset, "Trailing space(s)", true)
                                        .ifAutocorrectAllowed {
                                            autocorrect = true
                                        }
                                    modifiedLine
                                }

                                else -> {
                                    line
                                }
                            }
                        violationOffset += line.length + 1
                        modifiedLine
                    }
            if (autocorrect) {
                node.replaceTextWith(modifiedLines.joinToString(separator = "\n"))
            }
        }
    }

    private fun ASTNode.isPartOfKDoc(): Boolean = isPartOf(KDOC)

    private fun ASTNode.hasTrailingSpacesBeforeNewline() = text.contains(SPACE_OR_TAB_BEFORE_NEWLINE_REGEX)

    private fun ASTNode.removeTrailingSpacesBeforeNewline() {
        val newText =
            text.replace(
                regex = SPACE_OR_TAB_BEFORE_NEWLINE_REGEX,
                replacement = "\n",
            )
        replaceTextWith(newText)
    }

    private fun String.hasTrailingSpace() = takeLast(1) == " "

    private companion object {
        val SPACE_OR_TAB_BEFORE_NEWLINE_REGEX = Regex(" +\\n")
    }
}

public val NO_TRAILING_SPACES_RULE_ID: RuleId = NoTrailingSpacesRule().ruleId
