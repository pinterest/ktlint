package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPEALIAS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet

/**
 * Disallow KDoc except of classes, functions and xxx
 */
@SinceKtlint("1.2.0", EXPERIMENTAL)
public class KdocRule :
    StandardRule(
        id = "kdoc",
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ),
    Rule.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .takeIf { it.elementType == KDOC }
            ?.let {
                if (it.treeParent.elementType in allowedParentElementTypes) {
                    if (it.treeParent.firstChildNode != it) {
                        emit(
                            node.startOffset,
                            "A KDoc is allowed only at start of '${it.treeParent.elementType.debugName.lowercase()}'",
                            false,
                        )
                    }
                } else {
                    if (it.treeParent.elementType == FILE) {
                        emit(node.startOffset, "A dangling toplevel KDoc is not allowed", false)
                    } else {
                        emit(
                            node.startOffset,
                            "A KDoc is not allowed inside '${it.treeParent.elementType.debugName.lowercase()}'",
                            false,
                        )
                    }
                }
            }
    }

    private companion object {
        val allowedParentElementTypes =
            TokenSet.create(
                CLASS,
                ENUM_ENTRY,
                FUN,
                OBJECT_DECLARATION,
                PROPERTY,
                SECONDARY_CONSTRUCTOR,
                TYPEALIAS,
                VALUE_PARAMETER,
            )
    }
}

public val KDOC_RULE_ID: RuleId = KdocRule().ruleId
