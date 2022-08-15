package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.AutoCorrectErrorRule.Companion.STRING_VALUE_AFTER_AUTOCORRECT
import com.pinterest.ktlint.core.DummyRuleWithCustomEditorConfigProperty.Companion.SOME_CUSTOM_RULE_PROPERTY
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.core.Rule.VisitorModifier.RunOnRootNodeOnly
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.AFTER_CHILDREN
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.AFTER_LAST
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.BEFORE_CHILDREN
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.BEFORE_FIRST
import com.pinterest.ktlint.core.RuleExecutionCall.RuleMethod.VISIT
import com.pinterest.ktlint.core.RuleExecutionCall.VisitNodeType.CHILD
import com.pinterest.ktlint.core.RuleExecutionCall.VisitNodeType.ROOT
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.FILE
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.IMPORT_LIST
import com.pinterest.ktlint.core.ast.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class KtLintTest {
    /**
     * API Consumers directly use the ktlint-core module. Tests in this module should guarantee that the API is kept
     * stable.
     */
    @Nested
    inner class ApiConsumer {
        @Nested
        inner class LintViaExperimentalParams {
            @Test
            fun `Given that both ruleSets and ruleProviders are empty than throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            ruleProviders = emptySet(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("Provide exactly one of parameters 'ruleSets' or 'ruleProviders'")
            }

            @Test
            fun `Given that both ruleSets and ruleProviders are not empty than throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule()),
                            ),
                            ruleProviders = setOf(
                                RuleProvider { DummyRule() },
                            ),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("Provide exactly one of parameters 'ruleSets' or 'ruleProviders'")
            }

            @DisplayName("Lint called with deprecated rule sets parameter instead of rule providers")
            @Nested
            @Deprecated("Marked for removal in ktlint 0.48")
            inner class LintWithRuleSets {
                @Test
                fun `Given a non empty ruleset and empty userData then do not throw an error`() {
                    var numberOfRootNodesVisited = 0
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet(
                                    "standard",
                                    DummyRule { node ->
                                        if (node.isRoot()) {
                                            numberOfRootNodesVisited++
                                        }
                                    },
                                ),
                            ),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                    assertThat(numberOfRootNodesVisited).isEqualTo(1)
                }

                @Test
                fun `Given a non empty ruleset and userData that contains one default editor config property then throw an error`() {
                    assertThatThrownBy {
                        KtLint.lint(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleSets = listOf(
                                    RuleSet("standard", DummyRule()),
                                ),
                                userData = mapOf("max_line_length" to "80"),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                                "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                                "this is only required for properties that (potentially) contain a value that differs " +
                                "from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a non empty ruleset and userData that contains multiple default editor config properties then throw an error`() {
                    assertThatThrownBy {
                        KtLint.lint(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleSets = listOf(
                                    RuleSet("standard", DummyRule()),
                                ),
                                userData = mapOf(
                                    "indent_style" to "space",
                                    "indent_size" to "4",
                                ),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                                " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                                "Note that this is only required for properties that (potentially) contain a value that " +
                                "differs from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a non empty ruleset and userData that refers to a custom Rule property then do throw an error`() {
                    assertThatThrownBy {
                        KtLint.lint(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleSets = listOf(
                                    RuleSet("standard", DummyRuleWithCustomEditorConfigProperty()),
                                ),
                                userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                                " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                                "Note that this is only required for properties that (potentially) contain a value that " +
                                "differs from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                    val code =
                        """
                        val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                        val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                        """.trimIndent()
                    val callbacks = mutableListOf<CallbackResult>()
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            text = code,
                            ruleSets = listOf(
                                RuleSet("standard", AutoCorrectErrorRule()),
                            ),
                            userData = emptyMap(),
                            cb = { e, corrected ->
                                callbacks.add(
                                    CallbackResult(
                                        line = e.line,
                                        col = e.col,
                                        ruleId = e.ruleId,
                                        detail = e.detail,
                                        canBeAutoCorrected = e.canBeAutoCorrected,
                                        corrected = corrected,
                                    ),
                                )
                            },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                    assertThat(callbacks).containsExactly(
                        CallbackResult(
                            line = 1,
                            col = 12,
                            ruleId = "auto-correct",
                            detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                            canBeAutoCorrected = false,
                            corrected = false,
                        ),
                        CallbackResult(
                            line = 2,
                            col = 12,
                            ruleId = "auto-correct",
                            detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                            canBeAutoCorrected = true,
                            corrected = false,
                        ),
                    )
                }
            }

            @DisplayName("Lint called with rule providers")
            @Nested
            inner class LintWithRuleProviders {
                @Test
                fun `Given a non empty rule providers and empty userData then do not throw an error`() {
                    var numberOfRootNodesVisited = 0
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleProviders = setOf(
                                RuleProvider {
                                    DummyRule { node ->
                                        if (node.isRoot()) {
                                            numberOfRootNodesVisited++
                                        }
                                    }
                                },
                            ),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                    assertThat(numberOfRootNodesVisited).isEqualTo(1)
                }

                @Test
                fun `Given a non empty rule providers and userData that contains one default editor config property then throw an error`() {
                    assertThatThrownBy {
                        KtLint.lint(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleProviders = setOf(
                                    RuleProvider { DummyRule() },
                                ),
                                userData = mapOf("max_line_length" to "80"),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                                "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                                "this is only required for properties that (potentially) contain a value that differs " +
                                "from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a non empty rule providers and userData that contains multiple default editor config properties then throw an error`() {
                    assertThatThrownBy {
                        KtLint.lint(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleProviders = setOf(
                                    RuleProvider { DummyRule() },
                                ),
                                userData = mapOf(
                                    "indent_style" to "space",
                                    "indent_size" to "4",
                                ),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                                " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                                "Note that this is only required for properties that (potentially) contain a value that " +
                                "differs from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a non empty rule providers and userData that refers to a custom Rule property then do throw an error`() {
                    assertThatThrownBy {
                        KtLint.lint(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleProviders = setOf(
                                    RuleProvider { DummyRuleWithCustomEditorConfigProperty() },
                                ),
                                userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                                " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                                "Note that this is only required for properties that (potentially) contain a value that " +
                                "differs from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                    val code =
                        """
                        val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                        val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                        """.trimIndent()
                    val callbacks = mutableListOf<CallbackResult>()
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            text = code,
                            ruleProviders = setOf(
                                RuleProvider { AutoCorrectErrorRule() },
                            ),
                            userData = emptyMap(),
                            cb = { e, corrected ->
                                callbacks.add(
                                    CallbackResult(
                                        line = e.line,
                                        col = e.col,
                                        ruleId = e.ruleId,
                                        detail = e.detail,
                                        canBeAutoCorrected = e.canBeAutoCorrected,
                                        corrected = corrected,
                                    ),
                                )
                            },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                    assertThat(callbacks).containsExactly(
                        CallbackResult(
                            line = 1,
                            col = 12,
                            ruleId = "auto-correct",
                            detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                            canBeAutoCorrected = false,
                            corrected = false,
                        ),
                        CallbackResult(
                            line = 2,
                            col = 12,
                            ruleId = "auto-correct",
                            detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                            canBeAutoCorrected = true,
                            corrected = false,
                        ),
                    )
                }
            }

            @Test
            fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        text = code,
                        ruleSets = listOf(
                            RuleSet("standard", AutoCorrectErrorRule()),
                        ),
                        userData = emptyMap(),
                        cb = { e, corrected ->
                            callbacks.add(
                                CallbackResult(
                                    line = e.line,
                                    col = e.col,
                                    ruleId = e.ruleId,
                                    detail = e.detail,
                                    canBeAutoCorrected = e.canBeAutoCorrected,
                                    corrected = corrected,
                                ),
                            )
                        },
                        script = false,
                        editorConfigPath = null,
                        debug = false,
                    ),
                )
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = "auto-correct",
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = "auto-correct",
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                        canBeAutoCorrected = true,
                        corrected = false,
                    ),
                )
            }
        }

        @Nested
        inner class FormatViaExperimentalParams {
            @Test
            fun `Given that both ruleSets and ruleProviders are empty than throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            ruleProviders = emptySet(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("Provide exactly one of parameters 'ruleSets' or 'ruleProviders'")
            }

            @Test
            fun `Given that both ruleSets and ruleProviders are not empty than throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule()),
                            ),
                            ruleProviders = setOf(
                                RuleProvider { DummyRule() },
                            ),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                }.isInstanceOf(IllegalArgumentException::class.java)
                    .hasMessage("Provide exactly one of parameters 'ruleSets' or 'ruleProviders'")
            }

            @DisplayName("Format called with deprecated rule sets parameter instead of rule providers")
            @Nested
            @Deprecated("Marked for removal in ktlint 0.48")
            inner class FormatWithRuleSets {
                @Test
                fun `Given a non empty ruleset and empty userData then do not throw an error`() {
                    var numberOfRootNodesVisited = 0
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet(
                                    "standard",
                                    DummyRule { node ->
                                        if (node.isRoot()) {
                                            numberOfRootNodesVisited++
                                        }
                                    },
                                ),
                            ),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false,
                        ),
                    )
                    assertThat(numberOfRootNodesVisited).isEqualTo(1)
                }

                @Test
                fun `Given a non empty ruleset and userData that contains one default editor config property then throw an error`() {
                    assertThatThrownBy {
                        KtLint.format(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleSets = listOf(
                                    RuleSet("standard", DummyRule()),
                                ),
                                userData = mapOf("max_line_length" to "80"),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                                "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                                "this is only required for properties that (potentially) contain a value that differs " +
                                "from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a non empty ruleset and userData that contains multiple default editor config properties then throw an error`() {
                    assertThatThrownBy {
                        KtLint.format(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleSets = listOf(
                                    RuleSet("standard", DummyRule()),
                                ),
                                userData = mapOf(
                                    "indent_style" to "space",
                                    "indent_size" to "4",
                                ),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                                " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                                "Note that this is only required for properties that (potentially) contain a value that " +
                                "differs from the actual value in the '.editorconfig' file.",
                        )
                }

                @Test
                fun `Given a non empty ruleset and userData that refers to a custom Rule property then do throw an error`() {
                    assertThatThrownBy {
                        KtLint.format(
                            KtLint.ExperimentalParams(
                                fileName = "some-filename",
                                text = "fun main() {}",
                                ruleSets = listOf(
                                    RuleSet("standard", DummyRuleWithCustomEditorConfigProperty()),
                                ),
                                userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                                cb = { _, _ -> },
                                script = false,
                                editorConfigPath = null,
                                debug = false,
                            ),
                        )
                    }.isInstanceOf(IllegalStateException::class.java)
                        .hasMessage(
                            "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                                " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                                "Note that this is only required for properties that (potentially) contain a value that " +
                                "differs from the actual value in the '.editorconfig' file.",
                        )
                }
            }

            @Test
            fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
                val code =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                    """.trimIndent()
                val formattedCode =
                    """
                    val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                    val bar = "$STRING_VALUE_AFTER_AUTOCORRECT"
                    """.trimIndent()
                val callbacks = mutableListOf<CallbackResult>()
                val actualFormattedCode = KtLint.format(
                    KtLint.ExperimentalParams(
                        text = code,
                        ruleSets = listOf(
                            RuleSet("standard", AutoCorrectErrorRule()),
                        ),
                        userData = emptyMap(),
                        cb = { e, corrected ->
                            callbacks.add(
                                CallbackResult(
                                    line = e.line,
                                    col = e.col,
                                    ruleId = e.ruleId,
                                    detail = e.detail,
                                    canBeAutoCorrected = e.canBeAutoCorrected,
                                    corrected = corrected,
                                ),
                            )
                        },
                        script = false,
                        editorConfigPath = null,
                        debug = false,
                    ),
                )
                assertThat(actualFormattedCode).isEqualTo(formattedCode)
                assertThat(callbacks).containsExactly(
                    CallbackResult(
                        line = 1,
                        col = 12,
                        ruleId = "auto-correct",
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                        canBeAutoCorrected = false,
                        corrected = false,
                    ),
                    CallbackResult(
                        line = 2,
                        col = 12,
                        ruleId = "auto-correct",
                        detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                        canBeAutoCorrected = true,
                        corrected = true,
                    ),
                )
            }
        }

        @Test
        fun `Given a rule returning an errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
            val code =
                """
                val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                """.trimIndent()
            val formattedCode =
                """
                val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                val bar = "$STRING_VALUE_AFTER_AUTOCORRECT"
                """.trimIndent()
            val callbacks = mutableListOf<CallbackResult>()
            val actualFormattedCode = KtLint.format(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleSets = listOf(
                        RuleSet("standard", AutoCorrectErrorRule()),
                    ),
                    userData = emptyMap(),
                    cb = { e, corrected ->
                        callbacks.add(
                            CallbackResult(
                                line = e.line,
                                col = e.col,
                                ruleId = e.ruleId,
                                detail = e.detail,
                                canBeAutoCorrected = e.canBeAutoCorrected,
                                corrected = corrected,
                            ),
                        )
                    },
                    script = false,
                    editorConfigPath = null,
                    debug = false,
                ),
            )
            assertThat(actualFormattedCode).isEqualTo(formattedCode)
            assertThat(callbacks).containsExactly(
                CallbackResult(
                    line = 1,
                    col = 12,
                    ruleId = "auto-correct",
                    detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                    canBeAutoCorrected = false,
                    corrected = false,
                ),
                CallbackResult(
                    line = 2,
                    col = 12,
                    ruleId = "auto-correct",
                    detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                    canBeAutoCorrected = true,
                    corrected = true,
                ),
            )
        }
    }

    @DisplayName("Format called with rule providers")
    @Nested
    inner class FormatWithRuleProviders {
        @Test
        fun `Given a non empty rule providers and empty userData then do not throw an error`() {
            var numberOfRootNodesVisited = 0
            KtLint.format(
                KtLint.ExperimentalParams(
                    fileName = "some-filename",
                    text = "fun main() {}",
                    ruleProviders = setOf(
                        RuleProvider {
                            DummyRule { node ->
                                if (node.isRoot()) {
                                    numberOfRootNodesVisited++
                                }
                            }
                        },
                    ),
                    userData = emptyMap(),
                    cb = { _, _ -> },
                    script = false,
                    editorConfigPath = null,
                    debug = false,
                ),
            )
            assertThat(numberOfRootNodesVisited).isEqualTo(1)
        }

        @Test
        fun `Given a non empty rule providers and userData that contains one default editor config property then throw an error`() {
            assertThatThrownBy {
                KtLint.format(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleProviders = setOf(
                            RuleProvider { DummyRule() },
                        ),
                        userData = mapOf("max_line_length" to "80"),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false,
                    ),
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage(
                    "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                        "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                        "this is only required for properties that (potentially) contain a value that differs " +
                        "from the actual value in the '.editorconfig' file.",
                )
        }

        @Test
        fun `Given a non empty rule providers and userData that contains multiple default editor config properties then throw an error`() {
            assertThatThrownBy {
                KtLint.format(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleProviders = setOf(
                            RuleProvider { DummyRule() },
                        ),
                        userData = mapOf(
                            "indent_style" to "space",
                            "indent_size" to "4",
                        ),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false,
                    ),
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage(
                    "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                        " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                        "Note that this is only required for properties that (potentially) contain a value that " +
                        "differs from the actual value in the '.editorconfig' file.",
                )
        }

        @Test
        fun `Given a non empty rule providers and userData that refers to a custom Rule property then do throw an error`() {
            assertThatThrownBy {
                KtLint.format(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleProviders = setOf(
                            RuleProvider { DummyRuleWithCustomEditorConfigProperty() },
                        ),
                        userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false,
                    ),
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage(
                    "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                        " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                        "Note that this is only required for properties that (potentially) contain a value that " +
                        "differs from the actual value in the '.editorconfig' file.",
                )
        }

        @Test
        fun `Given a rule returning errors which can and can not be autocorrected than that state of the error can be retrieved in the callback`() {
            val code =
                """
                val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                val bar = "${AutoCorrectErrorRule.STRING_VALUE_TO_BE_AUTOCORRECTED}"
                """.trimIndent()
            val formattedCode =
                """
                val foo = "${AutoCorrectErrorRule.STRING_VALUE_NOT_TO_BE_CORRECTED}"
                val bar = "$STRING_VALUE_AFTER_AUTOCORRECT"
                """.trimIndent()
            val callbacks = mutableListOf<CallbackResult>()
            val actualFormattedCode = KtLint.format(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleProviders = setOf(
                        RuleProvider { AutoCorrectErrorRule() },
                    ),
                    userData = emptyMap(),
                    cb = { e, corrected ->
                        callbacks.add(
                            CallbackResult(
                                line = e.line,
                                col = e.col,
                                ruleId = e.ruleId,
                                detail = e.detail,
                                canBeAutoCorrected = e.canBeAutoCorrected,
                                corrected = corrected,
                            ),
                        )
                    },
                    script = false,
                    editorConfigPath = null,
                    debug = false,
                ),
            )
            assertThat(actualFormattedCode).isEqualTo(formattedCode)
            assertThat(callbacks).containsExactly(
                CallbackResult(
                    line = 1,
                    col = 12,
                    ruleId = "auto-correct",
                    detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED,
                    canBeAutoCorrected = false,
                    corrected = false,
                ),
                CallbackResult(
                    line = 2,
                    col = 12,
                    ruleId = "auto-correct",
                    detail = AutoCorrectErrorRule.ERROR_MESSAGE_CAN_BE_AUTOCORRECTED,
                    canBeAutoCorrected = true,
                    corrected = true,
                ),
            )
        }
    }

    @DisplayName("Calls to rules using 'visit' life cycle hook")
    @Nested
    @Deprecated("Marked for deleting in KtLint 0.48 after removal of 'visit' life cycle hook")
    inner class RuleExecutionCallsLegacy {
        @Test
        fun `Given a normal rule then execute on root node and child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(),
                            )
                        },
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunAsLateAsPossible),
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )
            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall("a", VISIT, ROOT, FILE),
                RuleExecutionCall("a", VISIT, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("a", VISIT, CHILD, IMPORT_LIST),
                RuleExecutionCall("b", VISIT, ROOT, FILE),
                RuleExecutionCall("b", VISIT, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("b", VISIT, CHILD, IMPORT_LIST),
            )
        }

        @Test
        fun `Given a run-on-root-node-only rule then execute on root node but not on child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    text = "fun main() {}",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(RunOnRootNodeOnly),
                            )
                        },
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                    RunAsLateAsPossible,
                                ),
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )
            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall("a", VISIT, ROOT, FILE),
                RuleExecutionCall("b", VISIT, ROOT, FILE),
            )
        }

        @Test
        fun `Given multiple rules which have to run in a certain order`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "e",
                                visitorModifiers = setOf(RunAsLateAsPossible),
                            )
                        },
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "d",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                    RunAsLateAsPossible,
                                ),
                            )
                        },
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                            )
                        },
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                ),
                            )
                        },
                        RuleProvider {
                            SimpleTestRuleLegacy(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "c",
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )
            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall("a", VISIT, ROOT, FILE),
                RuleExecutionCall("b", VISIT, ROOT, FILE),
                RuleExecutionCall("b", VISIT, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("b", VISIT, CHILD, IMPORT_LIST),
                RuleExecutionCall("c", VISIT, ROOT, FILE),
                RuleExecutionCall("c", VISIT, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("c", VISIT, CHILD, IMPORT_LIST),
                RuleExecutionCall("d", VISIT, ROOT, FILE),
                RuleExecutionCall("e", VISIT, ROOT, FILE),
                RuleExecutionCall("e", VISIT, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("e", VISIT, CHILD, IMPORT_LIST),
            )
        }
    }

    @DisplayName("Calls to rules defined in ktlint 0.47 and after")
    @Nested
    inner class RuleExecutionCalls {
        @Test
        fun `Given a normal rule then execute on root node and child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(),
                            )
                        },
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunAsLateAsPossible),
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )
            assertThat(ruleExecutionCalls).containsExactly(
                // File a
                RuleExecutionCall("a", BEFORE_FIRST),
                RuleExecutionCall("a", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("a", BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("a", AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("a", BEFORE_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("a", AFTER_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("a", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("a", AFTER_LAST),
                // File b
                RuleExecutionCall("b", BEFORE_FIRST),
                RuleExecutionCall("b", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("b", BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("b", AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("b", BEFORE_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("b", AFTER_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("b", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("b", AFTER_LAST),
            )
        }

        @Test
        fun `Given a run-on-root-node-only rule then execute on root node but not on child nodes`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    text = "fun main() {}",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(RunOnRootNodeOnly),
                            )
                        },
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                                visitorModifiers = setOf(RunOnRootNodeOnly, RunAsLateAsPossible),
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )
            assertThat(ruleExecutionCalls).containsExactly(
                // File a
                RuleExecutionCall("a", BEFORE_FIRST),
                RuleExecutionCall("a", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("a", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("a", AFTER_LAST),
                // File b
                RuleExecutionCall("b", BEFORE_FIRST),
                RuleExecutionCall("b", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("b", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("b", AFTER_LAST),
            )
        }

        @Test
        fun `Given multiple rules which have to run in a certain order`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.lint(
                KtLint.ExperimentalParams(
                    // An empty file results in nodes with elementTypes FILE, PACKAGE_DIRECTIVE and IMPORT_LIST respectively
                    text = "",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "e",
                                visitorModifiers = setOf(RunAsLateAsPossible),
                            )
                        },
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "d",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                    RunAsLateAsPossible,
                                ),
                            )
                        },
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "b",
                            )
                        },
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "a",
                                visitorModifiers = setOf(
                                    RunOnRootNodeOnly,
                                ),
                            )
                        },
                        RuleProvider {
                            SimpleTestRule(
                                ruleExecutionCalls = ruleExecutionCalls,
                                id = "c",
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )
            assertThat(ruleExecutionCalls).containsExactly(
                // File a (root only)
                RuleExecutionCall("a", BEFORE_FIRST),
                RuleExecutionCall("a", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("a", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("a", AFTER_LAST),
                // File b
                RuleExecutionCall("b", BEFORE_FIRST),
                RuleExecutionCall("b", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("b", BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("b", AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("b", BEFORE_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("b", AFTER_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("b", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("b", AFTER_LAST),
                // File c
                RuleExecutionCall("c", BEFORE_FIRST),
                RuleExecutionCall("c", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("c", BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("c", AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("c", BEFORE_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("c", AFTER_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("c", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("c", AFTER_LAST),
                // File d (root only)
                RuleExecutionCall("d", BEFORE_FIRST),
                RuleExecutionCall("d", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("d", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("d", AFTER_LAST),
                // File e
                RuleExecutionCall("e", BEFORE_FIRST),
                RuleExecutionCall("e", BEFORE_CHILDREN, ROOT, FILE),
                RuleExecutionCall("e", BEFORE_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("e", AFTER_CHILDREN, CHILD, PACKAGE_DIRECTIVE),
                RuleExecutionCall("e", BEFORE_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("e", AFTER_CHILDREN, CHILD, IMPORT_LIST),
                RuleExecutionCall("e", AFTER_CHILDREN, ROOT, FILE),
                RuleExecutionCall("e", AFTER_LAST),
            )
        }
    }

    @Test
    fun testFormatUnicodeBom() {
        val code = getResourceAsText("spec/format-unicode-bom.kt.spec")

        val actual = KtLint.format(
            KtLint.ExperimentalParams(
                text = code,
                ruleProviders = setOf(
                    RuleProvider { DummyRule() },
                ),
                cb = { _, _ -> },
            ),
        )

        assertThat(actual).isEqualTo(code)
    }

    @Nested
    inner class StopTraversal {
        @Test
        fun `Given that the traversal is stopped in the beforeFirstNode hook then do no traverse the AST but do call the afterLastNode hook`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = "class Foo",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                id = "stop-traversal",
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeFirstNode = true,
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )

            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall("stop-traversal", BEFORE_FIRST),
                RuleExecutionCall("stop-traversal", AFTER_LAST),
            )
        }

        @Test
        fun `Given that the traversal is stopped in the beforeVisitChildNodes when encountering the class with name Foo then classes InsideFoo and AfterFoo are not traversed`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            val code =
                """
                class FooBar {
                    class Foo {
                        class InsideFoo // Won't be visited as traversal is stopped when entering class Foo
                    }

                    class AfterFoo // Won't be visited as traversal is stopped when entering class Foo
                }
                """.trimIndent()
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                id = "stop-traversal",
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeVisitChildNodes = { node ->
                                    // Stop when Class Foo has been entered
                                    node.elementType == CLASS && node.findChildByType(IDENTIFIER)?.text == "Foo"
                                },
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )

            assertThat(ruleExecutionCalls)
                .filteredOn { it.elementType == null || it.classIdentifier != null }
                .containsExactly(
                    RuleExecutionCall("stop-traversal", BEFORE_FIRST),
                    RuleExecutionCall("stop-traversal", BEFORE_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall("stop-traversal", BEFORE_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall("stop-traversal", AFTER_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall("stop-traversal", AFTER_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall("stop-traversal", AFTER_LAST),
                )
        }

        @Test
        fun `Given that the traversal is stopped in the afterVisitChildNodes when encountering the class with name Foo then class AfterFoo is not traversed`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            val code =
                """
                class FooBar {
                    class Foo {
                        class InsideFoo // Is visited as traversal is stopped when after leaving class Foo
                    }

                    class AfterFoo // Won't be visited as traversal is stopped when entering class Foo
                }
                """.trimIndent()
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = code,
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                id = "stop-traversal",
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInAfterVisitChildNodes = { node ->
                                    // Stop when Class Foo has been visited
                                    node.elementType == CLASS && node.findChildByType(IDENTIFIER)?.text == "Foo"
                                },
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )

            assertThat(ruleExecutionCalls)
                .filteredOn { it.elementType == null || it.classIdentifier != null }
                .containsExactly(
                    RuleExecutionCall("stop-traversal", BEFORE_FIRST),
                    RuleExecutionCall("stop-traversal", BEFORE_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall("stop-traversal", BEFORE_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall("stop-traversal", BEFORE_CHILDREN, CHILD, CLASS, "InsideFoo"),
                    RuleExecutionCall("stop-traversal", AFTER_CHILDREN, CHILD, CLASS, "InsideFoo"),
                    RuleExecutionCall("stop-traversal", AFTER_CHILDREN, CHILD, CLASS, "Foo"),
                    RuleExecutionCall("stop-traversal", AFTER_CHILDREN, CHILD, CLASS, "FooBar"),
                    RuleExecutionCall("stop-traversal", AFTER_LAST),
                )
        }

        @Test
        fun `Given that the traversal is stopped in the afterLastNode hook then do nothing special as traversal is already stopped`() {
            val ruleExecutionCalls = mutableListOf<RuleExecutionCall>()
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = "class Foo",
                    ruleProviders = setOf(
                        RuleProvider {
                            SimpleTestRule(
                                id = "stop-traversal",
                                ruleExecutionCalls = ruleExecutionCalls,
                                stopTraversalInBeforeFirstNode = true,
                            )
                        },
                    ),
                    cb = { _, _ -> },
                ),
            )

            assertThat(ruleExecutionCalls).containsExactly(
                RuleExecutionCall("stop-traversal", BEFORE_FIRST),
                RuleExecutionCall("stop-traversal", AFTER_LAST),
            )
        }
    }

    @DisplayName("Given a rule having state which results in an exception when reused")
    @Nested
    inner class RuleWithState {
        @Test
        fun `Given that format is started using the ruleSets parameter then an exception is thrown`() {
            /**
             * Formatting some code with the [WithStateRule] using the [KtLint.ExperimentalParams.ruleSets] parameter
             * results in a [RuleExecutionException] because the same instance of the rule is used twice. [KtLint.format] is
             * not able to create a new instance of the rule as the instance is provided directly in the [RuleSet].
             */
            assertThatThrownBy {
                KtLint.format(
                    KtLint.ExperimentalParams(
                        text = "",
                        ruleSets = listOf(
                            RuleSet(
                                "with-state-and-rule-sets",
                                WithStateRule(),
                            ),
                        ),
                        cb = { _, _ -> },
                    ),
                )
            }.isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Rule has been visited before")
        }

        @Test
        fun `Given that format is started using the ruleProviders parameter then NO exception is thrown`() {
            /**
             * Formatting some code with the [WithStateRule] using the [KtLint.ExperimentalParams.ruleProviders] parameter
             * does not result in a [RuleExecutionException] because [KtLint.format] now is able to request a new instance
             * of the rule whenever the instance has been used before to traverse the AST.
             */
            KtLint.format(
                KtLint.ExperimentalParams(
                    text = "",
                    ruleProviders = setOf(
                        RuleProvider { WithStateRule() },
                    ),
                    cb = { _, _ -> },
                ),
            )
        }
    }
}

private class DummyRuleWithCustomEditorConfigProperty :
    Rule("dummy-rule-with-custom-editor-config-property"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(someCustomRuleProperty)

    companion object {
        const val SOME_CUSTOM_RULE_PROPERTY = "some-custom-rule-property"

        val someCustomRuleProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    SOME_CUSTOM_RULE_PROPERTY,
                    "some-custom-rule-property-description",
                    PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    setOf(true.toString(), false.toString()),
                ),
                defaultValue = false,
            )
    }
}

/**
 * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
 */
private open class DummyRule(
    val block: (node: ASTNode) -> Unit = {},
) : Rule(DUMMY_RULE_ID) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        block(node)
    }

    companion object {
        const val DUMMY_RULE_ID = "dummy-rule"
    }
}

/**
 * A dummy rule for testing
 */
private class AutoCorrectErrorRule : Rule("auto-correct") {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == REGULAR_STRING_PART) {
            when (node.text) {
                STRING_VALUE_TO_BE_AUTOCORRECTED -> {
                    emit(node.startOffset, ERROR_MESSAGE_CAN_BE_AUTOCORRECTED, true)
                    if (autoCorrect) {
                        (node as LeafElement).replaceWithText(STRING_VALUE_AFTER_AUTOCORRECT)
                    }
                }
                STRING_VALUE_NOT_TO_BE_CORRECTED ->
                    emit(node.startOffset, ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED, false)
            }
        }
    }

    companion object {
        const val STRING_VALUE_TO_BE_AUTOCORRECTED = "string-value-to-be-autocorrected"
        const val STRING_VALUE_NOT_TO_BE_CORRECTED = "string-value-not-to-be-corrected"
        const val STRING_VALUE_AFTER_AUTOCORRECT = "string-value-after-autocorrect"
        const val ERROR_MESSAGE_CAN_BE_AUTOCORRECTED = "This string value is not allowed and can be autocorrected"
        const val ERROR_MESSAGE_CAN_NOT_BE_AUTOCORRECTED =
            "This string value is not allowed but can not be autocorrected"
    }
}

/**
 * Rule in style up to ktlint 0.46.x in which a rule only has to override method [Rule.beforeVisitChildNodes]. For each invocation to
 * this method a [RuleExecutionCall] is added to the list of previously calls made.
 */
private class SimpleTestRuleLegacy(
    private val ruleExecutionCalls: MutableList<RuleExecutionCall>,
    id: String,
    visitorModifiers: Set<VisitorModifier> = emptySet(),
) : Rule(id, visitorModifiers) {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        ruleExecutionCalls.add(RuleExecutionCall(id, VISIT, node.visitNodeType, node.elementType))
    }
}

/**
 * Rule in style starting from ktlint 0.47.x in which a rule can can override method [Rule.beforeFirstNode],
 * [Rule.beforeVisitChildNodes], [Rule.afterVisitChildNodes] and [Rule.afterLastNode]. For each invocation to
 * this method a [RuleExecutionCall] is added to the list of previously calls made.
 */
private class SimpleTestRule(
    private val ruleExecutionCalls: MutableList<RuleExecutionCall>,
    id: String,
    visitorModifiers: Set<VisitorModifier> = emptySet(),
    private val stopTraversalInBeforeFirstNode: Boolean = false,
    private val stopTraversalInBeforeVisitChildNodes: (ASTNode) -> Boolean = { false },
    private val stopTraversalInAfterVisitChildNodes: (ASTNode) -> Boolean = { false },
    private val stopTraversalInAfterLastNode: Boolean = false,
) : Rule(id, visitorModifiers) {
    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        ruleExecutionCalls.add(RuleExecutionCall(id, BEFORE_FIRST))
        if (stopTraversalInBeforeFirstNode) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        ruleExecutionCalls.add(node.toRuleExecutionCall(id, BEFORE_CHILDREN))
        if (stopTraversalInBeforeVisitChildNodes(node)) {
            stopTraversalOfAST()
        }
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        ruleExecutionCalls.add(node.toRuleExecutionCall(id, VISIT))
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        ruleExecutionCalls.add(node.toRuleExecutionCall(id, AFTER_CHILDREN))
        if (stopTraversalInAfterVisitChildNodes(node)) {
            stopTraversalOfAST()
        }
    }

    override fun afterLastNode() {
        ruleExecutionCalls.add(RuleExecutionCall(id, AFTER_LAST))
        if (stopTraversalInAfterLastNode) {
            stopTraversalOfAST()
        }
    }

    private fun ASTNode.toRuleExecutionCall(id: String, ruleMethod: RuleExecutionCall.RuleMethod) =
        RuleExecutionCall(
            id,
            ruleMethod,
            visitNodeType,
            elementType,
            if (elementType == CLASS) {
                findChildByType(IDENTIFIER)?.text
            } else {
                null
            },
        )
}

private data class RuleExecutionCall(
    val ruleId: String,
    val ruleMethod: RuleMethod,
    val visitNodeType: VisitNodeType? = null,
    val elementType: IElementType? = null,
    val classIdentifier: String? = null,
) {
    enum class RuleMethod { BEFORE_FIRST, BEFORE_CHILDREN, VISIT, AFTER_CHILDREN, AFTER_LAST }
    enum class VisitNodeType { ROOT, CHILD }
}

private val ASTNode.visitNodeType: RuleExecutionCall.VisitNodeType
    get() =
        if (isRoot()) {
            ROOT
        } else {
            CHILD
        }

private fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()

private data class CallbackResult(
    val line: Int,
    val col: Int,
    val ruleId: String,
    val detail: String,
    val canBeAutoCorrected: Boolean,
    val corrected: Boolean,
)

/**
 * This rule throws an exception when it is visited more than once.
 */
private class WithStateRule : Rule("with-state") {
    private var hasNotBeenVisitedYet = true

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        check(hasNotBeenVisitedYet) {
            "Rule has been visited before"
        }
        hasNotBeenVisitedYet = false
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        emit(node.startOffset, "Fake violation which can be autocorrected", true)
    }
}
