package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.FILE_ANNOTATION_LIST
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_LIST
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.SHEBANG_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.TYPEALIAS
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.prevCodeSibling
import java.nio.file.Paths
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * If there is only one top level class/object/typealias in a given file, then its name should match the file's name.
 */
class FilenameRule : Rule("filename"), Rule.Modifier.RestrictToRoot {

    private val ignoreSet = setOf<IElementType>(
        FILE_ANNOTATION_LIST,
        PACKAGE_DIRECTIVE,
        IMPORT_LIST,
        WHITE_SPACE,
        EOL_COMMENT,
        BLOCK_COMMENT,
        KDOC,
        SHEBANG_COMMENT
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
            if (el.elementType == CLASS ||
                el.elementType == OBJECT_DECLARATION ||
                el.elementType == TYPEALIAS
            ) {
                if (className != null) {
                    // more than one class/object/typealias present
                    return
                }
                val id = el.findChildByType(IDENTIFIER)
                type = id?.prevCodeSibling()?.text
                className = id?.text
            } else if (!ignoreSet.contains(el.elementType)) {
                // https://github.com/android/android-ktx/blob/51005889235123f41492eaaecde3c623473dfe95/src/main/java/androidx/core/graphics/Path.kt case
                return
            }
        }
        if (className != null) {
            val unescapedClassName = className.replace("`", "")
            val name = Paths.get(filePath).fileName.toString().substringBefore(".")
            if (name != "package" && name != unescapedClassName) {
                emit(0, "$type $className should be declared in a file named $unescapedClassName.kt", false)
            }
        }
    }
}
