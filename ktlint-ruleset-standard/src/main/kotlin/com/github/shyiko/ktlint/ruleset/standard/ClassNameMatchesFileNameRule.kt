package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.nio.file.Paths

/**
 * If there is only one top level class in a given file, then its name should match the file's name
 */
class ClassNameMatchesFileNameRule : Rule("class-name-matches-file-name"), Rule.Modifier.RestrictToRoot {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val filePath = node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)

        // Ignore all non ".kt" files (including ".kts")
        if (filePath?.endsWith(".kt") != true) {
            return
        }

        val topLevelClassNames = node.getChildren(null)
            .filter { it.elementType == KtStubElementTypes.CLASS }
            .mapNotNull { it.findChildByType(KtTokens.IDENTIFIER)?.text }

        val name = Paths.get(filePath).fileName.toString().substringBefore(".")
        if (topLevelClassNames.size == 1 && name != topLevelClassNames.first()) {
            val className = topLevelClassNames.first()
            emit(0,
                "Class $className should be declared in a file named $className.kt",
                false)
        }
    }
}
