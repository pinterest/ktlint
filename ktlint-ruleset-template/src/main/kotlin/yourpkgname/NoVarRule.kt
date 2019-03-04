package yourpkgname

import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.VAR_KEYWORD
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoVarRule : Rule("no-var") {

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == VAR_KEYWORD) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
        }
    }
}
