package com.github.shyiko.ktlint.internal.path

import org.testng.Assert.assertTrue
import org.testng.Assert.assertFalse
import org.testng.annotations.Test
import java.io.File

class GlobFileFilterTest {

    private val dir = File("/tmp").canonicalPath

    @Test
    fun testNegation() {
        val filter = GlobFileFilter("$dir", "$dir/**/*.kt", "!$dir/**/*test*/**/*.kt", "!$dir/**/prefix*/**/*.kt",
            "!$dir/**/*suffix/**/*.kt")
        assertTrue(filter.accept(File("$dir/a.kt")))
        assertFalse(filter.accept(File("$dir/a/test_/a.kt")))
        assertFalse(filter.accept(File("$dir/a/_test_/a.kt")))
        assertFalse(filter.accept(File("$dir/a/_test/a.kt")))
        assertFalse(filter.accept(File("$dir/a/prefix_/a.kt")))
        assertFalse(filter.accept(File("$dir/a/prefix/a.kt")))
        assertTrue(filter.accept(File("$dir/a/_prefix/a.kt")))
        assertFalse(filter.accept(File("$dir/a/_suffix/a.kt")))
        assertFalse(filter.accept(File("$dir/a/suffix/a.kt")))
        assertTrue(filter.accept(File("$dir/a/suffix_/a.kt")))
        assertTrue(GlobFileFilter("/C:/ktlint", "/C:/ktlint/src/**/*.kt")
            .accept(File("/C:/ktlint/src/test/kotlin/com/github/shyiko/ktlint/LinterTest.kt")))
    }

}
