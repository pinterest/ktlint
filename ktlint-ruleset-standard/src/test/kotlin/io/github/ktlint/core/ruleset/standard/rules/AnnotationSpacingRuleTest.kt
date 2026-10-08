package io.github.ktlint.core.ruleset.standard.rules

import io.github.ktlint.core.test.KtLintAssertThat.Companion.assertThatRule
import io.github.ktlint.core.test.LintViolation
import org.junit.jupiter.api.Test

class AnnotationSpacingRuleTest {
    private val annotationSpacingRuleAssertThat = assertThatRule { AnnotationSpacingRule() }

    @Test
    fun `Given a modifier list not containing any annotation then do nothing`() {
        val code =
            """
            private fun foo() {}
            """.trimIndent()
        annotationSpacingRuleAssertThat(code).hasNoLintViolations()
    }

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
            .hasLintViolation(2, 1, "No blank line(s) expected between annotation and annotated construct")
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
            .hasLintViolation(2, 1, "No blank line(s) expected between annotation and annotated construct")
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
            .hasLintViolation(2, 1, "No blank line(s) expected between annotation and annotated construct")
            .isFormattedAs(formattedCode)
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
            .hasLintViolation(3, 1, "No blank line(s) expected between annotation and annotated construct")
            .isFormattedAs(formattedCode)
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
            .hasLintViolation(2, 1, "No blank line(s) expected between annotation and annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple occurrences of blank lines between annotations`() {
        val code =
            """
            @JvmField

            @JvmStatic


            @Foo
            fun foo() = Unit
            """.trimIndent()
        val formattedCode =
            """
            @JvmField
            @JvmStatic
            @Foo
            fun foo() = Unit
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "No blank line(s) expected between annotation and annotated construct"),
                LintViolation(4, 1, "No blank line(s) expected between annotation and annotated construct"),
            ).isFormattedAs(formattedCode)
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
            .hasLintViolation(2, 1, "No blank line(s) expected between annotation and annotated construct")
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
            .hasLintViolation(2, 1, "No comment(s) expected between annotation and the annotated construct")
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
            .hasLintViolation(2, 1, "No comment(s) expected between annotation and the annotated construct")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given multiple comment blocks between annotations and the annotated construct then report each comment`() {
        val code =
            """
            @Foo1
            // comment 1
            // comment 2
            @Foo2
            /*
             * comment 3
             */
            class Foo {
            }
            """.trimIndent()
        val formattedCode =
            """
            // comment 1
            // comment 2
            /*
             * comment 3
             */
            @Foo1
            @Foo2
            class Foo {
            }
            """.trimIndent()
        annotationSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, "No comment(s) expected between annotation and the annotated construct"),
                LintViolation(3, 1, "No comment(s) expected between annotation and the annotated construct"),
                LintViolation(5, 1, "No comment(s) expected between annotation and the annotated construct"),
            ).isFormattedAs(formattedCode)
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
