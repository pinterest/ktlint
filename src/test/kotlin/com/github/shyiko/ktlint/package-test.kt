package com.github.shyiko.ktlint

import com.github.shyiko.ktlint.rule.Rule
import com.github.shyiko.ktlint.rule.RuleViolation
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import java.util.ArrayList

val debugMode = false

fun Rule.lint(text: String): Collection<LintError> {
    val res = ArrayList<LintError>()
    KtLint.lint(text, {
        if (debugMode) {
            println("^^ lint error")
        }
        res.add(it)
    }, (if (debugMode) arrayOf<Pair<String, Rule>>("dump" to DumpRule()) else emptyArray<Pair<String, Rule>>()) +
        arrayOf("rule-id" to this))
    return res
}

fun Rule.format(text: String): String = KtLint.format(text,
    (if (debugMode) arrayOf<Pair<String, Rule>>("dump" to DumpRule()) else emptyArray<Pair<String, Rule>>()) +
        arrayOf("rule-id" to this))

class DumpRule : Rule {

    override fun visit(node: ASTNode, correct: Boolean, emit: (e: RuleViolation) -> Unit) {
        var level = -1
        var parent: ASTNode? = node
        do {
            level++
            parent = parent?.treeParent
        } while (parent != null)
        println("  ".repeat(level) + node.psi.javaClass.name +
            (if (node.getChildren(null).isEmpty()) " | \"" + node.text.escape() + "\"" else ""))
    }

    private fun String.escape() =
        this.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")

}
