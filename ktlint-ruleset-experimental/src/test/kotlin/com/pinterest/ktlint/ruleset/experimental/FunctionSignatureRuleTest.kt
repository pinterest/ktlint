package com.pinterest.ktlint.ruleset.experimental

import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.ruleset.experimental.FunctionSignatureRule.Companion.functionSignatureWrappingMinimumParametersProperty
import com.pinterest.ktlint.ruleset.standard.NoMultipleSpacesRule
import com.pinterest.ktlint.ruleset.standard.SpacingAroundColonRule
import com.pinterest.ktlint.ruleset.standard.SpacingAroundCommaRule
import com.pinterest.ktlint.ruleset.standard.SpacingAroundDotRule
import com.pinterest.ktlint.ruleset.standard.SpacingAroundOperatorsRule
import com.pinterest.ktlint.ruleset.standard.SpacingAroundParensRule
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.MAX_LINE_LENGTH_MARKER
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class FunctionSignatureRuleTest {
    private val functionSignatureWrappingRuleAssertThat = FunctionSignatureRule().assertThat()

    @Test
    fun `Given a single line function signature which is smaller than or equal to the max line length, and the function is followed by a body block, then do no change the signature`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER            $EOL_CHAR
            fun f(a: Any, b: Any, c: Any): String {
                // body
            }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given a single line function signature which is equal to the max line length but missing a space before the opening brace of the body block, and the function is followed by a body block, then do no change the signature`() {
        // Note: the remainder of the tests primarily uses the single expression body as it is a bit more concise than
        // the block expression.
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            fun f(a: Any, b: Any, c: Any): String{
                // body
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            fun f(
                a: Any,
                b: Any,
                c: Any
            ): String {
                // body
            }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .addAdditionalFormattingRule(FunctionStartOfBodySpacingRule())
            .hasLintViolation(2, 38, "Expected a single space before body block")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature which is greater then the max line length then reformat to a multiline signature`() {
        // Note: the remainder of the tests primarily uses the single expression body as it is a bit more concise than
        // the block expression.
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            fun f(a: Any, b: Any, c: Any): String {
                // body
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER           $EOL_CHAR
            fun f(
                a: Any,
                b: Any,
                c: Any
            ): String {
                // body
            }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 7, "Newline expected after opening parenthesis"),
                LintViolation(2, 15, "Parameter should start on a newline"),
                LintViolation(2, 23, "Parameter should start on a newline"),
                LintViolation(2, 29, "Newline expected before closing parenthesis")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature which is smaller than or equal to the max line length, and the function is followed by a body expression, then do not change the signature`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                  $EOL_CHAR
            fun f(string: String): String = "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given a single line function signature which is smaller than or equal to the max line length, and the function is followed by a body expression which does not fit on that same line, then do not change the signature but wrap the expression body`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            fun f(string: String): String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            fun f(string: String): String =
                "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 33, "Newline expected before expression body")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature and max-line-length is not set then do not rewrite a multiline signature`() {
        val code =
            """
            // No max line length marker!
            fun f(string: String): String = string.toUpperCase()
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a single line function signature with a length greater than the max line length then reformat to multiline signature`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR
            fun f(a: Any, b: Any, c: Any): String = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR
            fun f(
                a: Any,
                b: Any,
                c: Any
            ): String = "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 7, "Newline expected after opening parenthesis"),
                LintViolation(2, 15, "Parameter should start on a newline"),
                LintViolation(2, 23, "Parameter should start on a newline"),
                LintViolation(2, 29, "Newline expected before closing parenthesis")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given some function signatures containing at least one comment then do not reformat although the max line length is exceeded`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                     $EOL_CHAR
            private /* some comment */ fun f1(a: Any, b: Any): String = "some-result"
            private fun /* some comment */ f2(a: Any, b: Any): String = "some-result"
            private fun f3 /* some comment */ (a: Any, b: Any): String = "some-result"
            private fun f4( /* some comment */ a: Any, b: Any): String = "some-result"
            private fun f5(a /* some comment */: Any, b: Any): String = "some-result"
            private fun f6(a: /* some comment */ Any, b: Any): String = "some-result"
            private fun f7(a: Any /* some comment */, b: Any): String = "some-result"
            private fun f8(a: Any, b: Any) /* some comment */: String = "some-result"
            private fun f9(a: Any, b: Any): /* some comment */ String = "some-result"
            private fun f10(a: Any, b: Any): String /* some comment */ = "some-result"
            private fun f11(
                a: Any, // some-comment
                b: Any
            ): String = "f10"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given a function signature with a newline between the last parameter in the parameter list and the closing parenthesis, but which does not fit on a single line then reformat to a proper multiline signature`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER $EOL_CHAR
            fun f(a: Any, b: Any, c: Any
            ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER $EOL_CHAR
            fun f(
                a: Any,
                b: Any,
                c: Any
            ) = "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(2, 7, "Newline expected after opening parenthesis"),
                LintViolation(2, 15, "Parameter should start on a newline"),
                LintViolation(2, 23, "Parameter should start on a newline")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature with an expression body that is missing the required space just before the expression, but which after adding this spaces no longer fits on a single line, then push the expression body to the next line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            fun f(a: Any, b: Any, c: Any) ="some-result"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            fun f(a: Any, b: Any, c: Any) =
                "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 32, "Newline expected before expression body")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature with an expression body on the next line which actually fits on the same line as the function signature`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                  $EOL_CHAR
            fun f(a: Any, b: Any, c: Any) =
                "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                  $EOL_CHAR
            fun f(a: Any, b: Any, c: Any) = "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 32, "First line of body expression fits on same line as function signature")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature returning a lambda which not entire fits on the same line, then push the lambda to the next line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                             $EOL_CHAR                                                                                                    $EOL_CHAR
            fun foo(bar: String): (String) -> Boolean = { it == bar }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                             $EOL_CHAR                                                                                                    $EOL_CHAR
            fun foo(bar: String): (String) -> Boolean =
                { it == bar }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 45, "Newline expected before expression body")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a single line function signature returning a lambda on the next line which actually fits on the same line as the function signature`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                              $EOL_CHAR                                                                                                    $EOL_CHAR
            fun foo(bar: String): (String) -> Boolean =
                { it == bar }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                              $EOL_CHAR                                                                                                    $EOL_CHAR
            fun foo(bar: String): (String) -> Boolean = { it == bar }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolation(2, 44, "First line of body expression fits on same line as function signature")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function without parameters but with at least one space between the parenthesis then reformat to a proper single line signature`() {
        val code =
            """
            fun f( ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun f() = "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .hasLintViolation(1, 7, "No whitespace expected in empty parameter list")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a function without parameters but with at least one newline between the parenthesis then reformat to a proper single line signature`() {
        val code =
            """
            fun f(

            ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            fun f() = "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .hasLintViolation(1, 7, "No whitespace expected in empty parameter list")
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a multiline function signature which actually fits on a single line but for which the expression body does not fit at that same line then reformat the signature and wrap the body expression to the next line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            // Entire signature is just one character too long to fit on a single line
            // ...a: Any, b: Any, c: Any) = "some-result"
            fun f(
                a: Any,
                b: Any,
                c: Any
            ) = "some-result"
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR
            // Entire signature is just one character too long to fit on a single line
            // ...a: Any, b: Any, c: Any) = "some-result"
            fun f(a: Any, b: Any, c: Any) =
                "some-result"
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(5, 5, "No whitespace expected between opening parenthesis and first parameter name"),
                LintViolation(6, 5, "Single whitespace expected before parameter"),
                LintViolation(7, 5, "Single whitespace expected before parameter"),
                LintViolation(7, 11, "No whitespace expected between last parameter and closing parenthesis"),
                LintViolation(8, 5, "Newline expected before expression body")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class with a multiline function signature which actually fits on a single line but for which the expression body does not fit at that same line then reformat the signature and wrap the body expression to the next line`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            // Entire signature is just one character too long to fit on a single line
            //  fun(a: Any, b: Any, c: Any) = "some-result"
            class Foo {
                fun f(
                    a: Any,
                    b: Any,
                    c: Any
                ) = "some-result"
            }
            """.trimIndent()
        val formattedCode =
            """
            // $MAX_LINE_LENGTH_MARKER                   $EOL_CHAR
            // Entire signature is just one character too long to fit on a single line
            //  fun(a: Any, b: Any, c: Any) = "some-result"
            class Foo {
                fun f(a: Any, b: Any, c: Any) =
                    "some-result"
            }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasLintViolations(
                LintViolation(6, 9, "No whitespace expected between opening parenthesis and first parameter name"),
                LintViolation(7, 9, "Single whitespace expected before parameter"),
                LintViolation(8, 9, "Single whitespace expected before parameter"),
                LintViolation(
                    8,
                    15,
                    "No whitespace expected between last parameter and closing parenthesis"
                ),
                LintViolation(9, 9, "Newline expected before expression body")
            ).isFormattedAs(formattedCode)
    }

    @Nested
    inner class MinimumNumberOfParameters {
        @Test
        fun `Given a single line function signature which is smaller than or equal to the max line length but having too many parameters then do reformat as multiline signature`() {
            val code =
                """
                // $MAX_LINE_LENGTH_MARKER            $EOL_CHAR
                fun f(a: Any, b: Any, c: Any): String {
                    // body
                }
                """.trimIndent()
            val formattedCode =
                """
                // $MAX_LINE_LENGTH_MARKER            $EOL_CHAR
                fun f(
                    a: Any,
                    b: Any,
                    c: Any
                ): String {
                    // body
                }
                """.trimIndent()
            functionSignatureWrappingRuleAssertThat(code)
                .setMaxLineLength()
                .withEditorConfigOverride(functionSignatureWrappingMinimumParametersProperty to 2)
                .hasLintViolations(
                    LintViolation(2, 7, "Newline expected after opening parenthesis"),
                    LintViolation(2, 15, "Parameter should start on a newline"),
                    LintViolation(2, 23, "Parameter should start on a newline"),
                    LintViolation(2, 29, "Newline expected before closing parenthesis")
                ).isFormattedAs(formattedCode)
        }
    }

    @Test
    fun `Given a function declaration without default implementation in an interface`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER        $EOL_CHAR                                                                                                       $EOL_CHAR
            interface Foo {
                fun foo(a: Any, b: Any): String
            }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    @Test
    fun `Given an abstract function declaration`() {
        val code =
            """
            // $MAX_LINE_LENGTH_MARKER                 $EOL_CHAR                                                                                                       $EOL_CHAR
            abstract class Foo {
                abstract fun foo(a: Any, b: Any): String
            }
            """.trimIndent()
        functionSignatureWrappingRuleAssertThat(code)
            .setMaxLineLength()
            .hasNoLintViolations()
    }

    // The FunctionSignatureWrappingRule depends on a lot of different rules to do an initial clean up. The tests below
    // ensure that those rules effectively clean up so that the FunctionSignatureWrappingRule does not need to check for
    // it at all.
    @Nested
    inner class CleanUpByRelatedRules {
        @Test
        fun `Given a nullable type with a space before the quest then remove this space`() {
            val code =
                """
                fun String${UNEXPECTED_SPACES}?.f1() = "some-result"
                fun List<String${UNEXPECTED_SPACES}?>.f2() = "some-result"
                fun f3(string: String${UNEXPECTED_SPACES}?) = "some-result"
                fun f4(string: List<String${UNEXPECTED_SPACES}?>) = "some-result"
                fun f5(): String${UNEXPECTED_SPACES}? = "some-result"
                fun f6(): List<String${UNEXPECTED_SPACES}?> = listOf("some-result", null)
                fun f7(): List<String>${UNEXPECTED_SPACES}? = null
                """.trimIndent() // ktlint-disable string-template
            val formattedCode =
                """
                fun String?.f1() = "some-result"
                fun List<String?>.f2() = "some-result"
                fun f3(string: String?) = "some-result"
                fun f4(string: List<String?>) = "some-result"
                fun f5(): String? = "some-result"
                fun f6(): List<String?> = listOf("some-result", null)
                fun f7(): List<String>? = null
                """.trimIndent()
            functionSignatureWrappingRuleAssertThat(code)
                .addAdditionalFormattingRule(NullableTypeSpacingRule())
                .hasNoLintViolationsExceptInAdditionalFormattingRules()
                .isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a function signature contains redundant spaces then ensure that those are removed before running the function signature rule`() {
            val code =
                """
                private${UNEXPECTED_SPACES}fun f1() = "some-result"
                fun f2$UNEXPECTED_SPACES() = "some-result"
                fun f3(${UNEXPECTED_SPACES}a: Any) = "some-result"
                fun f4(a$UNEXPECTED_SPACES: Any) = "some-result"
                fun f5(a:${UNEXPECTED_SPACES}Any) = "some-result"
                fun f6(a: Any$UNEXPECTED_SPACES, b: Any) = "some-result"
                fun f7(a: Any,${UNEXPECTED_SPACES}b: Any) = "some-result"
                fun f8(a: Any, b: Any$UNEXPECTED_SPACES) = "some-result"
                fun f9(${UNEXPECTED_SPACES}vararg a: Any) = "some-result"
                fun f10(vararg${UNEXPECTED_SPACES}a: Any) = "some-result"
                fun f11()$UNEXPECTED_SPACES= "some-result"
                fun f12()$UNEXPECTED_SPACES: String = "some-result"
                fun f13():${UNEXPECTED_SPACES}String = "some-result"
                fun f14(): String$UNEXPECTED_SPACES= "some-result"
                fun f15() =$UNEXPECTED_SPACES"some-result"
                fun <${UNEXPECTED_SPACES}T> f16(t: T) = "some-result"
                fun <T>${UNEXPECTED_SPACES}f17(t: T) = "some-result"
                fun <T$UNEXPECTED_SPACES, U> f18(t: T, u: U) = "some-result"
                fun <T,${UNEXPECTED_SPACES}U> f19(t: T, u: U) = "some-result"
                fun <T, U$UNEXPECTED_SPACES> f20(t: T) = "some-result"
                fun Map<${UNEXPECTED_SPACES}Any, Any>.f21(a: Any, b: Any) = "some-result"
                fun Map<Any$UNEXPECTED_SPACES, Any>.f22(a: Any, b: Any) = "some-result"
                fun Map<Any,${UNEXPECTED_SPACES}Any>.f23(a: Any, b: Any) = "some-result"
                fun Map<Any, Any$UNEXPECTED_SPACES>.f24(a: Any, b: Any) = "some-result"
                fun Map<Any, Any>$UNEXPECTED_SPACES.f25(a: Any, b: Any) = "some-result"
                fun f26(block: (${UNEXPECTED_SPACES}T) -> String) = "some-result"
                fun f27(block: (T$UNEXPECTED_SPACES) -> String) = "some-result"
                fun f28(block: (T)$UNEXPECTED_SPACES-> String) = "some-result"
                fun f29(block: (T) ->${UNEXPECTED_SPACES}String) = "some-result"
                """.trimIndent()
            val formattedCode =
                """
                private fun f1() = "some-result"
                fun f2() = "some-result"
                fun f3(a: Any) = "some-result"
                fun f4(a: Any) = "some-result"
                fun f5(a: Any) = "some-result"
                fun f6(a: Any, b: Any) = "some-result"
                fun f7(a: Any, b: Any) = "some-result"
                fun f8(a: Any, b: Any) = "some-result"
                fun f9(vararg a: Any) = "some-result"
                fun f10(vararg a: Any) = "some-result"
                fun f11() = "some-result"
                fun f12(): String = "some-result"
                fun f13(): String = "some-result"
                fun f14(): String = "some-result"
                fun f15() = "some-result"
                fun <T> f16(t: T) = "some-result"
                fun <T> f17(t: T) = "some-result"
                fun <T, U> f18(t: T, u: U) = "some-result"
                fun <T, U> f19(t: T, u: U) = "some-result"
                fun <T, U> f20(t: T) = "some-result"
                fun Map<Any, Any>.f21(a: Any, b: Any) = "some-result"
                fun Map<Any, Any>.f22(a: Any, b: Any) = "some-result"
                fun Map<Any, Any>.f23(a: Any, b: Any) = "some-result"
                fun Map<Any, Any>.f24(a: Any, b: Any) = "some-result"
                fun Map<Any, Any>.f25(a: Any, b: Any) = "some-result"
                fun f26(block: (T) -> String) = "some-result"
                fun f27(block: (T) -> String) = "some-result"
                fun f28(block: (T) -> String) = "some-result"
                fun f29(block: (T) -> String) = "some-result"
                """.trimIndent()
            functionSignatureWrappingRuleAssertThat(code)
                .addAdditionalFormattingRule(
                    NoMultipleSpacesRule(),
                    SpacingAroundAngleBracketsRule(),
                    SpacingAroundParensRule(),
                    SpacingAroundDotRule(),
                    SpacingAroundCommaRule(),
                    SpacingAroundColonRule()
                ).hasLintViolations(
                    LintViolation(3, 10, "No whitespace expected between opening parenthesis and first parameter name"),
                    LintViolation(7, 17, "Single whitespace expected before parameter"),
                    LintViolation(
                        8,
                        22,
                        "No whitespace expected between last parameter and closing parenthesis"
                    ),
                    LintViolation(9, 10, "No whitespace expected between opening parenthesis and first parameter name")
                ).isFormattedAs(formattedCode)
        }

        @Test
        fun `Given a function signature missing required spaces then ensure that those are added before running the function signature rule`() {
            val code =
                """
                fun f1(a:${NO_SPACE}Any) = "some-result"
                fun f2(a: Any,${NO_SPACE}b: Any) = "some-result"
                fun f3()$NO_SPACE= "some-result"
                fun f4():${NO_SPACE}String = "some-result"
                fun f5(): String$NO_SPACE= "some-result"
                fun f6() =$NO_SPACE"some-result"
                fun <T>${NO_SPACE}f7(t: T) = "some-result"
                fun <T,${NO_SPACE}U> f8(t: T, u: U) = "some-result"
                fun Map<Any,${NO_SPACE}Any>.f9(a: Any, b: Any) = "some-result"
                fun f10(block: (T)$NO_SPACE-> String) = "some-result"
                fun f11(block: (T) ->${NO_SPACE}String) = "some-result"
                """.trimIndent()
            val formattedCode =
                """
                fun f1(a: Any) = "some-result"
                fun f2(a: Any, b: Any) = "some-result"
                fun f3() = "some-result"
                fun f4(): String = "some-result"
                fun f5(): String = "some-result"
                fun f6() = "some-result"
                fun <T> f7(t: T) = "some-result"
                fun <T, U> f8(t: T, u: U) = "some-result"
                fun Map<Any, Any>.f9(a: Any, b: Any) = "some-result"
                fun f10(block: (T) -> String) = "some-result"
                fun f11(block: (T) -> String) = "some-result"
                """.trimIndent()
            functionSignatureWrappingRuleAssertThat(code)
                .addAdditionalFormattingRule(
                    TypeParameterListSpacingRule(),
                    FunctionStartOfBodySpacingRule(),
                    FunctionTypeReferenceSpacingRule(),
                    SpacingAroundColonRule(),
                    SpacingAroundCommaRule(),
                    SpacingAroundOperatorsRule()
                ).hasLintViolation(2, 15, "Single whitespace expected before parameter")
                .isFormattedAs(formattedCode)
        }
    }

    private companion object {
        const val EOL_CHAR = '#'
        const val UNEXPECTED_SPACES = "  "
        const val NO_SPACE = ""
    }
}
