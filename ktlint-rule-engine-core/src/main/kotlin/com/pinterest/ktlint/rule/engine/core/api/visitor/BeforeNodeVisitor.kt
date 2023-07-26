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

public interface BeforeNodeVisitor {
    /**
     * This method is called on a node in AST before visiting the child nodes. This is repeated recursively for the child nodes resulting in
     * a depth first traversal of the AST. Note: this method won't be called for a node for which the visit method of the type of that node
     * has been overridden in the rule.
     *
     * ```
     * class ExampleRule(...) {
     *     override fun beforeIfExpression(
     *         ktIfExpression: KtIfExpression,
     *         autoCorrect: Boolean,
     *         emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
     *     ) {
     *         // Only called for nodes having element type 'IF'
     *     }

     *     override fun beforeWhenExpression(...) {
     *         // Only called for nodes having element type 'WHEN'
     *     }
     *
     *     override fun beforeVisitChildNodes(
     *         node: ASTNode,
     *         autoCorrect: Boolean,
     *         emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
     *     ) {
     *         // Called for nodes of other types than 'IF' and 'WHEN' because the 'beforeIfExpression' and 'beforeWhenExpression' are
     *         // overridden.
     *     }
     * }
     * ```
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {}

    /**
     * This method is called on a node of element type 'CLASS' in the AST before visiting the child nodes.
     *
     * @param ktClass the [KtClass] of a node with element type 'CLASS'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeClass(
        ktClass: KtClass,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktClass.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SECONDARY_CONSTRUCTOR' in the AST before visiting the child nodes.
     *
     * @param ktSecondaryConstructor the [KtSecondaryConstructor] of a node with element type 'SECONDARY_CONSTRUCTOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSecondaryConstructor(
        ktSecondaryConstructor: KtSecondaryConstructor,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSecondaryConstructor.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PRIMARY_CONSTRUCTOR' in the AST before visiting the child nodes.
     *
     * @param ktPrimaryConstructor the [KtPrimaryConstructor] of a node with element type 'PRIMARY_CONSTRUCTOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforePrimaryConstructor(
        ktPrimaryConstructor: KtPrimaryConstructor,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktPrimaryConstructor.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PROPERTY' in the AST before visiting the child nodes.
     *
     * @param ktProperty the [KtProperty] of a node with element type 'PROPERTY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeProperty(
        ktProperty: KtProperty,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktProperty.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPEALIAS' in the AST before visiting the child nodes.
     *
     * @param ktTypeAlias the [KtTypeAlias] of a node with element type 'TYPEALIAS'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeAlias(
        ktTypeAlias: KtTypeAlias,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeAlias.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DESTRUCTURING_DECLARATION' in the AST before visiting the child nodes.
     *
     * @param ktDestructuringDeclaration the [KtDestructuringDeclaration] of a node with element type 'DESTRUCTURING_DECLARATION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeDestructuringDeclaration(
        ktDestructuringDeclaration: KtDestructuringDeclaration,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktDestructuringDeclaration.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DESTRUCTURING_DECLARATION_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktDestructuringDeclarationEntry the [KtDestructuringDeclarationEntry] of a node with element type 'DESTRUCTURING_DECLARATION_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeDestructuringDeclarationEntry(
        ktDestructuringDeclarationEntry: KtDestructuringDeclarationEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktDestructuringDeclarationEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'kotlin.FILE' in the AST before visiting the child nodes.
     *
     * @param ktFile the [KtFile] of a node with element type 'kotlin.FILE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeFile(
        ktFile: KtFile,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktFile.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SCRIPT' in the AST before visiting the child nodes.
     *
     * @param ktScript the [KtScript] of a node with element type 'SCRIPT'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeScript(
        ktScript: KtScript,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktScript.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IMPORT_ALIAS' in the AST before visiting the child nodes.
     *
     * @param ktImportAlias the [KtImportAlias] of a node with element type 'IMPORT_ALIAS'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeImportAlias(
        ktImportAlias: KtImportAlias,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktImportAlias.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IMPORT_DIRECTIVE' in the AST before visiting the child nodes.
     *
     * @param ktImportDirective the [KtImportDirective] of a node with element type 'IMPORT_DIRECTIVE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeImportDirective(
        ktImportDirective: KtImportDirective,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktImportDirective.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IMPORT_LIST' in the AST before visiting the child nodes.
     *
     * @param ktImportList the [KtImportList] of a node with element type 'IMPORT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeImportList(
        ktImportList: KtImportList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktImportList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CLASS_BODY' in the AST before visiting the child nodes.
     *
     * @param ktClassBody the [KtClassBody] of a node with element type 'CLASS_BODY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeClassBody(
        ktClassBody: KtClassBody,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktClassBody.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'MODIFIER_LIST' in the AST before visiting the child nodes.
     *
     * @param ktModifierList the [KtModifierList] of a node with element type 'MODIFIER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeModifierList(
        ktModifierList: KtModifierList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktModifierList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ANNOTATION' in the AST before visiting the child nodes.
     *
     * @param ktAnnotation the [KtAnnotation] of a node with element type 'ANNOTATION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeAnnotation(
        ktAnnotation: KtAnnotation,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktAnnotation.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ANNOTATION_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktAnnotationEntry the [KtAnnotationEntry] of a node with element type 'ANNOTATION_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeAnnotationEntry(
        ktAnnotationEntry: KtAnnotationEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktAnnotationEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONSTRUCTOR_CALLEE' in the AST before visiting the child nodes.
     *
     * @param ktConstructorCalleeExpression the [KtConstructorCalleeExpression] of a node with element type 'CONSTRUCTOR_CALLEE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeConstructorCalleeExpression(
        ktConstructorCalleeExpression: KtConstructorCalleeExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktConstructorCalleeExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_PARAMETER_LIST' in the AST before visiting the child nodes.
     *
     * @param ktTypeParameterList the [KtTypeParameterList] of a node with element type 'TYPE_PARAMETER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeParameterList(
        ktTypeParameterList: KtTypeParameterList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeParameterList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_PARAMETER' in the AST before visiting the child nodes.
     *
     * @param ktTypeParameter the [KtTypeParameter] of a node with element type 'TYPE_PARAMETER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeParameter(
        ktTypeParameter: KtTypeParameter,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeParameter.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ENUM_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktEnumEntry the [KtEnumEntry] of a node with element type 'ENUM_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeEnumEntry(
        ktEnumEntry: KtEnumEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktEnumEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_PARAMETER_LIST' in the AST before visiting the child nodes.
     *
     * @param ktParameterList the [KtParameterList] of a node with element type 'VALUE_PARAMETER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeParameterList(
        ktParameterList: KtParameterList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktParameterList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_PARAMETER' in the AST before visiting the child nodes.
     *
     * @param ktParameter the [KtParameter] of a node with element type 'VALUE_PARAMETER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeParameter(
        ktParameter: KtParameter,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktParameter.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_TYPE_LIST' in the AST before visiting the child nodes.
     *
     * @param ktSuperTypeList the [KtSuperTypeList] of a node with element type 'SUPER_TYPE_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSuperTypeList(
        ktSuperTypeList: KtSuperTypeList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSuperTypeList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DELEGATED_SUPER_TYPE_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktDelegatedSuperTypeEntry the [KtDelegatedSuperTypeEntry] of a node with element type 'DELEGATED_SUPER_TYPE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeDelegatedSuperTypeEntry(
        ktDelegatedSuperTypeEntry: KtDelegatedSuperTypeEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktDelegatedSuperTypeEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_TYPE_CALL_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktSuperTypeCallEntry the [KtSuperTypeCallEntry] of a node with element type 'SUPER_TYPE_CALL_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSuperTypeCallEntry(
        ktSuperTypeCallEntry: KtSuperTypeCallEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSuperTypeCallEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_TYPE_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktSuperTypeEntry the [KtSuperTypeEntry] of a node with element type 'SUPER_TYPE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSuperTypeEntry(
        ktSuperTypeEntry: KtSuperTypeEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSuperTypeEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONTEXT_RECEIVER_LIST' in the AST before visiting the child nodes.
     *
     * @param ktContextReceiverList the [KtContextReceiverList] of a node with element type 'CONTEXT_RECEIVER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeContextReceiverList(
        ktContextReceiverList: KtContextReceiverList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktContextReceiverList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONSTRUCTOR_DELEGATION_CALL' in the AST before visiting the child nodes.
     *
     * @param ktConstructorDelegationCall the [KtConstructorDelegationCall] of a node with element type 'CONSTRUCTOR_DELEGATION_CALL'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeConstructorDelegationCall(
        ktConstructorDelegationCall: KtConstructorDelegationCall,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktConstructorDelegationCall.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PROPERTY_DELEGATE' in the AST before visiting the child nodes.
     *
     * @param ktPropertyDelegate the [KtPropertyDelegate] of a node with element type 'PROPERTY_DELEGATE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforePropertyDelegate(
        ktPropertyDelegate: KtPropertyDelegate,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktPropertyDelegate.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_REFERENCE' in the AST before visiting the child nodes.
     *
     * @param ktTypeReference the [KtTypeReference] of a node with element type 'TYPE_REFERENCE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeReference(
        ktTypeReference: KtTypeReference,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeReference.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_ARGUMENT_LIST' in the AST before visiting the child nodes.
     *
     * @param ktValueArgumentList the [KtValueArgumentList] of a node with element type 'VALUE_ARGUMENT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeValueArgumentList(
        ktValueArgumentList: KtValueArgumentList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktValueArgumentList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'VALUE_ARGUMENT' in the AST before visiting the child nodes.
     *
     * @param ktValueArgument the [KtValueArgument] of a node with element type 'VALUE_ARGUMENT'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeValueArgument(
        ktValueArgument: KtValueArgument,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktValueArgument.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'REFERENCE_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktReferenceExpression the [KtReferenceExpression] of a node with element type 'REFERENCE_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeReferenceExpression(
        ktReferenceExpression: KtReferenceExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktReferenceExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LABELED_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktLabeledExpression the [KtLabeledExpression] of a node with element type 'LABELED_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeLabeledExpression(
        ktLabeledExpression: KtLabeledExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktLabeledExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PREFIX_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktPrefixExpression the [KtPrefixExpression] of a node with element type 'PREFIX_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforePrefixExpression(
        ktPrefixExpression: KtPrefixExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktPrefixExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'POSTFIX_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktPostfixExpression the [KtPostfixExpression] of a node with element type 'POSTFIX_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforePostfixExpression(
        ktPostfixExpression: KtPostfixExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktPostfixExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BINARY_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktBinaryExpression the [KtBinaryExpression] of a node with element type 'BINARY_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeBinaryExpression(
        ktBinaryExpression: KtBinaryExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktBinaryExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'THROW' in the AST before visiting the child nodes.
     *
     * @param ktThrowExpression the [KtThrowExpression] of a node with element type 'THROW'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeThrowExpression(
        ktThrowExpression: KtThrowExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktThrowExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BREAK' in the AST before visiting the child nodes.
     *
     * @param ktBreakExpression the [KtBreakExpression] of a node with element type 'BREAK'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeBreakExpression(
        ktBreakExpression: KtBreakExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktBreakExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CONTINUE' in the AST before visiting the child nodes.
     *
     * @param ktContinueExpression the [KtContinueExpression] of a node with element type 'CONTINUE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeContinueExpression(
        ktContinueExpression: KtContinueExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktContinueExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IF' in the AST before visiting the child nodes.
     *
     * @param ktIfExpression the [KtIfExpression] of a node with element type 'IF'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeIfExpression(
        ktIfExpression: KtIfExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktIfExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN' in the AST before visiting the child nodes.
     *
     * @param ktWhenExpression the [KtWhenExpression] of a node with element type 'WHEN'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeWhenExpression(
        ktWhenExpression: KtWhenExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktWhenExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'COLLECTION_LITERAL_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktCollectionLiteralExpression the [KtCollectionLiteralExpression] of a node with element type 'COLLECTION_LITERAL_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeCollectionLiteralExpression(
        ktCollectionLiteralExpression: KtCollectionLiteralExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktCollectionLiteralExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TRY' in the AST before visiting the child nodes.
     *
     * @param ktTryExpression the [KtTryExpression] of a node with element type 'TRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTryExpression(
        ktTryExpression: KtTryExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTryExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'FOR' in the AST before visiting the child nodes.
     *
     * @param ktForExpression the [KtForExpression] of a node with element type 'FOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeForExpression(
        ktForExpression: KtForExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktForExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHILE' in the AST before visiting the child nodes.
     *
     * @param ktWhileExpression the [KtWhileExpression] of a node with element type 'WHILE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeWhileExpression(
        ktWhileExpression: KtWhileExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktWhileExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DO_WHILE' in the AST before visiting the child nodes.
     *
     * @param ktDoWhileExpression the [KtDoWhileExpression] of a node with element type 'DO_WHILE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeDoWhileExpression(
        ktDoWhileExpression: KtDoWhileExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktDoWhileExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LAMBDA_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktLambdaExpression the [KtLambdaExpression] of a node with element type 'LAMBDA_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeLambdaExpression(
        ktLambdaExpression: KtLambdaExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktLambdaExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ANNOTATED_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktAnnotatedExpression the [KtAnnotatedExpression] of a node with element type 'ANNOTATED_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeAnnotatedExpression(
        ktAnnotatedExpression: KtAnnotatedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktAnnotatedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CALL_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktCallExpression the [KtCallExpression] of a node with element type 'CALL_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeCallExpression(
        ktCallExpression: KtCallExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktCallExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ARRAY_ACCESS_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktArrayAccessExpression the [KtArrayAccessExpression] of a node with element type 'ARRAY_ACCESS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeArrayAccessExpression(
        ktArrayAccessExpression: KtArrayAccessExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktArrayAccessExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CALLABLE_REFERENCE_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktCallableReferenceExpression the [KtCallableReferenceExpression] of a node with element type 'CALLABLE_REFERENCE_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeCallableReferenceExpression(
        ktCallableReferenceExpression: KtCallableReferenceExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktCallableReferenceExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CLASS_LITERAL_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktClassLiteralExpression the [KtClassLiteralExpression] of a node with element type 'CLASS_LITERAL_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeClassLiteralExpression(
        ktClassLiteralExpression: KtClassLiteralExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktClassLiteralExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DOT_QUALIFIED_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktDotQualifiedExpression the [KtDotQualifiedExpression] of a node with element type 'DOT_QUALIFIED_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeDotQualifiedExpression(
        ktDotQualifiedExpression: KtDotQualifiedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktDotQualifiedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SAFE_ACCESS_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktSafeQualifiedExpression the [KtSafeQualifiedExpression] of a node with element type 'SAFE_ACCESS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSafeQualifiedExpression(
        ktSafeQualifiedExpression: KtSafeQualifiedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSafeQualifiedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'OBJECT_LITERAL' in the AST before visiting the child nodes.
     *
     * @param ktObjectLiteralExpression the [KtObjectLiteralExpression] of a node with element type 'OBJECT_LITERAL'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeObjectLiteralExpression(
        ktObjectLiteralExpression: KtObjectLiteralExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktObjectLiteralExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BLOCK' in the AST before visiting the child nodes.
     *
     * @param ktBlockExpression the [KtBlockExpression] of a node with element type 'BLOCK'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeBlockExpression(
        ktBlockExpression: KtBlockExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktBlockExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CATCH' in the AST before visiting the child nodes.
     *
     * @param ktCatchClause the [KtCatchClause] of a node with element type 'CATCH'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeCatchClause(
        ktCatchClause: KtCatchClause,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktCatchClause.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'FINALLY' in the AST before visiting the child nodes.
     *
     * @param ktFinallySection the [KtFinallySection] of a node with element type 'FINALLY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeFinallySection(
        ktFinallySection: KtFinallySection,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktFinallySection.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_ARGUMENT_LIST' in the AST before visiting the child nodes.
     *
     * @param ktTypeArgumentList the [KtTypeArgumentList] of a node with element type 'TYPE_ARGUMENT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeArgumentList(
        ktTypeArgumentList: KtTypeArgumentList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeArgumentList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'THIS_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktThisExpression the [KtThisExpression] of a node with element type 'THIS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeThisExpression(
        ktThisExpression: KtThisExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktThisExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SUPER_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktSuperExpression the [KtSuperExpression] of a node with element type 'SUPER_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSuperExpression(
        ktSuperExpression: KtSuperExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSuperExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PARENTHESIZED' in the AST before visiting the child nodes.
     *
     * @param ktParenthesizedExpression the [KtParenthesizedExpression] of a node with element type 'PARENTHESIZED'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeParenthesizedExpression(
        ktParenthesizedExpression: KtParenthesizedExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktParenthesizedExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'INITIALIZER_LIST' in the AST before visiting the child nodes.
     *
     * @param ktInitializerList the [KtInitializerList] of a node with element type 'INITIALIZER_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeInitializerList(
        ktInitializerList: KtInitializerList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktInitializerList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SCRIPT_INITIALIZER' in the AST before visiting the child nodes.
     *
     * @param ktScriptInitializer the [KtScriptInitializer] of a node with element type 'SCRIPT_INITIALIZER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeScriptInitializer(
        ktScriptInitializer: KtScriptInitializer,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktScriptInitializer.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'CLASS_INITIALIZER' in the AST before visiting the child nodes.
     *
     * @param ktClassInitializer the [KtClassInitializer] of a node with element type 'CLASS_INITIALIZER'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeClassInitializer(
        ktClassInitializer: KtClassInitializer,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktClassInitializer.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PROPERTY_ACCESSOR' in the AST before visiting the child nodes.
     *
     * @param ktPropertyAccessor the [KtPropertyAccessor] of a node with element type 'PROPERTY_ACCESSOR'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforePropertyAccessor(
        ktPropertyAccessor: KtPropertyAccessor,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktPropertyAccessor.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_CONSTRAINT_LIST' in the AST before visiting the child nodes.
     *
     * @param ktTypeConstraintList the [KtTypeConstraintList] of a node with element type 'TYPE_CONSTRAINT_LIST'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeConstraintList(
        ktTypeConstraintList: KtTypeConstraintList,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeConstraintList.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_CONSTRAINT' in the AST before visiting the child nodes.
     *
     * @param ktTypeConstraint the [KtTypeConstraint] of a node with element type 'TYPE_CONSTRAINT'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeConstraint(
        ktTypeConstraint: KtTypeConstraint,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeConstraint.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'USER_TYPE' in the AST before visiting the child nodes.
     *
     * @param ktUserType the [KtUserType] of a node with element type 'USER_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeUserType(
        ktUserType: KtUserType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktUserType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'DYNAMIC_TYPE' in the AST before visiting the child nodes.
     *
     * @param ktDynamicType the [KtDynamicType] of a node with element type 'DYNAMIC_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeDynamicType(
        ktDynamicType: KtDynamicType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktDynamicType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'FUNCTION_TYPE' in the AST before visiting the child nodes.
     *
     * @param ktFunctionType the [KtFunctionType] of a node with element type 'FUNCTION_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeFunctionType(
        ktFunctionType: KtFunctionType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktFunctionType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'BINARY_WITH_TYPE' in the AST before visiting the child nodes.
     *
     * @param ktBinaryExpressionWithTypeRHS the [KtBinaryExpressionWithTypeRHS] of a node with element type 'BINARY_WITH_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeBinaryExpressionWithTypeRHS(
        ktBinaryExpressionWithTypeRHS: KtBinaryExpressionWithTypeRHS,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktBinaryExpressionWithTypeRHS.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'STRING_TEMPLATE' in the AST before visiting the child nodes.
     *
     * @param ktStringTemplateExpression the [KtStringTemplateExpression] of a node with element type 'STRING_TEMPLATE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeStringTemplateExpression(
        ktStringTemplateExpression: KtStringTemplateExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktStringTemplateExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'NULLABLE_TYPE' in the AST before visiting the child nodes.
     *
     * @param ktNullableType the [KtNullableType] of a node with element type 'NULLABLE_TYPE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeNullableType(
        ktNullableType: KtNullableType,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktNullableType.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'TYPE_PROJECTION' in the AST before visiting the child nodes.
     *
     * @param ktTypeProjection the [KtTypeProjection] of a node with element type 'TYPE_PROJECTION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeTypeProjection(
        ktTypeProjection: KtTypeProjection,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktTypeProjection.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktWhenEntry the [KtWhenEntry] of a node with element type 'WHEN_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeWhenEntry(
        ktWhenEntry: KtWhenEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktWhenEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'IS_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktIsExpression the [KtIsExpression] of a node with element type 'IS_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeIsExpression(
        ktIsExpression: KtIsExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktIsExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_CONDITION_IS_PATTERN' in the AST before visiting the child nodes.
     *
     * @param ktWhenConditionIsPattern the [KtWhenConditionIsPattern] of a node with element type 'WHEN_CONDITION_IS_PATTERN'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeWhenConditionIsPattern(
        ktWhenConditionIsPattern: KtWhenConditionIsPattern,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktWhenConditionIsPattern.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_CONDITION_IN_RANGE' in the AST before visiting the child nodes.
     *
     * @param ktWhenConditionInRange the [KtWhenConditionInRange] of a node with element type 'WHEN_CONDITION_IN_RANGE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeWhenConditionInRange(
        ktWhenConditionInRange: KtWhenConditionInRange,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktWhenConditionInRange.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'WHEN_CONDITION_WITH_EXPRESSION' in the AST before visiting the child nodes.
     *
     * @param ktWhenConditionWithExpression the [KtWhenConditionWithExpression] of a node with element type 'WHEN_CONDITION_WITH_EXPRESSION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeWhenConditionWithExpression(
        ktWhenConditionWithExpression: KtWhenConditionWithExpression,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktWhenConditionWithExpression.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'OBJECT_DECLARATION' in the AST before visiting the child nodes.
     *
     * @param ktObjectDeclaration the [KtObjectDeclaration] of a node with element type 'OBJECT_DECLARATION'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeObjectDeclaration(
        ktObjectDeclaration: KtObjectDeclaration,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktObjectDeclaration.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LONG_STRING_TEMPLATE_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktBlockStringTemplateEntry the [KtBlockStringTemplateEntry] of a node with element type 'LONG_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeBlockStringTemplateEntry(
        ktBlockStringTemplateEntry: KtBlockStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktBlockStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'SHORT_STRING_TEMPLATE_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktSimpleNameStringTemplateEntry the [KtSimpleNameStringTemplateEntry] of a node with element type 'SHORT_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeSimpleNameStringTemplateEntry(
        ktSimpleNameStringTemplateEntry: KtSimpleNameStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktSimpleNameStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'LITERAL_STRING_TEMPLATE_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktLiteralStringTemplateEntry the [KtLiteralStringTemplateEntry] of a node with element type 'LITERAL_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeLiteralStringTemplateEntry(
        ktLiteralStringTemplateEntry: KtLiteralStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktLiteralStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'ESCAPE_STRING_TEMPLATE_ENTRY' in the AST before visiting the child nodes.
     *
     * @param ktEscapeStringTemplateEntry the [KtEscapeStringTemplateEntry] of a node with element type 'ESCAPE_STRING_TEMPLATE_ENTRY'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeEscapeStringTemplateEntry(
        ktEscapeStringTemplateEntry: KtEscapeStringTemplateEntry,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktEscapeStringTemplateEntry.node, autoCorrect, emit)

    /**
     * This method is called on a node of element type 'PACKAGE_DIRECTIVE' in the AST before visiting the child nodes.
     *
     * @param ktPackageDirective the [KtPackageDirective] of a node with element type 'PACKAGE_DIRECTIVE'
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforePackageDirective(
        ktPackageDirective: KtPackageDirective,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Unit = beforeVisitChildNodes(ktPackageDirective.node, autoCorrect, emit)
}
