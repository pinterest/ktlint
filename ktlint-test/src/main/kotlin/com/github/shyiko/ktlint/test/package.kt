package com.github.shyiko.ktlint.test

import com.andreapivetta.kolor.Color
import com.andreapivetta.kolor.Kolor
import com.github.shyiko.ktlint.core.Rule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.diagnostics.DiagnosticUtils
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import java.io.PrintStream

val debugAST = {
    (System.getProperty("ktlintDebug") ?: System.getenv("KTLINT_DEBUG") ?: "")
        .toLowerCase().split(",").contains("ast")
}

class DumpAST @JvmOverloads constructor(
    private val out: PrintStream = System.err,
    private val color: Boolean = false
) : Rule("dump") {

    private var lineNumberColumnLength: Int = 0
    private var lastNode: ASTNode? = null

    override fun visit(node: ASTNode, autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, corrected: Boolean) -> Unit) {
        if (node.elementType == KtStubElementTypes.FILE) {
            lineNumberColumnLength = (location(PsiTreeUtil.getDeepestLast(node.psi).node)?.line ?: 0)
                .let { var v = it; var c = 0; while (v > 0) { c++; v /= 10 }; c }
            lastNode = lastChildNodeOf(node)
        }
        var level = -1
        var parent: ASTNode? = node
        do {
            level++
            parent = parent?.treeParent
        } while (parent != null)
        out.println((
            location(node)
                ?.let { String.format("%${lineNumberColumnLength}s: ", it.line).gray() }
                // should only happen when autoCorrect=true and other rules mutate AST in a way that changes text length
                ?: String.format("%${lineNumberColumnLength}s: ", "?").gray()
            ) +
            "  ".repeat(level).gray() +
            colorClassName(node.psi.className) +
            " (".gray() + colorClassName(node.elementType.className) + "." + node.elementType + ")".gray() +
            if (node.getChildren(null).isEmpty()) " \"" + node.text.escape().yellow() + "\"" else "")
        if (lastNode == node) {
            out.println()
            out.println(" ".repeat(lineNumberColumnLength) +
                "  format: <line_number:> <node.psi::class> (<node.elementType>) \"<node.text>\"".gray())
            out.println(" ".repeat(lineNumberColumnLength) +
                "  legend: ~ = org.jetbrains.kotlin, c.i.p = com.intellij.psi".gray())
            out.println()
        }
    }

    private tailrec fun lastChildNodeOf(node: ASTNode): ASTNode? =
        if (node.lastChildNode == null) node else lastChildNodeOf(node.lastChildNode)

    private fun location(node: ASTNode) =
        DiagnosticUtils.getClosestPsiElement(node)
            ?.takeIf { it.isValid && it.containingFile != null }
            ?.let {
                try {
                    DiagnosticUtils.offsetToLineAndColumn(it.containingFile.viewProvider.document,
                        it.textRange.startOffset)
                } catch (e: Exception) {
                    null // DiagnosticUtils.offsetToLineAndColumn has knowledge of mutated AST
                }
            }

    private fun colorClassName(className: String): String {
        val name = className.substringAfterLast(".")
        return className.substring(0, className.length - name.length).gray() + name
    }

    private fun String.yellow() =
        if (color) Kolor.foreground(this, Color.YELLOW) else this
    private fun String.gray() =
        if (color) Kolor.foreground(this, Color.DARK_GRAY) else this

    private val Any.className
        get() = this.javaClass.name
            .replace("org.jetbrains.kotlin.", "~.")
            .replace("com.intellij.psi.", "c.i.p.")

    private fun String.escape() =
        this.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")
}
