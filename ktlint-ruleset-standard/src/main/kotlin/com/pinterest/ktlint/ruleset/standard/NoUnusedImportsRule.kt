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
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
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

    private val conditionalWhitelist = mapOf<String, (node: ASTNode) -> Boolean>(
        Pair(
            // Ignore provideDelegate if there is a `by` anywhere in the file
            "org.gradle.kotlin.dsl.provideDelegate",
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

    private val ref = mutableSetOf<String>()
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
            ref.add("*")
            imports.clear()
            var parentExpression: String? = null
            node.visit { vnode ->
                val psi = vnode.psi
                val type = vnode.elementType
                val text = vnode.text
                val parentImport = checkIfExpressionHasParentImport(text, type)
                parentExpression = if (parentImport) text.substringBeforeLast("(") else parentExpression
                if (type == KDocTokens.MARKDOWN_LINK && psi is KDocLink) {
                    val linkText = psi.getLinkText().replace("`", "")
                    ref.add(linkText.split('.').first())
                } else if ((type == REFERENCE_EXPRESSION || type == OPERATION_REFERENCE) &&
                    !vnode.isPartOf(IMPORT_DIRECTIVE)
                ) {
                    ref.add(text.trim('`'))
                    // If redundant import is present, it is filtered from the import list
                    parentExpression?.let { imports.removeIf { imp -> imp.endsWith(it) } }
                } else if (type == IMPORT_DIRECTIVE) {
                    buildNonRedundantImports(text)
                }
            }
        } else if (node.elementType == PACKAGE_DIRECTIVE) {
            val packageDirective = node.psi as KtPackageDirective
            packageName = packageDirective.qualifiedName
        } else if (node.elementType == IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val name = importDirective.importPath?.importedName?.asString()
            val importPath = importDirective.importPath?.pathStr!!
            if (importDirective.aliasName == null &&
                (packageName.isEmpty() || importPath.startsWith("$packageName.")) &&
                importPath.substring(packageName.length + 1).indexOf('.') == -1
            ) {
                emit(node.startOffset, "Unnecessary import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            } else if (name != null && (!ref.contains(name) || !isAValidImport(importPath)) &&
                !operatorSet.contains(name) &&
                !name.isComponentN() &&
                conditionalWhitelist[importPath]?.invoke(rootNode!!) != true
            ) {
                emit(node.startOffset, "Unused import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            }
        }
    }

    // Builds a list of imports. Takes care of having aliases in case it is assigned to imports
    private fun buildNonRedundantImports(text: String) {
        val element = text.split("import").last().trim()
        if (element.contains("as ")) {
            imports.addAll(listOf(element.split("as").first().replace("`", "").trim(), element.split("as").last().trim()))
        } else {
            imports.add(element)
        }
    }

    // Checks if any static method call is present in code with the parent import present in imports list
    private fun checkIfExpressionHasParentImport(text: String, type: IElementType): Boolean {
        val containsMethodCall = text.split(".").last().contains("(")
        return type == DOT_QUALIFIED_EXPRESSION && containsMethodCall && checkIfParentImportExists(text.substringBeforeLast("("))
    }

    // Contains list of all imports and checks if given import is present
    private fun checkIfParentImportExists(text: String): Boolean {
        imports.forEach {
            if (it.endsWith(text)) {
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
}
