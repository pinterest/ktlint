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
        // Ignore all non ".kt" files (including ".kts")
        if (node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)?.endsWith(".kt") != true) {
            return
        }

        val topLevelClassNames = mutableListOf<String>()

        node.visit {
            if (it.elementType == KtStubElementTypes.CLASS && it.treeParent.elementType == KtStubElementTypes.FILE) {
                val classIdentifier = it.findChildByType(KtTokens.IDENTIFIER)
                classIdentifier?.let {
                    val className = it.text
                    topLevelClassNames.add(className)
                }
            }
        }

        val name = Paths.get(node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)).fileName.toString().substringBefore(".")
        if (topLevelClassNames.size == 1 && name != topLevelClassNames.first()) {
            emit(0,
                "Single top level class name [${topLevelClassNames.first()}] does not match file name",
                false)
        }
    }
}
