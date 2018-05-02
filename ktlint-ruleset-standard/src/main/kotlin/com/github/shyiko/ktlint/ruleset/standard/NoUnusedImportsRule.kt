package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

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
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            ref.clear() // rule can potentially be executed more than once (when formatting)
            ref.add("*")
            node.visit { vnode ->
                val psi = vnode.psi
                val type = vnode.elementType
                if (type == KDocTokens.MARKDOWN_LINK && psi is KDocLink) {
                    val linkText = psi.getLinkText().replace("`", "")
                    ref.add(linkText.split('.').first())
                } else if ((type == KtNodeTypes.REFERENCE_EXPRESSION || type == KtNodeTypes.OPERATION_REFERENCE) &&
                    !psi.isPartOf(KtImportDirective::class)) {
                    ref.add(vnode.text.trim('`'))
                }
            }
        } else if (node.elementType == KtStubElementTypes.PACKAGE_DIRECTIVE) {
            val packageDirective = node.psi as KtPackageDirective
            packageName = packageDirective.qualifiedName
        } else if (node.elementType == KtStubElementTypes.IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val name = importDirective.importPath?.importedName?.asString()
            val importPath = importDirective.importPath?.pathStr!!
            if (importDirective.aliasName == null &&
                (packageName.isEmpty() || importPath.startsWith("$packageName.")) &&
                importPath.substring(packageName.length + 1).indexOf('.') == -1) {
                emit(importDirective.startOffset, "Unnecessary import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            } else if (name != null && !ref.contains(name) && !operatorSet.contains(name) && !name.isComponentN()) {
                emit(importDirective.startOffset, "Unused import", true)
                if (autoCorrect) {
                    importDirective.delete()
                }
            }
        }
    }

    private fun String.isComponentN() = componentNRegex.matches(this)
}
