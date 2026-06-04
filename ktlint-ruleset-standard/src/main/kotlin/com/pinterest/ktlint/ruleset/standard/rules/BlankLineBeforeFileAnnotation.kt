package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleV2
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Insert a blank line before a file annotation.
 */
@SinceKtlint("2.0", EXPERIMENTAL)
public class BlankLineBeforeFileAnnotation :
    StandardRule("blank-line-before-file-annotation"),
    RuleV2.Experimental {
    override fun beforeFirstNode(editorConfig: EditorConfig) {
        if (editorConfig[CODE_STYLE_PROPERTY] == CodeStyleValue.intellij_idea) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (node.elementType) {
            FILE_ANNOTATION_LIST -> {
                node
                    .takeUnless { it.prevLeaf.isBlankLine() }
                    ?.let { insertBeforeNode ->
                        emit(insertBeforeNode.startOffset, "Expected a blank line before the file annotation(s)", true)
                            .ifAutocorrectAllowed {
                                insertBeforeNode.upsertWhitespaceBeforeMe("\n".plus(node.indent))
                            }
                    }
                stopTraversalOfAST()
            }

            PACKAGE_DIRECTIVE, IMPORT_LIST -> {
                stopTraversalOfAST()
            }
        }
    }

    private fun ASTNode?.isBlankLine() = this == null || (elementType == WHITE_SPACE && text.count { it == '\n' } > 1)
}

public val BLANK_LINE_BEFORE_FILE_ANNOTATION_RULE_ID: RuleId = BlankLineBeforeFileAnnotation().ruleId
