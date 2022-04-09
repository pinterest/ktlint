package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import com.pinterest.ktlint.test.diffFileFormat
import com.pinterest.ktlint.test.format
import org.assertj.core.api.Assertions.assertThat
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
            .hasLintErrors(
                LintError(1, 9, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(2, 10, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(3, 10, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(4, 27, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(5, 28, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(6, 28, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(7, 13, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(8, 14, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(9, 14, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(10, 10, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(11, 11, "no-empty-class-body", "Unnecessary block (\"{}\")"),
                LintError(12, 11, "no-empty-class-body", "Unnecessary block (\"{}\")")
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
        noEmptyClassBodyRuleAssertThat(code).hasNoLintErrors()
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
        noEmptyClassBodyRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given an object declaration with empty body of an abstract class then do not return lint errors`() {
        val code =
            """
            abstract class TypeReference<T>
            val o = object : TypeReference<HashMap<String, String>>() {}
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a function declaration with empty body then do not return lint errors`() {
        val code =
            """
            fun main() {}
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun `Given a class declaration with a companion object with empty body then do not return lint errors`() {
        val code =
            """
            class Foo {
                companion object {}
            }
            """.trimIndent()
        noEmptyClassBodyRuleAssertThat(code).hasNoLintErrors()
    }

    @Test
    fun testFormat() {
        assertThat(
            NoEmptyClassBodyRule().diffFileFormat(
                "spec/no-empty-class-body/format.kt.spec",
                "spec/no-empty-class-body/format-expected.kt.spec"
            )
        ).isEmpty()
    }

    @Test
    fun testFormatEmptyClassBodyAtTheEndOfFile() {
        assertThat(NoEmptyClassBodyRule().format("class A {}\n")).isEqualTo("class A\n")
        assertThat(NoEmptyClassBodyRule().format("class A {}")).isEqualTo("class A")
    }
}
