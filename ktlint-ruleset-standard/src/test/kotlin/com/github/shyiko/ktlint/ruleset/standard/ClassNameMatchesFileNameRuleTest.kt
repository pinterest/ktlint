package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ClassNameMatchesFileNameRuleTest {

    @Test
    fun testMatchingSingleClassName() {
        assertThat(ClassNameMatchesFileNameRule().lint(
            """
                class A
            """.trimIndent(),
            fileName("/some/path/A.kt")
        )).isEmpty()
    }

    @Test
    fun testNonMatchingSingleClassName() {
        assertThat(ClassNameMatchesFileNameRule().lint(
            """
                class B
            """.trimIndent(),
            fileName("A.kt")
        )).isEqualTo(listOf(
            LintError(1, 1, "class-name-matches-file-name", "Class B should be declared in a file named B.kt")
        ))
    }

    @Test
    fun testMultipleTopLevelClasses() {
        assertThat(ClassNameMatchesFileNameRule().lint(
            """
                class B
                class C
            """.trimIndent(),
            fileName("A.kt")
        )).isEmpty()
    }

    @Test
    fun testMultipleNonTopLevelClasses() {
        assertThat(ClassNameMatchesFileNameRule().lint(
            """
                class B {
                    class C
                    class D
                }
            """.trimIndent(),
            fileName("A.kt")
        )).isEqualTo(listOf(
            LintError(1, 1, "class-name-matches-file-name", "Class B should be declared in a file named B.kt")
        ))
    }

    @Test
    fun testCaseSensitiveMatching() {
        assertThat(ClassNameMatchesFileNameRule().lint(
            """
                interface Woohoo
            """.trimIndent(),
            fileName("woohoo.kt")
        )).isEqualTo(listOf(
            LintError(1, 1, "class-name-matches-file-name", "Class Woohoo should be declared in a file named Woohoo.kt")
        ))
    }

    @Test
    fun testIgnoreKotlinScriptFiles() {
        assertThat(ClassNameMatchesFileNameRule().lint(
            """
                class B
            """.trimIndent(),
            fileName("A.kts")
        )).isEmpty()
    }

    private fun fileName(fileName: String) = mapOf("file_path" to fileName)
}
