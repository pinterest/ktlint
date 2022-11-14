package com.pinterest.ktlint.core.internal

import com.pinterest.ktlint.core.Code
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.KtLintRuleEngine.Companion.EDITOR_CONFIG_LOADER
import com.pinterest.ktlint.core.KtLintRuleEngine.Companion.UTF8_BOM
import com.pinterest.ktlint.core.KtLintRuleEngineConfiguration
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.KtLintParseException
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.PsiErrorElement
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile

private val KOTLIN_PSI_FILE_FACTORY_PROVIDER = KotlinPsiFileFactoryProvider()

internal class RuleExecutionContext private constructor(
    val rootNode: FileASTNode,
    val editorConfigProperties: EditorConfigProperties,
    val positionInTextLocator: (offset: Int) -> LineAndColumn,
) {
    lateinit var suppressionLocator: SuppressionLocator
        private set

    init {
        rebuildSuppressionLocator()
    }

    fun rebuildSuppressionLocator() {
        suppressionLocator = SuppressionLocatorBuilder.buildSuppressedRegionsLocator(rootNode)
    }

    fun executeRule(
        rule: Rule,
        fqRuleId: String,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        rule.startTraversalOfAST()
        rule.beforeFirstNode(editorConfigProperties)
        this.executeRuleOnNodeRecursively(rootNode, rule, fqRuleId, autoCorrect, emit)
        rule.afterLastNode()
    }

    private fun executeRuleOnNodeRecursively(
        node: ASTNode,
        rule: Rule,
        fqRuleId: String,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        /**
         * The [suppressionLocator] can be changed during each visit of node when running [KtLint.format]. So a new handler is to be built
         * before visiting the nodes.
         */
        val suppressHandler = SuppressHandler(suppressionLocator, rootNode, autoCorrect, emit)
        if (rule.shouldContinueTraversalOfAST()) {
            try {
                suppressHandler.handle(node, fqRuleId) { autoCorrect, emit ->
                    rule.beforeVisitChildNodes(node, autoCorrect, emit)
                }
                if (rule.shouldContinueTraversalOfAST()) {
                    node
                        .getChildren(null)
                        .forEach { childNode ->
                            suppressHandler.handle(childNode, fqRuleId) { autoCorrect, emit ->
                                this.executeRuleOnNodeRecursively(
                                    childNode,
                                    rule,
                                    fqRuleId,
                                    autoCorrect,
                                    emit,
                                )
                            }
                        }
                }
                suppressHandler.handle(node, fqRuleId) { autoCorrect, emit ->
                    rule.afterVisitChildNodes(node, autoCorrect, emit)
                }
            } catch (e: Exception) {
                if (autoCorrect) {
                    // line/col cannot be reliably mapped as exception might originate from a node not present in the
                    // original AST
                    throw RuleExecutionException(
                        0,
                        0,
                        fqRuleId,
                        // Prevent extreme long stack trace caused by recursive call and only pass root cause
                        e.cause ?: e,
                    )
                } else {
                    val (line, col) = positionInTextLocator(node.startOffset)
                    throw RuleExecutionException(
                        line,
                        col,
                        fqRuleId,
                        // Prevent extreme long stack trace caused by recursive call and only pass root cause
                        e.cause ?: e,
                    )
                }
            }
        }
    }

    companion object {
        internal fun createRuleExecutionContext(
            ktLintRuleEngineConfiguration: KtLintRuleEngineConfiguration,
            code: Code,
        ): RuleExecutionContext {
            val psiFileFactory = KOTLIN_PSI_FILE_FACTORY_PROVIDER.getKotlinPsiFileFactory(ktLintRuleEngineConfiguration.isInvokedFromCli)
            val normalizedText = normalizeText(code.content)
            val positionInTextLocator = buildPositionInTextLocator(normalizedText)

            val psiFileName = when {
                code.fileName != null -> code.fileName
                code.script -> "file.kts"
                else -> "file.kt"
            }
            val psiFile = psiFileFactory.createFileFromText(
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

            val editorConfigProperties = EDITOR_CONFIG_LOADER.load(
                filePath = code.filePath,
                rules = ktLintRuleEngineConfiguration.getRules(),
                editorConfigDefaults = ktLintRuleEngineConfiguration.editorConfigDefaults,
                editorConfigOverride = ktLintRuleEngineConfiguration.editorConfigOverride,
            )

            if (!code.isStdIn) {
                // TODO: Remove in KtLint 0.49
                rootNode.putUserData(KtLint.FILE_PATH_USER_DATA_KEY, code.filePath.toString())
            }

            return RuleExecutionContext(
                rootNode,
                editorConfigProperties,
                positionInTextLocator,
            )
        }

        private fun normalizeText(text: String): String {
            return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceFirst(UTF8_BOM, "")
        }

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

internal class RuleExecutionException(
    val line: Int,
    val col: Int,
    val ruleId: String,
    override val cause: Throwable,
) : Throwable(cause)
