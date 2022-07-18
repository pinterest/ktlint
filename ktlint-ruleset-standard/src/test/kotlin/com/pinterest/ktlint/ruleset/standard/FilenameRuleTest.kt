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
    fun testFileWithoutTopLevelDeclarations() {
        val code =
            """
            /*
             * copyright
             */
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/A.kt")
            .hasNoLintViolations()
    }

    @Test
    fun `Given a file without a top level class declaration then the filename should conform to PascalCase`() {
        val code =
            """
            /*
             * copyright
             */
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/$NON_PASCAL_CASE_NAME")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File name '$NON_PASCAL_CASE_NAME' should conform PascalCase")
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
            .asFileWithPath("/some/path/$UNEXPECTED_FILE_NAME")
            .hasLintViolationWithoutAutoCorrect(1, 1, "File '$UNEXPECTED_FILE_NAME' contains a single class and possibly also extension functions for that class and should be named same after that class 'Foo.kt'")
    }

    @ParameterizedTest(name = "Top level declaration: {0}")
    @ValueSource(
        strings = [
            "object Foo",
            "typealias Foo = String"
        ]
    )
    fun `Given a file containing one top level declaration then the file should be named after the identifier`(
        code: String
    ) {
        fileNameRuleAssertThat(code)
            .asFileWithPath(UNEXPECTED_FILE_NAME)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File '$UNEXPECTED_FILE_NAME' contains a single top level declaration and should be named 'Foo.kt'")
    }

    @ParameterizedTest(name = "Top level declaration: {0}")
    @ValueSource(
        strings = [
            "val foo",
            "const val FOO",
            "fun String.foo() = {}",
            "fun foo() = {}",
            "operator fun Foo.plus(other: Foo): Foo { /* ... */ }"
        ]
    )
    fun `Given a file containing one top level then the file should conform to PascalCase`(
        code: String
    ) {
        fileNameRuleAssertThat(code)
            .asFileWithPath(NON_PASCAL_CASE_NAME)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File name '$NON_PASCAL_CASE_NAME' should conform PascalCase")
    }

    @ParameterizedTest(name = "Top level declaration: {0}")
    @ValueSource(
        strings = [
            "val foo",
            "const val FOO"
        ]
    )
    fun `Given a file containing a single top level property declaration (non-private) and one private top level class declaration then the file should conform to PascalCase`(
        topLevelDeclaration: String
    ) {
        val code =
            """
            $topLevelDeclaration

            private class Bar
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath(NON_PASCAL_CASE_NAME)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File name '$NON_PASCAL_CASE_NAME' should conform PascalCase")
    }

    @ParameterizedTest(name = "Other top level declaration: {0}")
    @ValueSource(
        strings = [
            "fun String.bar() = {}",
            "fun bar() = {}",
            "object Bar",
            "typealias Bar = String",
            "val bar"
        ]
    )
    fun `Given a file containing a single top level class (non-private) and another top level declaration (non-private, not extending that class) then the file should conform to PascalCase notation but does not need to be named after that class`(
        otherTopLevelDeclaration: String
    ) {
        val code =
            """
            class Foo
            $otherTopLevelDeclaration
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath(NON_PASCAL_CASE_NAME)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File name '$NON_PASCAL_CASE_NAME' should conform PascalCase")
    }

    @ParameterizedTest(name = "Other top level declaration: {0}")
    @ValueSource(
        strings = [
            "fun Foo.foo() = {}",
            "private object Bar"
        ]
    )
    fun `Given a file containing a single top level class (non-private) and another top level declaration (private and,x xor extending that class) then the file should be named after that class`(
        otherTopLevelDeclaration: String
    ) {
        val code =
            """
            class Foo
            $otherTopLevelDeclaration
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath(UNEXPECTED_FILE_NAME)
            .hasLintViolationWithoutAutoCorrect(1, 1, "File '$UNEXPECTED_FILE_NAME' contains a single class and possibly also extension functions for that class and should be named same after that class 'Foo.kt'")
    }

    @Test
    fun `Issue 1530 - Given a file which name should match PascalCase then this name may also contain letters with diacritics`() {
        val code = "// some code"
        fileNameRuleAssertThat(code)
            .asFileWithPath("ŸëšThïsĮsÂllòwed123.kt")
            .hasNoLintViolations()
    }

    private companion object {
        const val NON_PASCAL_CASE_NAME = "nonPascalCaseName.kt"
        const val UNEXPECTED_FILE_NAME = "UnexpectedFileName.kt"
    }
}
