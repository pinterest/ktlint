package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling20
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

/**
 * https://developer.android.com/kotlin/style-guide#structure
 */
@SinceKtlint("1.x", EXPERIMENTAL)
public class PackageImportSpacingRule : StandardRule("package-import-spacing") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType == PACKAGE_DIRECTIVE }
            ?.let { packageNode ->
                val nextSibling = packageNode.treeNext ?: return
                val nextCodeSibling = packageNode.nextCodeSibling20
                if (nextCodeSibling?.elementType == IMPORT_DIRECTIVE) {
                    val newlineCount = nextSibling.text.count { it == '\n' }
                    if (newlineCount != 2) {
                        emit(
                            nextSibling.startOffset,
                            "Missing blank line between package statement and import statements",
                            true,
                        ).ifAutocorrectAllowed {
                            val corrected = "\n\n" + nextSibling.text.trimStart('\n')
                            (nextSibling as? LeafElement)?.rawReplaceWithText(corrected)
                        }
                    }
                }
            }
    }
}

public val PACKAGE_IMPORT_SPACING_RULE_ID: RuleId = PackageImportSpacingRule().ruleId