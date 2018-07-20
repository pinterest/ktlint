package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.getPrevSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.nio.file.Paths

/**
 * If there is only one top level class/object/typealias in a given file, then its name should match the file's name.
 */
class FilenameRule : Rule("filename"), Rule.Modifier.RestrictToRoot {

    private val ignoreSet = setOf<IElementType>(
        KtStubElementTypes.FILE_ANNOTATION_LIST,
        KtStubElementTypes.PACKAGE_DIRECTIVE,
        KtStubElementTypes.IMPORT_LIST,
        KtTokens.WHITE_SPACE,
        KtTokens.EOL_COMMENT,
        KtTokens.BLOCK_COMMENT,
        KtTokens.DOC_COMMENT,
        KtTokens.SHEBANG_COMMENT
    )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val filePath = node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)
        if (filePath?.endsWith(".kt") != true) {
            // ignore all non ".kt" files (including ".kts")
            return
        }
        var type: String? = null
        var className: String? = null
        for (el in node.getChildren(null)) {
            if (el.elementType == KtStubElementTypes.CLASS ||
                el.elementType == KtStubElementTypes.OBJECT_DECLARATION ||
                el.elementType == KtStubElementTypes.TYPEALIAS) {
                if (className != null) {
                    // more than one class/object/typealias present
                    return
                }
                val id = el.findChildByType(KtTokens.IDENTIFIER)
                type = id?.psi?.getPrevSiblingIgnoringWhitespaceAndComments(false)?.text
                className = id?.text
            } else if (!ignoreSet.contains(el.elementType)) {
                // https://github.com/android/android-ktx/blob/51005889235123f41492eaaecde3c623473dfe95/src/main/java/androidx/core/graphics/Path.kt case
                return
            }
        }
        if (className != null) {
            val name = Paths.get(filePath).fileName.toString().substringBefore(".")
            if (name != "package" && name != className) {
                emit(0, "$type $className should be declared in a file named $className.kt", false)
            }
        }
    }
}
