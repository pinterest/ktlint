package com.pinterest.ktlint.rule.engine.core.api.visitor.internal

import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.psi.KtElement

internal fun Map<IElementType, Class<out KtElement>>.generateNodeVisitor(): String {
    val imports =
        listOf(
            "import com.pinterest.ktlint.rule.engine.core.api.ElementType",
            "import com.pinterest.ktlint.rule.engine.core.util.safeAs",
            "import org.jetbrains.kotlin.com.intellij.lang.ASTNode",
            "import org.jetbrains.kotlin.psi.KtElement",
        )
    val elementTypeImports =
        map { (elementType, _) ->
            "import com.pinterest.ktlint.rule.engine.core.api.ElementType.${elementType.filename()}"
        }
    val ktElementImports = map { (_, clazz) -> "import ${clazz.canonicalName}" }
    val sortedImports =
        imports
            .plus(elementTypeImports)
            .plus(ktElementImports)
            .sorted()
            .joinToString(separator = "\n")

    val dispatchersBefore =
        map { (elementType, clazz) -> generateDispatchersBefore(elementType, clazz) }
            .joinToString(separator = "\n")
    val dispatchersAfter =
        map { (elementType, clazz) -> generateDispatchersAfter(elementType, clazz) }
            .joinToString(separator = "\n\n")
    return """
        |package com.pinterest.ktlint.rule.engine.core.api.visitor
        |
        |$sortedImports
        |
        |// IMPORTANT: This class is generated and therefore should not be changed manually.
        |
        |/**
        | * This visitor dispatches a request from the [KtlintRuleEngine] to visit a node to a more specified visitor function as declared in
        | * [BeforeNodeVisitor] or [RuleAfterNodeVisitor].
        | */
        |public open class NodeVisitor :
        |    BeforeNodeVisitor,
        |    AfterNodeVisitor {
        |    /**
        |     * This method is called on a node in the AST before visiting the child nodes. If the node can be transformed to a known Kt-type (for
        |     * example [KtIfExpression], the call is dispatched to the visit method [beforeIfExpression] for that specific type. Otherwise, the node
        |     * is dispatched to the default handler [beforeVisitChildNodes].
        |     *
        |     * The called dispatch function can process other nodes in the AST. But do not that rule suppressions on other nodes are not guaranteed
        |     * to be taken into account.
        |     *
        |     * @param node AST node
        |     * @param autoCorrect indicates whether rule should attempt autocorrection
        |     * @param emit a way for rule to notify about a violation (lint error)
        |     */
        |    public fun beforeNode(
        |        node: ASTNode,
        |        autoCorrect: Boolean,
        |        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        |    ) {
        |        val dispatched =
        |            when (node.elementType) {
        |$dispatchersBefore
        |                else -> false
        |            }
        |        if (!dispatched) {
        |            // In case no mapping exists for the element type, or if the PSI of the node could not safely be cast to the Kt-type, then fall back
        |            // on the generic handler.
        |            beforeVisitChildNodes(node, autoCorrect, emit)
        |        }
        |    }
        |
        |    /**
        |     * This method is called on a node in the AST after visiting the child nodes. If the node can be transformed to a known Kt-type (for
        |     * example [KtIfExpression], the call is dispatched to the visit method [afterIfExpression] for that specific type. Otherwise, the node
        |     * is dispatched to the default handler [afterVisitChildNodes].
        |     *
        |     * The called dispatch function can process other nodes in the AST. But do not that rule suppressions on other nodes are not guaranteed
        |     * to be taken into account.
        |     *
        |     * @param node AST node
        |     * @param autoCorrect indicates whether rule should attempt autocorrection
        |     * @param emit a way for rule to notify about a violation (lint error)
        |     */
        |    public fun afterNode(
        |        node: ASTNode,
        |        autoCorrect: Boolean,
        |        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        |    ) {
        |        val dispatched =
        |            when (node.elementType) {
        |$dispatchersAfter
        |                else -> false
        |            }
        |        if (!dispatched) {
        |            // In case no mapping exists for the element type, or if the PSI of the node could not safely be cast to the Kt-type, then fall back
        |            // on the generic handler.
        |            afterVisitChildNodes(node, autoCorrect, emit)
        |        }
        |    }
        |
        |    private inline fun <reified T : KtElement> ASTNode.dispatch(ktElement: (T) -> Unit): Boolean =
        |        psi
        |            .safeAs<T>()
        |            ?.let {
        |                ktElement(it)
        |                true
        |            }
        |            ?: false
        |}
        """.trimMargin()
        .plus("\n\n")
}

private fun generateDispatchersBefore(
    elementType: IElementType,
    ktClazz: Class<out KtElement>,
): String {
    val ktClassName = ktClazz.simpleName
    val functionName = "before${ktClassName.substring(2)}"
    return """
        |                ${elementType.filename()} ->
        |                    node.dispatch<$ktClassName> { element -> $functionName(element, autoCorrect, emit) }
        """.trimMargin()
}

private fun generateDispatchersAfter(
    elementType: IElementType,
    ktClazz: Class<out KtElement>,
): String {
    val ktClassName = ktClazz.simpleName
    val functionName = "after${ktClassName.substring(2)}"
    return """
        |                ${elementType.filename()} ->
        |                    node.dispatch<$ktClassName> { element -> $functionName(element, autoCorrect, emit) }
        """.trimMargin()
}

private fun IElementType.filename() =
    if (this == FILE) {
        "FILE"
    } else {
        debugName
    }
