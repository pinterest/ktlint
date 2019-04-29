package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.ast.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.testng.annotations.Test

class KtLintTest {

    @Test
    fun testRuleExecutionOrder() {
        open class R(private val bus: MutableList<String>, id: String) : Rule(id) {
            private var done = false
            override fun visit(
                node: ASTNode,
                autoCorrect: Boolean,
                emit: (issue: Issue) -> Unit
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
            "fun main() {}",
            listOf(
                RuleSet(
                    "standard",
                    object : R(bus, "d"), Rule.Modifier.RestrictToRootLast {},
                    R(bus, "b"),
                    object : R(bus, "a"), Rule.Modifier.RestrictToRoot {},
                    R(bus, "c")
                )
            )
        ) {}
        assertThat(bus).isEqualTo(listOf("file:a", "file:b", "file:c", "b", "c", "file:d"))
    }
}
