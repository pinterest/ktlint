package com.pinterest.ktlint.core

import com.pinterest.ktlint.core.DummyRuleWithCustomEditorConfigProperty.Companion.SOME_CUSTOM_RULE_PROPERTY
import com.pinterest.ktlint.core.KtLint.EDITOR_CONFIG_USER_DATA_KEY
import com.pinterest.ktlint.core.api.FeatureInAlphaState
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.isRoot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(FeatureInAlphaState::class)
class KtLintTest {
    /**
     * API Consumers directly use the ktlint-core module. Tests in this module should guarantee that the API is kept
     * stable.
     */
    @Nested
    inner class ApiConsumer {
        @Nested
        inner class LintViaDeprecatedParams {
            @Test
            fun `Given that an empty ruleSet is provided than throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("No runnable rules found. Please ensure that at least one is enabled.")
            }

            @Test
            fun `Given a non empty ruleset and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLint.lint(
                    KtLint.Params(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            )
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a non empty ruleset and userData that contains one default editor config property then throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that contains multiple default editor config properties then throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4"
                            ),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that refers to a custom Rule property then do throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRuleWithCustomEditorConfigProperty())
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData not referring any custom or default editor config property then this userData is received in the node`() {
                var actual: String? = null
                KtLint.lint(
                    KtLint.Params(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        actual = node.getUserData(EDITOR_CONFIG_USER_DATA_KEY)?.get(SOME_PROPERTY)
                                    }
                                }
                            )
                        ),
                        userData = mapOf(SOME_PROPERTY to SOME_PROPERTY_VALUE),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE)
            }
        }

        @Nested
        inner class LintViaExperimentalParams {
            @Test
            fun `Given that an empty ruleSet is provided than throw an error`() {
                assertThatThrownBy {
                    KtLint.lint(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("No runnable rules found. Please ensure that at least one is enabled.")
            }

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
                                }
                            )
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
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
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file."
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
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4"
                            ),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
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
                                RuleSet("standard", DummyRuleWithCustomEditorConfigProperty())
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData not referring any custom or default editor config property then this userData is received in the node`() {
                var actual: String? = null
                KtLint.lint(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        actual = node.getUserData(EDITOR_CONFIG_USER_DATA_KEY)?.get(SOME_PROPERTY)
                                    }
                                }
                            )
                        ),
                        userData = mapOf(SOME_PROPERTY to SOME_PROPERTY_VALUE),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE)
            }
        }

        @Nested
        inner class FormatViaDeprecatedParams {
            @Test
            fun `Given that an empty ruleSet is provided than throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("No runnable rules found. Please ensure that at least one is enabled.")
            }

            @Test
            fun `Given a non empty ruleset and empty userData then do not throw an error`() {
                var numberOfRootNodesVisited = 0
                KtLint.format(
                    KtLint.Params(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        numberOfRootNodesVisited++
                                    }
                                }
                            )
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(numberOfRootNodesVisited).isEqualTo(1)
            }

            @Test
            fun `Given a non empty ruleset and userData that contains one default editor config property then throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that contains multiple default editor config properties then throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4"
                            ),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData that refers to a custom Rule property then do throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.Params(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = listOf(
                                RuleSet("standard", DummyRuleWithCustomEditorConfigProperty())
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData not referring any custom or default editor config property then this userData is received in the node`() {
                var actual: String? = null
                KtLint.format(
                    KtLint.Params(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        actual = node.getUserData(EDITOR_CONFIG_USER_DATA_KEY)?.get(SOME_PROPERTY)
                                    }
                                }
                            )
                        ),
                        userData = mapOf(SOME_PROPERTY to SOME_PROPERTY_VALUE),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE)
            }
        }

        @Nested
        inner class FormatViaExperimentalParams {
            @Test
            fun `Given that an empty ruleSet is provided than throw an error`() {
                assertThatThrownBy {
                    KtLint.format(
                        KtLint.ExperimentalParams(
                            fileName = "some-filename",
                            text = "fun main() {}",
                            ruleSets = emptyList(),
                            userData = emptyMap(),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage("No runnable rules found. Please ensure that at least one is enabled.")
            }

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
                                }
                            )
                        ),
                        userData = emptyMap(),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
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
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf("max_line_length" to "80"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [max_line_length]. Such properties " +
                            "should be passed via the 'ExperimentalParams.editorConfigOverride' field. Note that " +
                            "this is only required for properties that (potentially) contain a value that differs " +
                            "from the actual value in the '.editorconfig' file."
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
                                RuleSet("standard", DummyRule())
                            ),
                            userData = mapOf(
                                "indent_style" to "space",
                                "indent_size" to "4"
                            ),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [indent_size, indent_style]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
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
                                RuleSet("standard", DummyRuleWithCustomEditorConfigProperty())
                            ),
                            userData = mapOf(SOME_CUSTOM_RULE_PROPERTY to "false"),
                            cb = { _, _ -> },
                            script = false,
                            editorConfigPath = null,
                            debug = false
                        )
                    )
                }.isInstanceOf(IllegalStateException::class.java)
                    .hasMessage(
                        "UserData should not contain '.editorconfig' properties [$SOME_CUSTOM_RULE_PROPERTY]. Such" +
                            " properties should be passed via the 'ExperimentalParams.editorConfigOverride' field. " +
                            "Note that this is only required for properties that (potentially) contain a value that " +
                            "differs from the actual value in the '.editorconfig' file."
                    )
            }

            @Test
            fun `Given a non empty ruleset and userData not referring any custom or default editor config property then this userData is received in the node`() {
                var actual: String? = null
                KtLint.format(
                    KtLint.ExperimentalParams(
                        fileName = "some-filename",
                        text = "fun main() {}",
                        ruleSets = listOf(
                            RuleSet(
                                "standard",
                                DummyRule { node ->
                                    if (node.isRoot()) {
                                        actual = node.getUserData(EDITOR_CONFIG_USER_DATA_KEY)?.get(SOME_PROPERTY)
                                    }
                                }
                            )
                        ),
                        userData = mapOf(SOME_PROPERTY to SOME_PROPERTY_VALUE),
                        cb = { _, _ -> },
                        script = false,
                        editorConfigPath = null,
                        debug = false
                    )
                )
                assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE)
            }
        }
    }

    @Test
    fun testRuleExecutionOrder() {
        open class R(
            private val bus: MutableList<String>,
            id: String,
            visitorModifiers: Set<VisitorModifier> = emptySet()
        ) : Rule(id, visitorModifiers) {
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
                        object : R(
                            bus = bus,
                            id = "e",
                            visitorModifiers = setOf(VisitorModifier.RunAsLateAsPossible)
                        ) {},
                        object : R(
                            bus = bus,
                            id = "d",
                            visitorModifiers = setOf(
                                VisitorModifier.RunOnRootNodeOnly,
                                VisitorModifier.RunAsLateAsPossible
                            )
                        ) {},
                        R(
                            bus = bus,
                            id = "b"
                        ),
                        object : R(
                            bus = bus,
                            id = "a",
                            visitorModifiers = setOf(
                                VisitorModifier.RunOnRootNodeOnly
                            )
                        ) {},
                        R(
                            bus = bus,
                            id = "c"
                        )
                    )
                ),
                cb = { _, _ -> }
            )
        )
        assertThat(bus).isEqualTo(listOf("file:a", "file:b", "file:c", "file:d", "file:e", "b", "c", "e"))
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

    private companion object {
        const val SOME_PROPERTY = "some-property"
        const val SOME_PROPERTY_VALUE = "some-property-value"
    }
}

@OptIn(FeatureInAlphaState::class)
private class DummyRuleWithCustomEditorConfigProperty :
    Rule("dummy-rule-with-custom-editor-config-property"),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(someCustomRuleProperty)

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
    }

    companion object {
        const val SOME_CUSTOM_RULE_PROPERTY = "some-custom-rule-property"

        val someCustomRuleProperty: UsesEditorConfigProperties.EditorConfigProperty<Boolean> =
            UsesEditorConfigProperties.EditorConfigProperty(
                type = PropertyType.LowerCasingPropertyType(
                    SOME_CUSTOM_RULE_PROPERTY,
                    "some-custom-rule-property-description",
                    PropertyType.PropertyValueParser.BOOLEAN_VALUE_PARSER,
                    setOf(true.toString(), false.toString())
                ),
                defaultValue = false
            )
    }
}

/**
 * A dummy rule for testing. Optionally the rule can be created with a lambda to be executed for each node visited.
 */
private open class DummyRule(
    val block: (node: ASTNode) -> Unit = {}
) : Rule("dummy-rule") {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        block(node)
    }
}

private fun getResourceAsText(path: String) =
    (ClassLoader.getSystemClassLoader().getResourceAsStream(path) ?: throw RuntimeException("$path not found"))
        .bufferedReader()
        .readText()
