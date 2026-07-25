package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.rule.engine.core.api.AutocorrectDecision
import io.github.ktlint.core.rule.engine.core.api.ElementType.IMPORT_LIST
import io.github.ktlint.core.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import io.github.ktlint.core.rule.engine.core.api.RuleId
import io.github.ktlint.core.rule.engine.core.api.RuleV2
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint
import io.github.ktlint.core.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import io.github.ktlint.core.rule.engine.core.api.ifAutocorrectAllowed
import io.github.ktlint.core.rule.engine.core.api.isWhiteSpaceWithNewline
import io.github.ktlint.core.rule.engine.core.api.replaceTextWith
import io.github.ktlint.core.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.psiUtil.siblings

/**
 * https://developer.android.com/kotlin/style-guide#structure
 */
@SinceKtlint("2.0", EXPERIMENTAL)
public class PackageImportSpacingRule :
    StandardRule("package-import-spacing"),
    RuleV2.Experimental {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        node
            .takeIf { it.elementType == PACKAGE_DIRECTIVE }
            ?.siblings()
            ?.takeWhile { it.elementType != IMPORT_LIST }
            ?.firstOrNull { it.isWhiteSpaceWithNewline }
            ?.takeIf { whitespace -> whitespace.text.count { it == '\n' } != 2 }
            ?.let { whitespace ->
                emit(
                    whitespace.startOffset + 1,
                    "Expected exactly one blank line between package statement and import statements",
                    true,
                ).ifAutocorrectAllowed { whitespace.replaceTextWith("\n\n") }
            }
    }
}

public val PACKAGE_IMPORT_SPACING_RULE_ID: RuleId = PackageImportSpacingRule().ruleId
