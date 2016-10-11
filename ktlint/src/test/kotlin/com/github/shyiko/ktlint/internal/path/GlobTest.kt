package com.github.shyiko.ktlint.internal.path

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.io.File

class GlobTest {

    private val dir = File("/tmp").canonicalPath

    @Test(timeOut = 3000)
    fun testCatastrophicBacktracking() {
        assertMatch(Glob(dir, "**/a/**/a/**/a/**/a/**/a/**/*.jsx"),
            "$dir/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/f.jsx")
        assertMatch(Glob(dir, "**/a/**/a/**/a/**/a/**/a/**/*.jsx"),
            "!$dir/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/f.kt")
    }

    @Test
    fun testBacktracking() {
        assertMatch(Glob(dir, "**/b/c"), "$dir/b/b/c")
        assertMatch(Glob(dir, "**/b/c"), "$dir/b/c/b/c")
        assertMatch(Glob(dir, "**/b/**/c/d"), "$dir/b/c/b/c/d")
    }

    @Test
    fun testGitIgnoreCompatibility() {
        fun gitIgnoreGlob(baseDir: String, pattern: String) = Glob(baseDir, pattern,
            restrictToBaseDir = true, includeChildren = true)
        assertMatch(gitIgnoreGlob(dir, "/*.js"), "$dir/f.js")
        assertMatch(gitIgnoreGlob(dir, "/*.js"), "!$dir/d/f.js")
        assertMatch(gitIgnoreGlob(dir, "/a/*.js"), "$dir/a/f.js")
        assertMatch(gitIgnoreGlob(dir, "/a/*.js"), "!$dir/a/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "**"), "$dir/f.js")
        assertMatch(gitIgnoreGlob(dir, "**/"), "$dir/f.js")
        assertMatch(gitIgnoreGlob(dir, "**"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "**/"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "a/**"), "$dir/a/b")
        assertMatch(gitIgnoreGlob(dir, "a/**/"), "$dir/a/b")
        assertMatch(gitIgnoreGlob(dir, "a/**"), "$dir/a/b/c")
        assertMatch(gitIgnoreGlob(dir, "a/**/"), "$dir/a/b/c")
        assertMatch(gitIgnoreGlob(dir, "**/b"), "$dir/b")
        assertMatch(gitIgnoreGlob(dir, "/**/b"), "$dir/b")
        assertMatch(gitIgnoreGlob(dir, "**/b"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "/**/b"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "**/b"), "$dir/a/b")
        assertMatch(gitIgnoreGlob(dir, "/**/b"), "$dir/a/b")
        assertMatch(gitIgnoreGlob(dir, "**/b"), "$dir/a/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "/**/b"), "$dir/a/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/**"), "$dir/b")
        assertMatch(gitIgnoreGlob(dir, "b/**"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "*.js"), "$dir/f.js")
        assertMatch(gitIgnoreGlob(dir, "*.js"), "$dir/d/f.js")
        assertMatch(gitIgnoreGlob(dir, "**/*.js"), "$dir/f.js")
        assertMatch(gitIgnoreGlob(dir, "**/*.js"), "$dir/d/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/*.js"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/*.js"), "!$dir/b/d/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/**/*.js"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/**/*.js"), "$dir/b/d/f.js")
        assertMatch(gitIgnoreGlob(dir, "b"), "$dir/b")
        assertMatch(gitIgnoreGlob(dir, "b"), "$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "b"), "$dir/b/d")
        assertMatch(gitIgnoreGlob(dir, "b"), "$dir/b/d/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/d"), "!$dir/b/f.js")
        assertMatch(gitIgnoreGlob(dir, "b/d"), "$dir/b/d/f.js")
    }

    @Test
    fun testMatching() {
        assertMatch(Glob(dir, "**"), "$dir/f.js")
        assertMatch(Glob(dir, "**/"), "$dir/f.js")
        assertMatch(Glob(dir, "**"), "$dir/b/f.js")
        assertMatch(Glob(dir, "**/"), "$dir/b/f.js")
        assertMatch(Glob(dir, "a/**"), "$dir/a/b")
        assertMatch(Glob(dir, "a/**/"), "$dir/a/b")
        assertMatch(Glob(dir, "a/**"), "$dir/a/b/c")
        assertMatch(Glob(dir, "a/**/"), "$dir/a/b/c")
        assertMatch(Glob(dir, "**/b"), "$dir/b")
        assertMatch(Glob(dir, "**/b"), "!$dir/b/f.js")
        assertMatch(Glob(dir, "**/b"), "$dir/a/b")
        assertMatch(Glob(dir, "**/b"), "!$dir/a/b/f.js")
        assertMatch(Glob(dir, "b/**"), "$dir/b")
        assertMatch(Glob(dir, "b/**"), "$dir/b/f.js")
        assertMatch(Glob(dir, "*.js"), "$dir/f.js")
        assertMatch(Glob(dir, "*.js"), "$dir/d/f.js")
        assertMatch(Glob(dir, "**/*.js"), "$dir/f.js")
        assertMatch(Glob(dir, "**/*.js"), "$dir/d/f.js")
        assertMatch(Glob(dir, "b/*.js"), "$dir/b/f.js")
        assertMatch(Glob(dir, "b/*.js"), "!$dir/b/d/f.js")
        assertMatch(Glob(dir, "b/**/*.js"), "$dir/b/f.js")
        assertMatch(Glob(dir, "b/**/*.js"), "$dir/b/d/f.js")
        assertMatch(Glob(dir, "b"), "$dir/b")
        assertMatch(Glob(dir, "b"), "!$dir/b/f.js")
        assertMatch(Glob(dir, "b"), "!$dir/b/d")
        assertMatch(Glob(dir, "b"), "!$dir/b/d/f.js")
        assertMatch(Glob(dir, "b/d"), "!$dir/b/f.js")
        assertMatch(Glob(dir, "b/d"), "!$dir/b/d/f.js")
        assertMatch(Glob(dir, "b/**/b"), "!$dir/c/b/b")
        // ...and now outside of cwd
        assertMatch(Glob(dir, "/*.js"), "!$dir/f.js")
        assertMatch(Glob(dir, "/*.js"), "!$dir/d/f.js")
        assertMatch(Glob(dir, "/a/*.js"), "!$dir/a.js")
        assertMatch(Glob(dir, "/a/*.js"), "/a/f.js")
        assertMatch(Glob(dir, "/a/*.js"), "!/a/b/f.js")
        assertMatch(Glob("$dir/a", "../b/*.js"), "$dir/b/f.js")
        assertMatch(Glob("$dir/a", "../b/*.js"), "!$dir/b/d/f.js")
        assertMatch(Glob("$dir/a", "../b/**/*.js"), "$dir/b/f.js")
        assertMatch(Glob("$dir/a", "../b/**/*.js"), "$dir/b/d/f.js")
        assertMatch(Glob("$dir/a", "../b"), "!$dir/b/f.js")
        assertMatch(Glob("$dir/a", "../b", includeChildren = true), "$dir/b/f.js")
        assertMatch(Glob("$dir/a", "../b"), "!$dir/b/d/f.js")
        assertMatch(Glob("$dir/a", "../b", includeChildren = true), "$dir/b/d/f.js")
        assertMatch(Glob("$dir/a", "../b/d"), "!$dir/b/f.js")
        assertMatch(Glob("$dir/a", "../b/d", includeChildren = true), "!$dir/b/f.js")
        assertMatch(Glob("$dir/a", "../b/d"), "!$dir/b/d/f.js")
        assertMatch(Glob("$dir/a", "../b/d", includeChildren = true), "$dir/b/d/f.js")
    }

    fun assertMatch(glob: Glob, target: String) {
        assertThat(glob.matches(target.removePrefix("!")))
            .describedAs("%s -> %s", glob, target).isEqualTo(!target.startsWith("!"))
    }

}
