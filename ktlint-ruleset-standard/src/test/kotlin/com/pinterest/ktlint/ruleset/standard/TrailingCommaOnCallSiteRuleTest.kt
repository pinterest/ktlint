package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.ruleset.standard.TrailingCommaOnCallSiteRule.Companion.allowTrailingCommaOnCallSiteProperty
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class TrailingCommaOnCallSiteRuleTest {
    private val ruleAssertThat =
        assertThatRule(
            provider = { TrailingCommaOnCallSiteRule() },
            additionalRuleProviders = setOf(
                // Apply the IndentationRule always as additional rule, so that the formattedCode in the unit test looks
                // correct.
                RuleProvider { IndentationRule() },
            ),
        )

    @Test
    fun `Given property allow trailing comma on call site is not set then remove trailing comma's`() {
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
        ruleAssertThat(code)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
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

            @Annotation([
                1,
                2 // The comma before the comment should be removed without removing the comment itself
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2 /* The comma before the comment should be removed without removing the comment itself */
            ])
            val foo3: Int = 0
            """.trimIndent()
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to false)
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

            @Annotation([
                1,
                2, // The comma should be inserted before the comment
            ])
            val foo2: Int = 0

            @Annotation([
                1,
                2, /* The comma should be inserted before the comment */
            ])
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
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
        ruleAssertThat(code)
            .withEditorConfigOverride(allowTrailingCommaOnCallSiteProperty to true)
            .hasLintViolations(
                LintViolation(6, 19, "Missing trailing comma before \"]\""),
                LintViolation(7, 6, "Missing trailing comma before \")\""),
            ).isFormattedAs(formattedCode)
    }
}
