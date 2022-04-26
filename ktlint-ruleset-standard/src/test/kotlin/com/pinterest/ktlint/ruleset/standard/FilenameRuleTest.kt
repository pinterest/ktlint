package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThat
import org.junit.jupiter.api.Test

class FilenameRuleTest {
    private val fileNameRuleAssertThat = FilenameRule().assertThat()

    @Test
    fun testMatchingSingleClassName() {
        for (
            src in listOf(
                "class A",
                "class `A`",
                "data class A(val v: Int)",
                "sealed class A",
                "interface A",
                "object A",
                "enum class A {A}",
                "typealias A = Set<Network.Node>",
                // >1 declaration case
                "class B\nfun A.f() {}"
            )
        ) {
            val code =
                """
                /*
                 * license
                 */
                @file:JvmName("Foo")
                package x
                import y.Z
                $src
                //
                """.trimIndent()
            fileNameRuleAssertThat(code)
                .asFileWithPath("/some/path/A.kt")
                .hasNoLintViolations()
        }
    }

    @Test
    fun testNonMatchingSingleClassName() {
        for (
            src in mapOf(
                "class A" to "class",
                "data class A(val v: Int)" to "class",
                "sealed class A" to "class",
                "interface A" to "interface",
                "object A" to "object",
                "enum class A {A}" to "class",
                "typealias A = Set<Network.Node>" to "typealias"
            )
        ) {
            val code =
                """
                /*
                 * license
                 */
                @file:JvmName("Foo")
                package x
                import y.Z
                ${src.key}
                //
                """.trimIndent()
            fileNameRuleAssertThat(code)
                .asFileWithPath("/some/path/UnexpectedFilename.kt")
                .hasLintViolationWithoutAutoCorrect(1, 1, "${src.value} A should be declared in a file named A.kt")
        }
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
    fun testMultipleTopLevelClasses() {
        val code =
            """
            class B
            class C
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/A.kt")
            .hasNoLintViolations()
    }

    @Test
    fun testMultipleNonTopLevelClasses() {
        val code =
            """
            class B {
                class C
                class D
            }
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("/some/path/A.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "class B should be declared in a file named B.kt")
    }

    @Test
    fun testCaseSensitiveMatching() {
        val code =
            """
            interface Woohoo
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("woohoo.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "interface Woohoo should be declared in a file named Woohoo.kt")
    }

    @Test
    fun testCaseEscapedClassNames() {
        val code =
            """
            class `A`
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("B.kt")
            .hasLintViolationWithoutAutoCorrect(1, 1, "class `A` should be declared in a file named A.kt")
    }

    @Test
    fun testIgnoreKotlinScriptFiles() {
        val code =
            """
            class B
            """.trimIndent()
        fileNameRuleAssertThat(code)
            .asFileWithPath("A.kts")
            .hasNoLintViolations()
    }
}
