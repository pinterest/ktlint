package com.pinterest.ktlint.rule.engine.core.api.visitor

import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotation
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtCatchClause
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContextReceiverList
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtDynamicType
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFinallySection
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunctionType
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtImportAlias
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtInitializerList
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParameterList
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtScript
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.KtSuperTypeList
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeArgumentList
import org.jetbrains.kotlin.psi.KtTypeConstraint
import org.jetbrains.kotlin.psi.KtTypeConstraintList
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeParameterList
import org.jetbrains.kotlin.psi.KtTypeProjection
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUserType
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression

public interface AfterNodeVisitor {
    /**
     * This method is called on a node in AST after visiting the child nodes. This is repeated recursively for the child nodes resulting in
     * a depth first traversal of the AST. Note: this method won't be called for a node for which the visit method of the type of that node
     * has been overridden in the rule.
     *
     * ```
     * class ExampleRule(...) {
     *     override fun afterIfExpression(
     *         ktIfExpression: KtIfExpression,
     *         autoCorrect: Boolean,
     *         emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
     *     ) {
     *         // Only called for nodes having element type 'IF'
     *     }

     *     override fun afterWhenExpression(...) {
     *         // Only called for nodes having element type 'WHEN'
     *     }
     *
     *     override fun afterVisitChildNodes(
     *         node: ASTNode,
     *         autoCorrect: Boolean,
     *         emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
     *     ) {
     *         // Called for nodes of other types than 'IF' and 'WHEN' because the 'afterIfExpression' and 'afterWhenExpression' are
     *         // overridden.
     *     }
     * }
     * ```
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {}

    /**
     * This method is called on a node of element type 'CLASS' in the AST after visiting the child nodes.
     *
     * @param ktClass the [KtClass] of a node with element type 'CLASS'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterClass(
        ktClass: KtClass,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktClass.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SECONDARY_CONSTRUCTOR' in the AST after visiting the child nodes.
     *
     * @param ktSecondaryConstructor the [KtSecondaryConstructor] of a node with element type 'SECONDARY_CONSTRUCTOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSecondaryConstructor(
        ktSecondaryConstructor: KtSecondaryConstructor,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSecondaryConstructor.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PRIMARY_CONSTRUCTOR' in the AST after visiting the child nodes.
     *
     * @param ktPrimaryConstructor the [KtPrimaryConstructor] of a node with element type 'PRIMARY_CONSTRUCTOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterPrimaryConstructor(
        ktPrimaryConstructor: KtPrimaryConstructor,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktPrimaryConstructor.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PROPERTY' in the AST after visiting the child nodes.
     *
     * @param ktProperty the [KtProperty] of a node with element type 'PROPERTY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterProperty(
        ktProperty: KtProperty,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktProperty.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPEALIAS' in the AST after visiting the child nodes.
     *
     * @param ktTypeAlias the [KtTypeAlias] of a node with element type 'TYPEALIAS'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeAlias(
        ktTypeAlias: KtTypeAlias,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeAlias.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DESTRUCTURING_DECLARATION' in the AST after visiting the child nodes.
     *
     * @param ktDestructuringDeclaration the [KtDestructuringDeclaration] of a node with element type 'DESTRUCTURING_DECLARATION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterDestructuringDeclaration(
        ktDestructuringDeclaration: KtDestructuringDeclaration,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktDestructuringDeclaration.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DESTRUCTURING_DECLARATION_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktDestructuringDeclarationEntry the [KtDestructuringDeclarationEntry] of a node with element type 'DESTRUCTURING_DECLARATION_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterDestructuringDeclarationEntry(
        ktDestructuringDeclarationEntry: KtDestructuringDeclarationEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktDestructuringDeclarationEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'kotlin.FILE' in the AST after visiting the child nodes.
     *
     * @param ktFile the [KtFile] of a node with element type 'kotlin.FILE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterFile(
        ktFile: KtFile,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktFile.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SCRIPT' in the AST after visiting the child nodes.
     *
     * @param ktScript the [KtScript] of a node with element type 'SCRIPT'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterScript(
        ktScript: KtScript,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktScript.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IMPORT_ALIAS' in the AST after visiting the child nodes.
     *
     * @param ktImportAlias the [KtImportAlias] of a node with element type 'IMPORT_ALIAS'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterImportAlias(
        ktImportAlias: KtImportAlias,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktImportAlias.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IMPORT_DIRECTIVE' in the AST after visiting the child nodes.
     *
     * @param ktImportDirective the [KtImportDirective] of a node with element type 'IMPORT_DIRECTIVE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterImportDirective(
        ktImportDirective: KtImportDirective,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktImportDirective.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IMPORT_LIST' in the AST after visiting the child nodes.
     *
     * @param ktImportList the [KtImportList] of a node with element type 'IMPORT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterImportList(
        ktImportList: KtImportList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktImportList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CLASS_BODY' in the AST after visiting the child nodes.
     *
     * @param ktClassBody the [KtClassBody] of a node with element type 'CLASS_BODY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterClassBody(
        ktClassBody: KtClassBody,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktClassBody.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'MODIFIER_LIST' in the AST after visiting the child nodes.
     *
     * @param ktModifierList the [KtModifierList] of a node with element type 'MODIFIER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterModifierList(
        ktModifierList: KtModifierList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktModifierList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ANNOTATION' in the AST after visiting the child nodes.
     *
     * @param ktAnnotation the [KtAnnotation] of a node with element type 'ANNOTATION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterAnnotation(
        ktAnnotation: KtAnnotation,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktAnnotation.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ANNOTATION_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktAnnotationEntry the [KtAnnotationEntry] of a node with element type 'ANNOTATION_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterAnnotationEntry(
        ktAnnotationEntry: KtAnnotationEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktAnnotationEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONSTRUCTOR_CALLEE' in the AST after visiting the child nodes.
     *
     * @param ktConstructorCalleeExpression the [KtConstructorCalleeExpression] of a node with element type 'CONSTRUCTOR_CALLEE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterConstructorCalleeExpression(
        ktConstructorCalleeExpression: KtConstructorCalleeExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktConstructorCalleeExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_PARAMETER_LIST' in the AST after visiting the child nodes.
     *
     * @param ktTypeParameterList the [KtTypeParameterList] of a node with element type 'TYPE_PARAMETER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeParameterList(
        ktTypeParameterList: KtTypeParameterList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeParameterList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_PARAMETER' in the AST after visiting the child nodes.
     *
     * @param ktTypeParameter the [KtTypeParameter] of a node with element type 'TYPE_PARAMETER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeParameter(
        ktTypeParameter: KtTypeParameter,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeParameter.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ENUM_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktEnumEntry the [KtEnumEntry] of a node with element type 'ENUM_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterEnumEntry(
        ktEnumEntry: KtEnumEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktEnumEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_PARAMETER_LIST' in the AST after visiting the child nodes.
     *
     * @param ktParameterList the [KtParameterList] of a node with element type 'VALUE_PARAMETER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterParameterList(
        ktParameterList: KtParameterList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktParameterList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_PARAMETER' in the AST after visiting the child nodes.
     *
     * @param ktParameter the [KtParameter] of a node with element type 'VALUE_PARAMETER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterParameter(
        ktParameter: KtParameter,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktParameter.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_TYPE_LIST' in the AST after visiting the child nodes.
     *
     * @param ktSuperTypeList the [KtSuperTypeList] of a node with element type 'SUPER_TYPE_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSuperTypeList(
        ktSuperTypeList: KtSuperTypeList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSuperTypeList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DELEGATED_SUPER_TYPE_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktDelegatedSuperTypeEntry the [KtDelegatedSuperTypeEntry] of a node with element type 'DELEGATED_SUPER_TYPE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterDelegatedSuperTypeEntry(
        ktDelegatedSuperTypeEntry: KtDelegatedSuperTypeEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktDelegatedSuperTypeEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_TYPE_CALL_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktSuperTypeCallEntry the [KtSuperTypeCallEntry] of a node with element type 'SUPER_TYPE_CALL_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSuperTypeCallEntry(
        ktSuperTypeCallEntry: KtSuperTypeCallEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSuperTypeCallEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_TYPE_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktSuperTypeEntry the [KtSuperTypeEntry] of a node with element type 'SUPER_TYPE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSuperTypeEntry(
        ktSuperTypeEntry: KtSuperTypeEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSuperTypeEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONTEXT_RECEIVER_LIST' in the AST after visiting the child nodes.
     *
     * @param ktContextReceiverList the [KtContextReceiverList] of a node with element type 'CONTEXT_RECEIVER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterContextReceiverList(
        ktContextReceiverList: KtContextReceiverList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktContextReceiverList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONSTRUCTOR_DELEGATION_CALL' in the AST after visiting the child nodes.
     *
     * @param ktConstructorDelegationCall the [KtConstructorDelegationCall] of a node with element type 'CONSTRUCTOR_DELEGATION_CALL'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterConstructorDelegationCall(
        ktConstructorDelegationCall: KtConstructorDelegationCall,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktConstructorDelegationCall.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PROPERTY_DELEGATE' in the AST after visiting the child nodes.
     *
     * @param ktPropertyDelegate the [KtPropertyDelegate] of a node with element type 'PROPERTY_DELEGATE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterPropertyDelegate(
        ktPropertyDelegate: KtPropertyDelegate,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktPropertyDelegate.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_REFERENCE' in the AST after visiting the child nodes.
     *
     * @param ktTypeReference the [KtTypeReference] of a node with element type 'TYPE_REFERENCE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeReference(
        ktTypeReference: KtTypeReference,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeReference.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_ARGUMENT_LIST' in the AST after visiting the child nodes.
     *
     * @param ktValueArgumentList the [KtValueArgumentList] of a node with element type 'VALUE_ARGUMENT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterValueArgumentList(
        ktValueArgumentList: KtValueArgumentList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktValueArgumentList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_ARGUMENT' in the AST after visiting the child nodes.
     *
     * @param ktValueArgument the [KtValueArgument] of a node with element type 'VALUE_ARGUMENT'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterValueArgument(
        ktValueArgument: KtValueArgument,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktValueArgument.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'REFERENCE_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktReferenceExpression the [KtReferenceExpression] of a node with element type 'REFERENCE_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterReferenceExpression(
        ktReferenceExpression: KtReferenceExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktReferenceExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LABELED_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktLabeledExpression the [KtLabeledExpression] of a node with element type 'LABELED_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterLabeledExpression(
        ktLabeledExpression: KtLabeledExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktLabeledExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PREFIX_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktPrefixExpression the [KtPrefixExpression] of a node with element type 'PREFIX_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterPrefixExpression(
        ktPrefixExpression: KtPrefixExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktPrefixExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'POSTFIX_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktPostfixExpression the [KtPostfixExpression] of a node with element type 'POSTFIX_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterPostfixExpression(
        ktPostfixExpression: KtPostfixExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktPostfixExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BINARY_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktBinaryExpression the [KtBinaryExpression] of a node with element type 'BINARY_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterBinaryExpression(
        ktBinaryExpression: KtBinaryExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktBinaryExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'THROW' in the AST after visiting the child nodes.
     *
     * @param ktThrowExpression the [KtThrowExpression] of a node with element type 'THROW'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterThrowExpression(
        ktThrowExpression: KtThrowExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktThrowExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BREAK' in the AST after visiting the child nodes.
     *
     * @param ktBreakExpression the [KtBreakExpression] of a node with element type 'BREAK'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterBreakExpression(
        ktBreakExpression: KtBreakExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktBreakExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONTINUE' in the AST after visiting the child nodes.
     *
     * @param ktContinueExpression the [KtContinueExpression] of a node with element type 'CONTINUE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterContinueExpression(
        ktContinueExpression: KtContinueExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktContinueExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IF' in the AST after visiting the child nodes.
     *
     * @param ktIfExpression the [KtIfExpression] of a node with element type 'IF'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterIfExpression(
        ktIfExpression: KtIfExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktIfExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN' in the AST after visiting the child nodes.
     *
     * @param ktWhenExpression the [KtWhenExpression] of a node with element type 'WHEN'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterWhenExpression(
        ktWhenExpression: KtWhenExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktWhenExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'COLLECTION_LITERAL_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktCollectionLiteralExpression the [KtCollectionLiteralExpression] of a node with element type 'COLLECTION_LITERAL_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterCollectionLiteralExpression(
        ktCollectionLiteralExpression: KtCollectionLiteralExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktCollectionLiteralExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TRY' in the AST after visiting the child nodes.
     *
     * @param ktTryExpression the [KtTryExpression] of a node with element type 'TRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTryExpression(
        ktTryExpression: KtTryExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTryExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'FOR' in the AST after visiting the child nodes.
     *
     * @param ktForExpression the [KtForExpression] of a node with element type 'FOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterForExpression(
        ktForExpression: KtForExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktForExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHILE' in the AST after visiting the child nodes.
     *
     * @param ktWhileExpression the [KtWhileExpression] of a node with element type 'WHILE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterWhileExpression(
        ktWhileExpression: KtWhileExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktWhileExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DO_WHILE' in the AST after visiting the child nodes.
     *
     * @param ktDoWhileExpression the [KtDoWhileExpression] of a node with element type 'DO_WHILE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterDoWhileExpression(
        ktDoWhileExpression: KtDoWhileExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktDoWhileExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LAMBDA_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktLambdaExpression the [KtLambdaExpression] of a node with element type 'LAMBDA_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterLambdaExpression(
        ktLambdaExpression: KtLambdaExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktLambdaExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ANNOTATED_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktAnnotatedExpression the [KtAnnotatedExpression] of a node with element type 'ANNOTATED_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterAnnotatedExpression(
        ktAnnotatedExpression: KtAnnotatedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktAnnotatedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CALL_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktCallExpression the [KtCallExpression] of a node with element type 'CALL_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterCallExpression(
        ktCallExpression: KtCallExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktCallExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ARRAY_ACCESS_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktArrayAccessExpression the [KtArrayAccessExpression] of a node with element type 'ARRAY_ACCESS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterArrayAccessExpression(
        ktArrayAccessExpression: KtArrayAccessExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktArrayAccessExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CALLABLE_REFERENCE_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktCallableReferenceExpression the [KtCallableReferenceExpression] of a node with element type 'CALLABLE_REFERENCE_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterCallableReferenceExpression(
        ktCallableReferenceExpression: KtCallableReferenceExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktCallableReferenceExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CLASS_LITERAL_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktClassLiteralExpression the [KtClassLiteralExpression] of a node with element type 'CLASS_LITERAL_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterClassLiteralExpression(
        ktClassLiteralExpression: KtClassLiteralExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktClassLiteralExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DOT_QUALIFIED_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktDotQualifiedExpression the [KtDotQualifiedExpression] of a node with element type 'DOT_QUALIFIED_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterDotQualifiedExpression(
        ktDotQualifiedExpression: KtDotQualifiedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktDotQualifiedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SAFE_ACCESS_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktSafeQualifiedExpression the [KtSafeQualifiedExpression] of a node with element type 'SAFE_ACCESS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSafeQualifiedExpression(
        ktSafeQualifiedExpression: KtSafeQualifiedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSafeQualifiedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'OBJECT_LITERAL' in the AST after visiting the child nodes.
     *
     * @param ktObjectLiteralExpression the [KtObjectLiteralExpression] of a node with element type 'OBJECT_LITERAL'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterObjectLiteralExpression(
        ktObjectLiteralExpression: KtObjectLiteralExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktObjectLiteralExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BLOCK' in the AST after visiting the child nodes.
     *
     * @param ktBlockExpression the [KtBlockExpression] of a node with element type 'BLOCK'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterBlockExpression(
        ktBlockExpression: KtBlockExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktBlockExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CATCH' in the AST after visiting the child nodes.
     *
     * @param ktCatchClause the [KtCatchClause] of a node with element type 'CATCH'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterCatchClause(
        ktCatchClause: KtCatchClause,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktCatchClause.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'FINALLY' in the AST after visiting the child nodes.
     *
     * @param ktFinallySection the [KtFinallySection] of a node with element type 'FINALLY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterFinallySection(
        ktFinallySection: KtFinallySection,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktFinallySection.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_ARGUMENT_LIST' in the AST after visiting the child nodes.
     *
     * @param ktTypeArgumentList the [KtTypeArgumentList] of a node with element type 'TYPE_ARGUMENT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeArgumentList(
        ktTypeArgumentList: KtTypeArgumentList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeArgumentList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'THIS_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktThisExpression the [KtThisExpression] of a node with element type 'THIS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterThisExpression(
        ktThisExpression: KtThisExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktThisExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktSuperExpression the [KtSuperExpression] of a node with element type 'SUPER_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSuperExpression(
        ktSuperExpression: KtSuperExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSuperExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PARENTHESIZED' in the AST after visiting the child nodes.
     *
     * @param ktParenthesizedExpression the [KtParenthesizedExpression] of a node with element type 'PARENTHESIZED'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterParenthesizedExpression(
        ktParenthesizedExpression: KtParenthesizedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktParenthesizedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'INITIALIZER_LIST' in the AST after visiting the child nodes.
     *
     * @param ktInitializerList the [KtInitializerList] of a node with element type 'INITIALIZER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterInitializerList(
        ktInitializerList: KtInitializerList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktInitializerList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SCRIPT_INITIALIZER' in the AST after visiting the child nodes.
     *
     * @param ktScriptInitializer the [KtScriptInitializer] of a node with element type 'SCRIPT_INITIALIZER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterScriptInitializer(
        ktScriptInitializer: KtScriptInitializer,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktScriptInitializer.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CLASS_INITIALIZER' in the AST after visiting the child nodes.
     *
     * @param ktClassInitializer the [KtClassInitializer] of a node with element type 'CLASS_INITIALIZER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterClassInitializer(
        ktClassInitializer: KtClassInitializer,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktClassInitializer.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PROPERTY_ACCESSOR' in the AST after visiting the child nodes.
     *
     * @param ktPropertyAccessor the [KtPropertyAccessor] of a node with element type 'PROPERTY_ACCESSOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterPropertyAccessor(
        ktPropertyAccessor: KtPropertyAccessor,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktPropertyAccessor.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_CONSTRAINT_LIST' in the AST after visiting the child nodes.
     *
     * @param ktTypeConstraintList the [KtTypeConstraintList] of a node with element type 'TYPE_CONSTRAINT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeConstraintList(
        ktTypeConstraintList: KtTypeConstraintList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeConstraintList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_CONSTRAINT' in the AST after visiting the child nodes.
     *
     * @param ktTypeConstraint the [KtTypeConstraint] of a node with element type 'TYPE_CONSTRAINT'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeConstraint(
        ktTypeConstraint: KtTypeConstraint,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeConstraint.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'USER_TYPE' in the AST after visiting the child nodes.
     *
     * @param ktUserType the [KtUserType] of a node with element type 'USER_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterUserType(
        ktUserType: KtUserType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktUserType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DYNAMIC_TYPE' in the AST after visiting the child nodes.
     *
     * @param ktDynamicType the [KtDynamicType] of a node with element type 'DYNAMIC_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterDynamicType(
        ktDynamicType: KtDynamicType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktDynamicType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'FUNCTION_TYPE' in the AST after visiting the child nodes.
     *
     * @param ktFunctionType the [KtFunctionType] of a node with element type 'FUNCTION_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterFunctionType(
        ktFunctionType: KtFunctionType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktFunctionType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BINARY_WITH_TYPE' in the AST after visiting the child nodes.
     *
     * @param ktBinaryExpressionWithTypeRHS the [KtBinaryExpressionWithTypeRHS] of a node with element type 'BINARY_WITH_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterBinaryExpressionWithTypeRHS(
        ktBinaryExpressionWithTypeRHS: KtBinaryExpressionWithTypeRHS,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktBinaryExpressionWithTypeRHS.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'STRING_TEMPLATE' in the AST after visiting the child nodes.
     *
     * @param ktStringTemplateExpression the [KtStringTemplateExpression] of a node with element type 'STRING_TEMPLATE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterStringTemplateExpression(
        ktStringTemplateExpression: KtStringTemplateExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktStringTemplateExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'NULLABLE_TYPE' in the AST after visiting the child nodes.
     *
     * @param ktNullableType the [KtNullableType] of a node with element type 'NULLABLE_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterNullableType(
        ktNullableType: KtNullableType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktNullableType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_PROJECTION' in the AST after visiting the child nodes.
     *
     * @param ktTypeProjection the [KtTypeProjection] of a node with element type 'TYPE_PROJECTION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterTypeProjection(
        ktTypeProjection: KtTypeProjection,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktTypeProjection.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktWhenEntry the [KtWhenEntry] of a node with element type 'WHEN_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterWhenEntry(
        ktWhenEntry: KtWhenEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktWhenEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IS_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktIsExpression the [KtIsExpression] of a node with element type 'IS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterIsExpression(
        ktIsExpression: KtIsExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktIsExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_CONDITION_IS_PATTERN' in the AST after visiting the child nodes.
     *
     * @param ktWhenConditionIsPattern the [KtWhenConditionIsPattern] of a node with element type 'WHEN_CONDITION_IS_PATTERN'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterWhenConditionIsPattern(
        ktWhenConditionIsPattern: KtWhenConditionIsPattern,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktWhenConditionIsPattern.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_CONDITION_IN_RANGE' in the AST after visiting the child nodes.
     *
     * @param ktWhenConditionInRange the [KtWhenConditionInRange] of a node with element type 'WHEN_CONDITION_IN_RANGE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterWhenConditionInRange(
        ktWhenConditionInRange: KtWhenConditionInRange,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktWhenConditionInRange.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_CONDITION_WITH_EXPRESSION' in the AST after visiting the child nodes.
     *
     * @param ktWhenConditionWithExpression the [KtWhenConditionWithExpression] of a node with element type 'WHEN_CONDITION_WITH_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterWhenConditionWithExpression(
        ktWhenConditionWithExpression: KtWhenConditionWithExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktWhenConditionWithExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'OBJECT_DECLARATION' in the AST after visiting the child nodes.
     *
     * @param ktObjectDeclaration the [KtObjectDeclaration] of a node with element type 'OBJECT_DECLARATION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterObjectDeclaration(
        ktObjectDeclaration: KtObjectDeclaration,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktObjectDeclaration.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LONG_STRING_TEMPLATE_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktBlockStringTemplateEntry the [KtBlockStringTemplateEntry] of a node with element type 'LONG_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterBlockStringTemplateEntry(
        ktBlockStringTemplateEntry: KtBlockStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktBlockStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SHORT_STRING_TEMPLATE_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktSimpleNameStringTemplateEntry the [KtSimpleNameStringTemplateEntry] of a node with element type 'SHORT_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterSimpleNameStringTemplateEntry(
        ktSimpleNameStringTemplateEntry: KtSimpleNameStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktSimpleNameStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LITERAL_STRING_TEMPLATE_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktLiteralStringTemplateEntry the [KtLiteralStringTemplateEntry] of a node with element type 'LITERAL_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterLiteralStringTemplateEntry(
        ktLiteralStringTemplateEntry: KtLiteralStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktLiteralStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ESCAPE_STRING_TEMPLATE_ENTRY' in the AST after visiting the child nodes.
     *
     * @param ktEscapeStringTemplateEntry the [KtEscapeStringTemplateEntry] of a node with element type 'ESCAPE_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterEscapeStringTemplateEntry(
        ktEscapeStringTemplateEntry: KtEscapeStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktEscapeStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PACKAGE_DIRECTIVE' in the AST after visiting the child nodes.
     *
     * @param ktPackageDirective the [KtPackageDirective] of a node with element type 'PACKAGE_DIRECTIVE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterPackageDirective(
        ktPackageDirective: KtPackageDirective,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = afterVisitChildNodes(ktPackageDirective.node, autoCorrect, emit)
}
