package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.io.File

/**
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/reference/coding-conventions.html#directory-structure)
 * @author yokotaso <yokotaso.t@gmail.com>
 */
class DirectoryStructureRule : Rule("directory-structure") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.PACKAGE_DIRECTIVE) {
            val qualifiedName = (node.psi as KtPackageDirective).qualifiedName
            if (qualifiedName.isEmpty()) {
                return
            }
            val filePath = node.psi.containingFile.node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY) ?: return
            if (!filePath.substringBeforeLast(File.separatorChar)
                    .endsWith(File.separatorChar + qualifiedName.replace('.', File.separatorChar))) {
                emit(node.startOffset, "Package directive doesn't match file location", false)
            }
        }
    }
}
