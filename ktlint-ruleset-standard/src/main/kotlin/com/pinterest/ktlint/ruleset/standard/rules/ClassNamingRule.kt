package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * https://kotlinlang.org/docs/coding-conventions.html#naming-rules
 *
 * The Kotlin convention does not allow explicitly to use backticked class name, but it makes sense to allow this as
 * well as it is more consistent with name of test functions.
 */
@SinceKtlint("0.48", EXPERIMENTAL)
@SinceKtlint("0.49", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class ClassNamingRule : StandardRule("class-naming") {
    private var allowBacktickedClassName = false

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (!allowBacktickedClassName && node.elementType == IMPORT_DIRECTIVE) {
            node
                .findChildByType(DOT_QUALIFIED_EXPRESSION)
                ?.text
                ?.takeIf { it.startsWith("org.junit.jupiter.api") }
                ?.let {
                    // Assume that each file that imports a Junit Jupiter Api class is a test class
                    allowBacktickedClassName = true
                }
        }

        node
            .takeIf { node.elementType == CLASS || node.elementType == OBJECT_DECLARATION }
            ?.findChildByType(IDENTIFIER)
            ?.takeUnless { it.isValidFunctionName() || it.isTestClass() || it.isTokenKeywordBetweenBackticks() }
            ?.let {
                emit(it.startOffset, "Class or object name should start with an uppercase letter and use camel case", false)
            }
    }

    private fun ASTNode.isValidFunctionName() = text.matches(VALID_CLASS_NAME_REGEXP)

    private fun ASTNode.isTestClass() = allowBacktickedClassName && hasBackTickedIdentifier()

    private fun ASTNode.hasBackTickedIdentifier() = text.matches(BACK_TICKED_FUNCTION_NAME_REGEXP)

    private fun ASTNode.isTokenKeywordBetweenBackticks() =
        this
            .takeIf { it.elementType == IDENTIFIER }
            ?.text
            .orEmpty()
            .removeSurrounding("`")
            .let { KEYWORDS.contains(it) }

    private companion object {
        val VALID_CLASS_NAME_REGEXP = "[A-Z][A-Za-z\\d]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        val BACK_TICKED_FUNCTION_NAME_REGEXP = Regex("`.*`")
        private val KEYWORDS =
            setOf(KtTokens.KEYWORDS, KtTokens.SOFT_KEYWORDS)
                .flatMap { tokenSet -> tokenSet.types.mapNotNull { it.debugName } }
                .filterNot { keyword ->
                    // The keyword sets contain a few 'keywords' which should be ignored. All valid keywords only contain lowercase
                    // characters
                    keyword.any { it.isUpperCase() }
                }.toSet()
    }
}

public val CLASS_NAMING_RULE_ID: RuleId = ClassNamingRule().ruleId
