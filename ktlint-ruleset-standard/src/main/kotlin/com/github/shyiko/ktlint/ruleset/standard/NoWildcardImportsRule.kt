package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

class NoWildcardImportsRule : Rule("no-wildcard-imports") {

    private var wildcardsAllowed = false

    override fun visit(node: ASTNode, autoCorrect: Boolean,
            emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            editorConfig.get("allow_wildcard_imports")?.toBoolean()?.apply {
                wildcardsAllowed = this
            }
        }
        if (!wildcardsAllowed && node.elementType == KtStubElementTypes.IMPORT_DIRECTIVE) {
            val importDirective = node.psi as KtImportDirective
            val path = importDirective.importPath?.pathStr
            if (path != null && !path.startsWith("kotlinx.android.synthetic") && path.contains('*')) {
                emit(node.startOffset, "Wildcard import", false)
            }
        }
    }
}
