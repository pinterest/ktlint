package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.RuleSetProvider
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

open class RuleSetProviderTest(
    private val rulesetClass: Class<out RuleSetProvider>,
    private val packageName: String
) {
    @Test
    fun checkAllRulesProvided() {
        val srcLocation = rulesetClass.protectionDomain.codeSource.location.path
        val rulesDir = File(srcLocation + packageName.replace(".", "/"))
        val packageRules = rulesDir.listFiles()
            ?.map { it.name.removeSuffix(".class") }
            ?.filter { it.endsWith("Rule") }
            ?: arrayListOf()

        val provider = rulesetClass
        val providerRules = provider.getDeclaredConstructor().newInstance().get().rules.map { it::class.java.simpleName }
        val diff = packageRules - providerRules.toSet()
        assertThat(diff)
            .withFailMessage("%s is missing to provide the following rules: \n%s", provider.simpleName, diff.joinToString(separator = "\n"))
            .hasSize(0)
    }
}
