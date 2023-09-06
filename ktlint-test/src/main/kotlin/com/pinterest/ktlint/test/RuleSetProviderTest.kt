package com.pinterest.ktlint.test

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Extend this test in any custom ruleset that implements a RuleSetProvider to ensure that each rule that is defined in that rule set is
 * actually defined in the RuleSetProvider.
 */
public open class RuleSetProviderTest(
    private val rulesetClass: Class<out RuleSetProviderV3>,
    private val packageName: String,
) {
    private val ruleSetProvider =
        rulesetClass
            .getDeclaredConstructor()
            .newInstance()
    private val rules =
        ruleSetProvider
            .getRuleProviders()
            .map { it.createNewRuleInstance() }

    @Test
    public fun checkAllRulesInPackageAreProvidedByRuleSetProvider() {
        val srcLocation =
            rulesetClass
                .protectionDomain
                .codeSource
                .location
                .path
        val rulesDir = File(srcLocation + packageName.replace(".", "/"))
        val packageRules =
            rulesDir
                .listFiles()
                ?.map { it.name.removeSuffix(".class") }
                ?.filter { it.endsWith("Rule") }
                ?: arrayListOf()
        assertThat(packageRules)
            .withFailMessage("No rules were found in package '$rulesDir'. Is the packagname '$packageName' correct?")
            .isNotEmpty

        val providerRules = rules.map { it::class.java.simpleName }
        val missingRules =
            packageRules
                .minus(providerRules.toSet())
                .joinToString(separator = NEWLINE_AND_INDENT)
        assertThat(missingRules)
            .withFailMessage(
                "${ruleSetProvider::class.simpleName} is missing to provide the following rules:${NEWLINE_AND_INDENT}$missingRules",
            ).isEmpty()
    }

    private companion object {
        const val NEWLINE_AND_INDENT = "\n\t"
    }
}
