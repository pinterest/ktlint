package yourpkgname

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.VAR_KEYWORD
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoVarRule : Rule("no-var") {

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.elementType == VAR_KEYWORD) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
        }
    }
}
