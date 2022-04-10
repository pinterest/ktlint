package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class NoEmptyClassBodyRuleTest {
    private val noEmptyClassBodyRuleAssertThat = NoEmptyClassBodyRule().assertThat()

    @Test
    fun `Given a class, interface, or object with empty body then do return lint errors`() {
        val code =
            """
            class C0{}
            class C1 {}
            class C1 { }
            data class DC0(val v: Any){}
            data class DC1(val v: Any) {}
            data class DC2(val v: Any) { }
            interface I0{}
            interface I1 {}
            interface I2 { }
            object O0{}
            object O1 {}
            object O2 { }
            """.trimIndent()
        val formattedCode =
            """
            class C0
            class C1
            class C1
            data class DC0(val v: Any)
            data class DC1(val v: Any)
            data class DC2(val v: Any)
            interface I0
            interface I1
            interface I2
            object O0
            object O1
            object O2
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 9, "Unnecessary block (\"{}\")"),
                LintViolation(2, 10, "Unnecessary block (\"{}\")"),
                LintViolation(3, 10, "Unnecessary block (\"{}\")"),
                LintViolation(4, 27, "Unnecessary block (\"{}\")"),
                LintViolation(5, 28, "Unnecessary block (\"{}\")"),
                LintViolation(6, 28, "Unnecessary block (\"{}\")"),
                LintViolation(7, 13, "Unnecessary block (\"{}\")"),
                LintViolation(8, 14, "Unnecessary block (\"{}\")"),
                LintViolation(9, 14, "Unnecessary block (\"{}\")"),
                LintViolation(10, 10, "Unnecessary block (\"{}\")"),
                LintViolation(11, 11, "Unnecessary block (\"{}\")"),
                LintViolation(12, 11, "Unnecessary block (\"{}\")")
            ).isFormattedAs(formattedCode)
    }

    @Test
    fun `Given a class, interface, or object without body then do not return lint errors`() {
        val code =
            """
            class C1
            data class DC1(val v: Any)
            interface I1
            object O1
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class, interface, or object with a non empty body then do not return lint errors`() {
        val code =
            """
            class C2 { /**/ }
            data class DC2(val v: Any) { /**/ }
            interface I2 { /**/ }
            object O2 { /**/ }
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given an object declaration with empty body of an abstract class then do not return lint errors`() {
        val code =
            """
            abstract class TypeReference<T>
            val o = object : TypeReference<HashMap<String, String>>() {}
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a function declaration with empty body then do not return lint errors`() {
        val code =
            """
            fun main() {}
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun `Given a class declaration with a companion object with empty body then do not return lint errors`() {
        val code =
            """
            class Foo {
                companion object {}
            }
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintViolations()
    }
}
