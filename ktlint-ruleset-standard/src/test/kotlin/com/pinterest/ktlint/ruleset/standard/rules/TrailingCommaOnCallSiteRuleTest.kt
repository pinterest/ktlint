package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.ruleset.standard.rules.TrailingCommaOnCallSiteRule.Companion.TRAILING_COMMA_ON_CALL_SITE_PROPERTY
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class TrailingCommaOnCallSiteRuleTest {
    private val trailingCommaOnCallSiteRuleAssertThat =
        assertThatRule(
            provider = { TrailingCommaOnCallSiteRule() },
            additionalRuleProviders =
                setOf(
                    // WrappingRule must be loaded in order to run TrailingCommaOnCallSiteRule
                    RuleProvider { WrappingRule() },
                    // Apply the IndentationRule always as additional rule, so that the formattedCode in the unit test looks
                    // correct.
                    RuleProvider { IndentationRule() },
                ),
        )

    @Test
    fun `Given property allow trailing comma on call site is not set then remove trailing commas`() {
        val code =
            """
            val foo1 = listOf("a", "b",)

            val foo2 = Pair(1, 2,)

            val foo3: List<String,> = emptyList()

            val foo4 = Array(2) { 42 }
            val bar4 = foo4[1,]

            annotation class Foo5(val params: IntArray)
            @Foo5([1, 2,])
            val foo5: Int = 0
            """.trimIndent()
        val formattedCode =
            """
            val foo1 = listOf("a", "b")

            val foo2 = Pair(1, 2)

            val foo3: List<String> = emptyList()

            val foo4 = Array(2) { 42 }
            val bar4 = foo4[1]

            annotation class Foo5(val params: IntArray)
            @Foo5([1, 2])
            val foo5: Int = 0
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 27, "Unnecessary trailing comma before \")\""),
                LintViolation(3, 21, "Unnecessary trailing comma before \")\""),
                LintViolation(5, 22, "Unnecessary trailing comma before \">\""),
                LintViolation(8, 18, "Unnecessary trailing comma before \"]\""),
                LintViolation(11, 12, "Unnecessary trailing comma before \"]\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from an argument list when present`() {
        val code =
            """
            val list1 = listOf("a", "b",)
            val list2 = listOf(
                "a",
                "b", // The comma before the comment should be removed without removing the comment itself
            )
            val list3 = listOf(
                "a",
                "b", /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        val formattedCode =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b" // The comma before the comment should be removed without removing the comment itself
            )
            val list3 = listOf(
                "a",
                "b" /* The comma before the comment should be removed without removing the comment itself */
            )
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to false)
            .hasLintViolations(
                LintViolation(1, 28, "Unnecessary trailing comma before \")\""),
                LintViolation(4, 8, "Unnecessary trailing comma before \")\""),
                LintViolation(8, 8, "Unnecessary trailing comma before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the argument list when missing`() {
        val code =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b" // The comma should be inserted before the comment
            )
            val list3 = listOf(
                "a",
                "b" /* The comma should be inserted before the comment */
            )
            """.trimIndent()
        val formattedCode =
            """
            val list1 = listOf("a", "b")
            val list2 = listOf(
                "a",
                "b", // The comma should be inserted before the comment
            )
            val list3 = listOf(
                "a",
                "b", /* The comma should be inserted before the comment */
            )
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolations(
                LintViolation(4, 8, "Missing trailing comma before \")\""),
                LintViolation(8, 8, "Missing trailing comma before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from the type argument list when present`() {
        val code =
            """
            val list1: List<String,> = emptyList()
            val list2: List<
                String, // The comma before the comment should be removed without removing the comment itself
            > = emptyList()
            val list3: List<
                String, /* The comma before the comment should be removed without removing the comment itself */
                > = emptyList()
            """.trimIndent()
        val formattedCode =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String // The comma before the comment should be removed without removing the comment itself
            > = emptyList()
            val list3: List<
                String /* The comma before the comment should be removed without removing the comment itself */
            > = emptyList()
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to false)
            .hasLintViolations(
                LintViolation(1, 23, "Unnecessary trailing comma before \">\""),
                LintViolation(3, 11, "Unnecessary trailing comma before \">\""),
                LintViolation(6, 11, "Unnecessary trailing comma before \">\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the type argument list when missing`() {
        val code =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String // The comma should be inserted before the comment
            > = emptyList()
            val list3: List<
                String /* The comma should be inserted before the comment */
            > = emptyList()
            """.trimIndent()
        val formattedCode =
            """
            val list1: List<String> = emptyList()
            val list2: List<
                String, // The comma should be inserted before the comment
            > = emptyList()
            val list3: List<
                String, /* The comma should be inserted before the comment */
            > = emptyList()
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolations(
                LintViolation(3, 11, "Missing trailing comma before \">\""),
                LintViolation(6, 11, "Missing trailing comma before \">\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from the array index when present`() {
        val code =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1,]
            val bar2 = foo[
                1, // The comma before the comment should be removed without removing the comment itself
            ]
            val bar3 = foo[
                1, /* The comma before the comment should be removed without removing the comment itself */
            ]
            """.trimIndent()
        val formattedCode =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1]
            val bar2 = foo[
                1 // The comma before the comment should be removed without removing the comment itself
            ]
            val bar3 = foo[
                1 /* The comma before the comment should be removed without removing the comment itself */
            ]
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to false)
            .hasLintViolations(
                LintViolation(2, 17, "Unnecessary trailing comma before \"]\""),
                LintViolation(4, 6, "Unnecessary trailing comma before \"]\""),
                LintViolation(7, 6, "Unnecessary trailing comma before \"]\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the array index when missing`() {
        val code =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1]
            val bar2 = foo[
                1 // The comma should be inserted before the comment
            ]
            val bar3 = foo[
                1 /* The comma should be inserted before the comment */
            ]
            """.trimIndent()
        val formattedCode =
            """
            val foo = Array(2) { 42 }
            val bar1 = foo[1]
            val bar2 = foo[
                1, // The comma should be inserted before the comment
            ]
            val bar3 = foo[
                1, /* The comma should be inserted before the comment */
            ]
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolations(
                LintViolation(4, 6, "Missing trailing comma before \"]\""),
                LintViolation(7, 6, "Missing trailing comma before \"]\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is not allowed on call site then remove it from the collection literal when present`() {
        val code =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2,])
            val foo1: Int = 0

            @Annotation([
                1,
                2, // The comma before the comment should be removed without removing the comment itself
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2, /* The comma before the comment should be removed without removing the comment itself */
            ])
            val foo3: Int = 0
            """.trimIndent()
        val formattedCode =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2])
            val foo1: Int = 0

            @Annotation(
                [
                    1,
                    2 // The comma before the comment should be removed without removing the comment itself
                ]
            )
            val foo2: Int = 0

            @Annotation(
                [
                    1,
                    2 /* The comma before the comment should be removed without removing the comment itself */
                ]
            )
            val foo3: Int = 0
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to false)
            .hasLintViolations(
                LintViolation(3, 18, "Unnecessary trailing comma before \"]\""),
                LintViolation(8, 6, "Unnecessary trailing comma before \"]\""),
                LintViolation(14, 6, "Unnecessary trailing comma before \"]\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that the trailing comma is required on call site then add it to the collection literal when missing`() {
        val code =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2])
            val foo1: Int = 0

            @Annotation([
                1,
                2 // The comma should be inserted before the comment
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2 /* The comma should be inserted before the comment */
            ])
            val foo3: Int = 0

            @Annotation(
                [
                    1,
                    2 /* The comma should be inserted before the comment */
                ],
                [
                    3,
                    4 /* The comma should be inserted before the comment */
                ]
            )
            val foo4: Int = 0
            """.trimIndent()
        val formattedCode =
            """
            annotation class Annotation(val params: IntArray)

            @Annotation([1, 2])
            val foo1: Int = 0

            @Annotation(
                [
                    1,
                    2, // The comma should be inserted before the comment
                ],
            )
            val foo2: Int = 0

            @Annotation(
                [
                    1,
                    2, /* The comma should be inserted before the comment */
                ],
            )
            val foo3: Int = 0

            @Annotation(
                [
                    1,
                    2, /* The comma should be inserted before the comment */
                ],
                [
                    3,
                    4, /* The comma should be inserted before the comment */
                ],
            )
            val foo4: Int = 0
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolations(
                LintViolation(8, 6, "Missing trailing comma before \"]\""),
                LintViolation(14, 6, "Missing trailing comma before \"]\""),
                LintViolation(21, 10, "Missing trailing comma before \"]\""),
                LintViolation(25, 10, "Missing trailing comma before \"]\""),
                LintViolation(26, 6, "Missing trailing comma before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given that a trailing comma is required on call site then still it should not be added to the setter`() {
        val code =
            """
            class Test {
                var foo = Bar()
                    set(value) {
                    }
            }
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasNoLintViolations()
    }

    @Test
    fun `1297 - Given that the trailing comma is required on call site the a trailing comma to collection literal when missing`() {
        val code =
            """
            annotation class FooBar(
                val foo1: Array<String> = [],
                val foo2: Array<String> = [],
                val bar1: String = ""
            )

            @FooBar(
                foo1 = [
                    "foo-1" // Add trailing comma as the argument value list foo1 is a multiline statement
                ],
                foo2 = ["foo-2"], // Do not add trailing comma in the array as the argument value list foo2 is a single line statement
                bar1 = "bar-1" // Add trailing comma as the outer argument value list of the annotation is a multiline statement
            )
            val fooBar = null
            """.trimIndent()
        val formattedCode =
            """
            annotation class FooBar(
                val foo1: Array<String> = [],
                val foo2: Array<String> = [],
                val bar1: String = ""
            )

            @FooBar(
                foo1 = [
                    "foo-1", // Add trailing comma as the argument value list foo1 is a multiline statement
                ],
                foo2 = ["foo-2"], // Do not add trailing comma in the array as the argument value list foo2 is a single line statement
                bar1 = "bar-1", // Add trailing comma as the outer argument value list of the annotation is a multiline statement
            )
            val fooBar = null
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolations(
                LintViolation(9, 16, "Missing trailing comma before \"]\""),
                LintViolation(12, 19, "Missing trailing comma before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1379 - Given that a trailing comma is required on call site then add trailing comma after array in annotation when missing`() {
        val code =
            """
            import kotlin.reflect.KClass

            @Foo(
                values = [
                    Foo::class,
                    Foo::class
                ]
            )
            annotation class Foo(val values: Array<KClass<*>>)
            """.trimIndent()
        val formattedCode =
            """
            import kotlin.reflect.KClass

            @Foo(
                values = [
                    Foo::class,
                    Foo::class,
                ],
            )
            annotation class Foo(val values: Array<KClass<*>>)
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolations(
                LintViolation(6, 19, "Missing trailing comma before \"]\""),
                LintViolation(7, 6, "Missing trailing comma before \")\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Issue 1602 - Given that an empty argument list with a comment, dot add a trailing comma`() {
        val code =
            """
            val foo1 = setOf<Int>(
                // some comment
            )
            val foo2 = setOf<Int>(
            )
            val foo1 = setOf<Int>()
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasNoLintViolations()
    }

    @Nested
    inner class `Issue 1642 - Given a multiline argument` {
        @Test
        fun `Issue 1642 - Given a single multiline argument then do not add a trailing comma`() {
            val code =
                """
                fun main() {
                    bar(
                        object : Foo {
                            override fun foo() {
                                "foo"
                            }
                        },
                    )
                }
                """.trimIndent()
            trailingCommaOnCallSiteRuleAssertThat(code)
                .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
                .hasNoLintViolations()
        }

        @Test
        fun `Issue 1642 - Given an argument followed by multiline argument then add a trailing comma`() {
            val code =
                """
                fun main() {
                    bar(
                        "foo",
                        object : Foo {
                            override fun foo() {
                                TODO("Not yet implemented")
                            }
                        }
                    )
                }
                """.trimIndent()
            trailingCommaOnCallSiteRuleAssertThat(code)
                .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
                .hasLintViolation(8, 10, "Missing trailing comma before \")\"")
        }

        @Test
        fun `Issue 1642 - Given a multiline argument followed by another argument then add a trailing comma`() {
            val code =
                """
                fun main() {
                    bar(
                        object : Foo {
                            override fun foo() {
                                TODO("Not yet implemented")
                            }
                        },
                        "foo"
                    )
                }
                """.trimIndent()
            trailingCommaOnCallSiteRuleAssertThat(code)
                .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
                .hasLintViolation(8, 14, "Missing trailing comma before \")\"")
        }
    }

    @Test
    fun `Issue 1676 - Given a trailing comma followed by a kdoc then do no add another trailing comma`() {
        val code =
            """
            val foo1 = setOf(
                1, /** Comment */
            )
            val foo1 = setOf(
                2,
                /** Comment */
            )
            val foo3 = setOf(
                3,
                /**
                 * Comment
                 */
            )
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasNoLintViolations()
    }

    // The test below covers for a bug which was quite hard to track down as it was a combination of two distinct bugs.
    // Code sample before formatting:
    //     val foo = foo(
    //         listOf(
    //             "bar",
    //         )
    //     )
    // Code sample after formatting (not wrong indenting of closing parenthesis after element "bar"):
    //     val foo = foo(
    //         listOf(
    //             "bar",
    //             ),
    //     )
    // The problem could not be reproduced within the IndentationRuleTest. This was caused by a problem regarding the
    // sort order of the rules. The code sample above was formatted with CLI parameter "--experimental" while in the
    // IndentationRuleTest the "experimental function-signature" was not added as additional rule. Without the
    // FunctionSignatureRule the effective order of the rules became:
    //   - standard:wrapping,
    //   - standard:indent,
    //   - standard:trailing-comma-on-call-site,
    // This is wrong as the indent rule may only run after the trailing comma rules. The problem was that the Rule
    // sorter did not take rules having multiple RunAfterRule modifier into account.
    //
    // The second problem was a problem in the TrailingCommaOnCallSiteRule. The problem can only be visualized by
    // inspecting the actual Psi which was generated. Before fixing this bug, the trailing comma was added as an element
    // of the PsiElement representing the 'listOf("bar")' instead of as an element of the PsiElement representing the
    // 'foo(...)'. This caused the IndentationRule to format the closing parenthesis after element "bar" incorrectly.
    @Test
    fun `Given a function call with a list parameter then add the trailing comma after the PsiElement representing the list`() {
        val code =
            """
            val foo = foo(
                listOf(
                    "bar",
                )
            )
            """.trimIndent()
        val formattedCode =
            """
            val foo = foo(
                listOf(
                    "bar",
                ),
            )
            """.trimIndent()
        trailingCommaOnCallSiteRuleAssertThat(code)
            .withEditorConfigOverride(TRAILING_COMMA_ON_CALL_SITE_PROPERTY to true)
            .hasLintViolation(4, 6, "Missing trailing comma before \")\"")
            .isFormattedAs(formattedCode)
    }
}
