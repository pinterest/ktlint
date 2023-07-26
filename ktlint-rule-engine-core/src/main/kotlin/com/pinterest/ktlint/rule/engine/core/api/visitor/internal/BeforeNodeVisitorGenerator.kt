package com.pinterest.ktlint.rule.engine.core.api.visitor.internal

import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart

internal fun Map<IElementType, Class<out KtElement>>.generateBeforeNodeVisitor(): String {
    val sortedImports =
        map { (_, clazz) -> "import ${clazz.canonicalName}" }
            .sorted()
            .joinToString(separator = "\n")

    val elementTypeFunctions =
        map { (elementType, clazz) -> generateBeforeFunction(elementType, clazz) }
            .joinToString(separator = "\n\n")
    return """
        |package com.pinterest.ktlint.rule.engine.core.api.visitor
        |
        |import org.jetbrains.kotlin.com.intellij.lang.ASTNode
        |$sortedImports
        |
        |public interface BeforeNodeVisitor {
        |$beforeVisitChildNodesFunction
        |
        |$elementTypeFunctions
        |}
        """.trimMargin()
        .plus("\n\n")
}

private val beforeVisitChildNodesFunction =
    """
    |    /**
    |     * This method is called on a node in AST before visiting the child nodes. This is repeated recursively for the child nodes resulting in
    |     * a depth first traversal of the AST. Note: this method won't be called for a node for which the visit method of the type of that node
    |     * has been overridden in the rule.
    |     *
    |     * ```
    |     * class ExampleRule(...) {
    |     *     override fun beforeIfExpression(
    |     *         ktIfExpression: KtIfExpression,
    |     *         autoCorrect: Boolean,
    |     *         emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    |     *     ) {
    |     *         // Only called for nodes having element type 'IF'
    |     *     }
    |
    |     *     override fun beforeWhenExpression(...) {
    |     *         // Only called for nodes having element type 'WHEN'
    |     *     }
    |     *
    |     *     override fun beforeVisitChildNodes(
    |     *         node: ASTNode,
    |     *         autoCorrect: Boolean,
    |     *         emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    |     *     ) {
    |     *         // Called for nodes of other types than 'IF' and 'WHEN' because the 'beforeIfExpression' and 'beforeWhenExpression' are
    |     *         // overridden.
    |     *     }
    |     * }
    |     * ```
    |     *
    |     * @param node AST node
    |     * @param autoCorrect indicates whether rule should attempt autocorrection
    |     * @param emit a way for rule to notify about a violation (lint error)
    |     */
    |    public fun beforeVisitChildNodes(
    |        node: ASTNode,
    |        autoCorrect: Boolean,
    |        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    |    ) {}
    """.trimMargin()

private fun generateBeforeFunction(
    elementType: IElementType,
    ktClazz: Class<out KtElement>,
): String {
    val elementTypeName = elementType.debugName
    val ktClassName = ktClazz.simpleName
    val functionName = "before${ktClassName.substring(2)}"
    val ktClassNameAsVariableName = ktClassName.decapitalizeSmart()
    return """
        |    /**
        |     * This method is called on a node of element type '$elementTypeName' in the AST before visiting the child nodes.
        |     *
        |     * @param $ktClassNameAsVariableName the [$ktClassName] of a node with element type '$elementTypeName'
        |     * @param autoCorrect indicates whether rule should attempt autocorrection
        |     * @param emit a way for rule to notify about a violation (lint error)
        |     */
        |    public fun $functionName(
        |        $ktClassNameAsVariableName: $ktClassName,
        |        autoCorrect: Boolean,
        |        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        |    ): Unit = beforeVisitChildNodes($ktClassNameAsVariableName.node, autoCorrect, emit)
        """.trimMargin()
}
