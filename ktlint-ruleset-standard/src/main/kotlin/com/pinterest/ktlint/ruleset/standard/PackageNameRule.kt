package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtPackageDirective

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 */
class PackageNameRule : Rule("package-name") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == PACKAGE_DIRECTIVE) {
            val qualifiedName = (node.psi as KtPackageDirective).qualifiedName
            if (qualifiedName.isEmpty()) {
                return
            }

            if (qualifiedName.contains('_')) {
                emit(node.startOffset, "Package name must not contain underscore", false)
                // "package name must be in lowercase" is violated by too many to projects in the wild to forbid
            }
        }
    }
}
