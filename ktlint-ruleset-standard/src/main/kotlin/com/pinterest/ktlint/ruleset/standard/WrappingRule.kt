package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.EditorConfig.Companion.loadEditorConfig
import com.pinterest.ktlint.core.EditorConfig.Companion.loadIndentConfig
import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.COMMA
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.LBRACKET
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.LT
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACKET
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lineIndent
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.core.ast.visit
import com.pinterest.ktlint.core.initKtLintKLogger
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeList

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * This rule inserts missing newlines (e.g. between parentheses of a multi-line function call). This logic previously
 * was part of the IndentationRule (phase 1).
 *
 * Current limitations:
 * - "all or nothing" (currently, rule can only be disabled for an entire file)
 * - Whenever a linebreak is inserted, this rules assumes that the parent node it indented correctly. So the indentation
 *   is fixed with respect to indentation of the parent. This is just a simple best effort for the case that the
 *   indentation rule is not run.
 */
public class WrappingRule : Rule(
    id = "wrapping",
    visitorModifiers = setOf(VisitorModifier.RunOnRootNodeOnly)
) {
    private companion object {
        private val lTokenSet = TokenSet.create(LPAR, LBRACE, LBRACKET, LT)
        private val rTokenSet = TokenSet.create(RPAR, RBRACE, RBRACKET, GT)
        private val matchingRToken =
            lTokenSet.types.zip(
                rTokenSet.types
            ).toMap()
    }

    private var line = 1
    private lateinit var indentConfig: IndentConfig

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        line = 1
        indentConfig = node.loadEditorConfig().loadIndentConfig()
        node.visit { n -> // TODO: Check whether this visit can be removed like other rules. This would disabling the rule for blocks and lines
            when (n.elementType) {
                LPAR, LBRACE, LBRACKET -> rearrangeBlock(n, autoCorrect, emit) // TODO: LT
                SUPER_TYPE_LIST -> rearrangeSuperTypeList(n, autoCorrect, emit)
                VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST -> rearrangeValueList(n, autoCorrect, emit)
                ARROW -> rearrangeArrow(n, autoCorrect, emit)
                WHITE_SPACE -> line += n.text.count { it == '\n' }
                CLOSING_QUOTE -> rearrangeClosingQuote(n, autoCorrect, emit)
            }
        }
    }

    private fun rearrangeBlock(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val rElementType = matchingRToken[node.elementType]
        var newlineInBetween = false
        var parameterListInBetween = false
        var numberOfArgs = 0
        var firstArg: ASTNode? = null
        // matching ), ] or }
        val r = node.nextSibling {
            val isValueArgument = it.elementType == VALUE_ARGUMENT
            val hasLineBreak = if (isValueArgument) it.hasLineBreak(LAMBDA_EXPRESSION, FUN) else it.hasLineBreak()
            newlineInBetween = newlineInBetween || hasLineBreak
            parameterListInBetween = parameterListInBetween || it.elementType == VALUE_PARAMETER_LIST
            if (isValueArgument) {
                numberOfArgs++
                firstArg = it
            }
            it.elementType == rElementType
        }!!
        if (
            !newlineInBetween ||
            // keep { p ->
            // }
            (node.elementType == LBRACE && parameterListInBetween) ||
            // keep ({
            // }) and (object : C {
            // })
            (
                numberOfArgs == 1 &&
                    firstArg?.firstChildNode?.elementType
                    ?.let { it == OBJECT_LITERAL || it == LAMBDA_EXPRESSION } == true
                )
        ) {
            return
        }
        if (!node.nextCodeLeaf()?.prevLeaf {
            // Skip comments, whitespace, and empty nodes
            !it.isPartOfComment() &&
                !it.isWhiteSpaceWithoutNewline() &&
                it.textLength > 0
        }.isWhiteSpaceWithNewline() &&
            // IDEA quirk:
            // if (true &&
            //     true
            // ) {
            // }
            // instead of
            // if (
            //     true &&
            //     true
            // ) {
            // }
            node.treeNext?.elementType != CONDITION
        ) {
            requireNewlineAfterLeaf(node, autoCorrect, emit)
        }
        if (!r.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineBeforeLeaf(r, autoCorrect, emit, node.treeParent.lineIndent())
        }
    }

    private fun rearrangeSuperTypeList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val entries = (node.psi as KtSuperTypeList).entries
        if (
            node.textContains('\n') &&
            entries.size > 1 &&
            // e.g.
            //
            // class A : B, C,
            //     D
            // or
            // class A : B, C({
            // }), D
            //
            // but not
            //
            // class A : B, C, D({
            // })
            !(
                entries.dropLast(1).all { it.elementType == SUPER_TYPE_ENTRY } &&
                    entries.last().elementType == SUPER_TYPE_CALL_ENTRY
                )
        ) {
            // put space after :
            if (!node.prevLeaf().isWhiteSpaceWithNewline()) {
                val colon = node.prevCodeLeaf()!!
                if (
                    !colon.prevLeaf().isWhiteSpaceWithNewline() &&
                    colon.prevCodeLeaf().let { it?.elementType != RPAR || !it.prevLeaf().isWhiteSpaceWithNewline() }
                ) {
                    requireNewlineAfterLeaf(colon, autoCorrect, emit, node.lineIndent() + indentConfig.indent)
                }
            }
            // put entries on separate lines
            // TODO: group emit()s below with the one above into one (similar to ParameterListWrappingRule)
            for (c in node.children()) {
                if (c.elementType == COMMA &&
                    !c.treeNext.isWhiteSpaceWithNewline() &&
                    !c.isFollowedByCommentOnSameLine()
                ) {
                    requireNewlineAfterLeaf(
                        nodeAfterWhichNewlineIsRequired = c,
                        autoCorrect = autoCorrect,
                        emit = emit,
                        indent = node.lineIndent()
                    )
                }
            }
        }
    }

    private fun ASTNode.isFollowedByCommentOnSameLine() =
        nextLeaf { !it.isWhiteSpaceWithoutNewline() }
            ?.isPartOfComment() == true

    private fun rearrangeValueList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        for (c in node.children()) {
            val hasLineBreak = when (c.elementType) {
                VALUE_ARGUMENT -> c.hasLineBreak(LAMBDA_EXPRESSION, FUN)
                VALUE_PARAMETER, ANNOTATION -> c.hasLineBreak()
                else -> false
            }
            if (hasLineBreak) {
                // rearrange
                //
                // a, b, value(
                // ), c, d
                //
                // to
                //
                // a, b,
                // value(
                // ),
                // c, d

                // insert \n in front of multi-line value
                val prevSibling = c.prevSibling { it.elementType != WHITE_SPACE }
                if (
                    prevSibling?.elementType == COMMA &&
                    !prevSibling.treeNext.isWhiteSpaceWithNewline()
                ) {
                    requireNewlineAfterLeaf(prevSibling, autoCorrect, emit)
                }
                // insert \n after multi-line value
                val nextSibling = c.nextSibling { it.elementType != WHITE_SPACE }
                if (
                    nextSibling?.elementType == COMMA &&
                    !nextSibling.treeNext.isWhiteSpaceWithNewline() &&
                    // value(
                    // ), // a comment
                    // c, d
                    nextSibling.treeNext?.treeNext?.psi !is PsiComment
                ) {
                    requireNewlineAfterLeaf(nextSibling, autoCorrect, emit)
                }
            }
        }
    }

    private fun rearrangeClosingQuote(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        node
            .treeParent
            .takeIf { it.elementType == STRING_TEMPLATE }
            ?.let { it.psi as KtStringTemplateExpression }
            ?.takeIf { it.isMultiLine() }
            ?.takeIf { it.isFollowedByTrimIndent() || it.isFollowedByTrimMargin() }
            ?.takeIf { node.treePrev.text.isNotBlank() }
            ?.let {
                // rewriting
                // """
                //     text
                // _""".trimIndent()
                // to
                // """
                //     text
                // _
                // """.trimIndent()
                emit(
                    node.startOffset,
                    "Missing newline before \"\"\"",
                    true
                )
                if (autoCorrect) {
                    node as LeafPsiElement
                    node.rawInsertBeforeMe(LeafPsiElement(REGULAR_STRING_PART, "\n"))
                }
                logger.trace { "$line: " + (if (!autoCorrect) "would have " else "") + "inserted newline before (closing) \"\"\"" }
            }
    }

    private fun mustBeFollowedByNewline(node: ASTNode): Boolean {
        // find EOL token (last token before \n)
        // if token is in lTokenSet
        //     find matching rToken
        //     return true if there is no newline after the rToken
        // return false
        val nextCodeSibling = node.nextCodeSibling() // e.g. BINARY_EXPRESSION
        var lToken = nextCodeSibling?.nextLeaf { it.isWhiteSpaceWithNewline() }?.prevCodeLeaf()
        if (lToken != null && lToken.elementType !in lTokenSet) {
            // special cases:
            // x = y.f({ z ->
            // })
            // x = y.f(0, 1,
            // 2, 3)
            lToken = lToken.prevLeaf { it.elementType in lTokenSet || it == node }
        }
        if (lToken != null && lToken.elementType in lTokenSet) {
            val rElementType = matchingRToken[lToken.elementType]
            val rToken = lToken.nextSibling { it.elementType == rElementType }
            return rToken?.treeParent == lToken.treeParent
        }
        if (nextCodeSibling?.textContains('\n') == false) {
            return true
        }
        return false
    }

    private fun rearrangeArrow(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val p = node.treeParent
        if (
            // check
            // `{ p -> ... }`
            // and
            // `when { m -> ... }`
            // only
            p.elementType.let { it != FUNCTION_LITERAL && it != WHEN_ENTRY } ||
            // ... and only if expression after -> spans multiple lines
            !p.textContains('\n') ||
            // permit
            // when {
            //     m -> 0 + d({
            //     })
            // }
            (p.elementType == WHEN_ENTRY && mustBeFollowedByNewline(node)) ||
            // permit
            // when (this) {
            //     in 0x1F600..0x1F64F, // Emoticons
            //     0x200D // Zero-width Joiner
            //     -> true
            // }
            (p.elementType == WHEN_ENTRY && node.prevLeaf()?.textContains('\n') == true)
        ) {
            return
        }
        if (!node.nextCodeLeaf()?.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineAfterLeaf(node, autoCorrect, emit)
        }
        val r = node.nextSibling { it.elementType == RBRACE } ?: return
        if (!r.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineBeforeLeaf(r, autoCorrect, emit, node.lineIndent())
        }
    }

    private fun requireNewlineBeforeLeaf(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        indent: String
    ) {
        emit(
            node.startOffset - 1,
            """Missing newline before "${node.text}"""",
            true
        )
        logger.trace { "$line: " + ((if (!autoCorrect) "would have " else "") + "inserted newline before ${node.text}") }
        if (autoCorrect) {
            (node.psi as LeafPsiElement).upsertWhitespaceBeforeMe("\n" + indent)
        }
    }

    private fun requireNewlineAfterLeaf(
        nodeAfterWhichNewlineIsRequired: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        indent: String? = null,
        nodeToFix: ASTNode = nodeAfterWhichNewlineIsRequired
    ) {
        emit(
            nodeAfterWhichNewlineIsRequired.startOffset + 1,
            """Missing newline after "${nodeAfterWhichNewlineIsRequired.text}"""",
            true
        )
        logger.trace { "$line: " + (if (!autoCorrect) "would have " else "") + "inserted newline after ${nodeAfterWhichNewlineIsRequired.text}" }
        if (autoCorrect) {
            val tempIndent = indent ?: (nodeToFix.lineIndent() + indentConfig.indent)
            (nodeToFix.psi as LeafPsiElement).upsertWhitespaceAfterMe("\n" + tempIndent)
        }
    }

    private fun KtStringTemplateExpression.isMultiLine(): Boolean {
        for (child in node.children()) {
            if (child.elementType == LITERAL_STRING_TEMPLATE_ENTRY) {
                val v = child.text
                if (v == "\n") {
                    return true
                }
            }
        }
        return false
    }

    private fun ASTNode.hasLineBreak(vararg ignoreElementTypes: IElementType): Boolean {
        if (isWhiteSpaceWithNewline()) return true
        return if (ignoreElementTypes.isEmpty()) {
            textContains('\n')
        } else {
            elementType !in ignoreElementTypes &&
                children().any { c -> c.textContains('\n') && c.elementType !in ignoreElementTypes }
        }
    }

    private fun KtStringTemplateExpression.isFollowedByTrimIndent() = isFollowedBy("trimIndent()")

    private fun KtStringTemplateExpression.isFollowedByTrimMargin() = isFollowedBy("trimMargin()")

    private fun KtStringTemplateExpression.isFollowedBy(callExpressionName: String) =
        this.node.nextSibling { it.elementType != DOT }
            .let { it?.elementType == CALL_EXPRESSION && it.text == callExpressionName }
}
