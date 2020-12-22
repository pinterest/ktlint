package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.Test

class KtLintTest {

    @Test
    fun testRuleExecutionOrder() {
        open class R(private val bus: MutableList<String>, id: String) : Rule(id) {
            private var done = false
            override fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
            ) {
                if (node.isRoot()) {
                    bus.add("file:$id")
                } else if (!done) {
                    bus.add(id)
                    done = true
                }
            }
        }
        val bus = mutableListOf<String>()
        KtLint.lint(
            KtLint.Params(
                text = "fun main() {}",
                ruleSets = listOf(
                    RuleSet(
                        "standard",
                        object : R(bus, "d"), Rule.Modifier.RestrictToRootLast {},
                        R(bus, "b"),
                        object : R(bus, "a"), Rule.Modifier.RestrictToRoot {},
                        R(bus, "c")
                    )
                ),
                cb = { _, _ -> }
            )
        )
        assertThat(bus).isEqualTo(listOf("file:a", "file:b", "file:c", "b", "c", "file:d"))
    }

    @Test
    fun testFormatUnicodeBom() {
        val code = getResourceAsText("spec/format-unicode-bom.kt.spec")

        val actual = KtLint.format(
            KtLint.Params(
                text = code,
                ruleSets = listOf(
                    RuleSet("standard", DummyRule())
                ),
                cb = { _, _ -> }
            )
        )

        assertThat(actual).isEqualTo(code)
    }
}

class DummyRule : Rule("dummy-rule") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        // The rule does not need to do anything except emit
        emit(node.startOffset, "Dummy rule", true)
    }
}

private fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()
