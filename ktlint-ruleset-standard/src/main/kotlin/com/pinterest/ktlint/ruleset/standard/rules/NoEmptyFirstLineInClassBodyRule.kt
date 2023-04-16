package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

public class NoEmptyFirstLineInClassBodyRule :
    StandardRule(
        id = "no-empty-first-line-in-class-body",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == CLASS_BODY) {
            node
                .firstChildNode // LBRACE
                .nextLeaf()
                .takeIf { it.isWhiteSpaceWithNewline() }
                ?.let { whitespace ->
                    val countNewlines =
                        whitespace
                            .text
                            .count { it == '\n' }
                    if (countNewlines > 1) {
                        emit(
                            whitespace.startOffset + 1,
                            "Class body should not start with blank line",
                            true,
                        )
                        if (autoCorrect) {
                            (whitespace as LeafPsiElement).rawReplaceWithText(
                                node.indent().plus(indentConfig.indent),
                            )
                        }
                    }
                }
        }
    }
}

public val NO_EMPTY_FIRST_LINE_IN_CLASS_BODY_RULE_ID: RuleId = NoEmptyFirstLineInClassBodyRule().ruleId
