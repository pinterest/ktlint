package com.pinterest.ruleset.test

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.lastChildLeafOrSelf
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ruleset.test.internal.Color
import com.pinterest.ruleset.test.internal.color
import java.io.PrintStream
import java.util.Locale
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens

public class DumpASTRule @JvmOverloads constructor(
    private val out: PrintStream = System.err,
    private val color: Boolean = false
) : Rule("dump") {

    private companion object {
        val elementTypeSet = ElementType::class.members.map { it.name }.toSet()
    }

    private var lineNumberColumnLength: Int = 0
    private var lastNode: ASTNode? = null

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, corrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            lineNumberColumnLength = node
                .lastChildLeafOrSelf()
                .lineNumberOrUnknown()
                .length
            lastNode = node.lastChildLeafOrSelf()
        }
        var level = -1
        var parent: ASTNode? = node
        do {
            level++
            parent = parent?.treeParent
        } while (parent != null)

        out.println(
            (
                node
                    .lineNumberOrUnknown()
                    .let { String.format("%${lineNumberColumnLength}s: ", it).dim() }
                ) +
                "  ".repeat(level).dim() +
                colorClassName(node.psi.className) +
                " (".dim() + colorClassName(elementTypeClassName(node.elementType)) + ")".dim() +
                if (node.getChildren(null).isEmpty()) " \"" + node.text.escape().brighten() + "\"" else ""
        )
        if (lastNode == node) {
            out.println()
            out.println(
                " ".repeat(lineNumberColumnLength) +
                    "  format: <line_number:> <node.psi::class> (<node.elementType>) \"<node.text>\"".dim()
            )
            if (node.lineNumberOrUnknown() == "Unknown") {
                out.println(
                    " ".repeat(lineNumberColumnLength) +
                        "          line_number 'Unknown' is caused by mutations in the AST during formatting".dim()
                )
            }
            out.println(
                " ".repeat(lineNumberColumnLength) +
                    "  legend: ~ = org.jetbrains.kotlin, c.i.p = com.intellij.psi".dim()
            )
            out.println()
        }
    }

    private fun ASTNode.lineNumberOrUnknown(): String {
        val lineNumber = try {
            lineNumber().toString()
        } catch (e: IndexOutOfBoundsException) {
            // Due to autocorrect mutations in the AST it can happen that the node's offset becomes invalid. As a result
            // the line number can not be determined.
            null
        }
        return lineNumber ?: "Unknown"
    }

    private fun elementTypeClassName(elementType: IElementType): String {
        var name = elementType.toString().substringAfterLast(".").uppercase(Locale.getDefault())
        if (name == "FLOAT_CONSTANT" && elementType == KtTokens.FLOAT_LITERAL) {
            // resolve KtNodeTypes.FLOAT_CONSTANT vs KtTokens.FLOAT_LITERAL(FLOAT_CONSTANT) conflict
            name = "FLOAT_LITERAL"
        }
        if (KtTokens.KEYWORDS.contains(elementType) || KtTokens.SOFT_KEYWORDS.contains(elementType)) {
            name = "${name}_KEYWORD"
        }
        return if (elementTypeSet.contains(name)) name else elementType.className + "." + elementType
    }

    private fun colorClassName(className: String): String {
        val name = className.substringAfterLast(".")
        return className.substring(0, className.length - name.length).dim() + name
    }

    private fun String.brighten() = optColor(Color.YELLOW)
    private fun String.dim() = optColor(Color.DARK_GRAY)
    private fun String.optColor(foreground: Color) = if (color) this.color(foreground) else this

    private val Any.className
        get() =
            this.javaClass.name
                .replace("org.jetbrains.kotlin.", "~.")
                .replace("com.intellij.psi.", "c.i.p.")

    private fun String.escape() =
        this.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t").replace("\r", "\\r")
}
