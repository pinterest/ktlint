package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class AnnotationSpacingRuleTest {
    private val annotationSpacingRuleAssertThat = AnnotationSpacingRule().assertThat()

    @Test
    fun `Given an annotation on the line above the annotated object`() {
        val code =
            """
            @JvmField
            fun foo() {}

            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a blank line between an annotation and the annotated object`() {
        val code =
            """
            @JvmField

            fun foo() {}
            """.trimIndent()
        val formattedCode =
            """
            @JvmField
            fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolation(1, 9, "Annotations should occur immediately before the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple blank lines between an annotation and the annotated object`() {
        val code =
            """
            @JvmField


            fun foo() {}
            """.trimIndent()
        val formattedCode =
            """
            @JvmField
            fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolation(1, 9, "Annotations should occur immediately before the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotation on the same line as the annotated construct is not wrapped`() {
        val code =
            """
            @JvmField fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a blank line between multiple annotations on same line and the annotated object`() {
        val code =
            """
            @JvmField @JvmStatic

            fun foo() = Unit
            """.trimIndent()
        val formattedCode =
            """
            @JvmField @JvmStatic
            fun foo() = Unit
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolations(
                // TODO: It is not correct that the error is reported twice
                LintViolation(1, 20, "Annotations should occur immediately before the annotated construct"),
                LintViolation(1, 20, "Annotations should occur immediately before the annotated construct")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a blank line between multiple annotations on distinct lines and the annotated object`() {
        val code =
            """
            @JvmField
            @JvmStatic

            fun foo() = Unit
            """.trimIndent()
        val formattedCode =
            """
            @JvmField
            @JvmStatic
            fun foo() = Unit
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolations(
                // TODO: It is not correct that the error is reported twice
                LintViolation(2, 10, "Annotations should occur immediately before the annotated construct"),
                LintViolation(2, 10, "Annotations should occur immediately before the annotated construct")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a blank line between annotations`() {
        val code =
            """
            @JvmField

            @JvmStatic
            fun foo() = Unit
            """.trimIndent()
        val formattedCode =
            """
            @JvmField
            @JvmStatic
            fun foo() = Unit
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolation(3, 10, "Annotations should occur immediately before the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a blank line between an annotation and the last annotation which is on the same line as the annotated construct`() {
        val code =
            """
            @JvmField

            @JvmStatic fun foo() = Unit
            """.trimIndent()
        val formattedCode =
            """
            @JvmField
            @JvmStatic fun foo() = Unit
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            // TODO: Offset of error is not correct
            .hasLintViolation(3, 10, "Annotations should occur immediately before the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1087 - Given a block comment between an annotation and the annotated construct`() {
        val code =
            """
            @Suppress("DEPRECATION") @Hello
            /**
             * block comment
             */
            class Foo {
            }
            """.trimIndent()
        val formattedCode =
            """
            /**
             * block comment
             */
            @Suppress("DEPRECATION") @Hello
            class Foo {
            }
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolation(1, 31, "Annotations should occur immediately before the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1087 - Given an EOL comment between an annotation and the annotated construct`() {
        val code =
            """
            @Suppress("DEPRECATION") @Hello
            // hello
            class Foo {
            }
            """.trimIndent()
        val formattedCode =
            """
            // hello
            @Suppress("DEPRECATION") @Hello
            class Foo {
            }
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolation(1, 31, "Annotations should occur immediately before the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given an annotation preceded by multiple blank lines`() {
        val code =
            """


            @Suppress("DEPRECATION")
            val foo = "foo"
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1168 - Given an EOL comment on line above the annotation`() {
        val code =
            """
            // comment
            @SuppressWarnings
            fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1168 - Given an EOL comment on same line as the annotation`() {
        val code =
            """
            @SuppressWarnings // comment
            fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1168 - Given a block comment above the annotation`() {
        val code =
            """
            /* comment */
            @SuppressWarnings
            fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Issue 1168 - Given a block comment on same line as the annotation`() {
        val code =
            """
            @SuppressWarnings /* comment */
            fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }
}
