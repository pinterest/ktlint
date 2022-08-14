package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class SpacingAroundAngleBracketRuleTest {
    private val spacingAroundAngleBracketsRuleAssertThat = assertThatRule { SpacingAroundAngleBracketsRule() }

    @Test
    fun `Given a type argument list with unnecessary spaces or newlines before or after the angle brackets`() {
        val code =
            """
            val a: Map< Int, String> = mapOf()
            val b: Map<Int, String > = mapOf()
            val c: Map <Int, String> = mapOf()
            val d: Map<
                Int, String
            > = mapOf()
            val e: Map<
                Int,
                String
            > = mapOf()
            val f: Map
                <
                    Int,
                    String
                > = mapOf()
            val g: Map<Int, List < String > > = mapOf()
            val h: Map<
                Int,
                List<
                    String
                >
            > = mapOf()
            """.trimIndent()
        val formattedCode =
            """
            val a: Map<Int, String> = mapOf()
            val b: Map<Int, String> = mapOf()
            val c: Map<Int, String> = mapOf()
            val d: Map<
                Int, String
                > = mapOf()
            val e: Map<
                Int,
                String
                > = mapOf()
            val f: Map<
                Int,
                String
                > = mapOf()
            val g: Map<Int, List<String>> = mapOf()
            val h: Map<
                Int,
                List<
                    String
                    >
                > = mapOf()
            """.trimIndent()
        spacingAroundAngleBracketsRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(1, 12, "Unexpected spacing after \"<\""),
                LintViolation(2, 23, "Unexpected spacing before \">\""),
                LintViolation(3, 11, "Unexpected spacing before \"<\""),
                LintViolation(11, 11, "Unexpected spacing before \"<\""),
                LintViolation(16, 21, "Unexpected spacing before \"<\""),
                LintViolation(16, 23, "Unexpected spacing after \"<\""),
                LintViolation(16, 30, "Unexpected spacing before \">\""),
                LintViolation(16, 32, "Unexpected spacing before \">\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given type parameter list with unnecessary spaces or newlines before or after the angle brackets`() {
        val code =
            """
            public class Foo1<Bar : String> {}
            public class Foo2< Bar : String> {}
            public class Foo3<Bar : String > {}
            public class Foo4 <Bar : String> {}
            public class Foo5
                <Bar : String> {}
            public class Foo6
                < Bar : String > {}
            public class Foo7<
                Bar1 : String,
                Bar2 : Map<
                    Int,
                    List< String >
                >
            > {}
            """.trimIndent()
        val formattedCode =
            """
            public class Foo1<Bar : String> {}
            public class Foo2<Bar : String> {}
            public class Foo3<Bar : String> {}
            public class Foo4<Bar : String> {}
            public class Foo5<Bar : String> {}
            public class Foo6<Bar : String> {}
            public class Foo7<
                Bar1 : String,
                Bar2 : Map<
                    Int,
                    List<String>
                    >
                > {}
            """.trimIndent()
        spacingAroundAngleBracketsRuleAssertThat(code)
            .addAdditionalRuleProvider { IndentationRule() }
            .hasLintViolations(
                LintViolation(2, 19, "Unexpected spacing after \"<\""),
                LintViolation(3, 31, "Unexpected spacing before \">\""),
                LintViolation(4, 18, "Unexpected spacing before \"<\""),
                LintViolation(5, 18, "Unexpected spacing before \"<\""),
                LintViolation(7, 18, "Unexpected spacing before \"<\""),
                LintViolation(8, 6, "Unexpected spacing after \"<\""),
                LintViolation(8, 19, "Unexpected spacing before \">\""),
                LintViolation(13, 14, "Unexpected spacing after \"<\""),
                LintViolation(13, 21, "Unexpected spacing before \">\""),
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a type parameter of an extension function preceded by multiple spaces`() {
        val code =
            """
            public class AngleTest<B : String> {
                val     <T> T.exhaustive get() = this;
                fun     <T> compare(other: T) {}
                var     <T> T.exhaustive: T get() = this;
            }
            """.trimIndent()
        spacingAroundAngleBracketsRuleAssertThat(code)
            // TODO: This is not consistent with other rules that do not allow to align columns
            .hasNoLintViolations()
    }

    @Test
    fun `Given the reified keyword inside angle brackets`() {
        val code =
            """
            interface Consumer< reified T    > {
                fun add(item: T)
            }
            """.trimIndent()
        val formattedCode =
            """
            interface Consumer<reified T> {
                fun add(item: T)
            }
            """.trimIndent()
        spacingAroundAngleBracketsRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 20, "Unexpected spacing after \"<\""),
                LintViolation(1, 30, "Unexpected spacing before \">\""),
            ).isFormattedAs(formattedCode)
    }
}
