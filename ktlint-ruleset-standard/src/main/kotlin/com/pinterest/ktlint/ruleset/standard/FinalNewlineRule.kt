package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.isRoot
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

@OptIn(FeatureInAlphaState::class)
public class FinalNewlineRule :
    Rule("final-newline"),
    Rule.Modifier.RestrictToRoot,
    UsesEditorConfigProperties {

    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> = listOf(
        insertNewLineProperty
    )

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            if (node.textLength == 0) return
            val editorConfigProperties: EditorConfigProperties =
                node.getUserData(KtLint.EDITOR_CONFIG_PROPERTIES_USER_DATA_KEY)!!
            val insertFinalNewline = editorConfigProperties.getEditorConfigValue(insertNewLineProperty)
            val lastNode = lastChildNodeOf(node)
            if (insertFinalNewline) {
                if (lastNode !is PsiWhiteSpace || !lastNode.textContains('\n')) {
                    emit(0, "File must end with a newline (\\n)", true)
                    if (autoCorrect) {
                        node.addChild(PsiWhiteSpaceImpl("\n"), null)
                    }
                }
            } else {
                if (lastNode is PsiWhiteSpace && lastNode.textContains('\n')) {
                    emit(lastNode.startOffset, "Redundant newline (\\n) at the end of file", true)
                    if (autoCorrect) {
                        lastNode.node.treeParent.removeChild(lastNode.node)
                    }
                }
            }
        }
    }

    private tailrec fun lastChildNodeOf(node: ASTNode): ASTNode? =
        if (node.lastChildNode == null) node else lastChildNodeOf(node.lastChildNode)

    public companion object {
        public val insertNewLineProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.insert_final_newline,
                defaultValue = true
            )
    }
}
