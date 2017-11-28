package com.github.shyiko.ktlint.test

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.RuleSet
import java.util.ArrayList

fun Rule.lint(text: String, userData: Map<String, String> = emptyMap(), script: Boolean = false): List<LintError> {
    val res = ArrayList<LintError>()
    val debug = debugAST()
    val f: L = if (script) KtLint::lintScript else KtLint::lint
    f(text, (if (debug) listOf(RuleSet("debug", DumpAST())) else emptyList()) +
            listOf(RuleSet("standard", this@lint)), userData) { e ->
        if (debug) {
            System.err.println("^^ lint error")
        }
        res.add(e)
    }
    return res
}

private typealias L = (
    text: String,
    ruleSets: Iterable<RuleSet>,
    userData: Map<String, String>,
    cb: (e: LintError) -> Unit
) -> Unit

fun Rule.format(
    text: String,
    userData: Map<String, String> = emptyMap(),
    cb: (e: LintError, corrected: Boolean) -> Unit = { _, _ -> },
    script: Boolean = false
): String {
    val f: F = if (script) KtLint::formatScript else KtLint::format
    return f(text, (if (debugAST()) listOf(RuleSet("debug", DumpAST())) else emptyList()) +
        listOf(RuleSet("standard", this@format)), userData, cb)
}

private typealias F = (
    text: String,
    ruleSets: Iterable<RuleSet>,
    userData: Map<String, String>,
    cb: (e: LintError, corrected: Boolean) -> Unit
) -> String
