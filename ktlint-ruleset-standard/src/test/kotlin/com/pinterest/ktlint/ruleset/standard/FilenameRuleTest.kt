package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class FilenameRuleTest {
    private val fileNameRuleAssertThat = FilenameRule().assertThat()

    @Test
    fun `Given a kotlin script file then ignore it`() {
        val code =
            """
            class Foo
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/Bar.kts")
            .hasNoLintViolations()
    }

    @Test
    fun `Given a file with name package-dot-kt then ignore the rule for this file`() {
        val code =
            """
            class Foo
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/package.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `Given a file without any toplevel declaration then the filename should conform to PascalCase`() {
        val code =
            """
            /*
             * copyright
             */
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path//some/path/FooBar.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `Given a file without a toplevel class declaration then the filename should conform to PascalCase`() {
        val code =
            """
            val foo = "foo"
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/not-pascal-case.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File name 'not-pascal-case.kt' should conform PascalCase")
    }

    @ParameterizedTest(name = "Class type: {0}, expected result: {1}")
    @ValueSource(
        strings = [
            "class Foo",
            "class `Foo`",
            "data class Foo",
            "data class `Foo`",
            "enum class Foo",
            "enum class `Foo`",
            "sealed class Foo",
            "sealed class `Foo`",
            "interface Foo",
            "interface `Foo`"
        ]
    )
    fun `Given a file containing a single declaration of a class type then the filename should match the class name`(code: String) {
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/bar.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File 'bar.kt' contains a single class and should be named same after that class 'Foo.kt'")
    }

    @ParameterizedTest(name = "Other toplevel declaration: {0}")
    @ValueSource(
        strings = [
            "class Foo",
            "class `Foo`",
            "data class Foo(val v: Int)",
            "sealed class Foo",
            "interface Foo",
            "object Foo",
            "enum class Foo {A}",
            "typealias Foo = Set<Network.Node>",
            "fun Foo.f() {}"
        ]
    )
    private fun `Given a file containing one toplevel class declaration and another toplevel declaration`(otherTopLevelDeclaration: String) {
        val code =
            """
            class Bar
            $otherTopLevelDeclaration
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/SomeDescriptiveName.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File name 'foo.kt' should conform PascalCase")
    }
}
