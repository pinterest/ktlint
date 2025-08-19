package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SinceKtlintAnnotationTest {
    @Test
    fun `Given all rules then each rule should have proper SinceKtlint annotations`() {
        val ruleSetProvider = StandardRuleSetProvider()
        val rules = ruleSetProvider.getRuleProviders().map { it.createNewRuleInstance() }

        val violations =
            rules.flatMap { rule ->
                val ruleClass = rule::class
                val annotations = ruleClass.annotations.filterIsInstance<SinceKtlint>()
                val isExperimental = rule is Rule.Experimental

                val annotationViolations =
                    when {
                        annotations.isEmpty() -> {
                            listOf("${ruleClass.simpleName} has no @SinceKtlint annotations")
                        }

                        isExperimental -> {
                            val experimentalAnnotations = annotations.filter { it.status == SinceKtlint.Status.EXPERIMENTAL }
                            val stableAnnotations = annotations.filter { it.status == SinceKtlint.Status.STABLE }

                            when {
                                experimentalAnnotations.isEmpty() -> {
                                    listOf(
                                        "${ruleClass.simpleName} implements Experimental but has no EXPERIMENTAL @SinceKtlint annotation",
                                    )
                                }

                                stableAnnotations.isNotEmpty() -> {
                                    listOf("${ruleClass.simpleName} implements Experimental but has STABLE @SinceKtlint annotation")
                                }

                                else -> {
                                    emptyList()
                                }
                            }
                        }

                        else -> {
                            val stableAnnotations = annotations.filter { it.status == SinceKtlint.Status.STABLE }
                            if (stableAnnotations.isEmpty()) {
                                listOf("${ruleClass.simpleName} is stable but has no STABLE @SinceKtlint annotation")
                            } else {
                                emptyList()
                            }
                        }
                    }

                val versionViolations =
                    annotations.mapNotNull { annotation ->
                        if (!isValidVersionFormat(annotation.version)) {
                            "${ruleClass.simpleName} has invalid version format '${annotation.version}' - should be 'X.Y' format without patch level"
                        } else {
                            null
                        }
                    }

                annotationViolations + versionViolations
            }

        assertThat(violations)
            .withFailMessage("Found @SinceKtlint annotation violations:\n${violations.joinToString("\n")}")
            .isEmpty()
    }

    private fun isValidVersionFormat(version: String): Boolean {
        val versionRegex = Regex("""^\d+\.\d+$""")
        return versionRegex.matches(version)
    }
}
