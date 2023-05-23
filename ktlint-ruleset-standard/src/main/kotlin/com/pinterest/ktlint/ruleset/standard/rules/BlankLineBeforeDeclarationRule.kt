package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * Insert a blank line before declarations. No blank line is inserted before between the class signature and the first declaration in the
 * class. Also, no blank lines are inserted between consecutive properties.
 */
public class BlankLineBeforeDeclarationRule :
    StandardRule("blank-line-before-declaration"),
    Rule.Experimental,
    Rule.OfficialCodeStyle {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            CLASS,
            FUN,
            PROPERTY,
            PROPERTY_ACCESSOR,
            ->
                visitDeclaration(node, autoCorrect, emit)
        }
    }

    private fun visitDeclaration(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node == node.firstCodeSiblingInClassBodyOrNull()) {
            // Allow missing blank line between class signature and first code sibling in class body:
            //   Class Foo {
            //      fun bar() {}
            //   }
            return
        }

        if (node.isConsecutiveProperty()) {
            // Allow consecutive properties:
            //   val foo = "foo"
            //   val bar = "bar"
            return
        }

        if (node.isLocalProperty()) {
            // Allow:
            //   fun foo() {
            //       bar()
            //       val foobar = "foobar"
            //   }
            return
        }

        node
            .siblings(false)
            .takeWhile { it.isWhiteSpace() || it.isPartOfComment() }
            .lastOrNull()
            ?.let { previous ->
                when {
                    !previous.isWhiteSpace() -> previous
                    !previous.text.startsWith("\n\n") -> node
                    else -> null
                }?.let { insertBeforeNode ->
                    emit(insertBeforeNode.startOffset, "Expected a blank line for this declaration", true)
                    if (autoCorrect) {
                        insertBeforeNode.upsertWhitespaceBeforeMe("\n".plus(node.indent()))
                    }
                }
            }
    }

    private fun ASTNode.firstCodeSiblingInClassBodyOrNull() =
        treeParent
            .takeIf { it.elementType == ElementType.CLASS_BODY }
            ?.findChildByType(LBRACE)
            ?.nextCodeSibling()

    private fun ASTNode.isConsecutiveProperty() =
        takeIf { it.propertyRelated() }
            ?.prevCodeSibling()
            ?.let { it.propertyRelated() || it.treeParent.propertyRelated() }
            ?: false

    private fun ASTNode.isLocalProperty() =
        takeIf { it.propertyRelated() }
            ?.treeParent
            ?.let { it.elementType == BLOCK }
            ?: false

    private fun ASTNode.propertyRelated() = elementType == PROPERTY || elementType == PROPERTY_ACCESSOR
}

public val BLANK_LINE_BEFORE_DECLARATION_RULE_ID: RuleId = BlankLineBeforeDeclarationRule().ruleId
