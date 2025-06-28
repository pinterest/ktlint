package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_TEMPLATE_ENTRY_END
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_TEMPLATE_ENTRY_START
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SHORT_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THIS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.KtlintKotlinCompiler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children20
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isCode
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

@SinceKtlint("0.9", STABLE)
public class StringTemplateRule : StandardRule("string-template") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        // code below is commented out because (setting aside potentially dangerous replaceChild part)
        // `val v: String = "$elementType"` would be rewritten to `val v: String = elementType.toString()` and it's not
        // immediately clear which is better
        // if (elementType === SHORT_STRING_TEMPLATE_ENTRY || elementType == LONG_STRING_TEMPLATE_ENTRY) {
        //    if (node.treePrev.elementType == OPEN_QUOTE && node.treeNext.elementType == CLOSING_QUOTE) {
        //        emit(node.treePrev.startOffset, "Redundant string template", true)
        //        val entryStart = node.psi.firstChild
        //        node.treeParent.treeParent.replaceChild(node.treeParent, entryStart.nextSibling.node)
        //    }
        if (node.elementType == LONG_STRING_TEMPLATE_ENTRY) {
            node.removeRedundantToString(emit)
            node.checkForRedundantCurlyBraces(emit)
        }
    }

    private fun ASTNode.removeRedundantToString(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        findChildByType(DOT_QUALIFIED_EXPRESSION)
            ?.let { dotQualifiedExpression ->
                dotQualifiedExpression
                    .splitCodeChildren()
                    .let { (receiver, dot, selector) ->
                        if (receiver.elementType != SUPER_EXPRESSION &&
                            selector.elementType == CALL_EXPRESSION &&
                            selector.text == "toString()"
                        ) {
                            emit(
                                dot.startOffset,
                                "Redundant \".toString()\" call in string template",
                                true,
                            ).ifAutocorrectAllowed {
                                dotQualifiedExpression.treeParent.addChild(receiver, dotQualifiedExpression)
                                dotQualifiedExpression.remove()
                                takeIf { it.isStringTemplate() }
                                    ?.removeCurlyBracesIfRedundant()
                            }
                        }
                    }
            }
    }

    private fun ASTNode.splitCodeChildren(): Triple<ASTNode, ASTNode, ASTNode> {
        require(elementType == DOT_QUALIFIED_EXPRESSION)
        return children20
            .filter { it.isCode }
            .toList()
            .also { require(it.size == 3) }
            .let { Triple(it[0], it[1], it[2]) }
    }

    private fun ASTNode.checkForRedundantCurlyBraces(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        takeIf { it.isStringTemplate() }
            ?.children20
            ?.firstOrNull { it.elementType != LONG_TEMPLATE_ENTRY_START }
            ?.takeIf { it.elementType == REFERENCE_EXPRESSION || it.elementType == THIS_EXPRESSION }
            ?.let {
                nextSibling20
                    ?.takeIf { nextSibling ->
                        nextSibling.elementType == CLOSING_QUOTE ||
                            (
                                nextSibling.elementType == LITERAL_STRING_TEMPLATE_ENTRY &&
                                    !nextSibling.text.substring(0, 1).isPartOfIdentifier()
                            )
                    }?.let {
                        emit(prevSibling20!!.startOffset + 2, "Redundant curly braces", true)
                            .ifAutocorrectAllowed { removeCurlyBracesIfRedundant() }
                    }
            }
    }

    private fun ASTNode.removeCurlyBracesIfRedundant() {
        if (isStringTemplate()) {
            val leftCurlyBraceNode = findChildByType(LONG_TEMPLATE_ENTRY_START)
            val rightCurlyBraceNode = findChildByType(LONG_TEMPLATE_ENTRY_END)
            if (leftCurlyBraceNode != null && rightCurlyBraceNode != null) {
                removeChild(leftCurlyBraceNode)
                removeChild(rightCurlyBraceNode)
                firstChildNode
                    .toShortStringTemplateNode()
                    .let { replaceChild(firstChildNode, it) }
            }
        }
    }

    private fun ASTNode.isStringTemplate() =
        text.startsWith("${'$'}{") &&
            text.substring(2, text.length - 1).isPartOfIdentifier()

    private fun String.isPartOfIdentifier() = this == "_" || this.all { it.isLetterOrDigit() }

    private fun ASTNode.toShortStringTemplateNode() =
        KtlintKotlinCompiler
            .createASTNodeFromText(
                """
                val foo = "${'$'}$text"
                """.trimIndent(),
            )?.findChildByType(PROPERTY)
            ?.findChildByType(STRING_TEMPLATE)
            ?.findChildByType(SHORT_STRING_TEMPLATE_ENTRY)
            ?: throw IllegalStateException("Cannot create short string template for string '$text")
}

public val STRING_TEMPLATE_RULE_ID: RuleId = StringTemplateRule().ruleId
