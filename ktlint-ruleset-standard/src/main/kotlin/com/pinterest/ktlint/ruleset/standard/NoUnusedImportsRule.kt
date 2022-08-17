package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BY_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.prevSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens.MARKDOWN_LINK
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

public class NoUnusedImportsRule : Rule("no-unused-imports") {
    private val ref = mutableSetOf(
        Reference("*", false),
    )
    private val parentExpressions = mutableSetOf<String>()
    private val imports = mutableMapOf<ImportPath, ASTNode>()
    private var packageName = ""
    private var rootNode: ASTNode? = null
    private var foundByKeyword = false

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isRoot()) {
            rootNode = node
        }
        when (node.elementType) {
            PACKAGE_DIRECTIVE -> {
                val packageDirective = node.psi as KtPackageDirective
                packageName = packageDirective.qualifiedName
            }
            IMPORT_DIRECTIVE -> {
                val importPath = (node.psi as KtImportDirective).importPath!!
                if (imports.containsKey(importPath)) {
                    // Emit directly when same import occurs more than once
                    emit(node.startOffset, "Unused import", true)
                    if (autoCorrect) {
                        node.psi.delete()
                    }
                } else {
                    imports[importPath] = node
                }
            }
            DOT_QUALIFIED_EXPRESSION -> {
                if (node.isExpressionForStaticImportWithExistingParentImport()) {
                    parentExpressions.add(node.text.substringBeforeLast("("))
                }
            }
            MARKDOWN_LINK -> {
                node
                    .psi
                    .safeAs<KDocLink>()
                    ?.let { kdocLink ->
                        val linkText = kdocLink.getLinkText().removeBackticksAndTrim()
                        ref.add(Reference(linkText.split('.').first(), false))
                        ref.add(Reference(linkText.split('.').last(), false))
                    }
            }
            REFERENCE_EXPRESSION, OPERATION_REFERENCE -> {
                if (!node.isPartOf(IMPORT_DIRECTIVE)) {
                    val identifier = if (node is CompositeElement) {
                        node.findChildByType(IDENTIFIER)
                    } else {
                        node
                    }
                    identifier
                        ?.let { identifier.text }
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            ref.add(
                                Reference(
                                    it.removeBackticksAndTrim(),
                                    node.psi.parentDotQualifiedExpression() != null,
                                ),
                            )
                        }
                }
            }
            BY_KEYWORD -> foundByKeyword = true
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == FILE) {
            val directCalls = ref.filter { !it.inDotQualifiedExpression }.map { it.text }
            parentExpressions.forEach { parent ->
                imports
                    .filterKeys { import ->
                        val importPath = import.pathStr.removeBackticksAndTrim()
                        importPath.endsWith(".$parent") && directCalls.none { importPath.endsWith(".$it") }
                    }.forEach { (importPath, importNode) ->
                        emit(importNode.startOffset, "Unused import", true)
                        if (autoCorrect) {
                            imports.remove(importPath, importNode)
                            importNode.removeImportDirective()
                        }
                    }
            }

            imports.forEach { (_, node) ->
                val importDirective = node.psi as KtImportDirective
                val name = importDirective.importPath?.importedName?.asString()?.removeBackticksAndTrim()
                val importPath = importDirective.importPath?.pathStr?.removeBackticksAndTrim()!!
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
                    !importPath.ignoreProvideDelegate()
                ) {
                    emit(node.startOffset, "Unused import", true)
                    if (autoCorrect) {
                        importDirective.delete()
                    }
                }
            }
        }
    }

    private fun String.ignoreProvideDelegate() =
        if (endsWith(".provideDelegate")) {
            // Ignore provideDelegate if the `by` keyword is found anywhere in the file
            foundByKeyword
        } else {
            false
        }

    private fun ASTNode.removeImportDirective() {
        require(this.elementType == IMPORT_DIRECTIVE)
        when {
            treeParent.firstChildNode == this -> {
                nextSibling { true }
                    ?.takeIf { it.isWhiteSpaceWithNewline() }
                    ?.let { it.treeParent.removeChild(it) }
            }
            treeParent.lastChildNode == this -> {
                prevSibling { true }
                    ?.takeIf { it.isWhiteSpaceWithNewline() }
                    ?.let { it.treeParent.removeChild(it) }
            }
            else -> {
                nextLeaf(true)
                    ?.takeIf { it.isWhiteSpaceWithNewline() }
                    ?.let { it.treeParent.removeChild(it) }
            }
        }
        treeParent.removeChild(this)
    }

    private fun ASTNode.isExpressionForStaticImportWithExistingParentImport(): Boolean {
        if (!containsMethodCall()) {
            return false
        }

        val methodCallExpression = text.substringBeforeLast(
            "(",
        )

        // Only check static imports; identified if they start with a capital letter indicating a
        // class name rather than a sub-package
        if (methodCallExpression.isNotEmpty() && methodCallExpression[0] !in 'A'..'Z') {
            return false
        }

        imports
            .filterKeys { it.pathStr.removeBackticksAndTrim().endsWith(".$methodCallExpression") }
            .forEach { import ->
                val count = imports.count {
                    it.key.pathStr.removeBackticksAndTrim().startsWith(
                        import.key.pathStr.removeBackticksAndTrim().substringBefore(methodCallExpression),
                    )
                }
                // Parent import and static import both are present
                if (count > 1) {
                    return true
                }
            }
        return false
    }

    private fun ASTNode.containsMethodCall() = text.split(".").last().contains("(")

    // Check if the import being checked is present in the filtered import list
    private fun isAValidImport(importPath: String) =
        imports.any {
            it.key.pathStr.removeBackticksAndTrim().contains(importPath)
        }

    private fun String.isComponentN() = componentNRegex.matches(this)

    private fun PsiElement.parentDotQualifiedExpression(): KtDotQualifiedExpression? {
        val callOrThis = (parent as? KtCallExpression)?.takeIf { it.calleeExpression == this } ?: this
        return (callOrThis.parent as? KtDotQualifiedExpression)?.takeIf { it.selectorExpression == callOrThis }
    }

    private fun String.removeBackticksAndTrim() = replace("`", "").trim()

    private data class Reference(val text: String, val inDotQualifiedExpression: Boolean)

    private companion object {
        val componentNRegex = Regex("^component\\d+$")

        val operatorSet = setOf(
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
            "getValue", "setValue",
        )
    }
}
