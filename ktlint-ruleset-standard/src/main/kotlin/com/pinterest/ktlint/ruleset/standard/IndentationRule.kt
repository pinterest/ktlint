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
import com.pinterest.ktlint.core.ast.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.CLOSING_QUOTE
import com.pinterest.ktlint.core.ast.ElementType.COLON
import com.pinterest.ktlint.core.ast.ElementType.COMMA
import com.pinterest.ktlint.core.ast.ElementType.CONDITION
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.core.ast.ElementType.ELSE
import com.pinterest.ktlint.core.ast.ElementType.ELVIS
import com.pinterest.ktlint.core.ast.ElementType.EOL_COMMENT
import com.pinterest.ktlint.core.ast.ElementType.EQ
import com.pinterest.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.pinterest.ktlint.core.ast.ElementType.GT
import com.pinterest.ktlint.core.ast.ElementType.KDOC
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
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
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
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithNewline
import com.pinterest.ktlint.core.ast.isWhiteSpaceWithoutNewline
import com.pinterest.ktlint.core.ast.nextCodeLeaf
import com.pinterest.ktlint.core.ast.nextCodeSibling
import com.pinterest.ktlint.core.ast.nextLeaf
import com.pinterest.ktlint.core.ast.nextSibling
import com.pinterest.ktlint.core.ast.parent
import com.pinterest.ktlint.core.ast.prevCodeLeaf
import com.pinterest.ktlint.core.ast.prevLeaf
import com.pinterest.ktlint.core.ast.prevSibling
import com.pinterest.ktlint.core.ast.upsertWhitespaceAfterMe
import com.pinterest.ktlint.core.ast.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.core.ast.visit
import java.lang.StringBuilder
import java.util.Deque
import java.util.LinkedList
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiComment
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperTypeList

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
class IndentationRule : Rule("indent"), Rule.Modifier.RestrictToRootLast {

    private companion object {
        // run `KTLINT_DEBUG=experimental/indent ktlint ...` to enable debug output
        private val debugMode =
            (System.getenv("KTLINT_DEBUG") ?: "").split(",").contains("experimental/indent")

        private inline fun debug(msg: () -> String) {
            if (debugMode) {
                System.err.println("[DEBUG] indent: ${msg()}")
            }
        }

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

    private inline fun debug(msg: () -> String) {
        Companion.debug { "$line: " + msg() }
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
        Companion.debug { "phase: rearrangement (auto correction ${if (autoCorrect) "on" else "off"})" }
        // step 1: insert newlines (if/where needed)
        var emitted = false
        rearrange(node, autoCorrect) { offset, errorMessage, canBeAutoCorrected ->
            emitted = true
            emit(offset, errorMessage, canBeAutoCorrected)
        }
        if (emitted && autoCorrect) {
            Companion.debug {
                "indenting:\n" +
                    node.text.split("\n").mapIndexed { i, v -> "\t${i + 1}: $v" }.joinToString("\n")
            }
        }
        reset()
        Companion.debug { "phase: indentation" }
        // step 2: correct indentation
        indent(node, autoCorrect, emit, editorConfig)
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
            newlineInBetween = newlineInBetween || it.textContains('\n')
            parameterListInBetween = parameterListInBetween || it.elementType == VALUE_PARAMETER_LIST
            if (it.elementType == VALUE_ARGUMENT) {
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
            if (
                (c.elementType == VALUE_PARAMETER || c.elementType == VALUE_ARGUMENT || c.elementType == ANNOTATION) &&
                c.textContains('\n')
            ) {
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
            val lf = rToken?.nextLeaf { it.isWhiteSpaceWithNewline() }
            return lf?.parent({ it == p }) == null
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
        debug { (if (!autoCorrect) "would have " else "") + "inserted newline before ${node.text}" }
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
        debug { (if (!autoCorrect) "would have " else "") + "inserted newline after ${node.text}" }
        if (autoCorrect) {
            (node.psi as LeafPsiElement).upsertWhitespaceAfterMe("\n ")
        }
    }

    private class IndentContext {
        private val exitAdj = mutableMapOf<ASTNode, Int>()
        val ignored = mutableSetOf<ASTNode>()
        val blockOpeningLineStack: Deque<Int> = LinkedList<Int>()
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
                        if (prevBlockLine != line) {
                            expectedIndent++
                            debug { "++${n.text} -> $expectedIndent" }
                        }
                        ctx.blockOpeningLineStack.push(line)
                    }
                    RPAR, RBRACE, RBRACKET -> {
                        // ]}) should decrease expectedIndent by 1
                        val blockLine = ctx.blockOpeningLineStack.pop()
                        val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
                        if (prevBlockLine != blockLine) {
                            expectedIndent--
                            debug { "--${n.text} -> $expectedIndent" }
                        }
                    }
                    LT ->
                        // <T>
                        if (n.treeParent.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
                            expectedIndent++
                            debug { "++${n.text} -> $expectedIndent" }
                        }
                    GT ->
                        // <T>
                        if (n.treeParent.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST }) {
                            expectedIndent--
                            debug { "--${n.text} -> $expectedIndent" }
                        }
                    SUPER_TYPE_LIST ->
                        // class A :
                        //     SUPER_TYPE_LIST
                        adjustExpectedIndentInsideSuperTypeList(n)
                    SUPER_TYPE_CALL_ENTRY -> {
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
                                    prevLeaf?.elementType == EQ ->
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
                                        p.isPartOf(CONDITION) ->
                                        // if (condition(
                                        //         params
                                        //     )
                                        // )
                                        adjustExpectedIndentAfterLparInsideCondition(n, ctx)
                                }
                                visitWhiteSpace(n, autoCorrect, emit, editorConfig)
                                if (ctx.localAdj != 0) {
                                    expectedIndent += ctx.localAdj
                                    ctx.localAdj = 0
                                }
                            } else if (n.isPartOf(KDOC)) {
                                visitWhiteSpace(n, autoCorrect, emit, editorConfig)
                            }
                            line += n.text.count { it == '\n' }
                        }
                    EOL_COMMENT ->
                        if (debugMode && n.text == "// ktlint-debug-print-expected-indent") {
                            debug { "expected indent: $expectedIndent" }
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
                    debug { "adjusted ${n.elementType} by $adj -> $expectedIndent" }
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
            debug { "++inside(${p.elementType}) -> $expectedIndent" }
            val siblingType = nextSibling.elementType
            val e = if (
                siblingType == DOT ||
                siblingType == SAFE_ACCESS ||
                siblingType == ELVIS
            ) {
                nextSibling.treeNext
            } else {
                nextSibling
            }
            ctx.ignored.add(p)
            ctx.exitAdjBy(p, -1)
        }
    }

    private fun adjustExpectedIndentInsideBinaryExpression(n: ASTNode, ctx: IndentContext) {
        if (!n.isPartOfCondition()) {
            val p = n.treeParent
            if (!ctx.ignored.contains(p)) {
                expectedIndent++
                debug { "++inside(${p.elementType}) -> $expectedIndent" }
                val rOperand = n.nextSibling { sibling ->
                    sibling.elementType != OPERATION_REFERENCE &&
                        sibling.elementType != WHITE_SPACE
                }!!
                ctx.exitAdjBy(rOperand, -1)
            }
            val nextSibling = n.treeNext
            if (
                nextSibling?.elementType.let {
                    it == BINARY_EXPRESSION || it == BINARY_WITH_TYPE
                }
            ) {
                ctx.localAdj = -1
                debug { "--inside(${nextSibling.elementType}) -> $expectedIndent" }
                ctx.exitAdjBy(p, 1)
            }
        }
    }

    private fun adjustExpectedIndentInFrontOfControlBlock(n: ASTNode, ctx: IndentContext) {
        val nextSibling = n.treeNext
        expectedIndent++
        debug { "++in_front(${nextSibling.elementType}) -> $expectedIndent" }
        ctx.exitAdjBy(nextSibling, -1)
    }

    private fun adjustExpectedIndentInFrontOfPropertyAccessor(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        debug { "++in_front(${n.treeNext.elementType}) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeNext, -1)
    }

    private fun adjustExpectedIndentInFrontOfSuperTypeList(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        debug { "++in_front(${n.treeNext.elementType}) -> $expectedIndent" }
        ctx.localAdj = -1
    }

    private fun adjustExpectedIndentInsideSuperTypeList(n: ASTNode) {
        expectedIndent++
        debug { "++inside(${n.elementType}) -> $expectedIndent" }
    }

    private fun adjustExpectedIndentAfterSuperTypeList(n: ASTNode) {
        expectedIndent--
        debug { "--after(${n.elementType}) -> $expectedIndent" }
    }

    private fun adjustExpectedIndentInsideSuperTypeCall(n: ASTNode, ctx: IndentContext) {
        // Don't adjust indents for initializer lists
        if (n.treeParent?.elementType != SUPER_TYPE_LIST) {
            return
        }

        if (
            // n.treePrev == null &&
            // n.treeParent.elementType == SUPER_TYPE_LIST &&
            n.prevLeaf()?.textContains('\n') == false
        ) {
            expectedIndent--
            debug { "--inside(${n.elementType}) -> $expectedIndent" }
            ctx.exitAdjBy(n, 1)
        }
    }

    private fun adjustExpectedIndentAfterEq(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        debug { "++after(EQ) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeParent, -1)
    }

    private fun adjustExpectedIndentAfterArrow(n: ASTNode, ctx: IndentContext) {
        // Only adjust indents for arrows inside of when statements. Lambda arrows should not increase indent.
        if (n.treeParent?.elementType == WHEN_ENTRY) {
            val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
            if (prevBlockLine != line) {
                expectedIndent++
                debug { "++after(ARROW) -> $expectedIndent" }
                ctx.exitAdjBy(n.treeParent, -1)
            }
        }
    }

    private fun adjustExpectedIndentAfterColon(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        debug { "++after(COLON) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeParent, -1)
    }

    private fun adjustExpectedIndentAfterLparInsideCondition(n: ASTNode, ctx: IndentContext) {
        expectedIndent++
        debug { "++inside(CONDITION) -> $expectedIndent" }
        ctx.exitAdjBy(n.treeParent, -1)
    }

    private fun indentStringTemplate(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        editorConfig: EditorConfig
    ) {
        val psi = node.psi as KtStringTemplateExpression
        if (psi.isMultiLine() && psi.isFollowedByTrimIndent()) {
            val children = node.children()
            val prefixLength =
                children
                    .fold(StringBuilder()) { sb, child ->
                        when (child.elementType) {
                            LITERAL_STRING_TEMPLATE_ENTRY -> {
                                val text = child.text
                                // bail if indentation contains Tab
                                for (c in text) {
                                    if (c == '\t') {
                                        return // bail
                                    }
                                    if (!c.isWhitespace()) {
                                        break
                                    }
                                }
                                sb.append(text)
                            }
                            LONG_STRING_TEMPLATE_ENTRY -> sb.append("${'$'}{}")
                            SHORT_STRING_TEMPLATE_ENTRY -> sb.append("${'$'}")
                            else -> sb
                        }
                    }
                    .split('\n')
                    .filter(String::isNotBlank)
                    .map { it.indentLength() }
                    .min() ?: 0
            val expectedPrefixLength = expectedIndent * editorConfig.indentSize
            // TODO: uncomment, once it's clear how to indent stuff within string templates
//            if (prefixLength != expectedPrefixLength) {
//                for (child in children) {
//                    if (child.isPrecededByLFStringTemplateEntry()) {
//                        when (child.elementType) {
//                            LITERAL_STRING_TEMPLATE_ENTRY -> {
//                                val v = child.text
//                                if (v != "\n") {
//                                    val indentLength = v.indentLength()
//                                    val expectedIndentLength = indentLength - prefixLength + expectedPrefixLength
//                                    if (indentLength != expectedIndentLength) {
//                                        reindentStringTemplateEntry(
//                                            child,
//                                            autoCorrect,
//                                            emit,
//                                            indentLength,
//                                            expectedIndentLength
//                                        )
//                                    }
//                                }
//                            }
//                            LONG_STRING_TEMPLATE_ENTRY, SHORT_STRING_TEMPLATE_ENTRY -> {
//                                if (expectedPrefixLength != 0) {
//                                    preindentStringTemplateEntry(
//                                        child.firstChildNode,
//                                        autoCorrect,
//                                        emit,
//                                        expectedPrefixLength
//                                    )
//                                }
//                            }
//                        }
//                    }
//                }
//            }
            val closingQuote = children.find { it.elementType == CLOSING_QUOTE }!!
            if (closingQuote.treePrev.text == "\n") {
                // rewriting
                // (
                //     """
                // """.trimIndent()
                // )
                // to
                // (
                //     """
                //     """.trimIndent()
                // )
                if (expectedPrefixLength != 0) {
                    preindentStringTemplateEntry(closingQuote, autoCorrect, emit, expectedPrefixLength)
                }
            } else if (!closingQuote.treePrev.text.isBlank()) {
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
                    closingQuote.startOffset,
                    "Missing newline before \"\"\"",
                    true
                )
                if (autoCorrect) {
                    closingQuote as LeafPsiElement
                    closingQuote.rawInsertBeforeMe(LeafPsiElement(REGULAR_STRING_PART, "\n"))
                    closingQuote.rawInsertBeforeMe(
                        LeafPsiElement(REGULAR_STRING_PART, " ".repeat(expectedPrefixLength))
                    )
                }
                debug {
                    (if (!autoCorrect) "would have " else "") +
                        "inserted newline before (closing) \"\"\""
                }
            } else { // preceded by blank LITERAL_STRING_TEMPLATE_ENTRY
                val child = closingQuote.treePrev
                val indentLength = child.text.length
                if (indentLength != expectedPrefixLength) {
                    reindentStringTemplateEntry(child, autoCorrect, emit, indentLength, expectedPrefixLength)
                }
            }
        }
    }

    private fun preindentStringTemplateEntry(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        expectedIndentLength: Int
    ) {
        emit(
            node.startOffset,
            "Unexpected indentation (0) (should be $expectedIndentLength)",
            true
        )
        if (autoCorrect) {
            (node as LeafPsiElement).rawInsertBeforeMe(
                LeafPsiElement(REGULAR_STRING_PART, " ".repeat(expectedIndentLength))
            )
        }
        debug {
            (if (!autoCorrect) "would have " else "") +
                "changed indentation before ${node.text} to $expectedIndentLength (from 0)"
        }
    }

    private fun reindentStringTemplateEntry(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        indentLength: Int,
        expectedIndentLength: Int
    ) {
        emit(
            node.startOffset,
            "Unexpected indentation ($indentLength) (should be $expectedIndentLength)",
            true
        )
        if (autoCorrect) {
            (node.firstChildNode as LeafPsiElement).rawReplaceWithText(
                " ".repeat(expectedIndentLength) + node.text.substring(indentLength)
            )
        }
        debug {
            (if (!autoCorrect) "would have " else "") +
                "changed indentation to $expectedIndentLength (from $indentLength)"
        }
    }

    private fun ASTNode.isPrecededByLFStringTemplateEntry() =
        treePrev?.let { it.elementType == LITERAL_STRING_TEMPLATE_ENTRY && it.text == "\n" } == true

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

    private fun KtStringTemplateExpression.isFollowedByTrimIndent() =
        this.node.nextSibling { it.elementType != DOT }
            .let { it?.elementType == CALL_EXPRESSION && it.text == "trimIndent()" }

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
                    if (' ' in nodeIndent) {
                        emit(
                            node.startOffset + text.length - nodeIndent.length,
                            "Unexpected space character(s)",
                            true
                        )
                        // First normalize the indent to spaces using the tab width.
                        val asSpaces = nodeIndent.replace("\t", " ".repeat(editorConfig.tabWidth))
                        // Then divide that space-based indent into tabs.
                        "\t".repeat(asSpaces.length / editorConfig.tabWidth)
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
            debug {
                (if (!autoCorrect) "would have " else "") +
                    "changed indentation to $expectedIndentLength (from ${normalizedNodeIndent.length})"
            }
            if (autoCorrect) {
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
}
