package com.github.shyiko.ktlint.test

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import java.io.PrintStream

val debugAST = {
    (System.getProperty("ktlintDebug") ?: System.getenv("KTLINT_DEBUG") ?: "")
        .toLowerCase().split(",").contains("ast")
}

class DumpAST @JvmOverloads constructor(private val out: PrintStream = System.err) : Rule("dump") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, corrected: Boolean) -> Unit) {
        var level = -1
        var parent: ASTNode? = node
        do {
            level++
            parent = parent?.treeParent
        } while (parent != null)
        out.println("  ".repeat(level) + node.psi.className + " (${node.elementType.className})" +
            if (node.getChildren(null).isEmpty()) " | \"" + node.text.escape() + "\"" else "")
    }

    private val Any.className
        get() = this.javaClass.name
            .replace("org.jetbrains.kotlin.", "o.j.k.")
            .replace("com.intellij.psi.", "c.i.p.")

    private fun String.escape() =
        this.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")
}
