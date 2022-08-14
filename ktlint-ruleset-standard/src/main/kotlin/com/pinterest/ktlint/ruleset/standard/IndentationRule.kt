package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.IndentConfig
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.SPACE
import com.pinterest.ktlint.core.IndentConfig.IndentStyle.TAB
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentSizeProperty
import com.pinterest.ktlint.core.api.DefaultEditorConfigProperties.indentStyleProperty
import com.pinterest.ktlint.core.api.EditorConfigProperties
import com.pinterest.ktlint.core.api.UsesEditorConfigProperties
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.BINARY_WITH_TYPE
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.BY_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.COLON
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DELEGATED_SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FOR
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.LBRACKET
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.LT
import com.pinterest.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PARENTHESIZED
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_DELEGATE
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACKET
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.SHORT_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_CONSTRAINT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHERE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.lineNumber
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevCodeSibling
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.initKtLintKLogger
import com.pinterest.ktlint.ruleset.standard.IndentationRule.IndentContext.Block
import com.pinterest.ktlint.ruleset.standard.IndentationRule.IndentContext.Block.BlockIndentationType.REGULAR
import com.pinterest.ktlint.ruleset.standard.IndentationRule.IndentContext.Block.BlockIndentationType.SAME_AS_PREVIOUS_BLOCK
import java.util.Deque
import java.util.LinkedList
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.leaves

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

public class IndentationRule :
    Rule(
        id = "indent",
        visitorModifiers = setOf(
            VisitorModifier.RunAsLateAsPossible,
            VisitorModifier.RunAfterRule(
                ruleId = "experimental:function-signature",
                loadOnlyWhenOtherRuleIsLoaded = false,
                runOnlyWhenOtherRuleIsEnabled = false,
            ),
        ),
    ),
    UsesEditorConfigProperties {
    override val editorConfigProperties: List<UsesEditorConfigProperties.EditorConfigProperty<*>> =
        listOf(
            indentSizeProperty,
            indentStyleProperty,
        )
    private var indentConfig = IndentConfig.DEFAULT_INDENT_CONFIG

    private var line = 1
    private var expectedIndent = 0 // TODO: merge into IndentContext

    private val ctx = IndentContext()

    override fun beforeFirstNode(editorConfigProperties: EditorConfigProperties) {
        indentConfig = IndentConfig(
            indentStyle = editorConfigProperties.getEditorConfigValue(indentStyleProperty),
            tabWidth = editorConfigProperties.getEditorConfigValue(indentSizeProperty),
        )
        if (indentConfig.disabled) {
            stopTraversalOfAST()
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.isRoot()) {
            val firstNotEmptyLeaf = node.nextLeaf()
            if (firstNotEmptyLeaf?.let { it.elementType == WHITE_SPACE && !it.textContains('\n') } == true) {
                visitWhiteSpace(firstNotEmptyLeaf, autoCorrect, emit, ctx)
            }
        }

        when (node.elementType) {
            LPAR, LBRACE, LBRACKET -> {
                // ({[ should increase expectedIndent by 1
                val prevBlock = ctx.blockStack.peek()
                when {
                    node.isClosedOnSameLine() -> {
                        logger.trace {
                            "$line: block starting with ${node.text} is opened and closed on the same line, " +
                                "expected indent is kept unchanged -> $expectedIndent"
                        }
                        ctx.blockStack.push(
                            Block(node.elementType, line, SAME_AS_PREVIOUS_BLOCK),
                        )
                    }
                    node.isAfterValueParameterOnSameLine() -> {
                        logger.trace {
                            "$line: block starting with ${node.text} starts on same line as the previous value " +
                                "parameter value ended, expected indent is kept unchanged -> $expectedIndent"
                        }
                        ctx.blockStack.push(
                            Block(node.elementType, line, SAME_AS_PREVIOUS_BLOCK),
                        )
                    }
                    prevBlock != null && line == prevBlock.line -> {
                        logger.trace {
                            "$line: block starting with ${node.text} starts on same line as the previous block, " +
                                "expected indent is kept unchanged -> $expectedIndent"
                        }
                        ctx.blockStack.push(
                            Block(node.elementType, line, SAME_AS_PREVIOUS_BLOCK),
                        )
                    }
                    else -> {
                        if (node.isPartOfForLoopConditionWithMultilineExpression()) {
                            logger.trace { "$line: block starting with ${node.text} -> Keep at $expectedIndent" }
                            ctx.blockStack.push(
                                Block(node.elementType, line, SAME_AS_PREVIOUS_BLOCK),
                            )
                        } else {
                            expectedIndent++
                            logger.trace { "$line: block starting with ${node.text} -> Increase to $expectedIndent" }
                            ctx.blockStack.push(
                                Block(node.elementType, line, REGULAR),
                            )
                        }
                    }
                }
                logger.trace {
                    ctx.blockStack.iterator().asSequence().toList()
                        .joinToString(
                            separator = "\n\t",
                            prefix = "Stack (newest first) after pushing new element:\n\t",
                        )
                }
            }
            RPAR, RBRACE, RBRACKET -> {
                // ]}) should decrease expectedIndent by 1
                logger.trace {
                    ctx.blockStack.iterator().asSequence().toList()
                        .joinToString(
                            separator = "\n\t",
                            prefix = "Stack before popping newest element from top of stack:\n\t",
                        )
                }
                val block = ctx.blockStack.pop()
                when (block.blockIndentationType) {
                    SAME_AS_PREVIOUS_BLOCK -> {
                        logger.trace { "$line: block closed with ${node.elementType}. BlockIndentationType ${block.blockIndentationType} -> keep indent unchanged at $expectedIndent" }
                    }
                    REGULAR -> {
                        expectedIndent--
                        logger.trace { "$line: block closed with ${node.elementType}.  -> Decrease indent to $expectedIndent" }
                    }
                }

                val pairedLeft = node.pairedLeft()
                val byKeywordOnSameLine = pairedLeft.prevLeafOnSameLine(BY_KEYWORD)
                if (byKeywordOnSameLine != null &&
                    byKeywordOnSameLine.prevLeaf()?.isWhiteSpaceWithNewline() == true &&
                    node.leavesOnSameLine(forward = true).all { it.isWhiteSpace() || it.isPartOfComment() }
                ) {
                    expectedIndent--
                    logger.trace { "$line: --on same line as by keyword ${node.text} -> $expectedIndent" }
                }
            }
            LT ->
                // <T>
                if (node.treeParent.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
                    expectedIndent++
                    logger.trace { "$line: ++${node.text} -> $expectedIndent" }
                }
            GT ->
                // <T>
                if (node.treeParent.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
                    expectedIndent--
                    logger.trace { "$line: --${node.text} -> $expectedIndent" }
                }
            SUPER_TYPE_LIST ->
                // class A :
                //     SUPER_TYPE_LIST
                adjustExpectedIndentInsideSuperTypeList(node)
            SUPER_TYPE_CALL_ENTRY, DELEGATED_SUPER_TYPE_ENTRY -> {
                // IDEA quirk:
                //
                // class A : B({
                //     f() {}
                // }),
                //     C({
                //         f() {}
                //     })
                //
                // instead of expected
                //
                // class A : B({
                //         f() {}
                //     }),
                //     C({
                //         f() {}
                //     })
                adjustExpectedIndentInsideSuperTypeCall(node, ctx)
            }
            STRING_TEMPLATE ->
                indentStringTemplate(node, autoCorrect, emit)
            DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION, BINARY_EXPRESSION, BINARY_WITH_TYPE -> {
                val prevBlock = ctx.blockStack.peek()
                if (prevBlock != null && prevBlock.line == line) {
                    ctx.ignored.add(node)
                }
            }
            FUNCTION_LITERAL ->
                adjustExpectedIndentInFunctionLiteral(node, ctx)
            WHITE_SPACE ->
                if (node.textContains('\n')) {
                    if (
                        !node.isPartOfComment() &&
                        !node.isPartOfTypeConstraint() // FIXME IndentationRuleTest.testLintWhereClause not checked
                    ) {
                        val p = node.treeParent
                        val nextSibling = node.treeNext
                        val prevLeaf = node.prevLeaf { !it.isPartOfComment() && !it.isWhiteSpaceWithoutNewline() }
                        when {
                            p.elementType.let {
                                it == DOT_QUALIFIED_EXPRESSION || it == SAFE_ACCESS_EXPRESSION
                            } ->
                                // value
                                //     .x()
                                //     .y
                                adjustExpectedIndentInsideQualifiedExpression(node, ctx)
                            p.elementType.let {
                                it == BINARY_EXPRESSION || it == BINARY_WITH_TYPE
                            } ->
                                // value
                                //     + x()
                                //     + y
                                adjustExpectedIndentInsideBinaryExpression(node, ctx)
                            nextSibling?.elementType.let {
                                it == THEN || it == ELSE || it == BODY
                            } ->
                                // if (...)
                                //     THEN
                                // else
                                //     ELSE
                                // while (...)
                                //     BODY
                                adjustExpectedIndentInFrontOfControlBlock(node, ctx)
                            nextSibling?.elementType == PROPERTY_ACCESSOR ->
                                // val f: Type =
                                //     PROPERTY_ACCESSOR get() = ...
                                //     PROPERTY_ACCESSOR set() = ...
                                adjustExpectedIndentInFrontOfPropertyAccessor(node, ctx)
                            nextSibling?.elementType == SUPER_TYPE_LIST ->
                                // class C :
                                //     SUPER_TYPE_LIST
                                adjustExpectedIndentInFrontOfSuperTypeList(node, ctx)
                            prevLeaf?.elementType == EQ && p.elementType != VALUE_ARGUMENT ->
                                // v =
                                //     value
                                adjustExpectedIndentAfterEq(node, ctx)
                            prevLeaf?.elementType == ARROW ->
                                // when {
                                //    v ->
                                //        value
                                // }
                                adjustExpectedIndentAfterArrow(node, ctx)
                            prevLeaf?.elementType == COLON ->
                                // fun fn():
                                //     Int
                                adjustExpectedIndentAfterColon(node, ctx)
                            prevLeaf?.elementType == LPAR &&
                                p.elementType == VALUE_ARGUMENT_LIST &&
                                p.parent(CONDITION)?.takeIf { !it.prevLeaf().isWhiteSpaceWithNewline() } != null ->
                                // if (condition(
                                //         params
                                //     )
                                // )
                                adjustExpectedIndentAfterLparInsideCondition(node, ctx)
                        }
                        visitWhiteSpace(node, autoCorrect, emit, ctx)
                        if (ctx.localAdj != 0) {
                            expectedIndent += ctx.localAdj
                            logger.trace { "$line: ++${ctx.localAdj} on whitespace containing new line (${node.elementType}) -> $expectedIndent" }
                            ctx.localAdj = 0
                        }
                    } else if (node.isPartOf(KDOC)) {
                        visitWhiteSpace(node, autoCorrect, emit, ctx)
                    }
                    line += node.text.count { it == '\n' }
                }
            EOL_COMMENT ->
                if (node.text == "// ktlint-debug-print-expected-indent") {
                    logger.trace { "$line: expected indent: $expectedIndent" }
                }
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        when (node.elementType) {
            SUPER_TYPE_LIST ->
                adjustExpectedIndentAfterSuperTypeList(node)
            DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION, BINARY_EXPRESSION, BINARY_WITH_TYPE ->
                ctx.ignored.remove(node)
        }
        val adj = ctx.clearExitAdj(node)
        if (adj != null) {
            expectedIndent += adj
            logger.trace { "$line: adjusted ${node.elementType} by $adj -> $expectedIndent" }
        }
    }

    override fun afterLastNode() {
        // The expectedIndent should never be negative. If so, it is very likely that ktlint crashes at runtime when
        // autocorrecting is executed while no error occurs with linting only. Such errors often are not found in unit
        // tests, as the examples are way more simple than realistic code.
        assert(expectedIndent >= 0)
    }

    private fun adjustExpectedIndentInsideQualifiedExpression(n: ASTNode, ctx: IndentContext) {
        val p = n.parent({
            it.treeParent.elementType != DOT_QUALIFIED_EXPRESSION && it.treeParent.elementType != SAFE_ACCESS_EXPRESSION
        },) ?: return
        val nextSibling = n.treeNext
        if (!ctx.ignored.contains(p) && nextSibling != null) {
            if (p.treeParent.elementType == PROPERTY_DELEGATE &&
                p.treeParent?.lineNumber() != p.treeParent.prevCodeSibling()?.lineNumber()
            ) {
                expectedIndent += 2
                logger.trace { "$line: ++dot-qualified-expression in property delegate -> $expectedIndent" }
                ctx.ignored.add(p)
                ctx.exitAdjBy(p, -2)
            } else {
                expectedIndent++
                logger.trace { "$line: ++inside(${p.elementType}) -> $expectedIndent" }
                ctx.ignored.add(p)
                ctx.exitAdjBy(p, -1)
            }
        }
    }

    private fun adjustExpectedIndentInsideBinaryExpression(n: ASTNode, ctx: IndentContext) {
        if (!n.isPartOfCondition()) {
            val p = n.treeParent
            if (!ctx.ignored.contains(p)) {
                expectedIndent++
                logger.trace { "$line: ++inside(${p.elementType}) -> $expectedIndent" }
                val rOperand = n.nextSibling { sibling ->
                    sibling.elementType != OPERATION_REFERENCE &&
                        sibling.elementType != WHITE_SPACE
                }!!
                ctx.exitAdjBy(rOperand, -1)
            }
            val nextSibling = n.treeNext
            if (
                nextSibling?.elementType.let { it == BINARY_EXPRESSION || it == BINARY_WITH_TYPE } &&
                nextSibling.children().firstOrNull { it.elementType == OPERATION_REFERENCE }
                    ?.firstChildNode?.elementType != ELVIS &&
                nextSibling.firstChildNode.elementType != CALL_EXPRESSION
            ) {
                ctx.localAdj = -1
                logger.trace { "$line: --inside(${nextSibling.elementType}) -> $expectedIndent" }
                ctx.exitAdjBy(p, 1)
            }
        }
    }

    private fun adjustExpectedIndentInFrontOfControlBlock(n: ASTNode, ctx: IndentContext) {
        val nextSibling = n.treeNext
        expectedIndent++
        logger.trace { "$line: ++in_front(${nextSibling.elementType}) -> $expectedIndent" }
        ctx.exitAdjBy(nextSibling, -1)
    }

    private fun adjustExpectedIndentInFrontOfPropertyAccessor(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        logger.trace { "$line: ++in_front(${n.treeNext.elementType}) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeNext, -1)
    }

    private fun adjustExpectedIndentInFrontOfSuperTypeList(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        logger.trace { "$line: ++in_front(${n.treeNext.elementType}) -> $expectedIndent" }
        ctx.localAdj = -1
    }

    private fun adjustExpectedIndentInsideSuperTypeList(n: ASTNode) {
        expectedIndent++
        logger.trace { "$line: ++inside(${n.elementType}) -> $expectedIndent" }
    }

    private fun adjustExpectedIndentAfterSuperTypeList(n: ASTNode) {
        val byKeywordLeaf = n
            .findChildByType(DELEGATED_SUPER_TYPE_ENTRY)
            ?.findChildByType(BY_KEYWORD)
        if (n.prevLeaf()?.textContains('\n') == true &&
            byKeywordLeaf?.prevLeaf().isWhiteSpaceWithNewline()
        ) {
            return
        }
        if (byKeywordLeaf?.prevLeaf()?.textContains('\n') == true &&
            byKeywordLeaf.prevLeaf()?.treeParent?.nextLeaf()?.elementType == IDENTIFIER
        ) {
            return
        }
        expectedIndent--
        logger.trace { "$line: --after(${n.elementType}) -> $expectedIndent" }
    }

    private fun adjustExpectedIndentInsideSuperTypeCall(n: ASTNode, ctx: IndentContext) {
        // Don't adjust indents for initializer lists
        if (n.treeParent?.elementType != SUPER_TYPE_LIST) {
            return
        }
        if (n.prevLeaf()?.textContains('\n') == false) {
            expectedIndent--
            logger.trace { "$line: --inside(${n.elementType}) -> $expectedIndent" }
            ctx.exitAdjBy(n, 1)
        }
    }

    private fun adjustExpectedIndentAfterEq(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        logger.trace { "$line: ++after(EQ) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeParent, -1)
    }

    private fun adjustExpectedIndentAfterArrow(n: ASTNode, ctx: IndentContext) {
        // Only adjust indents for arrows inside of when statements. Lambda arrows should not increase indent.
        if (n.treeParent?.elementType == WHEN_ENTRY) {
            val prevBlock = ctx.blockStack.peek()
            if (prevBlock == null || line != prevBlock.line) {
                expectedIndent++
                logger.trace { "$line: ++after(ARROW) -> $expectedIndent" }
                ctx.exitAdjBy(n.treeParent, -1)
            }
        }
    }

    private fun adjustExpectedIndentAfterColon(n: ASTNode, ctx: IndentContext) {
        when {
            n.isPartOf(FUN) -> {
                expectedIndent++
                logger.trace { "$line: ++after(COLON IN FUN) -> $expectedIndent" }
                val returnType = n.nextCodeSibling()
                ctx.exitAdjBy(returnType!!, -1)
            }
            n.treeParent.isPartOf(SECONDARY_CONSTRUCTOR) -> {
                expectedIndent++
                logger.trace { "$line: ++after(COLON IN CONSTRUCTOR) -> $expectedIndent" }
                val nextCodeSibling = n.nextCodeSibling()
                ctx.exitAdjBy(nextCodeSibling!!, -1)
            }
            else -> {
                expectedIndent++
                logger.trace { "$line: ++after(COLON) -> $expectedIndent" }
                ctx.exitAdjBy(n.treeParent, -1)
            }
        }
    }

    private fun adjustExpectedIndentAfterLparInsideCondition(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        logger.trace { "$line: ++inside(CONDITION) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeParent, -1)
    }

    private fun adjustExpectedIndentInFunctionLiteral(n: ASTNode, ctx: IndentContext) {
        require(n.elementType == FUNCTION_LITERAL)

        var countNonWhiteSpaceElementsBeforeArrow = 0
        var arrowNode: ASTNode? = null
        var hasWhiteSpaceWithNewLine = false
        val iterator = n.children().iterator()
        while (iterator.hasNext()) {
            val child = iterator.next()
            if (child.elementType == ARROW) {
                arrowNode = child
                break
            }
            if (child.elementType == WHITE_SPACE) {
                hasWhiteSpaceWithNewLine = hasWhiteSpaceWithNewLine || child.text.contains("\n")
            } else {
                countNonWhiteSpaceElementsBeforeArrow++
            }
        }

        if (arrowNode != null && hasWhiteSpaceWithNewLine) {
            expectedIndent++
            logger.trace { "$line: ++after(FUNCTION_LITERAL) -> $expectedIndent" }
            ctx.exitAdjBy(arrowNode.prevCodeSibling()!!, -1)
        }
    }

    private fun indentStringTemplate(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        node
            .let { it.psi as KtStringTemplateExpression }
            .takeIf { it.isFollowedByTrimIndent() || it.isFollowedByTrimMargin() }
            ?.takeIf { it.isMultiLine() }
            ?.let {
                if (node.containsMixedIndentationCharacters()) {
                    // It can not be determined with certainty how mixed indentation characters should be interpreted.
                    // The trimIndent function handles tabs and spaces equally (one tabs equals one space) while the user
                    // might expect that the tab size in the indentation is more than one space.
                    emit(
                        node.startOffset,
                        "Indentation of multiline string should not contain both tab(s) and space(s)",
                        false,
                    )
                    return
                }

                val prefixLength = node.children()
                    .filterNot { it.elementType == OPEN_QUOTE }
                    .filterNot { it.elementType == CLOSING_QUOTE }
                    .filter { it.prevLeaf()?.text == "\n" }
                    .filterNot { it.text == "\n" }
                    .let { indents ->
                        val indentsExceptBlankIndentBeforeClosingQuote = indents
                            .filterNot { it.isIndentBeforeClosingQuote() }
                        if (indentsExceptBlankIndentBeforeClosingQuote.count() > 0) {
                            indentsExceptBlankIndentBeforeClosingQuote
                        } else {
                            indents
                        }
                    }
                    .map { it.text.indentLength() }
                    .minOrNull() ?: 0

                val correctedExpectedIndent = if (node.prevLeaf()?.text == "\n") {
                    // In case the opening quotes are placed at the start of the line, then expect all lines inside the
                    // string literal and the closing quotes to have no indent as well.
                    0
                } else {
                    expectedIndent
                }
                val expectedIndentation = indentConfig.indent.repeat(correctedExpectedIndent)
                val expectedPrefixLength = correctedExpectedIndent * indentConfig.indent.length
                node.children()
                    .forEach {
                        if (it.prevLeaf()?.text == "\n" &&
                            (
                                it.isLiteralStringTemplateEntry() ||
                                    it.isVariableStringTemplateEntry() ||
                                    it.isClosingQuote()
                                )
                        ) {
                            val (actualIndent, actualContent) =
                                if (it.isIndentBeforeClosingQuote()) {
                                    it.text.splitIndentAt(it.text.length)
                                } else if (it.isVariableStringTemplateEntry() && it.isFirstNonBlankElementOnLine()) {
                                    it.getFirstElementOnSameLine().text.splitIndentAt(expectedPrefixLength)
                                } else {
                                    it.text.splitIndentAt(prefixLength)
                                }
                            if (indentConfig.containsUnexpectedIndentChar(actualIndent)) {
                                val offsetFirstWrongIndentChar =
                                    indentConfig.indexOfFirstUnexpectedIndentChar(actualIndent)
                                emit(
                                    it.startOffset + offsetFirstWrongIndentChar,
                                    "Unexpected '${indentConfig.unexpectedIndentCharDescription}' character(s) in margin of multiline string",
                                    true,
                                )
                                if (autoCorrect) {
                                    (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                        expectedIndentation + actualContent,
                                    )
                                }
                            } else if (actualIndent != expectedIndentation && it.isIndentBeforeClosingQuote()) {
                                // It is a deliberate choice not to fix the indents inside the string literal except the line which only contains
                                // the closing quotes.
                                emit(
                                    it.startOffset,
                                    "Unexpected indent of multiline string closing quotes",
                                    true,
                                )
                                if (autoCorrect) {
                                    if (it.firstChildNode == null) {
                                        (it as LeafPsiElement).rawInsertBeforeMe(
                                            LeafPsiElement(REGULAR_STRING_PART, expectedIndentation),
                                        )
                                    } else {
                                        (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                            expectedIndentation + actualContent,
                                        )
                                    }
                                }
                            }
                        }
                    }
            }
    }

    private fun KtStringTemplateExpression.isMultiLine(): Boolean {
        for (child in node.children()) {
            if (child.elementType == LITERAL_STRING_TEMPLATE_ENTRY) {
                val v = child.text
                if (v == "\n") {
                    return true
                }
            }
        }
        return false
    }

    private fun String.indentLength() =
        indexOfFirst { !it.isWhitespace() }.let { if (it == -1) length else it }

    private fun visitWhiteSpace(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        ctx: IndentContext,
    ) {
        val text = node.text
        val nodeIndent = text.substringAfterLast("\n")
        val nextLeaf = node.nextLeaf()
        val nextLeafElementType = nextLeaf?.elementType
        if (nextLeafElementType == OPEN_QUOTE && nextLeaf.text == "\"\"\"" && nodeIndent.isEmpty()) {
            return // raw strings ("""") are allowed at column 0
        }
        val comment = nextLeaf?.parent({ it.psi is PsiComment }, strict = false)
        if (comment != null) {
            if (nodeIndent.isEmpty()) {
                return // comments are allowed at column 0
            }
            if (comment.textContains('\n') && comment.elementType == BLOCK_COMMENT) {
                // FIXME: while we cannot assume any kind of layout inside a block comment,
                // `/*` and `*/` can still be indented
                return
            }
        }
        // adjusting expectedIndent based on what is in front
        val adjustedExpectedIndent = expectedIndent + when {
            // IDEA quirk:
            // val v = (
            //     true
            //     )
            // instead of expected
            // val v = (
            //     true
            // )
            nextLeafElementType == RPAR &&
                node.treeParent?.elementType == PARENTHESIZED ->
                0
            // IDEA quirk:
            // class A : T<
            //     K,
            //     V
            //     > {
            // }
            // instead of expected
            // class A : T<
            //     K,
            //     V
            // > {
            // }
            nextLeafElementType == COLON ->
                1
            nextLeafElementType == GT &&
                node.treeParent?.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST } ->
                0
            nextLeafElementType in rTokenSet ->
                if (node.isPartOfForLoopConditionWithMultilineExpression()) {
                    0
                } else {
                    -1
                }
            // IDEA quirk:
            // val i: Int
            //     by lazy { 1 }
            // instead of expected
            // val i: Int
            // by lazy { 1 }
            nextLeafElementType == BY_KEYWORD -> {
                if (node.isPartOf(DELEGATED_SUPER_TYPE_ENTRY) &&
                    node.treeParent.prevLeaf()?.textContains('\n') == true
                ) {
                    0
                } else if (node.isPartOf(DELEGATED_SUPER_TYPE_ENTRY) &&
                    node.treeParent.nextLeaf()?.elementType == IDENTIFIER
                ) {
                    0
                } else {
                    expectedIndent++
                    logger.trace { "$line: ++whitespace followed by BY keyword -> $expectedIndent" }
                    1
                }
            }
            // IDEA quirk:
            // var value: DataClass =
            //     DataClass("too long line")
            //     private set
            //
            //  instead of expected:
            //  var value: DataClass =
            //      DataClass("too long line")
            //          private set
            node.nextCodeSibling()?.elementType == PROPERTY_ACCESSOR &&
                node.treeParent.findChildByType(EQ)?.nextLeaf().isWhiteSpaceWithNewline() -> {
                // Fix the indent of the body block/expression of the setter/getter
                //  IDEA formatting:
                //    private var foo: String =
                //        "foo"
                //        get() = value
                //        set(value) {
                //            field = value
                //        }
                //  instead of
                //    private var foo: String =
                //        "foo"
                //        get() = value
                //        set(value) {
                //                field = value
                //            }
                expectedIndent--
                val propertyAccessor = node.nextCodeSibling()!!
                ctx.exitAdjBy(propertyAccessor, 1)

                // Fix the indent before the setter/getter
                -1
            }
            else -> 0
        }
        // indentation with incorrect characters replaced
        val normalizedNodeIndent =
            when (indentConfig.indentStyle) {
                SPACE -> {
                    if ('\t' in nodeIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected tab character(s)",
                            true,
                        )
                        indentConfig.toNormalizedIndent(nodeIndent)
                    } else {
                        nodeIndent
                    }
                }
                TAB -> {
                    val isKdocIndent = node.isKDocIndent()
                    val indentWithoutKdocIndent =
                        if (node.isKDocIndent()) {
                            nodeIndent.removeSuffix(" ")
                        } else {
                            nodeIndent
                        }
                    if (' ' in indentWithoutKdocIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected space character(s)",
                            true,
                        )
                        indentConfig.toNormalizedIndent(indentWithoutKdocIndent) +
                            // Re-add the kdoc indent when it was present before
                            if (isKdocIndent) {
                                " "
                            } else {
                                ""
                            }
                    } else {
                        nodeIndent
                    }
                }
            }
        val expectedIndent = indentConfig.indent.repeat(adjustedExpectedIndent) +
            // +1 space before * in `/**\n *\n */`
            if (comment?.elementType == KDOC && nextLeafElementType != KDOC_START) " " else ""
        if (normalizedNodeIndent != expectedIndent) {
            emit(
                node.startOffset + text.length - nodeIndent.length,
                "Unexpected indentation (${normalizedNodeIndent.length}) (should be ${expectedIndent.length})",
                true,
            )
            logger.trace {
                "$line: " + (if (!autoCorrect) "would have " else "") + "changed indentation to ${expectedIndent.length} (from ${normalizedNodeIndent.length})"
            }
        }
        if (autoCorrect) {
            if (nodeIndent != normalizedNodeIndent || normalizedNodeIndent != expectedIndent) {
                (node as LeafPsiElement).rawReplaceWithText(
                    text.substringBeforeLast("\n") + "\n" + expectedIndent,
                )
            }
        }
    }

    // e.g.
    // if (condition), while (condition), for (condition), ...
    private fun ASTNode.isPartOfCondition(): Boolean {
        var n = this
        while (n.treeParent?.elementType.let { it == BINARY_EXPRESSION || it == BINARY_WITH_TYPE }) {
            n = n.treeParent
        }
        return n.treeParent?.elementType == CONDITION
    }

    // e.g.
    // where T1 : SubType...,
    //       T2 : SubType...
    private fun ASTNode.isPartOfTypeConstraint() =
        isPartOf(TYPE_CONSTRAINT_LIST) || nextLeaf()?.elementType == WHERE_KEYWORD

    private fun ASTNode.pairedLeft(): ASTNode {
        val leftType = when (elementType) {
            RPAR -> LPAR
            RBRACE -> LBRACE
            RBRACKET -> LBRACKET
            else -> null
        }
        requireNotNull(leftType) { "Element type '$leftType' not allowed" }

        val pairedLeft = treeParent.findChildByType(leftType)
        checkNotNull(pairedLeft) { "Can not find the '$leftType' element in same parent" }
        return pairedLeft
    }

    private fun ASTNode.leavesOnSameLine(forward: Boolean): Sequence<ASTNode> =
        leaves(forward = forward).takeWhile { !it.isWhiteSpaceWithNewline() }

    private fun ASTNode.prevLeafOnSameLine(prevLeafType: IElementType): ASTNode? =
        leavesOnSameLine(forward = false).firstOrNull { it.elementType == prevLeafType }

    private fun ASTNode.isAfterValueParameterOnSameLine(): Boolean {
        // Expect the current node to be the start of a block
        require(elementType == LPAR || elementType == LBRACE || elementType == LBRACKET)

        // Check if the block is the first code element of a value parameter
        this
            .prevCodeLeaf()
            ?.nextCodeSibling()
            ?.takeIf { it.elementType == VALUE_ARGUMENT }
            ?.let { valueArgument ->
                if (valueArgument == valueArgument.treeParent.findChildByType(VALUE_ARGUMENT)) {
                    // This is the first value argument in the list, so by definition it is not *after* another value argument
                    return false
                }

                valueArgument
                    .leaves(forward = false)
                    .takeWhile { it.isWhiteSpaceWithoutNewline() || it.elementType != VALUE_ARGUMENT }
                    .firstOrNull()
                    ?.let {
                        if (it.isWhiteSpaceWithoutNewline() || it.elementType == VALUE_ARGUMENT) {
                            // No newline has been found between the current value argument and the previous value argument
                            return true
                        }
                    }
            }

        return false
    }

    private fun ASTNode.containsMixedIndentationCharacters(): Boolean {
        assert((this.psi as KtStringTemplateExpression).isMultiLine())
        val nonBlankLines = this
            .text
            .split("\n")
            .filterNot { it.startsWith("\"\"\"") }
            .filterNot { it.endsWith("\"\"\"") }
            .filterNot { it.isBlank() }
        val prefixLength = nonBlankLines.minOfOrNull { it.indentLength() } ?: 0
        val distinctIndentCharacters = nonBlankLines
            .joinToString(separator = "") {
                it.splitIndentAt(prefixLength).first
            }
            .toCharArray()
            .distinct()
            .count()
        return distinctIndentCharacters > 1
    }

    private fun ASTNode.isClosedOnSameLine(): Boolean {
        val closingElementType = matchingRToken[elementType]
        var cur: ASTNode? = this
        while (cur != null && cur != closingElementType) {
            if (cur.text.contains("\n")) {
                return false
            }
            cur = cur.nextSibling { true }
        }
        return true
    }

    private class IndentContext {
        private val exitAdj = mutableMapOf<ASTNode, Int>()
        val ignored = mutableSetOf<ASTNode>()
        val blockStack: Deque<Block> = LinkedList()
        var localAdj: Int = 0

        fun exitAdjBy(node: ASTNode, change: Int) {
            exitAdj.compute(node) { _, v -> (v ?: 0) + change }
        }

        fun clearExitAdj(node: ASTNode): Int? =
            exitAdj.remove(node)

        data class Block(
            // Element type used for opening the block
            val openingElementType: IElementType,
            // Line at which the block is opened
            val line: Int,
            // Type of indentation to be used for the block
            val blockIndentationType: BlockIndentationType,
        ) {
            enum class BlockIndentationType {
                /**
                 * Indent the body of the block one level deeper by increasing the expected indentation level with 1.
                 * Decrease the expected indentation level just before the closing element of the block.
                 */
                REGULAR,

                /**
                 * Keep the indent of the body of the block identical to the indent of the previous block, so do not change
                 * the expected indentation level. The indentation of the closing element has to be decreased one level
                 * without altering the expected indentation level.
                 */
                SAME_AS_PREVIOUS_BLOCK,
            }
        }
    }

    private companion object {
        private val lTokenSet = TokenSet.create(LPAR, LBRACE, LBRACKET, LT)
        private val rTokenSet = TokenSet.create(RPAR, RBRACE, RBRACKET, GT)
        private val matchingRToken =
            lTokenSet.types.zip(
                rTokenSet.types,
            ).toMap()
    }
}

private fun ASTNode.isKDocIndent() =
    if (text.lastOrNull() == ' ') {
        // The indentation of a KDoc comment contains a space as the last character regardless of the indentation style
        // (tabs or spaces) except for the starting line of the KDoc comment
        nextLeaf()?.elementType == KDOC_LEADING_ASTERISK || nextLeaf()?.elementType == KDOC_END
    } else {
        false
    }

private fun ASTNode.isIndentBeforeClosingQuote() =
    elementType == CLOSING_QUOTE || (text.isBlank() && nextCodeSibling()?.elementType == CLOSING_QUOTE)

private fun ASTNode.isLiteralStringTemplateEntry() =
    elementType == LITERAL_STRING_TEMPLATE_ENTRY && text != "\n"

private fun ASTNode.isVariableStringTemplateEntry() =
    elementType == LONG_STRING_TEMPLATE_ENTRY || elementType == SHORT_STRING_TEMPLATE_ENTRY

private fun ASTNode.isClosingQuote() =
    elementType == CLOSING_QUOTE

private fun ASTNode.isFirstNonBlankElementOnLine(): Boolean {
    var node: ASTNode? = getFirstElementOnSameLine()
    while (node != null && node != this && node.text.isWhitespace()) {
        node = node.nextLeaf()
    }
    return node != this
}

private fun String.isWhitespace() =
    none { !it.isWhitespace() }

private fun ASTNode.getFirstElementOnSameLine(): ASTNode {
    val firstLeafOnLine = prevLeaf { it.text == "\n" }
    return if (firstLeafOnLine == null) {
        this
    } else {
        firstLeafOnLine.nextLeaf(includeEmpty = true) ?: this
    }
}

/**
 * Splits the string at the given index or at the first non white space character before that index. The returned pair
 * consists of the indentation and the second part contains the remainder. Note that the second part still can start
 * with whitespace characters in case the original strings starts with more white space characters than the requested
 * split index.
 */
private fun String.splitIndentAt(index: Int): Pair<String, String> {
    assert(index >= 0)
    val firstNonWhitespaceIndex = indexOfFirst { !it.isWhitespace() }.let {
        if (it == -1) {
            this.length
        } else {
            it
        }
    }
    val safeIndex = kotlin.math.min(firstNonWhitespaceIndex, index)
    return Pair(
        first = this.take(safeIndex),
        second = this.substring(safeIndex),
    )
}

private fun KtStringTemplateExpression.isFollowedByTrimIndent() = isFollowedBy("trimIndent()")

private fun KtStringTemplateExpression.isFollowedByTrimMargin() = isFollowedBy("trimMargin()")

private fun KtStringTemplateExpression.isFollowedBy(callExpressionName: String) =
    this.node.nextSibling { it.elementType != DOT }
        .let { it?.elementType == CALL_EXPRESSION && it.text == callExpressionName }

/**
 *  A for-loop for which the condition contains a sibling node containing a newline is not correctly formatted by the
 *  default formatter of IntelliJ IDEA (https://youtrack.jetbrains.com/issue/IDEA-293691/Format-Kotlin-for-loop). When
 *  using the correct indentation level, it conflicts with the IntelliJ IDEA formatting, so until the aforementioned bug
 *  is resolved, ktlint will produce the same format as IntelliJ default formatter.
 */
private fun ASTNode.isPartOfForLoopConditionWithMultilineExpression(): Boolean {
    if (treeParent.elementType != FOR) {
        return false
    }
    if (this.elementType != LPAR) {
        return treeParent.findChildByType(LPAR)!!.isPartOfForLoopConditionWithMultilineExpression()
    }
    require(elementType == LPAR) {
        "Node should be the LPAR of the FOR loop"
    }

    // Iterate all sibling node until RPAR to check whether the node contains a newline. Note that it does not matter
    // whether is code sibling contains a newline.
    var node: ASTNode? = this
    while (node != null && node.elementType != RPAR) {
        if (node.isWhiteSpaceWithNewline()) {
            return true
        }
        node = node.nextSibling { true }
    }
    return false
}
