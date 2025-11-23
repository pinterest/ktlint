package yourpkgname

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VAR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

class NoVarRule :
    Rule(
        ruleId = RuleId("$CUSTOM_RULE_SET_ID:no-var"),
        about =
            About(
                maintainer = "Your name",
                repositoryUrl = "https://github.com/your/project/",
                issueTrackerUrl = "https://github.com/your/project/issues",
            ),
    ),
    RuleAutocorrectApproveHandler {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == VAR_KEYWORD) {
            emit(node.startOffset, "Unexpected var, use val instead", false)
        }
    }
}
