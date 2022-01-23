package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.EditorConfig
import com.pinterest.ktlint.core.EditorConfig.IndentStyle
import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.ANNOTATION
import com.pinterest.ktlint.core.ast.ElementType.ARROW
import com.pinterest.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.BINARY_WITH_TYPE
import com.pinterest.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.BODY
import com.pinterest.ktlint.core.ast.ElementType.BY_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.COLON
import com.pinterest.ktlint.core.ast.ElementType.COMMA
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DELEGATED_SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.KDOC
import com.pinterest.ktlint.core.ast.ElementType.KDOC_END
import com.pinterest.ktlint.core.ast.ElementType.KDOC_LEADING_ASTERISK
import com.pinterest.ktlint.core.ast.ElementType.KDOC_START
import com.pinterest.ktlint.core.ast.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.LBRACE
import com.pinterest.ktlint.core.ast.ElementType.LBRACKET
import com.pinterest.ktlint.core.ast.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.LPAR
import com.pinterest.ktlint.core.ast.ElementType.LT
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.core.ast.ElementType.PARENTHESIZED
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.core.ast.ElementType.RBRACE
import com.pinterest.ktlint.core.ast.ElementType.RBRACKET
import com.pinterest.ktlint.core.ast.ElementType.REGULAR_STRING_PART
import com.pinterest.ktlint.core.ast.ElementType.RPAR
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.core.ast.ElementType.SHORT_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.core.ast.ElementType.THEN
import com.pinterest.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_CONSTRAINT_LIST
import com.pinterest.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.core.ast.ElementType.WHERE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.WHITE_SPACE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isPartOf
import com.pinterest.ktlint.core.ast.isPartOfComment
import com.pinterest.ktlint.core.ast.isWhiteSpace
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevCodeSibling
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.core.ast.visit
import com.pinterest.ktlint.core.initKtLintKLogger
import java.util.Deque
import java.util.LinkedList
import mu.KotlinLogging
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.psiUtil.leaves

private val logger = KotlinLogging.logger {}.initKtLintKLogger()

/**
 * ktlint's rule that checks & corrects indentation.
 *
 * To keep things simple, we walk the AST twice:
 * - 1st pass - insert missing newlines (e.g. between parentheses of a multi-line function call)
 * - 2st pass - correct indentation
 *
 * Current limitations:
 * - "all or nothing" (currently, rule can only be disabled for an entire file)
 */
class IndentationRule : Rule(
    id = "indent",
    visitorModifiers = setOf(
        VisitorModifier.RunOnRootNodeOnly,
        VisitorModifier.RunAsLateAsPossible
    )
) {

    private companion object {
        private val lTokenSet = TokenSet.create(LPAR, LBRACE, LBRACKET, LT)
        private val rTokenSet = TokenSet.create(RPAR, RBRACE, RBRACKET, GT)
        private val matchingRToken =
            lTokenSet.types.zip(
                rTokenSet.types
            ).toMap()
    }

    private var line = 1
    private var expectedIndent = 0 // TODO: merge into IndentContext

    private fun reset() {
        line = 1
        expectedIndent = 0
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
        if (editorConfig.indentSize <= 1) {
            return
        }
        reset()
        logger.trace { "phase: rearrangement (auto correction ${if (autoCorrect) "on" else "off"})" }
        // step 1: insert newlines (if/where needed)
        var emitted = false
        rearrange(node, autoCorrect) { offset, errorMessage, canBeAutoCorrected ->
            emitted = true
            emit(offset, errorMessage, canBeAutoCorrected)
        }
        if (emitted && autoCorrect) {
            logger.trace {
                "indenting:\n" +
                    node
                        .text
                        .split("\n")
                        .mapIndexed { i, v -> "\t${i + 1}: $v" }
                        .joinToString("\n")
            }
        }
        reset()
        logger.trace { "phase: indentation" }
        // step 2: correct indentation
        indent(node, autoCorrect, emit, editorConfig)

        // The expectedIndent should never be negative. If so, it is very likely that ktlint crashes at runtime when
        // autocorrecting is executed while no error occurs with linting only. Such errors often are not found in unit
        // tests, as the examples are way more simple than realistic code.
        assert(expectedIndent >= 0)
    }

    private fun rearrange(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        node.visit { n ->
            when (n.elementType) {
                LPAR, LBRACE, LBRACKET -> rearrangeBlock(n, autoCorrect, emit) // TODO: LT
                SUPER_TYPE_LIST -> rearrangeSuperTypeList(n, autoCorrect, emit)
                VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST -> rearrangeValueList(n, autoCorrect, emit)
                ARROW -> rearrangeArrow(n, autoCorrect, emit)
                WHITE_SPACE -> line += n.text.count { it == '\n' }
                CLOSING_QUOTE -> rearrangeClosingQuote(n, autoCorrect, emit)
            }
        }
    }

    private fun rearrangeBlock(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val rElementType = matchingRToken[node.elementType]
        var newlineInBetween = false
        var parameterListInBetween = false
        var numberOfArgs = 0
        var firstArg: ASTNode? = null
        // matching ), ] or }
        val r = node.nextSibling {
            val isValueArgument = it.elementType == VALUE_ARGUMENT
            val hasLineBreak = if (isValueArgument) it.hasLineBreak(LAMBDA_EXPRESSION, FUN) else it.hasLineBreak()
            newlineInBetween = newlineInBetween || hasLineBreak
            parameterListInBetween = parameterListInBetween || it.elementType == VALUE_PARAMETER_LIST
            if (isValueArgument) {
                numberOfArgs++
                firstArg = it
            }
            it.elementType == rElementType
        }!!
        if (
            !newlineInBetween ||
            // keep { p ->
            // }
            (node.elementType == LBRACE && parameterListInBetween) ||
            // keep ({
            // }) and (object : C {
            // })
            (
                numberOfArgs == 1 &&
                    firstArg?.firstChildNode?.elementType
                    ?.let { it == OBJECT_LITERAL || it == LAMBDA_EXPRESSION } == true
                )
        ) {
            return
        }
        if (!node.nextCodeLeaf()?.prevLeaf {
            // Skip comments, whitespace, and empty nodes
            !it.isPartOfComment() &&
                !it.isWhiteSpaceWithoutNewline() &&
                it.textLength > 0
        }.isWhiteSpaceWithNewline() &&
            // IDEA quirk:
            // if (true &&
            //     true
            // ) {
            // }
            // instead of
            // if (
            //     true &&
            //     true
            // ) {
            // }
            node.treeNext?.elementType != CONDITION
        ) {
            requireNewlineAfterLeaf(node, autoCorrect, emit)
        }
        if (!r.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineBeforeLeaf(r, autoCorrect, emit)
        }
    }

    private fun rearrangeSuperTypeList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val entries = (node.psi as KtSuperTypeList).entries
        if (
            node.textContains('\n') &&
            entries.size > 1 &&
            // e.g.
            //
            // class A : B, C,
            //     D
            // or
            // class A : B, C({
            // }), D
            //
            // but not
            //
            // class A : B, C, D({
            // })
            !(
                entries.head().all { it.elementType == SUPER_TYPE_ENTRY } &&
                    entries.last().elementType == SUPER_TYPE_CALL_ENTRY
                )
        ) {
            // put space after :
            if (!node.prevLeaf().isWhiteSpaceWithNewline()) {
                val colon = node.prevCodeLeaf()!!
                if (
                    !colon.prevLeaf().isWhiteSpaceWithNewline() &&
                    colon.prevCodeLeaf().let { it?.elementType != RPAR || !it.prevLeaf().isWhiteSpaceWithNewline() }
                ) {
                    requireNewlineAfterLeaf(colon, autoCorrect, emit)
                }
            }
            // put entries on separate lines
            // TODO: group emit()s below with the one above into one (similar to ParameterListWrappingRule)
            for (c in node.children()) {
                if (c.elementType == COMMA && !c.treeNext.isWhiteSpaceWithNewline()) {
                    requireNewlineAfterLeaf(c, autoCorrect, emit)
                }
            }
        }
    }

    private fun rearrangeValueList(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        for (c in node.children()) {
            val hasLineBreak = when (c.elementType) {
                VALUE_ARGUMENT -> c.hasLineBreak(LAMBDA_EXPRESSION, FUN)
                VALUE_PARAMETER, ANNOTATION -> c.hasLineBreak()
                else -> false
            }
            if (hasLineBreak) {
                // rearrange
                //
                // a, b, value(
                // ), c, d
                //
                // to
                //
                // a, b,
                // value(
                // ),
                // c, d

                // insert \n in front of multi-line value
                val prevSibling = c.prevSibling { it.elementType != WHITE_SPACE }
                if (
                    prevSibling?.elementType == COMMA &&
                    !prevSibling.treeNext.isWhiteSpaceWithNewline()
                ) {
                    requireNewlineAfterLeaf(prevSibling, autoCorrect, emit)
                }
                // insert \n after multi-line value
                val nextSibling = c.nextSibling { it.elementType != WHITE_SPACE }
                if (
                    nextSibling?.elementType == COMMA &&
                    !nextSibling.treeNext.isWhiteSpaceWithNewline() &&
                    // value(
                    // ), // a comment
                    // c, d
                    nextSibling.treeNext?.treeNext?.psi !is PsiComment
                ) {
                    requireNewlineAfterLeaf(nextSibling, autoCorrect, emit)
                }
            }
        }
    }

    private fun rearrangeClosingQuote(
        n: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val treeParent = n.treeParent
        if (treeParent.elementType == STRING_TEMPLATE) {
            val treeParentPsi = treeParent.psi as KtStringTemplateExpression
            if (treeParentPsi.isMultiLine() && n.treePrev.text.isNotBlank()) {
                // rewriting
                // """
                //     text
                // _""".trimIndent()
                // to
                // """
                //     text
                // _
                // """.trimIndent()
                emit(
                    n.startOffset,
                    "Missing newline before \"\"\"",
                    true
                )
                if (autoCorrect) {
                    n as LeafPsiElement
                    n.rawInsertBeforeMe(LeafPsiElement(REGULAR_STRING_PART, "\n"))
                }
                logger.trace { "$line: " + (if (!autoCorrect) "would have " else "") + "inserted newline before (closing) \"\"\"" }
            }
        }
    }

    private fun mustBeFollowedByNewline(node: ASTNode): Boolean {
        // find EOL token (last token before \n)
        // if token is in lTokenSet
        //     find matching rToken
        //     return true if there is no newline after the rToken
        // return false
        val p = node.treeParent
        val nextCodeSibling = node.nextCodeSibling() // e.g. BINARY_EXPRESSION
        var lToken = nextCodeSibling?.nextLeaf { it.isWhiteSpaceWithNewline() }?.prevCodeLeaf()
        if (lToken != null && lToken.elementType !in lTokenSet) {
            // special cases:
            // x = y.f({ z ->
            // })
            // x = y.f(0, 1,
            // 2, 3)
            lToken = lToken.prevLeaf { it.elementType in lTokenSet || it == node }
        }
        if (lToken != null && lToken.elementType in lTokenSet) {
            val rElementType = matchingRToken[lToken.elementType]
            val rToken = lToken.nextSibling { it.elementType == rElementType }
            return rToken?.treeParent == lToken.treeParent
        }
        if (nextCodeSibling?.textContains('\n') == false) {
            return true
        }
        return false
    }

    private fun rearrangeArrow(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val p = node.treeParent
        if (
            // check
            // `{ p -> ... }`
            // and
            // `when { m -> ... }`
            // only
            p.elementType.let { it != FUNCTION_LITERAL && it != WHEN_ENTRY } ||
            // ... and only if expression after -> spans multiple lines
            !p.textContains('\n') ||
            // permit
            // when {
            //     m -> 0 + d({
            //     })
            // }
            (p.elementType == WHEN_ENTRY && mustBeFollowedByNewline(node)) ||
            // permit
            // when (this) {
            //     in 0x1F600..0x1F64F, // Emoticons
            //     0x200D // Zero-width Joiner
            //     -> true
            // }
            (p.elementType == WHEN_ENTRY && node.prevLeaf()?.textContains('\n') == true)
        ) {
            return
        }
        if (!node.nextCodeLeaf()?.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineAfterLeaf(node, autoCorrect, emit)
        }
        val r = node.nextSibling { it.elementType == RBRACE } ?: return
        if (!r.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineBeforeLeaf(r, autoCorrect, emit)
        }
    }

    private fun requireNewlineBeforeLeaf(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        emit(
            node.startOffset - 1,
            """Missing newline before "${node.text}"""",
            true
        )
        logger.trace { "$line: " + ((if (!autoCorrect) "would have " else "") + "inserted newline before ${node.text}") }
        if (autoCorrect) {
            (node.psi as LeafPsiElement).upsertWhitespaceBeforeMe("\n ")
        }
    }

    private fun requireNewlineAfterLeaf(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        emit(
            node.startOffset + 1,
            """Missing newline after "${node.text}"""",
            true
        )
        logger.trace { "$line: " + (if (!autoCorrect) "would have " else "") + "inserted newline after ${node.text}" }
        if (autoCorrect) {
            (node.psi as LeafPsiElement).upsertWhitespaceAfterMe("\n ")
        }
    }

    private class IndentContext {
        private val exitAdj = mutableMapOf<ASTNode, Int>()
        val ignored = mutableSetOf<ASTNode>()
        val blockOpeningLineStack: Deque<Int> = LinkedList()
        var localAdj: Int = 0
        fun exitAdjBy(node: ASTNode, change: Int) {
            exitAdj.compute(node) { _, v -> (v ?: 0) + change }
        }
        fun clearExitAdj(node: ASTNode): Int? =
            exitAdj.remove(node)
    }

    private fun indent(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        editorConfig: EditorConfig
    ) {
        val firstNotEmptyLeaf = node.nextLeaf()
        if (firstNotEmptyLeaf?.let { it.elementType == WHITE_SPACE && !it.textContains('\n') } == true) {
            visitWhiteSpace(firstNotEmptyLeaf, autoCorrect, emit, editorConfig)
        }
        val ctx = IndentContext()
        node.visit(
            { n ->
                when (n.elementType) {
                    LPAR, LBRACE, LBRACKET -> {
                        // ({[ should increase expectedIndent by 1
                        val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
                        val leftBrace = n.takeIf { it.elementType == LBRACE }
                        if (prevBlockLine != line && !leftBrace.isAfterLambdaArgumentOnSameLine()) {
                            expectedIndent++
                            logger.trace { "$line: ++${n.text} -> $expectedIndent" }
                        }
                        ctx.blockOpeningLineStack.push(line)
                    }
                    RPAR, RBRACE, RBRACKET -> {
                        // ]}) should decrease expectedIndent by 1
                        val blockLine = ctx.blockOpeningLineStack.pop()
                        val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
                        val pairedLeft = n.pairedLeft()
                        if (prevBlockLine != blockLine && !pairedLeft.isAfterLambdaArgumentOnSameLine()) {
                            expectedIndent--
                            logger.trace { "$line: --on(${n.elementType}) -> $expectedIndent" }

                            val byKeywordOnSameLine = pairedLeft?.prevLeafOnSameLine(BY_KEYWORD)
                            if (byKeywordOnSameLine != null &&
                                byKeywordOnSameLine.prevLeaf()?.isWhiteSpaceWithNewline() == true &&
                                n.leavesOnSameLine(forward = true).all { it.isWhiteSpace() || it.isPartOfComment() }
                            ) {
                                expectedIndent--
                                logger.trace { "$line: --on same line as by keyword ${n.text} -> $expectedIndent" }
                            }
                        }
                    }
                    LT ->
                        // <T>
                        if (n.treeParent.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
                            expectedIndent++
                            logger.trace { "$line: ++${n.text} -> $expectedIndent" }
                        }
                    GT ->
                        // <T>
                        if (n.treeParent.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
                            expectedIndent--
                            logger.trace { "$line: --${n.text} -> $expectedIndent" }
                        }
                    SUPER_TYPE_LIST ->
                        // class A :
                        //     SUPER_TYPE_LIST
                        adjustExpectedIndentInsideSuperTypeList(n)
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
                        adjustExpectedIndentInsideSuperTypeCall(n, ctx)
                    }
                    STRING_TEMPLATE ->
                        indentStringTemplate(n, autoCorrect, emit, editorConfig)
                    DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION, BINARY_EXPRESSION, BINARY_WITH_TYPE -> {
                        val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
                        if (prevBlockLine == line) {
                            ctx.ignored.add(n)
                        }
                    }
                    FUNCTION_LITERAL ->
                        adjustExpectedIndentInFunctionLiteral(n, ctx)
                    WHITE_SPACE ->
                        if (n.textContains('\n')) {
                            if (
                                !n.isPartOfComment() &&
                                !n.isPartOfTypeConstraint() // FIXME IndentationRuleTest.testLintWhereClause not checked
                            ) {
                                val p = n.treeParent
                                val nextSibling = n.treeNext
                                val prevLeaf = n.prevLeaf { !it.isPartOfComment() && !it.isWhiteSpaceWithoutNewline() }
                                when {
                                    p.elementType.let {
                                        it == DOT_QUALIFIED_EXPRESSION || it == SAFE_ACCESS_EXPRESSION
                                    } ->
                                        // value
                                        //     .x()
                                        //     .y
                                        adjustExpectedIndentInsideQualifiedExpression(n, ctx)
                                    p.elementType.let {
                                        it == BINARY_EXPRESSION || it == BINARY_WITH_TYPE
                                    } ->
                                        // value
                                        //     + x()
                                        //     + y
                                        adjustExpectedIndentInsideBinaryExpression(n, ctx)
                                    nextSibling?.elementType.let {
                                        it == THEN || it == ELSE || it == BODY
                                    } ->
                                        // if (...)
                                        //     THEN
                                        // else
                                        //     ELSE
                                        // while (...)
                                        //     BODY
                                        adjustExpectedIndentInFrontOfControlBlock(n, ctx)
                                    nextSibling?.elementType == PROPERTY_ACCESSOR ->
                                        // val f: Type =
                                        //     PROPERTY_ACCESSOR get() = ...
                                        //     PROPERTY_ACCESSOR set() = ...
                                        adjustExpectedIndentInFrontOfPropertyAccessor(n, ctx)
                                    nextSibling?.elementType == SUPER_TYPE_LIST ->
                                        // class C :
                                        //     SUPER_TYPE_LIST
                                        adjustExpectedIndentInFrontOfSuperTypeList(n, ctx)
                                    prevLeaf?.elementType == EQ && p.elementType != VALUE_ARGUMENT ->
                                        // v =
                                        //     value
                                        adjustExpectedIndentAfterEq(n, ctx)
                                    prevLeaf?.elementType == ARROW ->
                                        // when {
                                        //    v ->
                                        //        value
                                        // }
                                        adjustExpectedIndentAfterArrow(n, ctx)
                                    prevLeaf?.elementType == COLON ->
                                        // fun fn():
                                        //     Int
                                        adjustExpectedIndentAfterColon(n, ctx)
                                    prevLeaf?.elementType == LPAR &&
                                        p.elementType == VALUE_ARGUMENT_LIST &&
                                        p.parent(CONDITION)?.takeIf { !it.prevLeaf().isWhiteSpaceWithNewline() } != null ->
                                        // if (condition(
                                        //         params
                                        //     )
                                        // )
                                        adjustExpectedIndentAfterLparInsideCondition(n, ctx)
                                }
                                visitWhiteSpace(n, autoCorrect, emit, editorConfig)
                                if (ctx.localAdj != 0) {
                                    expectedIndent += ctx.localAdj
                                    logger.trace { "$line: ++${ctx.localAdj} on whitespace containing new line (${n.elementType}) -> $expectedIndent" }
                                    ctx.localAdj = 0
                                }
                            } else if (n.isPartOf(KDOC)) {
                                visitWhiteSpace(n, autoCorrect, emit, editorConfig)
                            }
                            line += n.text.count { it == '\n' }
                        }
                    EOL_COMMENT ->
                        if (n.text == "// ktlint-debug-print-expected-indent") {
                            logger.trace { "$line: expected indent: $expectedIndent" }
                        }
                }
            },
            { n ->
                when (n.elementType) {
                    SUPER_TYPE_LIST ->
                        adjustExpectedIndentAfterSuperTypeList(n)
                    DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION, BINARY_EXPRESSION, BINARY_WITH_TYPE ->
                        ctx.ignored.remove(n)
                }
                val adj = ctx.clearExitAdj(n)
                if (adj != null) {
                    expectedIndent += adj
                    logger.trace { "$line: adjusted ${n.elementType} by $adj -> $expectedIndent" }
                }
            }
        )
    }

    private fun adjustExpectedIndentInsideQualifiedExpression(n: ASTNode, ctx: IndentContext) {
        val p = n.parent({
            it.treeParent.elementType != DOT_QUALIFIED_EXPRESSION && it.treeParent.elementType != SAFE_ACCESS_EXPRESSION
        }) ?: return
        val nextSibling = n.treeNext
        if (!ctx.ignored.contains(p) && nextSibling != null) {
            expectedIndent++
            logger.trace { "$line: ++inside(${p.elementType}) -> $expectedIndent" }
            ctx.ignored.add(p)
            ctx.exitAdjBy(p, -1)
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
            val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
            if (prevBlockLine != line) {
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
        editorConfig: EditorConfig
    ) {
        val psi = node.psi as KtStringTemplateExpression
        if (psi.isMultiLine()) {
            if (node.containsMixedIndentationCharacters()) {
                // It can not be determined with certainty how mixed indentation characters should be interpreted.
                // The trimIndent function handles tabs and spaces equally (one tabs equals one space) while the user
                // might expect that the tab size in the indentation is more than one space.
                emit(
                    node.startOffset,
                    "Indentation of multiline string should not contain both tab(s) and space(s)",
                    false
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
                .min() ?: 0

            val correctedExpectedIndent = if (node.prevLeaf()?.text == "\n") {
                // In case the opening quotes are placed at the start of the line, then expect all lines inside the
                // string literal and the closing quotes to have no indent as well.
                0
            } else {
                expectedIndent
            }
            val expectedIndentation = editorConfig.repeatIndent(correctedExpectedIndent)
            val expectedPrefixLength = correctedExpectedIndent * editorConfig.indentSize
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
                        val (wrongIndentChar, wrongIndentDescription) = editorConfig.wrongIndentChar()
                        if (actualIndent.contains(wrongIndentChar)) {
                            val offsetFirstWrongIndentChar = actualIndent.indexOfFirst(wrongIndentChar)
                            emit(
                                it.startOffset + offsetFirstWrongIndentChar,
                                "Unexpected '$wrongIndentDescription' character(s) in margin of multiline string",
                                true
                            )
                            if (autoCorrect) {
                                (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                    expectedIndentation + actualContent
                                )
                            }
                        } else if (actualIndent != expectedIndentation && it.isIndentBeforeClosingQuote()) {
                            // It is a deliberate choice not to fix the indents inside the string literal except the line which only contains
                            // the closing quotes.
                            emit(
                                it.startOffset,
                                "Unexpected indent of multiline string closing quotes",
                                true
                            )
                            if (autoCorrect) {
                                if (it.firstChildNode == null) {
                                    (it as LeafPsiElement).rawInsertBeforeMe(
                                        LeafPsiElement(REGULAR_STRING_PART, expectedIndentation)
                                    )
                                } else {
                                    (it.firstChildNode as LeafPsiElement).rawReplaceWithText(
                                        expectedIndentation + actualContent
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
        editorConfig: EditorConfig
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
            nextLeafElementType in rTokenSet -> -1
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
            node.nextCodeSibling()?.elementType == PROPERTY_ACCESSOR && node.treeParent.findChildByType(EQ)?.nextLeaf().isWhiteSpaceWithNewline() -> -1
            else -> 0
        }
        // indentation with incorrect characters replaced
        val normalizedNodeIndent =
            when (editorConfig.indentStyle) {
                IndentStyle.SPACE -> {
                    if ('\t' in nodeIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected tab character(s)",
                            true
                        )
                        nodeIndent.replace("\t", " ".repeat(editorConfig.tabWidth))
                    } else {
                        nodeIndent
                    }
                }
                IndentStyle.TAB -> {
                    val isKdocIndent = node.isKDocIndent()
                    val indentWithoutKdocIndent =
                        if (isKdocIndent) {
                            nodeIndent.removeSuffix(" ")
                        } else {
                            nodeIndent
                        }
                    if (' ' in indentWithoutKdocIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected space character(s)",
                            true
                        )
                        // First normalize the indent to spaces using the tab width.
                        val asSpaces = nodeIndent.replace("\t", " ".repeat(editorConfig.tabWidth))
                        // Then divide that space-based indent into tabs.
                        "\t".repeat(asSpaces.length / editorConfig.tabWidth) +
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
        val indentLength =
            when (editorConfig.indentStyle) {
                IndentStyle.SPACE -> editorConfig.indentSize
                IndentStyle.TAB -> 1
            }
        val expectedIndentLength =
            adjustedExpectedIndent * indentLength +
                // +1 space before * in `/**\n *\n */`
                if (comment?.elementType == KDOC && nextLeafElementType != KDOC_START) 1 else 0
        if (normalizedNodeIndent.length != expectedIndentLength) {
            emit(
                node.startOffset + text.length - nodeIndent.length,
                "Unexpected indentation (${normalizedNodeIndent.length}) (should be $expectedIndentLength)",
                true
            )
            logger.trace {
                "$line: " + (if (!autoCorrect) "would have " else "") + "changed indentation to $expectedIndentLength (from ${normalizedNodeIndent.length})"
            }
        }
        if (autoCorrect) {
            if (nodeIndent != normalizedNodeIndent || normalizedNodeIndent.length != expectedIndentLength) {
                val indent = when (editorConfig.indentStyle) {
                    IndentStyle.SPACE -> " "
                    IndentStyle.TAB -> "\t"
                }
                (node as LeafPsiElement).rawReplaceWithText(
                    text.substringBeforeLast("\n") + "\n" +
                        indent.repeat(expectedIndentLength)
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

    private fun ASTNode.pairedLeft(): ASTNode? {
        val rightType = elementType
        val leftType = when (rightType) {
            RPAR -> LPAR
            RBRACE -> LBRACE
            RBRACKET -> LBRACKET
            else -> return null
        }
        var node: ASTNode? = prevLeaf()
        while (node != null) {
            node = when (node.elementType) {
                leftType -> return node
                rightType -> node.treeParent
                else -> node.prevLeaf()
            }
        }
        return null
    }

    private fun ASTNode.leavesOnSameLine(forward: Boolean): Sequence<ASTNode> =
        leaves(forward = forward).takeWhile { !it.isWhiteSpaceWithNewline() }

    private fun ASTNode.prevLeafOnSameLine(prevLeafType: IElementType): ASTNode? =
        leavesOnSameLine(forward = false).firstOrNull { it.elementType == prevLeafType }

    private fun ASTNode?.isAfterLambdaArgumentOnSameLine(): Boolean {
        if (this == null) return false
        val prevComma = prevLeafOnSameLine(RBRACE)?.nextCodeLeaf()?.takeIf { it.elementType == COMMA } ?: return false
        return prevComma.parent(VALUE_ARGUMENT_LIST) == parent(VALUE_ARGUMENT_LIST)
    }

    private fun ASTNode.hasLineBreak(vararg ignoreElementTypes: IElementType): Boolean {
        if (isWhiteSpaceWithNewline()) return true
        return if (ignoreElementTypes.isEmpty()) {
            textContains('\n')
        } else {
            elementType !in ignoreElementTypes &&
                children().any { c -> c.textContains('\n') && c.elementType !in ignoreElementTypes }
        }
    }

    private fun ASTNode.containsMixedIndentationCharacters(): Boolean {
        assert((this.psi as KtStringTemplateExpression).isMultiLine())
        val nonBlankLines = this
            .text
            .split("\n")
            .filterNot { it.startsWith("\"\"\"") }
            .filterNot { it.endsWith("\"\"\"") }
            .filterNot { it.isBlank() }
        val prefixLength = nonBlankLines
            .map { it.indentLength() }
            .min() ?: 0
        val distinctIndentCharacters = nonBlankLines
            .joinToString(separator = "") {
                it.splitIndentAt(prefixLength).first
            }
            .toCharArray()
            .distinct()
            .count()
        return distinctIndentCharacters > 1
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

private fun EditorConfig.repeatIndent(indentLevel: Int) =
    when (indentStyle) {
        IndentStyle.SPACE -> " ".repeat(indentLevel * indentSize)
        IndentStyle.TAB -> "\t".repeat(indentLevel)
    }

private fun EditorConfig.wrongIndentChar(): Pair<Char, String> =
    when (indentStyle) {
        IndentStyle.SPACE -> Pair('\t', "tab")
        IndentStyle.TAB -> Pair(' ', "space")
    }

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
        second = this.substring(safeIndex)
    )
}

private fun String.indexOfFirst(char: Char) =
    indexOfFirst { it == char }
