package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUN_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OVERRIDE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * https://kotlinlang.org/docs/coding-conventions.html#function-names
 */
@SinceKtlint("0.48", EXPERIMENTAL)
@SinceKtlint("1.0", STABLE)
public class FunctionNamingRule : StandardRule("function-naming") {
    private var isTestClass = false

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (!isTestClass && node.elementType == IMPORT_DIRECTIVE) {
            (node.psi as KtImportDirective)
                .importPath
                ?.pathStr
                ?.takeIf {
                    it.startsWith(ORG_JUNIT) || it.startsWith(ORG_TESTNG) || it.startsWith(KOTLIN_TEST)
                }?.let {
                    // Assume that each file that imports a Junit Jupiter Api class is a test class
                    isTestClass = true
                }
        }

        node
            .takeIf { node.elementType == FUN }
            ?.takeUnless {
                node.isFactoryMethod() ||
                    node.isTestMethod() ||
                    node.hasValidFunctionName() ||
                    node.isAnonymousFunction() ||
                    node.isOverrideFunction()
            }?.let {
                val identifierOffset =
                    node
                        .findChildByType(IDENTIFIER)
                        ?.startOffset
                        ?: 1
                emit(
                    identifierOffset,
                    "Function name should start with a lowercase letter (except factory methods) and use camel case",
                    false,
                )
            }
    }

    private fun ASTNode.isFactoryMethod() =
        (this.psi as KtFunction)
            .let { it.hasDeclaredReturnType() && it.name == it.typeReference?.text }

    private fun ASTNode.isTestMethod() = isTestClass && hasValidTestFunctionName()

    private fun ASTNode.hasValidTestFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_TEST_FUNCTION_NAME_REGEXP)

    private fun ASTNode.hasValidFunctionName() =
        findChildByType(IDENTIFIER)
            ?.text
            .orEmpty()
            .matches(VALID_FUNCTION_NAME_REGEXP)

    private fun ASTNode.isAnonymousFunction() =
        VALUE_PARAMETER_LIST ==
            findChildByType(FUN_KEYWORD)
                ?.nextCodeSibling()
                ?.elementType

    /*
     * A function override should not be reported as the interface of class that defines the function might be out of scope of the project
     * in which case the function name can not be changed. Note that the function will still be reported at the interface or class itself
     * whenever that interface or class is defined inside the scope of the project.
     */
    private fun ASTNode.isOverrideFunction() =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == OVERRIDE_KEYWORD }

    private companion object {
        val VALID_FUNCTION_NAME_REGEXP = "[a-z][A-Za-z\\d]*".regExIgnoringDiacriticsAndStrokesOnLetters()
        val VALID_TEST_FUNCTION_NAME_REGEXP = "(`.*`)|([a-z][A-Za-z\\d_]*)".regExIgnoringDiacriticsAndStrokesOnLetters()
        private const val KOTLIN_TEST = "kotlin.test"
        private const val ORG_JUNIT = "org.junit"
        private const val ORG_TESTNG = "org.testng"
    }
}

public val FUNCTION_NAMING_RULE_ID: RuleId = FunctionNamingRule().ruleId
