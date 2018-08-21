package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.net.URI
import java.nio.file.Paths

/**
 * @author yokotaso <yokotaso.t@gmail.com>
 */
class DirectoryStructureRuleTest {

    @Test
    fun testOK() {
        assertOK("package hoge.fuga", "/hoge/fuga/A.kt")
        assertOK("package hoge.fuga\nclass B{}", "/hoge/fuga/B.kt")
        assertOK("package hoge.fuga\nclass C{}", "/var/tmp/hoge/fuga/C.kt")
        assertOK("package hoge.fuga\nclass B{}\nclass C{}", "/var/tmp/hoge/fuga/Mix.kt")
        assertOK("package hoge.fuga\nfun main() {}", "/var/tmp/hoge/fuga/main.kt")
        assertOK("class A{}", "/var/tmp/hoge/fuga/A.kt")
    }

    @Test
    fun testNOK() {
        assertNOK(
            "package hoge.fuga",
            "/hoge/moge/A.kt",
            listOf(LintError(1, 1, "directory-structure", "Package directive doesn't match file location"))
        )
    }

    @Test
    fun testOKUpperCase() {
        assertOK(
            "package hoge.moge.hogeMoge",
            "/hoge/moge/hogeMoge/A.kt"
        )
    }

    private fun fileName(fileName: String) =
        mapOf("file_path" to Paths.get(URI.create("file:///$fileName")).toString())

    private fun assertOK(ktScript: String, fileName: String) {
        assertThat(DirectoryStructureRule().lint(ktScript, fileName(fileName))).isEmpty()
    }

    private fun assertNOK(ktScript: String, fileName: String, lintErrors: List<LintError>) {
        assertThat(DirectoryStructureRule().lint(ktScript, fileName(fileName))).isEqualTo(lintErrors)
    }
}
