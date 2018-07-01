package com.github.shyiko.ktlint.ruleset.standard

import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.test.lint
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.net.URI
import java.nio.file.Paths

class PackageNameRuleTest {

    @Test
    fun testPackageName_Success() {
        lintSuccess("package hoge.fuga", "/hoge/fuga/A.kt")
        lintSuccess("package hoge.fuga\nclass B{}", "/hoge/fuga/B.kt")
        lintSuccess("package hoge.fuga\nclass C{}", "/var/tmp/hoge/fuga/C.kt")
        lintSuccess("package hoge.fuga\nclass B{}\nclass C{}", "/var/tmp/hoge/fuga/Mix.kt")
        lintSuccess("package hoge.fuga\nfun main() {}", "/var/tmp/hoge/fuga/main.kt")
        lintSuccess("class A{}", "/var/tmp/hoge/fuga/A.kt")
    }

    @Test
    fun testPackageName_Failed() {
        lintFailed(
            "package hoge.fuga",
            "/hoge/moge/A.kt",
            listOf(LintError(1, 1, "package-name-rule", "package name should match directory name.")
        ))
    }

    @Test
    fun testPackageName_UnderScoreContains() {
        lintFailed(
            "package hoge.moge.hoge_moge",
            "/hoge/moge/hoge_moge/A.kt",
            listOf(LintError(1, 1, "package-name-rule", "package names should be not contain underscore.")
            ))
    }

    @Test
    fun testPackageName_UpperCaseContains() {
        lintFailed(
            "package hoge.moge.hogeMoge",
            "/hoge/moge/hogeMoge/A.kt",
            listOf(LintError(1, 1, "package-name-rule", "package names should be all lowercase.")
            ))
    }

    private fun fileName(fileName: String) = mapOf("file_path" to Paths.get(URI.create("file:///$fileName")).toString())

    private fun lintSuccess(ktScript: String, fileName: String) {
        assertThat(PackageNameRule().lint(ktScript, fileName(fileName))).isEmpty()
    }

    private fun lintFailed(ktScript: String, fileName: String, lintErrors: List<LintError>) {
        assertThat(PackageNameRule().lint(ktScript, fileName(fileName))).isEqualTo(lintErrors)
    }
}
