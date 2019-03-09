package com.github.shyiko.ktlint.ruleset.experimental

import com.github.shyiko.ktlint.core.EditorConfig
import com.github.shyiko.ktlint.core.KtLint
import com.github.shyiko.ktlint.core.Rule
import com.github.shyiko.ktlint.core.ast.ElementType.ARROW
import com.github.shyiko.ktlint.core.ast.ElementType.BINARY_EXPRESSION
import com.github.shyiko.ktlint.core.ast.ElementType.BINARY_WITH_TYPE
import com.github.shyiko.ktlint.core.ast.ElementType.BLOCK_COMMENT
import com.github.shyiko.ktlint.core.ast.ElementType.BODY
import com.github.shyiko.ktlint.core.ast.ElementType.COMMA
import com.github.shyiko.ktlint.core.ast.ElementType.CONDITION
import com.github.shyiko.ktlint.core.ast.ElementType.DOT
import com.github.shyiko.ktlint.core.ast.ElementType.DOT_QUALIFIED_EXPRESSION
import com.github.shyiko.ktlint.core.ast.ElementType.ELSE
import com.github.shyiko.ktlint.core.ast.ElementType.ELVIS
import com.github.shyiko.ktlint.core.ast.ElementType.EOL_COMMENT
import com.github.shyiko.ktlint.core.ast.ElementType.EQ
import com.github.shyiko.ktlint.core.ast.ElementType.FUNCTION_LITERAL
import com.github.shyiko.ktlint.core.ast.ElementType.GT
import com.github.shyiko.ktlint.core.ast.ElementType.KDOC
import com.github.shyiko.ktlint.core.ast.ElementType.KDOC_START
import com.github.shyiko.ktlint.core.ast.ElementType.LAMBDA_EXPRESSION
import com.github.shyiko.ktlint.core.ast.ElementType.LBRACE
import com.github.shyiko.ktlint.core.ast.ElementType.LBRACKET
import com.github.shyiko.ktlint.core.ast.ElementType.LPAR
import com.github.shyiko.ktlint.core.ast.ElementType.LT
import com.github.shyiko.ktlint.core.ast.ElementType.OBJECT_LITERAL
import com.github.shyiko.ktlint.core.ast.ElementType.OPEN_QUOTE
import com.github.shyiko.ktlint.core.ast.ElementType.OPERATION_REFERENCE
import com.github.shyiko.ktlint.core.ast.ElementType.PARENTHESIZED
import com.github.shyiko.ktlint.core.ast.ElementType.PROPERTY_ACCESSOR
import com.github.shyiko.ktlint.core.ast.ElementType.RBRACE
import com.github.shyiko.ktlint.core.ast.ElementType.RBRACKET
import com.github.shyiko.ktlint.core.ast.ElementType.RPAR
import com.github.shyiko.ktlint.core.ast.ElementType.SAFE_ACCESS
import com.github.shyiko.ktlint.core.ast.ElementType.SAFE_ACCESS_EXPRESSION
import com.github.shyiko.ktlint.core.ast.ElementType.SUPER_TYPE_CALL_ENTRY
import com.github.shyiko.ktlint.core.ast.ElementType.SUPER_TYPE_ENTRY
import com.github.shyiko.ktlint.core.ast.ElementType.SUPER_TYPE_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.THEN
import com.github.shyiko.ktlint.core.ast.ElementType.TYPE_ARGUMENT_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.TYPE_CONSTRAINT_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.TYPE_PARAMETER_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.VALUE_ARGUMENT
import com.github.shyiko.ktlint.core.ast.ElementType.VALUE_ARGUMENT_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.VALUE_PARAMETER
import com.github.shyiko.ktlint.core.ast.ElementType.VALUE_PARAMETER_LIST
import com.github.shyiko.ktlint.core.ast.ElementType.WHEN_ENTRY
import com.github.shyiko.ktlint.core.ast.ElementType.WHERE_KEYWORD
import com.github.shyiko.ktlint.core.ast.ElementType.WHITE_SPACE
import com.github.shyiko.ktlint.core.ast.children
import com.github.shyiko.ktlint.core.ast.comment
import com.github.shyiko.ktlint.core.ast.isPartOf
import com.github.shyiko.ktlint.core.ast.isPartOfComment
import com.github.shyiko.ktlint.core.ast.nextCodeLeaf
import com.github.shyiko.ktlint.core.ast.nextCodeSibling
import com.github.shyiko.ktlint.core.ast.nextLeaf
import com.github.shyiko.ktlint.core.ast.nextSibling
import com.github.shyiko.ktlint.core.ast.parent
import com.github.shyiko.ktlint.core.ast.prevCodeLeaf
import com.github.shyiko.ktlint.core.ast.prevLeaf
import com.github.shyiko.ktlint.core.ast.prevSibling
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceAfterMe
import com.github.shyiko.ktlint.core.ast.upsertWhitespaceBeforeMe
import com.github.shyiko.ktlint.core.ast.visit
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.psi.KtSuperTypeList
import java.util.Deque
import java.util.LinkedList

/**
 * ktlint's rule that checks & corrects indentation.
 *
 * To keep things simple, we walk the AST twice:
 * - 1st pass - insert missing newlines (e.g. between parentheses of a multi-line function call)
 * - 2st pass - correct indentation
 *
 * Current limitations:
 * - indent_style=tab not supported.
 * - "all or nothing" (currently, rule can only be disabled for an entire file)
 */
class IndentationRule : Rule("indent"), Rule.Modifier.RestrictToRoot {

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
            lTokenSet.types.zip(rTokenSet.types).toMap()
    }

    private var line = 1
    private var expectedIndent = 0

    private fun reset() {
        line = 1
        expectedIndent = 0
    }

    private inline fun debug(msg: () -> String) {
        IndentationRule.debug { "$line: " + msg() }
    }

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        val editorConfig = node.getUserData(KtLint.EDITOR_CONFIG_USER_DATA_KEY)!!
        if (editorConfig.indentStyle == EditorConfig.IntentStyle.TAB || editorConfig.indentSize <= 1) {
            return
        }
        reset()
        IndentationRule.debug { "auto-correction on: $autoCorrect" }
        // step 1: insert newlines (if/where needed)
        var emitted = false
        rearrange(node, autoCorrect) { offset, errorMessage, canBeAutoCorrected ->
            emitted = true
            emit(offset, errorMessage, canBeAutoCorrected)
        }
        if (!autoCorrect && emitted) {
            // stop if there are missing newlines
            // return FIXME
        }
        reset()
        IndentationRule.debug { "finished rearranging. indenting..." }
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
                EQ -> rearrangeEq(n, autoCorrect, emit)
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
        if (!node.nextCodeLeaf()?.prevLeaf().isWhiteSpaceWithNewline() &&
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
                if (colon.prevCodeLeaf().let { it?.elementType != RPAR || !it.prevLeaf().isWhiteSpaceWithNewline() }) {
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
                (c.elementType == VALUE_PARAMETER || c.elementType == VALUE_ARGUMENT) &&
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
                    !nextSibling.treeNext.isWhiteSpaceWithNewline()
                ) {
                    requireNewlineAfterLeaf(nextSibling, autoCorrect, emit)
                }
            }
        }
    }

    private fun rearrangeEq(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (
            !node.nextCodeSibling()?.elementType.let {
                it == DOT_QUALIFIED_EXPRESSION ||
                    it == SAFE_ACCESS_EXPRESSION ||
                    it == BINARY_EXPRESSION ||
                    it == BINARY_WITH_TYPE
            } ||
            !node.nextSubstringContains('\n') ||
            mustBeFollowedByNewline(node)
        ) {
            return
        }
        val nextCodeLeaf = node.nextCodeLeaf()!!
        // val v = (...
        if (nextCodeLeaf.elementType in lTokenSet) {
            return
        }
        if (!nextCodeLeaf.prevLeaf().isWhiteSpaceWithNewline()) {
            requireNewlineAfterLeaf(node, autoCorrect, emit)
        }
    }

    private fun ASTNode.nextSubstringContains(c: Char): Boolean {
        var n = this.treeNext
        while (n != null) {
            if (n.textContains(c)) {
                return true
            }
            n = n.treeNext
        }
        return false
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
            (p.elementType == WHEN_ENTRY && mustBeFollowedByNewline(node))
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
                    // TODO: test
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
                                !n.isPartOfTypeConstraint() // FIXME
                            ) {
                                val p = n.treeParent
                                val nextSibling = n.treeNext
                                val prevLeaf = n.prevLeaf()
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
                                }
                                visitWhiteSpace(n, autoCorrect, emit, editorConfig)
                                if (ctx.localAdj != 0) {
                                    expectedIndent += ctx.localAdj
                                    ctx.localAdj = 0
                                }
                            } else
                                if (n.isPartOf(KDOC)) {
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
        val p = n.treeParent
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
            ctx.exitAdjBy(e, -1)
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
        if (nextSibling.nextCodeLeaf()?.elementType !in lTokenSet) {
            expectedIndent++
            debug { "++in_front(${nextSibling.elementType}) -> $expectedIndent" }
            ctx.exitAdjBy(nextSibling, -1)
        }
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
        val prevBlockLine = ctx.blockOpeningLineStack.peek() ?: -1
        if (prevBlockLine != line) {
            expectedIndent++
            debug { "++after(ARROW) -> $expectedIndent" }
            ctx.exitAdjBy(n.treeParent, -1)
        }
    }

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
        val comment = nextLeaf?.comment()
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
            nextLeafElementType == GT &&
                node.treeParent?.elementType.let { it == TYPE_PARAMETER_LIST || it == TYPE_ARGUMENT_LIST } ->
                0
            nextLeafElementType in rTokenSet -> -1
            else -> 0
        }
        // indentation with all \t replaced
        val normalizedNodeIndent =
            if (nodeIndent.contains('\t')) {
                emit(
                    node.startOffset + text.length - nodeIndent.length,
                    "Unexpected Tab character(s)",
                    true
                )
                nodeIndent.replace("\t", " ".repeat(editorConfig.tabWidth))
            } else {
                nodeIndent
            }
        val expectedIndentLength =
            adjustedExpectedIndent * editorConfig.indentSize +
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
                (node as LeafPsiElement).rawReplaceWithText(
                    text.substringBeforeLast("\n") + "\n" +
                        " ".repeat(expectedIndentLength)
                )
            }
        }
    }

    private fun ASTNode?.isWhiteSpaceWithNewline() =
        this != null && elementType == WHITE_SPACE && textContains('\n')

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
