package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.Parameter
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SinceKtlintAnnotationTest {
    @ParameterizedClass(name = "{argumentSetName}")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @MethodSource("allRules")
    @Nested
    inner class `Given a STABLE or EXPERIMENTAL rule` {
        @Parameter
        lateinit var rule: Rule

        @Test
        fun `The rule has a valid version number not containing the patch level`() {
            val actual =
                rule
                    .sinceKtlintAnnotations()
                    .all { isValidVersionFormat(it.version) }

            assertThat(actual).isTrue
        }

        fun allRules(): Stream<Arguments> = rules { true }

        private fun isValidVersionFormat(version: String): Boolean {
            val versionRegex = Regex("""^\d+\.\d+$""")
            return versionRegex.matches(version)
        }
    }

    @ParameterizedClass(name = "{argumentSetName}")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @MethodSource("stableRules")
    @Nested
    inner class `Given a STABLE rule, eg not implementing interface Experimental` {
        @Parameter
        lateinit var stableRule: Rule

        @Test
        fun `The rule has exactly 1 @SinceKtlint annotation with status STABLE`() {
            val actual =
                stableRule
                    .sinceKtlintAnnotations()
                    .count { it.status == SinceKtlint.Status.STABLE }

            assertThat(actual).isEqualTo(1)
        }

        @Test
        fun `The rule has at most 1 @SinceKtlint annotation with status EXPERIMENTAL`() {
            val actual =
                stableRule
                    .sinceKtlintAnnotations()
                    .count { it.status == SinceKtlint.Status.EXPERIMENTAL }

            assertThat(actual).isLessThanOrEqualTo(1)
        }

        fun stableRules(): Stream<Arguments> = rules { it !is Rule.Experimental }
    }

    @ParameterizedClass(name = "{argumentSetName}")
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @MethodSource("experimentalRules")
    @Nested
    inner class `Given an EXPERIMENTAL rule, eg implementing interface Experimental` {
        @Parameter
        lateinit var experimentalRule: Rule

        @Test
        fun `The rule has exactly 1 @SinceKtlint annotation with status EXPERIMENTAL`() {
            val actual =
                experimentalRule
                    .sinceKtlintAnnotations()
                    .count { it.status == SinceKtlint.Status.EXPERIMENTAL }

            assertThat(actual).isEqualTo(1)
        }

        @Test
        fun `The rule should not have @SinceKtlint annotation with status STABLE`() {
            val actual = experimentalRule.sinceKtlintAnnotations().none { it.status == SinceKtlint.Status.STABLE }

            assertThat(actual).isTrue
        }

        fun experimentalRules(): Stream<Arguments> = rules { it is Rule.Experimental }
    }

    private fun Rule.sinceKtlintAnnotations(): List<SinceKtlint> = this::class.annotations.filterIsInstance<SinceKtlint>()

    private fun rules(predicate2: (Rule) -> Boolean): Stream<Arguments> =
        StandardRuleSetProvider()
            .getRuleProviders()
            .map { it.createNewRuleInstance() }
            .filter { predicate2(it) }
            .map { Arguments.argumentSet(it.ruleId.value, it) }
            .let { argumentSets -> Stream.of(*argumentSets.toTypedArray()) }
}
