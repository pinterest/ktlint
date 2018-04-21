package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.nio.file.Paths

/**
 * If there is only one top level class in a given file, then its name should match the file's name
 */
class ClassNameMatchesFileNameRule : Rule("class-name-matches-file-name"), Rule.Modifier.RestrictToRoot {

    private class NameWithOffset(val name: String, val offset: Int)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        fileName: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val topLevelClassNames = mutableListOf<NameWithOffset>()

        node.visit {
            if (it.elementType == KtStubElementTypes.CLASS && it.treeParent.elementType == KtStubElementTypes.FILE) {
                val ktClass = it.findChildByType(KtTokens.IDENTIFIER)
                ktClass?.let {
                    val className = it.text
                    topLevelClassNames.add(NameWithOffset(className, it.startOffset))
                }
            }
        }

        val name = Paths.get(fileName).fileName.toString().substringBefore(".")
        if (topLevelClassNames.size == 1 && name != topLevelClassNames.first().name) {
            emit(topLevelClassNames.first().offset,
                "Single top level class name [${topLevelClassNames.first().name}] does not match file name",
                false)
        }
    }
}
