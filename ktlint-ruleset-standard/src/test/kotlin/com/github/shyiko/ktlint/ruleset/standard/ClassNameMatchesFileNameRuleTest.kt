package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lintWithFileName
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class ClassNameMatchesFileNameRuleTest {

    @Test
    fun testMatchingSingleClassName() {
        assertThat(ClassNameMatchesFileNameRule().lintWithFileName("/some/path/A.kt",
            """
                class A
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testNonMatchingSingleClassName() {
        assertThat(ClassNameMatchesFileNameRule().lintWithFileName("A.kt",
            """
                class B
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 7, "class-name-matches-file-name", "Single top level class name [B] does not match file name")
        ))
    }

    @Test
    fun testMultipleTopLevelClasses() {
        assertThat(ClassNameMatchesFileNameRule().lintWithFileName("A.kt",
            """
                class B
                class C
            """.trimIndent()
        )).isEmpty()
    }

    @Test
    fun testMultipleNonTopLevelClasses() {
        assertThat(ClassNameMatchesFileNameRule().lintWithFileName("A.kt",
            """
                class B {
                    class C
                    class D
                }
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 7, "class-name-matches-file-name", "Single top level class name [B] does not match file name")
        ))
    }

    @Test
    fun testCaseSensitiveMatching() {
        assertThat(ClassNameMatchesFileNameRule().lintWithFileName("woohoo.kt",
            """
                interface Woohoo
            """.trimIndent()
        )).isEqualTo(listOf(
            LintError(1, 11, "class-name-matches-file-name", "Single top level class name [Woohoo] does not match file name")
        ))
    }
}
