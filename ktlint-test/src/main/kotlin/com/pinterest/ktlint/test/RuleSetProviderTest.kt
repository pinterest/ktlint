package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.RuleSetProvider
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

public open class RuleSetProviderTest(
    private val rulesetClass: Class<out RuleSetProvider>,
    private val packageName: String
) {
    private val ruleSetProvider =
        rulesetClass
            .getDeclaredConstructor()
            .newInstance()
    private val rules =
        ruleSetProvider
            .get()
            .rules

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

    @Test
    public fun checkAllRulesArePrefixedWithTheRuleSetId() {
        val ruleSetId = ruleSetProvider.get().id
        val rulesWithoutRuleSetId =
            rules
                .map { it.id }
                .filterNot { it.startsWith("$ruleSetId:") || ruleSetId == "standard" }
                .sorted()
                .joinToString(separator = separator)

        assertThat(rulesWithoutRuleSetId)
            .withFailMessage("${ruleSetProvider::class.simpleName} provides rules for which the rule id is not prefixed with '$ruleSetId':${separator}$rulesWithoutRuleSetId")
            .isEmpty()
    }

    private companion object {
        const val separator = "\n\t"
    }
}
