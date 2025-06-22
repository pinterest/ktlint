package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONSTRUCTOR_CALLEE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_START
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithoutNewline20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * Ensures there are no extra spaces around parentheses.
 *
 * See https://kotlinlang.org/docs/reference/coding-conventions.html#horizontal-whitespace
 */
@SinceKtlint("0.24", STABLE)
public class SpacingAroundParensRule : StandardRule("paren-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == LPAR || node.elementType == RPAR) {
            val spacingBefore = node.isUnexpectedSpacingBeforeParenthesis()
            val spacingAfter = node.isUnexpectedSpacingAfterParenthesis()
            when {
                spacingBefore && spacingAfter -> node.fixUnexpectedSpacingAround(emit)
                spacingBefore -> node.fixUnexpectedSpacingBefore(emit)
                spacingAfter -> node.fixUnexpectSpacingAfter(emit)
            }
        }
    }

    private fun ASTNode.isUnexpectedSpacingBeforeParenthesis(): Boolean =
        when {
            prevLeaf().isWhiteSpaceWithNewline20 && hasNoNewlineAfterLpar() -> {
                true
            }

            !prevLeaf().isWhiteSpaceWithoutNewline20 -> {
                false
            }

            elementType == LPAR -> {
                treeParent?.elementType in elementListTokenSet &&
                    (
                        isUnexpectedSpacingBetweenIdentifierAndElementList() ||
                            isUnexpectedSpacingInCallToSuper() ||
                            isUnexpectedSpacingInExplicitConstructor() ||
                            isUnexpectedSpacingInSuperTypeCallEntry()
                    )
            }

            elementType == RPAR -> {
                // Disallow:
                //    val foo = fn("foo" )
                //    val foo = fn( )
                prevLeaf()?.prevSibling()?.elementType != LPAR
            }

            else -> {
                false
            }
        }

    private fun ASTNode.isUnexpectedSpacingBetweenIdentifierAndElementList() =
        prevLeaf()
            ?.takeIf { it.isWhiteSpace20 }
            ?.takeIf {
                // Disallow:
                //     fun foo () {}
                // and
                //     @Deprecated ("bar)
                //     fun foo() {}
                it.prevLeaf()?.elementType == IDENTIFIER
            }?.let {
                // But do allow:
                //     val foo: @Composable () -> Unit
                treeParent?.treeParent?.elementType != FUNCTION_TYPE
            }
            ?: false

    private fun ASTNode.isUnexpectedSpacingInCallToSuper() =
        prevLeaf()
            ?.takeIf { it.isWhiteSpace20 }
            ?.let {
                // Disallow:
                //     class Foo : Bar {
                //         constructor(string: String) : super ()
                //     }
                it.prevLeaf()?.elementType == SUPER_KEYWORD
            }
            ?: false

    private fun ASTNode.isUnexpectedSpacingInExplicitConstructor() =
        prevLeaf()
            ?.takeIf { it.isWhiteSpace20 }
            ?.let {
                // Disallow:
                //     class Foo constructor ()
                it.prevLeaf()?.treeParent?.elementType == PRIMARY_CONSTRUCTOR
            }
            ?: false

    private fun ASTNode.isUnexpectedSpacingInSuperTypeCallEntry() =
        prevLeaf()
            ?.takeIf { it.isWhiteSpace20 }
            ?.let {
                // Disallow:
                //     class Foo : Bar ("test")
                //     class Foo : Bar<String> ("test")
                treeParent.treeParent.elementType == SUPER_TYPE_CALL_ENTRY &&
                    it.prevSibling()?.elementType == CONSTRUCTOR_CALLEE
            }
            ?: false

    private fun ASTNode.isUnexpectedSpacingAfterParenthesis(): Boolean =
        when {
            elementType == LPAR && nextSibling().isWhiteSpaceWithNewline20 && hasNoOtherNewlineBeforeRpar() -> {
                true
            }

            elementType == LPAR -> {
                nextLeaf
                    ?.takeUnless { it.isNextLeafAComment() }
                    ?.let { it.isUnexpectedSpaceAfterLpar() || it.isUnexpectedNewlineAfterLpar() }
                    ?: false
            }

            else -> {
                false
            }
        }

    private fun ASTNode.isUnexpectedSpaceAfterLpar() =
        // Disallow:
        //     val foo = fn( )
        //     val foo = fn( "bar")
        //     val foo = ( (1 + 2) / 3)
        isWhiteSpaceWithoutNewline20

    private fun ASTNode.isUnexpectedNewlineAfterLpar() =
        // Disallow:
        //     val foo = fn(
        //         )
        isWhiteSpaceWithNewline20 && nextLeaf?.elementType == RPAR

    private fun ASTNode.hasNoOtherNewlineBeforeRpar() =
        nextSibling()
            .takeIf { it.isWhiteSpaceWithNewline20 }
            ?.siblings()
            ?.takeWhile { it.elementType != RPAR }
            ?.none { it.isWhiteSpaceWithNewline20 }
            ?: false

    private fun ASTNode.isNextLeafAComment(): Boolean = nextLeaf?.elementType in commentTypes

    private fun ASTNode.hasNoNewlineAfterLpar() =
        prevSibling()
            .takeIf { it.isWhiteSpaceWithNewline20 }
            ?.takeUnless { it.prevSibling()?.elementType == LPAR }
            ?.siblings(false)
            ?.takeWhile { it.elementType != LPAR }
            ?.none { it.textContains('\n') }
            ?: false

    private fun ASTNode.fixUnexpectedSpacingAround(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(startOffset, "Unexpected spacing around \"$text\"", true)
            .ifAutocorrectAllowed {
                prevLeaf()!!.remove()
                nextLeaf!!.remove()
            }
    }

    private fun ASTNode.fixUnexpectedSpacingBefore(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(prevLeaf()!!.startOffset, "Unexpected spacing before \"${text}\"", true)
            .ifAutocorrectAllowed { prevLeaf()?.remove() }
    }

    private fun ASTNode.fixUnexpectSpacingAfter(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        emit(startOffset + 1, "Unexpected spacing after \"$text\"", true)
            .ifAutocorrectAllowed { nextLeaf!!.remove() }
    }

    private companion object {
        val elementListTokenSet = TokenSet.create(VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST)
        val commentTypes = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT, KDOC_START)
    }
}

public val SPACING_AROUND_PARENS_RULE_ID: RuleId = SpacingAroundParensRule().ruleId
