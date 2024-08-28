package com.pinterest.ktlint.rule.engine.internal

import com.pinterest.ktlint.logger.api.initKtLintKLogger
import com.pinterest.ktlint.rule.engine.api.Code
import com.pinterest.ktlint.rule.engine.api.KtLintParseException
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine
import com.pinterest.ktlint.rule.engine.api.KtLintRuleEngine.Companion.UTF8_BOM
import com.pinterest.ktlint.rule.engine.api.KtLintRuleException
import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleAutocorrectApproveHandler
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.RuleExecution
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.createRuleExecutionEditorConfigProperty
import com.pinterest.ktlint.rule.engine.internal.rulefilter.InternalRuleProvidersFilter
import com.pinterest.ktlint.rule.engine.internal.rulefilter.RuleExecutionRuleFilter
import com.pinterest.ktlint.rule.engine.internal.rulefilter.RunAfterRuleFilter
import com.pinterest.ktlint.rule.engine.internal.rulefilter.applyRuleFilters
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import kotlin.io.path.pathString

private val KOTLIN_PSI_FILE_FACTORY_PROVIDER = KotlinPsiFileFactoryProvider()

private val LOGGER = KotlinLogging.logger {}.initKtLintKLogger()

internal class RuleExecutionContext private constructor(
    val code: Code,
    val rootNode: FileASTNode,
    val ruleProviders: Set<RuleProvider>,
    val editorConfig: EditorConfig,
    val positionInTextLocator: (offset: Int) -> LineAndColumn,
) {
    private var suppressionLocator = SuppressionLocator(editorConfig)

    fun executeRule(
        rule: Rule,
        autocorrectHandler: AutocorrectHandler,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        try {
            rule.startTraversalOfAST()
            rule.beforeFirstNode(
                // The rule gets access to an EditConfig which is filtered by the properties which are actually registered as being used by
                // the rule. In this way it can be forced that the rule actually registers the properties that it uses and the field becomes
                // reliable to be used by for example the ".editorconfig" file generator.
                editorConfig.filterBy(
                    rule.usesEditorConfigProperties
                        // Provide the CODE_STYLE_PROPERTY as this property is needed to determine the default value of an
                        // EditorConfigProperty that is not explicitly defined.
                        .plus(CODE_STYLE_PROPERTY)
                        // Provide the rule execution property for the "standard:max-line-length" property based on whether a rule provider
                        // for this rule exists. This property is required to determine whether the property `max_line_length` needs to be
                        // taken into account.
                        .plus(
                            RuleId("standard:max-line-length")
                                .createRuleExecutionEditorConfigProperty(
                                    if (ruleProviders.any { it.ruleId.value == "standard:max-line-length" }) {
                                        RuleExecution.enabled
                                    } else {
                                        RuleExecution.disabled
                                    },
                                ),
                        ),
                ),
            )
            this.executeRuleOnNodeRecursively(rootNode, rule, autocorrectHandler, emitAndApprove)
            rule.afterLastNode()
        } catch (e: RuleExecutionException) {
            throw KtLintRuleException(
                e.line,
                e.col,
                e.rule.ruleId.value,
                """
                Rule '${e.rule.ruleId.value}' throws exception in file '${code.fileNameOrStdin()}' at position (${e.line}:${e.col})
                   Rule maintainer: ${e.rule.about.maintainer}
                   Issue tracker  : ${e.rule.about.issueTrackerUrl}
                   Repository     : ${e.rule.about.repositoryUrl}
                """.trimIndent(),
                e.cause,
            )
        }
    }

    private fun executeRuleOnNodeRecursively(
        node: ASTNode,
        rule: Rule,
        autocorrectHandler: AutocorrectHandler,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (rule.shouldContinueTraversalOfAST()) {
            try {
                if (rule is RuleAutocorrectApproveHandler) {
                    executeRuleWithAutocorrectApproveHandlerOnNodeRecursively(node, rule, autocorrectHandler, emitAndApprove)
                } else {
                    executeRuleWithoutAutocorrectApproveHandlerOnNodeRecursively(node, rule, autocorrectHandler, emitAndApprove)
                }
            } catch (e: Exception) {
                if (autocorrectHandler is NoneAutocorrectHandler) {
                    val (line, col) = positionInTextLocator(node.startOffset)
                    throw RuleExecutionException(
                        rule,
                        line,
                        col,
                        // Prevent extreme long stack trace caused by recursive call and only pass root cause
                        e.cause ?: e,
                    )
                } else {
                    // line/col cannot be reliably mapped as exception might originate from a node not present in the
                    // original AST
                    throw RuleExecutionException(
                        rule,
                        0,
                        0,
                        // Prevent extreme long stack trace caused by recursive call and only pass root cause
                        e.cause ?: e,
                    )
                }
            }
        }
    }

    @Deprecated(message = "Remove in Ktlint 2.0")
    private fun executeRuleWithoutAutocorrectApproveHandlerOnNodeRecursively(
        node: ASTNode,
        rule: Rule,
        autocorrectHandler: AutocorrectHandler,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(rule !is RuleAutocorrectApproveHandler)
        val autoCorrect =
            autocorrectHandler is AllAutocorrectHandler ||
                (
                    autocorrectHandler is LintErrorAutocorrectHandler &&
                        autocorrectHandler.autocorrectRuleWithoutAutocorrectApproveHandler
                )
        val emitOnly = emitAndApprove.onlyEmit()
        val suppress = suppressionLocator.suppress(rootNode, node.startOffset, rule)
        if (!suppress) {
            rule.beforeVisitChildNodes(node, autoCorrect, emitOnly)
        }
        if (rule.shouldContinueTraversalOfAST()) {
            node
                .getChildren(null)
                .forEach { childNode ->
                    this.executeRuleOnNodeRecursively(
                        childNode,
                        rule,
                        autocorrectHandler,
                        emitAndApprove,
                    )
                }
        }
        if (!suppress) {
            rule.afterVisitChildNodes(node, autoCorrect, emitOnly)
        }
    }

    private fun executeRuleWithAutocorrectApproveHandlerOnNodeRecursively(
        node: ASTNode,
        rule: Rule,
        autocorrectHandler: AutocorrectHandler,
        emitAndApprove: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        require(rule is RuleAutocorrectApproveHandler)
        val suppress = suppressionLocator.suppress(rootNode, node.startOffset, rule)
        if (!suppress) {
            rule.beforeVisitChildNodes(node, emitAndApprove)
        }
        if (rule.shouldContinueTraversalOfAST()) {
            node
                .getChildren(null)
                .forEach { childNode ->
                    this.executeRuleOnNodeRecursively(
                        childNode,
                        rule,
                        autocorrectHandler,
                        emitAndApprove,
                    )
                }
        }
        if (!suppress) {
            rule.afterVisitChildNodes(node, emitAndApprove)
        }
    }

    // Simplify the emitAndApprove to an emit only lambda which can be used in the legacy (deprecated) functions
    @Deprecated(message = "Remove in Ktlint 2.0")
    private fun ((offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision).onlyEmit() =
        {
                offset: Int,
                errorMessage: String,
                canBeAutoCorrected: Boolean,
            ->
            this(offset, errorMessage, canBeAutoCorrected)
            Unit
        }

    companion object {
        internal fun createRuleExecutionContext(
            ktLintRuleEngine: KtLintRuleEngine,
            code: Code,
        ): RuleExecutionContext {
            val psiFileFactory = KOTLIN_PSI_FILE_FACTORY_PROVIDER.getKotlinPsiFileFactory(ktLintRuleEngine)
            val normalizedText = normalizeText(code.content)
            val positionInTextLocator = buildPositionInTextLocator(normalizedText)

            val psiFileName =
                code
                    .filePath
                    ?.pathString
                    ?: if (code.script) {
                        "File.kts"
                    } else {
                        "File.kt"
                    }
            val psiFile =
                psiFileFactory.createFileFromText(
                    psiFileName,
                    KotlinLanguage.INSTANCE,
                    normalizedText,
                ) as KtFile
            psiFile
                .findErrorElement()
                ?.let { errorElement ->
                    val (line, col) = positionInTextLocator(errorElement.textOffset)
                    throw KtLintParseException(line, col, errorElement.errorDescription)
                }

            val rootNode = psiFile.node

            val editorConfig =
                ktLintRuleEngine
                    .editorConfigLoader
                    .load(code.filePath)
                    .also {
                        // TODO: Remove warning below in KtLint 0.52 or later as some users skips multiple versions
                        it.warnIfPropertyIsObsolete("disabled_rules", "0.49")
                        // TODO: Remove warning below in KtLint 0.52 or later as some users skips multiple versions
                        it.warnIfPropertyIsObsolete("ktlint_disabled_rules", "0.49")
                    }

            val ruleProviders =
                ktLintRuleEngine
                    .applyRuleFilters(
                        InternalRuleProvidersFilter(ktLintRuleEngine),
                        RuleExecutionRuleFilter(editorConfig),
                        RunAfterRuleFilter(),
                    )

            return RuleExecutionContext(
                code,
                rootNode,
                ruleProviders,
                editorConfig,
                positionInTextLocator,
            )
        }

        private fun normalizeText(text: String): String =
            text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceFirst(UTF8_BOM, "")

        private fun PsiElement.findErrorElement(): PsiErrorElement? {
            if (this is PsiErrorElement) {
                return this
            }
            this.children.forEach { child ->
                val errorElement = child.findErrorElement()
                if (errorElement != null) {
                    return errorElement
                }
            }
            return null
        }
    }
}

private fun EditorConfig.warnIfPropertyIsObsolete(
    propertyName: String,
    ktlintVersion: String,
) {
    if (this.contains(propertyName)) {
        LOGGER.warn {
            "Editorconfig property '$propertyName' is obsolete and is not used by KtLint starting from version $ktlintVersion. Remove " +
                "the property from all '.editorconfig' files."
        }
    }
}

private class RuleExecutionException(
    val rule: Rule,
    val line: Int,
    val col: Int,
    override val cause: Throwable,
) : Throwable(cause)
