package com.github.shyiko.ktlint.test

import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.LintError
import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.RuleSet
import java.util.ArrayList

fun Rule.lint(text: String): List<LintError> {
    val res = ArrayList<LintError>()
    val debug = debugAST()
    KtLint.lint(text, (if (debug) listOf(RuleSet("debug", DumpAST())) else emptyList()) +
        listOf(RuleSet("standard", this@lint)), {
        if (debug) {
            System.err.println("^^ lint error")
        }
        res.add(it)
    })
    return res
}

fun Rule.format(text: String, cb: (e: LintError, corrected: Boolean) -> Unit = { _1, _2 -> }): String =
    KtLint.format(text, (if (debugAST()) listOf(RuleSet("debug", DumpAST())) else emptyList()) +
        listOf(RuleSet("standard", this@format)), cb)
