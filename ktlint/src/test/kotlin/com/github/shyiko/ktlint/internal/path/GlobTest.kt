package com.github.shyiko.ktlint.internal.path

import com.github.shyiko.ktlint.internal.path.Glob
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test

class GlobTest {

    @Test(timeOut = 3000)
    fun testCatastrophicBacktracking() {
        assertMatch(Glob("/tmp", "**/a/**/a/**/a/**/a/**/a/**/*.jsx"),
            "/tmp/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/f.jsx")
        assertMatch(Glob("/tmp", "**/a/**/a/**/a/**/a/**/a/**/*.jsx"),
            "!/tmp/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/a/f.kt")
    }

    @Test
    fun testGitIgnoreCompatibility() {
        fun gitIgnoreGlob(baseDir: String, pattern: String) = Glob(baseDir, pattern,
            restrictToBaseDir = true, includeChildren = true)
        assertMatch(gitIgnoreGlob("/tmp", "/*.js"), "/tmp/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "/*.js"), "!/tmp/d/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "/a/*.js"), "/tmp/a/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "/a/*.js"), "!/tmp/a/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**"), "/tmp/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**/"), "/tmp/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**/"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "a/**"), "/tmp/a/b")
        assertMatch(gitIgnoreGlob("/tmp", "a/**/"), "/tmp/a/b")
        assertMatch(gitIgnoreGlob("/tmp", "a/**"), "/tmp/a/b/c")
        assertMatch(gitIgnoreGlob("/tmp", "a/**/"), "/tmp/a/b/c")
        assertMatch(gitIgnoreGlob("/tmp", "**/b"), "/tmp/b")
        assertMatch(gitIgnoreGlob("/tmp", "/**/b"), "/tmp/b")
        assertMatch(gitIgnoreGlob("/tmp", "**/b"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "/**/b"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**/b"), "/tmp/a/b")
        assertMatch(gitIgnoreGlob("/tmp", "/**/b"), "/tmp/a/b")
        assertMatch(gitIgnoreGlob("/tmp", "**/b"), "/tmp/a/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "/**/b"), "/tmp/a/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/**"), "/tmp/b")
        assertMatch(gitIgnoreGlob("/tmp", "b/**"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "*.js"), "/tmp/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "*.js"), "/tmp/d/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**/*.js"), "/tmp/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "**/*.js"), "/tmp/d/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/*.js"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/*.js"), "!/tmp/b/d/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/**/*.js"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/**/*.js"), "/tmp/b/d/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b"), "/tmp/b")
        assertMatch(gitIgnoreGlob("/tmp", "b"), "/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b"), "/tmp/b/d")
        assertMatch(gitIgnoreGlob("/tmp", "b"), "/tmp/b/d/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/d"), "!/tmp/b/f.js")
        assertMatch(gitIgnoreGlob("/tmp", "b/d"), "/tmp/b/d/f.js")
    }

    @Test
    fun testMatching() {
        assertMatch(Glob("/tmp", "**"), "/tmp/f.js")
        assertMatch(Glob("/tmp", "**/"), "/tmp/f.js")
        assertMatch(Glob("/tmp", "**"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp", "**/"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp", "a/**"), "/tmp/a/b")
        assertMatch(Glob("/tmp", "a/**/"), "/tmp/a/b")
        assertMatch(Glob("/tmp", "a/**"), "/tmp/a/b/c")
        assertMatch(Glob("/tmp", "a/**/"), "/tmp/a/b/c")
        assertMatch(Glob("/tmp", "**/b"), "/tmp/b")
        assertMatch(Glob("/tmp", "**/b"), "!/tmp/b/f.js")
        assertMatch(Glob("/tmp", "**/b"), "/tmp/a/b")
        assertMatch(Glob("/tmp", "**/b"), "!/tmp/a/b/f.js")
        assertMatch(Glob("/tmp", "b/**"), "/tmp/b")
        assertMatch(Glob("/tmp", "b/**"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp", "*.js"), "/tmp/f.js")
        assertMatch(Glob("/tmp", "*.js"), "/tmp/d/f.js")
        assertMatch(Glob("/tmp", "**/*.js"), "/tmp/f.js")
        assertMatch(Glob("/tmp", "**/*.js"), "/tmp/d/f.js")
        assertMatch(Glob("/tmp", "b/*.js"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp", "b/*.js"), "!/tmp/b/d/f.js")
        assertMatch(Glob("/tmp", "b/**/*.js"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp", "b/**/*.js"), "/tmp/b/d/f.js")
        assertMatch(Glob("/tmp", "b"), "/tmp/b")
        assertMatch(Glob("/tmp", "b"), "!/tmp/b/f.js")
        assertMatch(Glob("/tmp", "b"), "!/tmp/b/d")
        assertMatch(Glob("/tmp", "b"), "!/tmp/b/d/f.js")
        assertMatch(Glob("/tmp", "b/d"), "!/tmp/b/f.js")
        assertMatch(Glob("/tmp", "b/d"), "!/tmp/b/d/f.js")
        // ...and now outside of cwd
        assertMatch(Glob("/tmp", "/*.js"), "!/tmp/f.js")
        assertMatch(Glob("/tmp", "/*.js"), "!/tmp/d/f.js")
        assertMatch(Glob("/tmp", "/a/*.js"), "!/tmp/a.js")
        assertMatch(Glob("/tmp", "/a/*.js"), "/a/f.js")
        assertMatch(Glob("/tmp", "/a/*.js"), "!/a/b/f.js")
        assertMatch(Glob("/tmp/a", "../b/*.js"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp/a", "../b/*.js"), "!/tmp/b/d/f.js")
        assertMatch(Glob("/tmp/a", "../b/**/*.js"), "/tmp/b/f.js")
        assertMatch(Glob("/tmp/a", "../b/**/*.js"), "/tmp/b/d/f.js")
        assertMatch(Glob("/tmp/a", "../b"), "!/tmp/b/f.js")
        assertMatch(Glob("/tmp/a", "../b", includeChildren = true), "/tmp/b/f.js")
        assertMatch(Glob("/tmp/a", "../b"), "!/tmp/b/d/f.js")
        assertMatch(Glob("/tmp/a", "../b", includeChildren = true), "/tmp/b/d/f.js")
        assertMatch(Glob("/tmp/a", "../b/d"), "!/tmp/b/f.js")
        assertMatch(Glob("/tmp/a", "../b/d", includeChildren = true), "!/tmp/b/f.js")
        assertMatch(Glob("/tmp/a", "../b/d"), "!/tmp/b/d/f.js")
        assertMatch(Glob("/tmp/a", "../b/d", includeChildren = true), "/tmp/b/d/f.js")
    }

    fun assertMatch(glob: Glob, target: String) {
        assertThat(glob.matches(target.removePrefix("!")))
            .describedAs("%s -> %s", glob, target).isEqualTo(!target.startsWith("!"))
    }

}
