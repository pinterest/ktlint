package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Issue
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
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
    private val ref = mutableSetOf<String>()
    private var packageName = ""

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (issue: Issue) -> Unit
    ) {
        if (node.isRoot()) {
            ref.clear() // rule can potentially be executed more than once (when formatting)
            ref.add("*")
            node.visit { vnode ->
                val psi = vnode.psi
                val type = vnode.elementType
                if (type == KDocTokens.MARKDOWN_LINK && psi is KDocLink) {
                    val linkText = psi.getLinkText().replace("`", "")
                    ref.add(linkText.split('.').first())
                } else if ((type == REFERENCE_EXPRESSION || type == OPERATION_REFERENCE) &&
                    !vnode.isPartOf(IMPORT_DIRECTIVE)
                ) {
                    ref.add(vnode.text.trim('`'))
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
                emit(Issue(node.startOffset, "Unnecessary import", true))
                if (autoCorrect) {
                    importDirective.delete()
                }
            } else if (name != null && !ref.contains(name) && !operatorSet.contains(name) && !name.isComponentN()) {
                emit(Issue(node.startOffset, "Unused import", true))
                if (autoCorrect) {
                    importDirective.delete()
                }
            }
        }
    }

    private fun String.isComponentN() = componentNRegex.matches(this)
}
