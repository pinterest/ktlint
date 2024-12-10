package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_INITIALIZER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EQ
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RETURN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.rule.engine.core.util.safeAs
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.stubs.elements.KtTokenSets

/**
 * Insert a blank line before declarations. No blank line is inserted before between a class or method signature and the first declaration
 * in the class or method respectively. Also, no blank lines are inserted between consecutive properties.
 */
@SinceKtlint("0.50", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class BlankLineBeforeDeclarationRule :
    StandardRule("blank-line-before-declaration"),
    Rule.OfficialCodeStyle {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        when (node.elementType) {
            CLASS,
            CLASS_INITIALIZER,
            FUN,
            OBJECT_DECLARATION,
            PROPERTY,
            PROPERTY_ACCESSOR,
            -> {
                visitDeclaration(node, emit)
            }
        }
    }

    private fun visitDeclaration(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isFirstCodeSiblingInClassBody()) {
            // No blank line between class signature and first declaration in class body:
            //   class Foo {
            //      fun bar() {}
            //   }
            //   class Foo {
            //      class bar
            //   }
            return
        }

        if (node.isFirstCodeSiblingInBlock()) {
            // No blank line between opening brace of block and first code sibling in class body:
            //   fun foo() {
            //      class Bar
            //   }
            //   val foo = {
            //      fun bar() {}
            //   }
            return
        }

        if (node.isFirstCodeSiblingInBodyOfFunctionLiteral()) {
            // No blank line between opening brace of function literal and declaration as first code sibling:
            //   val foo = {
            //      fun bar() {}
            //   }
            //   val foo = { _ ->
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

        if (node.elementType == PROPERTY && node.treeParent.elementType == WHEN) {
            // Allow:
            //   when(val foo = foo()) {
            //       ...
            //   }
            return
        }

        if (node.elementType == FUN &&
            (node.prevCodeSibling()?.elementType == EQ || node.prevCodeSibling()?.elementType == RETURN_KEYWORD)
        ) {
            // Allow:
            //   val foo =
            //       fun(): String {
            //           return "foo"
            //       }
            return
        }

        if (node.elementType == FUN && node.treeParent.elementType == VALUE_ARGUMENT) {
            // Allow:
            //   val foo1 = foo2(fun() = 42)
            return
        }

        if (node.elementType == OBJECT_DECLARATION && node.treeParent.elementType == OBJECT_LITERAL) {
            // Allow:
            //   fun foo() =
            //      object : Foo() {
            //          // some declarations
            //      }
            return
        }

        node
            .takeIf { KtTokenSets.DECLARATION_TYPES.contains(it.elementType) }
            ?.takeIf {
                val prevLeaf = it.prevLeaf()
                prevLeaf != null && (!prevLeaf.isWhiteSpace() || !prevLeaf.text.startsWith("\n\n"))
            }?.let { insertBeforeNode ->
                emit(insertBeforeNode.startOffset, "Expected a blank line for this declaration", true)
                    .ifAutocorrectAllowed {
                        insertBeforeNode.upsertWhitespaceBeforeMe("\n".plus(node.indent()))
                    }
            }
    }

    private fun ASTNode.isFirstCodeSiblingInClassBody() =
        this ==
            treeParent
                .takeIf { it.elementType == CLASS_BODY }
                ?.findChildByType(LBRACE)
                ?.nextCodeSibling()

    private fun ASTNode.isFirstCodeSiblingInBlock() =
        this ==
            treeParent
                .takeIf { it.elementType == BLOCK }
                ?.findChildByType(LBRACE)
                ?.nextCodeSibling()

    private fun ASTNode.isFirstCodeSiblingInBodyOfFunctionLiteral() =
        this ==
            treeParent
                .takeIf { it.elementType == BLOCK && it.treeParent.elementType == FUNCTION_LITERAL }
                ?.treeParent
                ?.psi
                ?.safeAs<KtFunctionLiteral>()
                ?.bodyExpression
                ?.node
                ?.children()
                ?.firstOrNull { !it.isWhiteSpace() && !it.isPartOfComment() }

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
