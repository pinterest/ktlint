package yourpkgname

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

public class NoVarRule : Rule(
    ruleId = RuleId("$CUSTOM_RULE_SET_ID:no-var"),
    about = About(
        maintainer = "Your name",
        repositoryUrl = "https://github.com/your/project/",
        issueTrackerUrl = "https://github.com/your/project/issues",
    ),
) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == ElementType.VAR_KEYWORD) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
        }
    }
}
