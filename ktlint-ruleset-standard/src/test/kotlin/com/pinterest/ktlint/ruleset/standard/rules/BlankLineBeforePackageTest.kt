package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRuleBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.EnumSource.Mode.EXCLUDE
import org.junit.jupiter.params.provider.EnumSource.Mode.INCLUDE

class BlankLineBeforePackageTest {
    private val blankLineBeforePackageRuleAssertThat =
        assertThatRuleBuilder { BlankLineBeforePackage() }
            .addAdditionalRuleProvider { NoConsecutiveBlankLinesRule() }
            .assertThat()

    // Just for one single test evaluates that the rule is working for `ktlint_official` and `android_studio` code styles, but not for
    // `intellij_idea`. It is assumed that all other tests (do not) work similarly.
    @ParameterizedTest(name = "Code style: {0}")
    @EnumSource(CodeStyleValue::class, mode = INCLUDE, names = ["ktlint_official", "android_studio"])
    fun `Given a copyright comment and package statement not separated by a blank line then insert a blank line in between`(
        codeStyleValue: CodeStyleValue,
    ) {
        val code =
            """
            /*
             * Copyright comment
             */
            package foo
            """.trimIndent()
        val formattedCode =
            """
            /*
             * Copyright comment
             */

            package foo
            """.trimIndent()
        blankLineBeforePackageRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to codeStyleValue.name)
            .hasLintViolation(4, 1, "Expected a blank line before the package statement")
            .isFormattedAs(formattedCode)
    }

    @ParameterizedTest(name = "Code style: {0}")
    @EnumSource(CodeStyleValue::class, mode = EXCLUDE, names = ["ktlint_official", "android_studio"])
    fun `Given a copyright comment and package statement not separated by a blank line then do not insert a blank line in between`(
        codeStyleValue: CodeStyleValue,
    ) {
        val code =
            """
            /*
             * Copyright comment
             */
            package foo
            """.trimIndent()
        blankLineBeforePackageRuleAssertThat(code)
            .withEditorConfigOverride(CODE_STYLE_PROPERTY to codeStyleValue.name)
            .hasNoLintViolations()
    }

    @Test
    fun `Given a package statement at the first line then do not insert a blank line before it`() {
        val code =
            """
            package Foo
            """.trimIndent()
        blankLineBeforePackageRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a copyright comment and package statement separated by exactly one blank line then do not insert another blank line in between`() {
        val code =
            """
            /*
             * Copyright comment
             */

            package foo
            """.trimIndent()
        blankLineBeforePackageRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a copyright comment and package statement separated by too many blank line then do remove the redundant blank lines`() {
        val code =
            """
            /*
             * Copyright comment
             */


            package foo
            """.trimIndent()
        val formattedCode =
            """
            /*
             * Copyright comment
             */

            package foo
            """.trimIndent()
        blankLineBeforePackageRuleAssertThat(code)
            .hasNoLintViolationsExceptInAdditionalRules()
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a file annotation and package statement not separated by a blank line then do not insert a blank line in between`() {
        val code =
            """
            @file:bar
            package foo
            """.trimIndent()
        val formattedCode =
            """
            @file:bar

            package foo
            """.trimIndent()
        blankLineBeforePackageRuleAssertThat(code)
            .hasLintViolation(2, 1, "Expected a blank line before the package statement")
            .isFormattedAs(formattedCode)
    }
}
