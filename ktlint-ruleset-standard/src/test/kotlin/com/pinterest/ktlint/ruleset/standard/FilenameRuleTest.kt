package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.LintError
import com.pinterest.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FilenameRuleTest {

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
            assertThat(
                FilenameRule().lint(
                    "/some/path/A.kt",
                    """
                    /*
                     * license
                     */
                    @file:JvmName("Foo")
                    package x
                    import y.Z
                    $src
                    //
                    """.trimIndent(),
                )
            ).isEmpty()
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
            assertThat(
                FilenameRule().lint(
                    "/some/path/B.kt",
                    """
                    /*
                     * license
                     */
                    @file:JvmName("Foo")
                    package x
                    import y.Z
                    ${src.key}
                    //
                    """.trimIndent(),
                )
            ).isEqualTo(
                listOf(
                    LintError(1, 1, "filename", "${src.value} A should be declared in a file named A.kt")
                )
            )
        }
    }

    @Test
    fun testFileWithoutTopLevelDeclarations() {
        assertThat(
            FilenameRule().lint(
                "A.kt",
                """
                /*
                 * copyright
                 */
                """.trimIndent(),
            )
        ).isEmpty()
    }

    @Test
    fun testMultipleTopLevelClasses() {
        assertThat(
            FilenameRule().lint(
                "A.kt",
                """
                class B
                class C
                """.trimIndent(),
            )
        ).isEmpty()
    }

    @Test
    fun testMultipleNonTopLevelClasses() {
        assertThat(
            FilenameRule().lint(
                "A.kt",
                """
                class B {
                    class C
                    class D
                }
                """.trimIndent(),
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "filename", "class B should be declared in a file named B.kt")
            )
        )
    }

    @Test
    fun testCaseSensitiveMatching() {
        assertThat(
            FilenameRule().lint(
                "woohoo.kt",
                """
                interface Woohoo
                """.trimIndent(),
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "filename", "interface Woohoo should be declared in a file named Woohoo.kt")
            )
        )
    }

    @Test
    fun testCaseEscapedClassNames() {
        assertThat(
            FilenameRule().lint(
                "B.kt",
                """
                class `A`
                """.trimIndent(),
            )
        ).isEqualTo(
            listOf(
                LintError(1, 1, "filename", "class `A` should be declared in a file named A.kt")
            )
        )
    }

    @Test
    fun testIgnoreKotlinScriptFiles() {
        assertThat(
            FilenameRule().lint(
                "A.kts",
                """
                class B
                """.trimIndent(),
            )
        ).isEmpty()
    }
}
