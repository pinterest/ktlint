package com.github.shyiko.ktlint.test

import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

val debugAST = {
    (System.getProperty("ktlintDebug") ?: System.getenv("KTLINT_DEBUG") ?: "")
        .toLowerCase().split(",").contains("ast")
}

class DumpAST : Rule("dump") {

    override fun visit(node: ASTNode, autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, corrected: Boolean) -> Unit) {
        var level = -1
        var parent: ASTNode? = node
        do {
            level++
            parent = parent?.treeParent
        } while (parent != null)
        System.err.println("  ".repeat(level) + node.psi.javaClass.name + " (${node.elementType})" +
            (if (node.getChildren(null).isEmpty()) " | \"" + node.text.escape() + "\"" else ""))
    }

    private fun String.escape() =
        this.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")
}
