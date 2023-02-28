package com.pinterest.ktlint.rule.engine.internal

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Test

class VisitorProviderTest {
    @Test
    fun `When no runnable rules are found for the root node, the visit function on the root node is not executed`() {
        assertThatNoException()
            .isThrownBy {
                VisitorProvider(
                    ruleRunners = emptySet(),
                    recreateRuleSorter = true,
                ).visitor()
                    .invoke { _, _ ->
                        assertThat(false)
                            .withFailMessage("The visitor provider should not have called this lambda in case it has no rule runners")
                            .isTrue
                    }
            }
    }
}
