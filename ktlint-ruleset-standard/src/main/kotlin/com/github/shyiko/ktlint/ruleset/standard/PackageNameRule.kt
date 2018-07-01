package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.io.File

class PackageNameRule : Rule("package-name-rule") {
    private var filePath: String? = null

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == KtStubElementTypes.FILE) {
            filePath = node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)
        }

        if (node.elementType == KtStubElementTypes.PACKAGE_DIRECTIVE) {
            val qualifiedName = (node.psi as KtPackageDirective).qualifiedName
            if (packageNameNotContainsDirectoryPath(qualifiedName)) {
                emit(node.startOffset, "package name should match directory name.", false)
            }

            if (qualifiedName.any { it.isUpperCase() }) {
                emit(node.startOffset, "package names should be all lowercase.", false)
            }

            if (qualifiedName.contains('_', false)) {
                emit(node.startOffset, "package names should be not contain underscore.", false)
            }
        }
    }

    private fun packageNameNotContainsDirectoryPath(qualifiedName: String): Boolean {
        var filePathFromQualifiedName = qualifiedName.replace('.', File.separatorChar, false)
        // Skip package name is empty
        if (filePathFromQualifiedName.isEmpty()) {
            return false
        }

        val actualFilePath = filePath.orEmpty()
        return actualFilePath.contains(filePathFromQualifiedName).not()
    }
}
