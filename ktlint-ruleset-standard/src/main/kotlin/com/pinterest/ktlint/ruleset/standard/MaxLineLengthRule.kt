package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeSibling
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective

class MaxLineLengthRule : Rule("max-line-length"), Rule.Modifier.Last {

    private var maxLineLength: Int = -1
    private var rangeTree = RangeTree()

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
            maxLineLength = editorConfig.maxLineLength
            if (maxLineLength <= 0) {
                return
            }
            val errorOffset = arrayListOf<Int>()
            val text = node.text
            val lines = text.split("\n")
            var offset = 0
            for (line in lines) {
                if (line.length > maxLineLength) {
                    val el = node.psi.findElementAt(offset + line.length - 1)!!.node
                    if (!el.isPartOf(KDoc::class) && !el.isPartOfRawMultiLineString()) {
                        if (!el.isPartOf(PsiComment::class)) {
                            if (!el.isPartOf(KtPackageDirective::class) && !el.isPartOf(KtImportDirective::class)) {
                                // fixme:
                                // normally we would emit here but due to API limitations we need to hold off until
                                // node spanning the same offset is 'visit'ed
                                // (for ktlint-disable directive to have effect (when applied))
                                // this will be rectified in the upcoming release(s)
                                errorOffset.add(offset)
                            }
                        } else {
                            // Allow ktlint-disable comments to exceed max line length
                            if (!el.text.startsWith("// ktlint-disable")) {
                                // if comment is the only thing on the line - fine, otherwise emit an error
                                val prevLeaf = el.prevCodeSibling()
                                if (prevLeaf != null && prevLeaf.startOffset >= offset) {
                                    // fixme:
                                    // normally we would emit here but due to API limitations we need to hold off until
                                    // node spanning the same offset is 'visit'ed
                                    // (for ktlint-disable directive to have effect (when applied))
                                    // this will be rectified in the upcoming release(s)
                                    errorOffset.add(offset)
                                }
                            }
                        }
                    }
                }
                offset += line.length + 1
            }
            rangeTree = RangeTree(errorOffset)
        } else if (!rangeTree.isEmpty() && node.psi is LeafPsiElement) {
            rangeTree
                .query(node.startOffset, node.startOffset + node.textLength)
                .forEach { offset ->
                    emit(offset, "Exceeded max line length ($maxLineLength)", false)
                }
        }
    }

    private fun ASTNode.isPartOfRawMultiLineString() =
        parent(ElementType.STRING_TEMPLATE, strict = false)
            ?.let { it.firstChildNode.text == "\"\"\"" && it.textContains('\n') } == true
}

class RangeTree(seq: List<Int> = emptyList()) {

    private var emptyArrayView = ArrayView(0, 0)
    private var arr: IntArray = seq.toIntArray()

    init {
        if (arr.isNotEmpty()) {
            arr.reduce { p, n -> require(p <= n) { "Input must be sorted" }; n }
        }
    }

    // runtime: O(log(n)+k), where k is number of matching points
    // space: O(1)
    fun query(vmin: Int, vmax: Int): RangeTree.ArrayView {
        var r = arr.size - 1
        if (r == -1 || vmax < arr[0] || arr[r] < vmin) {
            return emptyArrayView
        }
        // binary search for min(arr[l] >= vmin)
        var l = 0
        while (l < r) {
            val m = (r + l) / 2
            if (vmax < arr[m]) {
                r = m - 1
            } else if (arr[m] < vmin) {
                l = m + 1
            } else {
                // arr[l] ?<=? vmin <= arr[m] <= vmax ?<=? arr[r]
                if (vmin <= arr[l]) break else l++ // optimization
                r = m
            }
        }
        if (l > r || arr[l] < vmin) {
            return emptyArrayView
        }
        // find max(k) such as arr[k] < vmax
        var k = l
        while (k < arr.size) {
            if (arr[k] >= vmax) {
                break
            }
            k++
        }
        return ArrayView(l, k)
    }

    fun isEmpty() = arr.isEmpty()

    inner class ArrayView(private var l: Int, private val r: Int) {

        val size: Int = r - l

        fun get(i: Int): Int {
            if (i < 0 || i >= size) {
                throw IndexOutOfBoundsException()
            }
            return arr[l + i]
        }

        inline fun forEach(cb: (v: Int) -> Unit) {
            var i = 0
            while (i < size) {
                cb(get(i++))
            }
        }

        override fun toString(): String {
            if (l == r) {
                return "[]"
            }
            val sb = StringBuilder("[")
            var i = l
            while (i < r) {
                sb.append(arr[i]).append(", ")
                i++
            }
            sb.replace(sb.length - 2, sb.length, "")
            sb.append("]")
            return sb.toString()
        }
    }
}
