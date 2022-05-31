package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import java.nio.file.Paths
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode

/**
 * [Kotlin lang documentation](https://kotlinlang.org/docs/coding-conventions.html#source-file-names):
 * If a Kotlin file contains a single class (potentially with related top-level declarations), its name should be
 * the same as the name of the class, with the `.kt` extension appended. If a file contains multiple classes,
 * or only top-level declarations, choose a name describing what the file contains, and name the file accordingly.
 * Use upper camel case with an uppercase first letter (also known as Pascal case),
 * for example, `ProcessDeclarations.kt`.
 *
 * According to issue https://youtrack.jetbrains.com/issue/KTIJ-21897/Kotlin-coding-convention-file-naming-for-class,
 * "class" above should be read as any type of class (data class, enum class, sealed class) and interfaces.
 *
 * Exceptions to this rule:
 * * file without `.kt` extension
 * * file with name `package.kt`
 */
public class FilenameRule : Rule(
    id = "filename",
    visitorModifiers = setOf(
        VisitorModifier.RunOnRootNodeOnly
    )
) {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        node as FileASTNode? ?: error("node is not ${FileASTNode::class} but ${node::class}")

        val filePath = node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)
        if (filePath?.endsWith(".kt") != true) {
            // ignore all non ".kt" files (including ".kts")
            return
        }

        val fileName = Paths.get(filePath).fileName.toString().substringBefore(".")
        if (fileName == "package") {
            // ignore package.kt filename
            return
        }

        val topLevelClassNames = topLevelClassNames(node)
        if (topLevelClassNames.size == 1) {
            // If the file only contains one top level class, then its filename should be identical to the class name
            fileName.shouldMatchClassName(topLevelClassNames.first(), emit)
        } else {
            fileName.shouldMatchPascalCase(emit)
        }
    }

    private fun topLevelClassNames(fileNode: ASTNode): List<String> {
        return fileNode
            .getChildren(null)
            .filterNotNull()
            .filter { it.elementType == CLASS }
            .mapNotNull {
                it
                    .findChildByType(IDENTIFIER)
                    ?.text
                    ?.removeSurrounding("`")
            }.toList()
    }

    private fun String.shouldMatchClassName(
        className: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (this != className) {
            emit(
                0,
                "File '$this.kt' contains a single class and should be named same after that class '$className.kt'",
                false
            )
        }
    }

    private fun String.shouldMatchPascalCase(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (!pascalCaseRegEx.matches(this)) {
            emit(0, "File name '$this.kt' should conform PascalCase", false)
        }
    }

    private companion object {
        val pascalCaseRegEx = Regex("""^[A-Z][A-Za-z0-9]*$""")
    }
}
