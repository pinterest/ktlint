package com.pinterest.ktlint.rule.engine.core.api

import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.reflect.KFunction1

class RuleKtTest {
    @Test
    fun `Given a rule with an unqualified rule id than the rule can not be instantiated`() {
        assertThatThrownBy { creatRule("some-unqualified-rule-id") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Rule with id 'some-unqualified-rule-id' must match regexp '[a-z]+(-[a-z]+)*:[a-z]+(-[a-z]+)*'")
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule id: `{1}`")
    @ValueSource(
        strings = [
            "standard:rule-id",
            "custom:rule-id",
        ],
    )
    fun `Given a rule with a qualified rule id then return the rule id`(id: String) {
        val rule = creatRule(id)
        assertThat(rule.ruleId.value).isEqualTo(id)
    }

    @ParameterizedTest(name = "Qualified rule id: `{0}`, expected rule set id: `{1}`")
    @CsvSource(
        value = [
            "standard:rule-id,standard",
            "custom:rule-id,custom",
        ],
    )
    fun `Given a qualified rule id then return the rule set id`(
        id: String,
        ruleSetId: String,
    ) {
        val rule = creatRule(id)
        assertThat(rule.ruleId.ruleSetId.value).isEqualTo(ruleSetId)
    }

    @Test
    fun `Dispatch to IfExpression`() {
        val rule = object : KtElementRule() {
            override fun beforeIfExpression(
                ktIfExpression: KtIfExpression,
                autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
            ) {
                emit(ktIfExpression.startOffset, "Found if expression", false)
            }
        }

        val code =
            """
            fun foo() {
                if (true) {
                    println("foo")
                }
            }
            """.trimIndent()

        assertThat(rule.lint(code)).containsExactly("Found if expression")
    }

    private fun creatRule(ruleId: String) =
        object : Rule(
            ruleId = RuleId(ruleId),
            about = About(),
        ) {}

    private open class KtElementRule : Rule(
        ruleId = RuleId("test:kt-element"),
        about = About()
    )

    private fun KtElementRule.lint(code: String): List<String> {
        val details = mutableListOf<String>()
        KtLintRuleEngine(
            ruleProviders = setOf(RuleProvider { this }),
        ).lint(Code.fromSnippet(code)) { lintError ->
            details.add(lintError.detail)
        }
        return details.toList()
    }
}
