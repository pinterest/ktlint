package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes

/**
 * @see [Kotlin Style Guide](https://kotlinlang.org/docs/reference/coding-conventions.html#naming-rules)
 * @see [Android Style Guide](https://android.github.io/kotlin-guides/style.html#package-names)
 * @author yokotaso <yokotaso.t@gmail.com>
 */
class PackageNameRule : Rule("package-name") {

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
            // https://github.com/shyiko/ktlint/issues/280
/*
            val filePath = node.psi.containingFile.node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY) ?: return
            val fileDir = filePath.substringBeforeLast(File.separatorChar)
                .replace('.', File.separatorChar) // kotlinc a/b.c/d/E.kt produces a/b/c/d/E.class
            if (!fileDir.endsWith(File.separatorChar + qualifiedName.replace('.', File.separatorChar))) {
                emit(node.startOffset, "Package directive doesn't match file location", false)
            }
*/
            if (qualifiedName.contains('_')) {
                emit(node.startOffset, "Package name must not contain underscore", false)
                // "package name must be in lowercase" is violated by too many to projects in the wild to forbid
            }
        }
    }
}
