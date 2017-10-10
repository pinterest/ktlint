package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class LambdaOutsideOfParensRule : Rule("lambda-outside-of-parens") {

    fun PsiElement.isLambda() = this is KtValueArgument && this.getArgumentExpression() is KtLambdaExpression

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val psi = node.psi
        if (psi is KtCallExpression) {
            /*
             org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
               org.jetbrains.kotlin.psi.KtNameReferenceExpression (REFERENCE_EXPRESSION)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (IDENTIFIER) | "f0"
               org.jetbrains.kotlin.psi.KtValueArgumentList (VALUE_ARGUMENT_LIST)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (LPAR) | "("
                 org.jetbrains.kotlin.psi.KtValueArgument (VALUE_ARGUMENT)
                   org.jetbrains.kotlin.psi.KtLambdaExpression (LAMBDA_EXPRESSION)
                     org.jetbrains.kotlin.psi.KtFunctionLiteral (FUNCTION_LITERAL)
                       ...
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (RPAR) | ")"

             org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
               org.jetbrains.kotlin.psi.KtNameReferenceExpression (REFERENCE_EXPRESSION)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (IDENTIFIER) | "f1"
               org.jetbrains.kotlin.psi.KtValueArgumentList (VALUE_ARGUMENT_LIST)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (LPAR) | "("
                 org.jetbrains.kotlin.psi.KtValueArgument (VALUE_ARGUMENT)
                   ...
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (COMMA) | ","
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) | " "
                 org.jetbrains.kotlin.psi.KtValueArgument (VALUE_ARGUMENT)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (RPAR) | ")"

             org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
               org.jetbrains.kotlin.psi.KtNameReferenceExpression (REFERENCE_EXPRESSION)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (IDENTIFIER) | "f3"
               org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) | " "
               org.jetbrains.kotlin.psi.KtLambdaArgument (LAMBDA_ARGUMENT)
                 org.jetbrains.kotlin.psi.KtLambdaExpression (LAMBDA_EXPRESSION)
                   org.jetbrains.kotlin.psi.KtFunctionLiteral (FUNCTION_LITERAL)
                     ...

             org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
               org.jetbrains.kotlin.psi.KtNameReferenceExpression (REFERENCE_EXPRESSION)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (IDENTIFIER) | "f4"
               org.jetbrains.kotlin.psi.KtValueArgumentList (VALUE_ARGUMENT_LIST)
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (LPAR) | "("
                 org.jetbrains.kotlin.psi.KtValueArgument (VALUE_ARGUMENT)
                   ...
                 org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (RPAR) | ")"
               org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) | " "
               org.jetbrains.kotlin.psi.KtLambdaArgument (LAMBDA_ARGUMENT)
             */
            val args = psi.valueArguments
            if (!args.isEmpty()) {
                val lastArg = args.last()
                if (lastArg.isLambda()) {
                    // if there is more than 1 lambda - they must be all enclosed in ()
                    if (args.count { it.isLambda() } > 1) {
                        //if (lastArg.parent !is KtValueArgumentList && args.none { it.isNamed() }) {
                        if (lastArg is KtLambdaArgument && args.none { it.isNamed() }) {
                            emit(lastArg.startOffset, "Lambda must be enclosed in parentheses", true)
                            if (autoCorrect) {
                                lastArg.moveInsideParentheses()
                            }
                        }
                    } else {
                        // otherwise lambda should be put outside
                        if (lastArg.parent is KtValueArgumentList) {
                            emit(lastArg.startOffset, "Lambda must be outside of parentheses", true)
                            if (autoCorrect) {
                                psi.moveFunctionLiteralOutsideParentheses()
                            }
                        }
                    }
                }
            }
        }

    }

    // Modified version of
    // https://github.com/JetBrains/kotlin/blob/0f9d31c9d1b536c301ce67c9cd4cb130e90f1bd1/idea/idea-core/
    //   src/org/jetbrains/kotlin/idea/core/psiModificationUtils.kt#L67
    // Original copyright 2010-2015 JetBrains s.r.o., Licensed under the Apache License, Version 2.0.
    fun KtLambdaArgument.moveInsideParentheses() {
        val oldCallExpression = parent as KtCallExpression
        val newCallExpression = oldCallExpression.copy() as KtCallExpression
        val psiFactory = KtPsiFactory(project)
        val argument = psiFactory.createArgument(this.getLambdaExpression())
        val functionLiteralArgument = newCallExpression.lambdaArguments.firstOrNull()!!
        val valueArgumentList = newCallExpression.valueArgumentList ?: psiFactory.createCallArguments("()")
        valueArgumentList.addArgument(argument)
        (functionLiteralArgument.prevSibling as? PsiWhiteSpace)?.delete()
        if (newCallExpression.valueArgumentList != null) {
            functionLiteralArgument.delete()
        } else {
            functionLiteralArgument.replace(valueArgumentList)
        }
        val lastArg = valueArgumentList.arguments.last()
        val prevLeaf = PsiTreeUtil.prevLeaf(lastArg)
        if (prevLeaf is LeafPsiElement && prevLeaf.node.elementType == KtTokens.COMMA) {
            prevLeaf.rawInsertAfterMe(PsiWhiteSpaceImpl(" "))
        }
        oldCallExpression.replace(newCallExpression)
    }

    // Modified version of
    // https://github.com/JetBrains/kotlin/blob/0f9d31c9d1b536c301ce67c9cd4cb130e90f1bd1/idea/idea-core/
    //   src/org/jetbrains/kotlin/idea/core/psiModificationUtils.kt#L105
    // Original copyright 2010-2015 JetBrains s.r.o., Licensed under the Apache License, Version 2.0.
    fun KtCallExpression.moveFunctionLiteralOutsideParentheses() {
        val argumentList = valueArgumentList!!
        val argument = argumentList.arguments.last()
        val expression = argument.getArgumentExpression()!!
        val dummyCall = KtPsiFactory(this).createExpressionByPattern("foo()$0:'{}'", expression,
            reformat = false) as KtCallExpression
        val functionLiteralArgument = dummyCall.lambdaArguments.single()
        if (argumentList.arguments.size == 1 && calleeExpression !is KtCallExpression) {
            argumentList.delete()
        } else {
            val prevLeaf = PsiTreeUtil.prevLeaf(argument)
            if (prevLeaf is PsiWhiteSpace) {
                prevLeaf.node.treeParent.removeChild(prevLeaf.node)
            }
            val nextLeaf = PsiTreeUtil.nextLeaf(argument)
            if (nextLeaf is PsiWhiteSpace) {
                nextLeaf.node.treeParent.removeChild(nextLeaf.node)
            }
             argumentList.removeArgument(argument)
        }
        this.node.addChild(PsiWhiteSpaceImpl(" "), null)
        this.node.addChild(functionLiteralArgument.node, null)
    }

}
