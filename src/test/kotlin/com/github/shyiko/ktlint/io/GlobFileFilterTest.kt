package com.github.shyiko.ktlint.io

import org.testng.Assert.assertTrue
import org.testng.Assert.assertFalse
import org.testng.annotations.Test
import java.io.File

class GlobFileFilterTest {

    @Test
    fun testNegation() {
        val filter = GlobFileFilter("/tmp", "/tmp/**/*.kt", "!/tmp/**/*test*/**/*.kt", "!/tmp/**/prefix*/**/*.kt",
            "!/tmp/**/*suffix/**/*.kt")
        assertTrue(filter.accept(File("/tmp/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/test_/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/_test_/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/_test/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/prefix_/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/prefix/a.kt")))
        assertTrue(filter.accept(File("/tmp/a/_prefix/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/_suffix/a.kt")))
        assertFalse(filter.accept(File("/tmp/a/suffix/a.kt")))
        assertTrue(filter.accept(File("/tmp/a/suffix_/a.kt")))
        assertTrue(GlobFileFilter("/C:/ktlint", "/C:/ktlint/src/**/*.kt")
            .accept(File("/C:/ktlint/src/test/kotlin/com/github/shyiko/ktlint/LinterTest.kt")))
    }

}
