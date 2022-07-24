package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.RuleSetProviderV2
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public open class RuleSetProviderTest(
    private val rulesetClass: Class<out RuleSetProviderV2>,
    private val packageName: String
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
        val srcLocation = rulesetClass.protectionDomain.codeSource.location.path
        val rulesDir = File(srcLocation + packageName.replace(".", "/"))
        val packageRules = rulesDir.listFiles()
            ?.map { it.name.removeSuffix(".class") }
            ?.filter { it.endsWith("Rule") }
            ?: arrayListOf()

        val providerRules = rules.map { it::class.java.simpleName }
        val missingRules =
            packageRules
                .minus(providerRules.toSet())
                .joinToString(separator = separator)
        assertThat(missingRules)
            .withFailMessage("${ruleSetProvider::class.simpleName} is missing to provide the following rules:${separator}$missingRules")
            .isEmpty()
    }

    private companion object {
        const val separator = "\n\t"
    }
}
