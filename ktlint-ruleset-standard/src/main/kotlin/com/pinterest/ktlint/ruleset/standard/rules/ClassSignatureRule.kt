package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLON
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONSTRUCTOR_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.EXPECT_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.RPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHITE_SPACE
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAsLateAsPossible
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CODE_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.CodeStyleValue.ktlint_official
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY_OFF
import com.pinterest.ktlint.rule.engine.core.api.hasModifier
import com.pinterest.ktlint.rule.engine.core.api.indent
import com.pinterest.ktlint.rule.engine.core.api.isPartOfComment
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextCodeLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevCodeSibling
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceAfterMe
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement

/**
 * Formats the class signature according to https://kotlinlang.org/docs/coding-conventions.html#class-headers
 *
 * In code style `ktlint_official` class headers containing 2 or more parameters are formatted as multiline signature. As the Kotlin Coding
 * conventions do not specify what is meant with a "few parameters", no default is set for other code styles.
 */
@SinceKtlint("1.0", EXPERIMENTAL)
public class ClassSignatureRule :
    StandardRule(
        id = "class-signature",
        visitorModifiers =
            setOf(
                // Disallow comments at unexpected locations in the type parameter list
                //     class Foo<in /** some comment */ Bar>
                RunAfterRule(TYPE_PARAMETER_COMMENT_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                // Disallow comments at unexpected locations in the value parameter list
                //     class Foo(
                //        bar /* some comment */: Bar
                //     )
                RunAfterRule(VALUE_PARAMETER_COMMENT_RULE_ID, REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED),
                // Run after wrapping and spacing rules
                RunAsLateAsPossible,
            ),
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
                MAX_LINE_LENGTH_PROPERTY,
                FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY,
            ),
    ),
    Rule.Experimental {
    private var codeStyle = CODE_STYLE_PROPERTY.defaultValue
    private var indentConfig = DEFAULT_INDENT_CONFIG
    private var maxLineLength = MAX_LINE_LENGTH_PROPERTY.defaultValue
    private var classSignatureWrappingMinimumParameters = FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        codeStyle = editorConfig[CODE_STYLE_PROPERTY]
        classSignatureWrappingMinimumParameters = editorConfig[FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY]
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
        maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == CLASS) {
            visitClass(node, emit, autoCorrect)
        }
    }

    private fun ASTNode.classSignatureNodes(excludeSuperTypes: Boolean): List<ASTNode> {
        // Find the nodes that are to be placed on the same line if no max line length is set
        //     internal class Foo(bar: String) { // Class without super type
        // or
        //     public class Foo(bar: String) : Bar { // Class with exactly one super type
        // or
        //     private class Foo(bar: String) : // Class with multiple super types which have to be wrapped to next lines
        val firstCodeChild = getFirstChildInSignature()
        val lastNodeInPrimaryClassSignatureLine =
            takeIf { excludeSuperTypes }
                // When the class extends multiple super types or if the super type list contains a newline, all super types are wrapped
                // on separate line
                ?.findChildByType(COLON)
                ?: findChildByType(CLASS_BODY)?.firstChildNode
        return collectLeavesRecursively()
            .childrenBetween(
                startASTNodePredicate = { it == firstCodeChild },
                endASTNodePredicate = { it == lastNodeInPrimaryClassSignatureLine },
            )
    }

    private fun ASTNode.superTypes() =
        findChildByType(SUPER_TYPE_LIST)
            ?.children()
            ?.filterNot { it.isWhiteSpace() || it.isPartOfComment() || it.elementType == COMMA }

    private fun ASTNode.hasMultilineSuperTypeList() = findChildByType(SUPER_TYPE_LIST)?.textContains('\n') == true

    private fun ASTNode.getFirstChildInSignature(): ASTNode? {
        findChildByType(MODIFIER_LIST)
            ?.let { modifierList ->
                val iterator = modifierList.children().iterator()
                var currentNode: ASTNode
                while (iterator.hasNext()) {
                    currentNode = iterator.next()
                    if (currentNode.elementType != ANNOTATION &&
                        currentNode.elementType != ANNOTATION_ENTRY &&
                        currentNode.elementType != WHITE_SPACE
                    ) {
                        return currentNode
                    }
                }
                return modifierList.nextCodeSibling()
            }
        return nextCodeLeaf()
    }

    private fun visitClass(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        require(node.elementType == CLASS)

        val wrapPrimaryConstructorParameters =
            node.hasTooManyParameters() ||
                node.containsMultilineParameter() ||
                (codeStyle == ktlint_official && node.containsAnnotatedParameter()) ||
                (isMaxLineLengthSet() && classSignatureExcludingSuperTypesExceedsMaxLineLength(node, emit, autoCorrect)) ||
                (!isMaxLineLengthSet() && node.classSignatureExcludingSuperTypesIsMultiline())
        fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = wrapPrimaryConstructorParameters, dryRun = false)
        fixWhitespacesInSuperTypeList(node, emit, autoCorrect, wrappedPrimaryConstructor = wrapPrimaryConstructorParameters)
        fixClassBody(node, emit, autoCorrect)
    }

    private fun classSignatureExcludingSuperTypesExceedsMaxLineLength(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ): Boolean {
        val actualClassSignatureLength = node.getClassSignatureLength(excludeSuperTypes = true)
        // Calculate the length of the class signature in case it, excluding the super types, would be rewritten as single
        // line (and without a maximum line length). The white space correction will be calculated via a dry run of the
        // actual fix.
        val length =
            actualClassSignatureLength +
                // Calculate the white space correction in case the signature would be rewritten to a single line
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = true)
        return length > maxLineLength
    }

    private fun ASTNode.classSignatureExcludingSuperTypesIsMultiline() =
        classSignatureNodes(excludeSuperTypes = true)
            .any { it.textContains('\n') }

    private fun ASTNode.getClassSignatureLength(excludeSuperTypes: Boolean) =
        indent(false).length + getClassSignatureNodesLength(excludeSuperTypes)

    private fun ASTNode.getClassSignatureNodesLength(excludeSuperTypes: Boolean) =
        classSignatureNodes(excludeSuperTypes)
            .joinTextToString()
            .length

    private fun ASTNode.containsMultilineParameter(): Boolean =
        getPrimaryConstructorParameterListOrNull()
            ?.children()
            .orEmpty()
            .filter { it.elementType == VALUE_PARAMETER }
            .any { it.textContains('\n') }

    private fun ASTNode.containsAnnotatedParameter(): Boolean =
        getPrimaryConstructorParameterListOrNull()
            ?.children()
            .orEmpty()
            .filter { it.elementType == VALUE_PARAMETER }
            .any { it.isAnnotated() }

    private fun ASTNode.isAnnotated() =
        findChildByType(MODIFIER_LIST)
            ?.children()
            .orEmpty()
            .any { it.elementType == ANNOTATION_ENTRY }

    private fun fixWhiteSpacesInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val primaryConstructorParameterList = node.getPrimaryConstructorParameterListOrNull()
        val hasNoValueParameters =
            primaryConstructorParameterList
                ?.children()
                .orEmpty()
                .none { it.elementType == VALUE_PARAMETER }

        whiteSpaceCorrection +=
            if (hasNoValueParameters) {
                fixWhiteSpacesInEmptyValueParameterList(node, emit, autoCorrect, dryRun)
            } else {
                fixWhiteSpacesBeforeFirstParameterInValueParameterList(node, emit, autoCorrect, multiline, dryRun) +
                    fixWhiteSpacesBetweenParametersInValueParameterList(node, emit, autoCorrect, multiline, dryRun) +
                    fixWhiteSpaceBeforeClosingParenthesis(node, emit, autoCorrect, multiline, dryRun)
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesInEmptyValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        node
            .takeUnless {
                // https://kotlinlang.org/docs/multiplatform-expect-actual.html#rules-for-expected-and-actual-declarations
                it.hasModifier(EXPECT_KEYWORD)
            }?.getPrimaryConstructorParameterListOrNull()
            ?.takeUnless { it.containsComment() }
            ?.takeUnless {
                // Allow:
                //     class Foo constructor() { ... }
                it.prevCodeSibling()?.elementType == CONSTRUCTOR_KEYWORD
            }?.let { parameterList ->
                if (!dryRun) {
                    emit(parameterList.startOffset, "No parenthesis expected", true)
                }
                if (autoCorrect && !dryRun) {
                    parameterList.treeParent.removeChild(parameterList)
                } else {
                    whiteSpaceCorrection -= parameterList.textLength
                }
            }

        return whiteSpaceCorrection
    }

    private fun ASTNode.containsComment() = children().any { it.isPartOfComment() }

    private fun fixWhiteSpacesBeforeFirstParameterInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = node.getPrimaryConstructorParameterListOrNull()
        val firstParameterInList =
            valueParameterList
                ?.children()
                ?.first { it.elementType == VALUE_PARAMETER }
                ?: return 0

        val firstParameter = firstParameterInList.firstChildNode
        firstParameter
            ?.prevLeaf()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceBeforeIdentifier ->
                if (multiline) {
                    if (whiteSpaceBeforeIdentifier == null ||
                        !whiteSpaceBeforeIdentifier.textContains('\n')
                    ) {
                        if (!dryRun) {
                            emit(firstParameterInList.startOffset, "Newline expected after opening parenthesis", true)
                        }
                        // Let indent rule determine the exact indent
                        val expectedParameterIndent = indentConfig.childIndentOf(node)
                        if (autoCorrect && !dryRun) {
                            valueParameterList.firstChildNode.upsertWhitespaceAfterMe(expectedParameterIndent)
                        } else {
                            whiteSpaceCorrection += expectedParameterIndent.length - (whiteSpaceBeforeIdentifier?.textLength ?: 0)
                        }
                    }
                } else {
                    if (whiteSpaceBeforeIdentifier != null) {
                        if (!dryRun) {
                            emit(
                                firstParameter!!.startOffset,
                                "No whitespace expected between opening parenthesis and first parameter name",
                                true,
                            )
                        }
                        if (autoCorrect && !dryRun) {
                            whiteSpaceBeforeIdentifier.treeParent.removeChild(whiteSpaceBeforeIdentifier)
                        } else {
                            whiteSpaceCorrection -= whiteSpaceBeforeIdentifier.textLength
                        }
                    }
                }
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpacesBetweenParametersInValueParameterList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val valueParameterList = node.getPrimaryConstructorParameterListOrNull()
        val firstParameterInList =
            valueParameterList
                ?.children()
                ?.first { it.elementType == VALUE_PARAMETER }
                ?: return 0

        valueParameterList
            .children()
            .filter { it.elementType == VALUE_PARAMETER }
            .filter { it != firstParameterInList }
            .forEach { valueParameter ->
                val firstChildNodeInValueParameter = valueParameter.firstChildNode
                firstChildNodeInValueParameter
                    ?.prevLeaf()
                    ?.takeIf { it.elementType == WHITE_SPACE }
                    .let { whiteSpaceBeforeIdentifier ->
                        if (multiline) {
                            if (whiteSpaceBeforeIdentifier == null ||
                                !whiteSpaceBeforeIdentifier.textContains('\n')
                            ) {
                                if (!dryRun) {
                                    emit(valueParameter.startOffset, "Parameter should start on a newline", true)
                                }
                                // Let IndentationRule determine the exact indent
                                val expectedParameterIndent = indentConfig.childIndentOf(node)
                                if (autoCorrect && !dryRun) {
                                    firstChildNodeInValueParameter.upsertWhitespaceBeforeMe(expectedParameterIndent)
                                } else {
                                    whiteSpaceCorrection += expectedParameterIndent.length - (whiteSpaceBeforeIdentifier?.textLength ?: 0)
                                }
                            }
                        } else {
                            if (whiteSpaceBeforeIdentifier == null || whiteSpaceBeforeIdentifier.text != " ") {
                                if (!dryRun) {
                                    emit(firstChildNodeInValueParameter!!.startOffset, "Single whitespace expected before parameter", true)
                                }
                                if (autoCorrect && !dryRun) {
                                    firstChildNodeInValueParameter.upsertWhitespaceBeforeMe(" ")
                                } else {
                                    whiteSpaceCorrection += 1 - (whiteSpaceBeforeIdentifier?.textLength ?: 0)
                                }
                            }
                        }
                    }
            }

        return whiteSpaceCorrection
    }

    private fun fixWhiteSpaceBeforeClosingParenthesis(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        multiline: Boolean,
        dryRun: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val closingParenthesisPrimaryConstructor =
            node
                .getPrimaryConstructorParameterListOrNull()
                ?.findChildByType(RPAR)
        closingParenthesisPrimaryConstructor
            ?.prevSibling()
            ?.takeIf { it.elementType == WHITE_SPACE }
            .let { whiteSpaceBeforeClosingParenthesis ->
                if (multiline) {
                    if (whiteSpaceBeforeClosingParenthesis == null ||
                        !whiteSpaceBeforeClosingParenthesis.textContains('\n')
                    ) {
                        if (!dryRun) {
                            emit(closingParenthesisPrimaryConstructor!!.startOffset, "Newline expected before closing parenthesis", true)
                        }
                        // Let IndentationRule determine the exact indent
                        val expectedParameterIndent = node.indent()
                        if (autoCorrect && !dryRun) {
                            closingParenthesisPrimaryConstructor!!.upsertWhitespaceBeforeMe(expectedParameterIndent)
                        } else {
                            whiteSpaceCorrection += expectedParameterIndent.length - (whiteSpaceBeforeClosingParenthesis?.textLength ?: 0)
                        }
                    }
                } else {
                    if (whiteSpaceBeforeClosingParenthesis != null &&
                        whiteSpaceBeforeClosingParenthesis.nextLeaf()?.elementType == RPAR
                    ) {
                        if (!dryRun) {
                            emit(
                                whiteSpaceBeforeClosingParenthesis.startOffset,
                                "No whitespace expected between last parameter and closing parenthesis",
                                true,
                            )
                        }
                        if (autoCorrect && !dryRun) {
                            whiteSpaceBeforeClosingParenthesis.treeParent.removeChild(whiteSpaceBeforeClosingParenthesis)
                        } else {
                            whiteSpaceCorrection -= whiteSpaceBeforeClosingParenthesis.textLength
                        }
                    }
                }
            }
        return whiteSpaceCorrection
    }

    private fun fixWhitespacesInSuperTypeList(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
        wrappedPrimaryConstructor: Boolean,
    ): Int {
        var whiteSpaceCorrection = 0

        val superTypes = node.superTypes() ?: return 0

        if (superTypes.first().elementType != SUPER_TYPE_CALL_ENTRY) {
            superTypes
                .firstOrNull { it.elementType == SUPER_TYPE_CALL_ENTRY }
                ?.let { superTypeCallEntry ->
                    emit(superTypeCallEntry.startOffset, "Super type call must be first super type", true)
                    if (autoCorrect) {
                        val superTypeList = node.findChildByType(SUPER_TYPE_LIST) ?: return 0
                        val originalFirstSuperType = superTypes.first()
                        val commaBeforeSuperTypeCall = requireNotNull(superTypeCallEntry.prevSibling { it.elementType == COMMA })

                        // Remove the whitespace before the super type call and do not insert a new whitespace as it will be fixed later
                        superTypeCallEntry
                            .prevSibling()
                            ?.takeIf { it.elementType == WHITE_SPACE }
                            ?.let { whitespaceBeforeSuperTypeCallEntry ->
                                superTypeList.removeChild(whitespaceBeforeSuperTypeCallEntry)
                            }

                        superTypeList.addChild(superTypeCallEntry, superTypes.first())
                        superTypeList.addChild(commaBeforeSuperTypeCall, originalFirstSuperType)
                    }
                }
        }

        if (superTypes.count() == 1) {
            if (wrappedPrimaryConstructor) {
                // Format
                //     class ClassWithPrimaryConstructorWhichWillBeWrapped(...) :
                //         SomeSuperTypeEntry
                // to
                //     class ClassWithPrimaryConstructorWhichWillBeWrapped(
                //         ...
                //     ) : SomeSuperTypeEntry
                superTypes
                    .first()
                    .let { firstSuperType ->
                        firstSuperType
                            .prevLeaf()
                            .takeIf { it.isWhiteSpaceWithNewline() }
                            ?.takeUnless { it.prevSibling()?.elementType == EOL_COMMENT }
                            ?.let { whiteSpaceBeforeSuperType ->
                                val expectedWhitespace = " "
                                if (whiteSpaceBeforeSuperType.text != expectedWhitespace) {
                                    emit(firstSuperType.startOffset, "Expected single space before the super type", true)
                                    if (autoCorrect) {
                                        firstSuperType.upsertWhitespaceBeforeMe(expectedWhitespace)
                                    }
                                }
                            }
                    }
            } else {
                superTypes
                    .first()
                    .firstChildNode
                    ?.let { superTypeFirstChildNode ->
                        superTypeFirstChildNode
                            .prevLeaf()
                            ?.takeIf { it.elementType == WHITE_SPACE }
                            .let { whiteSpaceBeforeIdentifier ->
                                if (node.hasMultilineSuperTypeList() ||
                                    classSignaturesIncludingFirstSuperTypeExceedsMaxLineLength(node, emit, autoCorrect)
                                ) {
                                    if (whiteSpaceBeforeIdentifier == null ||
                                        !whiteSpaceBeforeIdentifier.textContains('\n')
                                    ) {
                                        emit(superTypeFirstChildNode.startOffset, "Super type should start on a newline", true)
                                        if (autoCorrect) {
                                            // Let IndentationRule determine the exact indent
                                            superTypeFirstChildNode.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                                        }
                                    }
                                } else {
                                    val expectedWhitespace = " "
                                    if (whiteSpaceBeforeIdentifier == null ||
                                        whiteSpaceBeforeIdentifier.text != expectedWhitespace
                                    ) {
                                        emit(superTypeFirstChildNode.startOffset, "Expected single space before the super type", true)
                                        if (autoCorrect) {
                                            superTypeFirstChildNode.upsertWhitespaceBeforeMe(expectedWhitespace)
                                        }
                                    }
                                }
                            }
                    }
            }
        } else {
            superTypes
                .forEachIndexed { index, superType ->
                    val firstChildNodeInSuperType = superType.firstChildNode
                    firstChildNodeInSuperType
                        ?.prevLeaf()
                        ?.takeIf { it.elementType == WHITE_SPACE }
                        .let { whiteSpaceBeforeIdentifier ->
                            if (index == 0 && node.hasMultilinePrimaryConstructor()) {
                                val expectedWhitespace = " "
                                if (whiteSpaceBeforeIdentifier?.prevLeaf()?.elementType != EOL_COMMENT &&
                                    (whiteSpaceBeforeIdentifier == null || whiteSpaceBeforeIdentifier.text != expectedWhitespace)
                                ) {
                                    emit(firstChildNodeInSuperType.startOffset, "Expected single space before the first super type", true)
                                    if (autoCorrect) {
                                        firstChildNodeInSuperType.upsertWhitespaceBeforeMe(expectedWhitespace)
                                    }
                                }
                            } else {
                                if (whiteSpaceBeforeIdentifier == null ||
                                    !whiteSpaceBeforeIdentifier.textContains('\n')
                                ) {
                                    emit(firstChildNodeInSuperType.startOffset, "Super type should start on a newline", true)
                                    if (autoCorrect) {
                                        // Let IndentationRule determine the exact indent
                                        firstChildNodeInSuperType.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                                    }
                                }
                            }
                        }
                }
        }

        // Disallow:
        //    class Foo : Bar<String> ("foobar")
        superTypes
            .filter { it.elementType == SUPER_TYPE_CALL_ENTRY }
            .forEach { superTypeCallEntry ->
                superTypeCallEntry
                    .findChildByType(WHITE_SPACE)
                    ?.let { whitespace ->
                        emit(whitespace.startOffset, "No whitespace expected", true)
                        if (autoCorrect) {
                            whitespace.treeParent.removeChild(whitespace)
                        }
                    }
            }

        return whiteSpaceCorrection
    }

    private fun classSignaturesIncludingFirstSuperTypeExceedsMaxLineLength(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ): Boolean {
        val actualClassSignatureLength = node.getClassSignatureLength(excludeSuperTypes = false)
        // Calculate the length of the class signature in case it, including the super types, would be rewritten as single
        // line (and without a maximum line length). The white space correction will be calculated via a dry run of the
        // actual fix.
        val length =
            actualClassSignatureLength +
                // Calculate the white space correction in case the signature would be rewritten to a single line
                fixWhiteSpacesInValueParameterList(node, emit, autoCorrect, multiline = false, dryRun = true)
        return length > maxLineLength
    }

    private fun ASTNode.hasMultilinePrimaryConstructor() =
        findChildByType(PRIMARY_CONSTRUCTOR)
            ?.findChildByType(VALUE_PARAMETER_LIST)
            ?.findChildByType(RPAR)
            ?.prevLeaf { !it.isPartOfComment() }
            .isWhiteSpaceWithNewline()

    private fun fixClassBody(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
        autoCorrect: Boolean,
    ) {
        node
            .findChildByType(CLASS_BODY)
            ?.let { classBody ->
                if (classBody.prevLeaf()?.text != " ") {
                    emit(classBody.startOffset, "Expected a single space before class body", true)
                    if (autoCorrect) {
                        classBody
                            .prevLeaf(true)
                            ?.upsertWhitespaceAfterMe(" ")
                    }
                }
            }
    }

    private fun isMaxLineLengthSet() = maxLineLength != MAX_LINE_LENGTH_PROPERTY_OFF

    private fun List<ASTNode>.collectLeavesRecursively(): List<ASTNode> = flatMap { it.collectLeavesRecursively() }

    private fun ASTNode.collectLeavesRecursively(): List<ASTNode> =
        if (psi is LeafElement) {
            listOf(this)
        } else {
            children()
                .flatMap { it.collectLeavesRecursively() }
                .toList()
        }

    private fun List<ASTNode>.childrenBetween(
        startASTNodePredicate: (ASTNode) -> Boolean,
        endASTNodePredicate: (ASTNode) -> Boolean,
    ): List<ASTNode> {
        val iterator = iterator()
        var currentNode: ASTNode
        val childrenBetween: MutableList<ASTNode> = mutableListOf()

        while (iterator.hasNext()) {
            currentNode = iterator.next()
            if (startASTNodePredicate(currentNode)) {
                childrenBetween.add(currentNode)
                break
            }
        }

        while (iterator.hasNext()) {
            currentNode = iterator.next()
            childrenBetween.add(currentNode)
            if (endASTNodePredicate(currentNode)) {
                break
            }
        }

        return childrenBetween
    }

    private fun List<ASTNode>.joinTextToString(): String = collectLeavesRecursively().joinToString(separator = "") { it.text }

    private fun ASTNode.hasTooManyParameters(): Boolean = countParameters() >= classSignatureWrappingMinimumParameters

    private fun ASTNode.countParameters() =
        getPrimaryConstructorParameterListOrNull()
            ?.children()
            .orEmpty()
            .count { it.elementType == VALUE_PARAMETER }

    private fun ASTNode.getPrimaryConstructorParameterListOrNull() =
        findChildByType(PRIMARY_CONSTRUCTOR)?.findChildByType(VALUE_PARAMETER_LIST)

    public companion object {
        private const val FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET = Int.MAX_VALUE
        public val FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY: EditorConfigProperty<Int> =
            EditorConfigProperty(
                type =
                    PropertyType.LowerCasingPropertyType(
                        "ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than",
                        "Force wrapping of the parameters of the class signature in case it contains at least the specified " +
                            "number of parameters, even in case the entire class signature would fit on a single line. " +
                            "Use value 'unset' to disable this setting.",
                        PropertyType.PropertyValueParser.POSITIVE_INT_VALUE_PARSER,
                        setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "unset"),
                    ),
                defaultValue = FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET,
                ktlintOfficialCodeStyleDefaultValue = 1,
                propertyMapper = { property, _ ->
                    if (property?.isUnset == true) {
                        FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET
                    } else {
                        property?.getValueAs<Int>()
                    }
                },
                propertyWriter = { property ->
                    if (property == FORCE_MULTILINE_WHEN_PARAMETER_COUNT_GREATER_OR_EQUAL_THAN_PROPERTY_UNSET) {
                        "unset"
                    } else {
                        property.toString()
                    }
                },
            )
    }
}

public val CLASS_SIGNATURE_RULE_ID: RuleId = ClassSignatureRule().ruleId
