package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BY_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective

class NoUnusedImportsRule : Rule("no-unused-imports") {

    private val componentNRegex = Regex("^component\\d+$")

    private val operatorSet = setOf(
        // unary
        "unaryPlus", "unaryMinus", "not",
        // inc/dec
        "inc", "dec",
        // arithmetic
        "plus", "minus", "times", "div", "rem", "mod", "rangeTo",
        // in
        "contains",
        // indexed access
        "get", "set",
        // invoke
        "invoke",
        // augmented assignments
        "plusAssign", "minusAssign", "timesAssign", "divAssign", "modAssign",
        // (in)equality
        "equals",
        // comparison
        "compareTo",
        // iteration (https://github.com/shyiko/ktlint/issues/40)
        "iterator",
        // by (https://github.com/shyiko/ktlint/issues/54)
        "getValue", "setValue"
    )

    private val conditionalWhitelist = mapOf<(String) -> Boolean, (node: ASTNode) -> Boolean>(
        Pair(
            // Ignore provideDelegate if there is a `by` anywhere in the file
            { importPath -> importPath.endsWith(".provideDelegate") },
            { rootNode ->
                var hasByKeyword = false
                rootNode.visit { child ->
                    if (child.elementType == BY_KEYWORD) {
                        hasByKeyword = true
                        return@visit
                    }
                }
                hasByKeyword
            }
        )
    )

    private data class Reference(val text: String, val inDotQualifiedExpression: Boolean)
    private val ref = mutableSetOf<Reference>()
    private val parentExpressions = mutableSetOf<String>()
    private val imports = mutableSetOf<String>()
    private var packageName = ""
    private var rootNode: ASTNode? = null

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            rootNode = node
            ref.clear() // rule can potentially be executed more than once (when formatting)
            ref.add(Reference("*", false))
            parentExpressions.clear()
            imports.clear()
            node.visit { vnode ->
                val psi = vnode.psi
                val type = vnode.elementType
                val text = vnode.text
                if (checkIfExpressionHasParentImport(text, type)) {
                    parentExpressions.add(text.substringBeforeLast("("))
                }
                if (type == KDocTokens.MARKDOWN_LINK && psi is KDocLink) {
                    val linkText = psi.getLinkText().removeBackticks()
                    ref.add(Reference(linkText.split('.').first(), false))
                } else if ((type == REFERENCE_EXPRESSION || type == OPERATION_REFERENCE) &&
                    !vnode.isPartOf(IMPORT_DIRECTIVE)
                ) {
                    ref.add(Reference(text.removeBackticks(), psi.parentDotQualifiedExpression() != null))
                } else if (type == IMPORT_DIRECTIVE) {
                    imports += (vnode.psi as KtImportDirective).importPath!!.pathStr.removeBackticks().trim()
                }
            }
            val directCalls = ref.filter { !it.inDotQualifiedExpression }.map { it.text }
            parentExpressions.forEach { parent ->
                imports.removeIf { imp ->
                    imp.endsWith(".$parent") && directCalls.none { imp.endsWith(".$it") }
                }
            }
        } else if (node.elementType == PACKAGE_DIRECTIVE) {
            val packageDirective = node.psi as KtPackageDirective
            packageName = packageDirective.qualifiedName
        } else if (node.elementType == IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val name = importDirective.importPath?.importedName?.asString()?.removeBackticks()
            val importPath = importDirective.importPath?.pathStr?.removeBackticks()!!
            if (importDirective.aliasName == null &&
                (packageName.isEmpty() || importPath.startsWith("$packageName.")) &&
                importPath.substring(packageName.length + 1).indexOf('.') == -1
            ) {
                emit(node.startOffset, "Unnecessary import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            } else if (name != null && (!ref.map { it.text }.contains(name) || !isAValidImport(importPath)) &&
                !operatorSet.contains(name) &&
                !name.isComponentN() &&
                conditionalWhitelist
                    .filterKeys { selector -> selector(importPath) }
                    .none { (_, condition) -> condition(rootNode!!) }
            ) {
                emit(node.startOffset, "Unused import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            }
        }
    }

    // Checks if any static method call is present in code with the parent import present in imports list
    private fun checkIfExpressionHasParentImport(text: String, type: IElementType): Boolean {
        val containsMethodCall = text.split(".").last().contains("(")
        return type == DOT_QUALIFIED_EXPRESSION && containsMethodCall && checkIfParentImportExists(text.substringBeforeLast("("))
    }

    // Contains list of all imports and checks if given import is present
    private fun checkIfParentImportExists(text: String): Boolean {
        // Only check static imports; identified if they start with a capital letter indicating a
        // class name rather than a sub-package
        if (text.isNotEmpty() && text[0] !in 'A'..'Z') {
            return false
        }

        val staticImports = imports.filter { it.endsWith(".$text") }
        staticImports.forEach { import ->
            val count = imports.count {
                it.startsWith(import.substringBefore(text))
            }
            // Parent import and static import both are present
            if (count > 1) {
                return true
            }
        }
        return false
    }

    // Check if the import being checked is present in the filtered import list
    private fun isAValidImport(importPath: String): Boolean {
        return imports.contains(importPath)
    }

    private fun String.isComponentN() = componentNRegex.matches(this)

    private fun PsiElement.parentDotQualifiedExpression(): KtDotQualifiedExpression? {
        val callOrThis = (parent as? KtCallExpression)?.takeIf { it.calleeExpression == this } ?: this
        return (callOrThis.parent as? KtDotQualifiedExpression)?.takeIf { it.selectorExpression == callOrThis }
    }

    private fun String.removeBackticks() = replace("`", "")
}
