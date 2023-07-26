package com.pinterest.ktlint.rule.engine.core.api.visitor

import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANNOTATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ARRAY_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BINARY_WITH_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BLOCK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BREAK
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALLABLE_REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CALL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CATCH
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_BODY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_INITIALIZER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CLASS_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COLLECTION_LITERAL_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONSTRUCTOR_CALLEE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONSTRUCTOR_DELEGATION_CALL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTEXT_RECEIVER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.CONTINUE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DELEGATED_SUPER_TYPE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DESTRUCTURING_DECLARATION_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DO_WHILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DYNAMIC_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ENUM_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ESCAPE_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FINALLY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FUNCTION_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IF
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_ALIAS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.INITIALIZER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LABELED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LAMBDA_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LITERAL_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LONG_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.NULLABLE_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OBJECT_LITERAL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PARENTHESIZED
import com.pinterest.ktlint.rule.engine.core.api.ElementType.POSTFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PRIMARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_ACCESSOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PROPERTY_DELEGATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SCRIPT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SCRIPT_INITIALIZER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SECONDARY_CONSTRUCTOR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SHORT_STRING_TEMPLATE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.STRING_TEMPLATE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_CALL_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SUPER_TYPE_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THIS_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.THROW
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPEALIAS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_CONSTRAINT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_CONSTRAINT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_PROJECTION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.USER_TYPE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_ARGUMENT_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.VALUE_PARAMETER_LIST
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_CONDITION_IN_RANGE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_CONDITION_IS_PATTERN
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_CONDITION_WITH_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHEN_ENTRY
import com.pinterest.ktlint.rule.engine.core.api.ElementType.WHILE
import com.pinterest.ktlint.rule.engine.core.util.safeAs
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
import org.jetbrains.kotlin.psi.KtElement
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

// IMPORTANT: This class is generated and therefore should not be changed manually.

/**
 * This visitor dispatches a request from the [KtlintRuleEngine] to visit a node to a more specified visitor function as declared in
 * [BeforeNodeVisitor] or [RuleAfterNodeVisitor].
 */
public open class NodeVisitor :
    BeforeNodeVisitor,
    AfterNodeVisitor {
    /**
     * This method is called on a node in the AST before visiting the child nodes. If the node can be transformed to a known Kt-type (for
     * example [KtIfExpression], the call is dispatched to the visit method [beforeIfExpression] for that specific type. Otherwise, the node
     * is dispatched to the default handler [beforeVisitChildNodes].
     *
     * The called dispatch function can process other nodes in the AST. But do not that rule suppressions on other nodes are not guaranteed
     * to be taken into account.
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun beforeNode(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val dispatched =
            when (node.elementType) {
                CLASS ->
                    node.dispatch<KtClass> { element -> beforeClass(element, autoCorrect, emit) }
                SECONDARY_CONSTRUCTOR ->
                    node.dispatch<KtSecondaryConstructor> { element -> beforeSecondaryConstructor(element, autoCorrect, emit) }
                PRIMARY_CONSTRUCTOR ->
                    node.dispatch<KtPrimaryConstructor> { element -> beforePrimaryConstructor(element, autoCorrect, emit) }
                PROPERTY ->
                    node.dispatch<KtProperty> { element -> beforeProperty(element, autoCorrect, emit) }
                TYPEALIAS ->
                    node.dispatch<KtTypeAlias> { element -> beforeTypeAlias(element, autoCorrect, emit) }
                DESTRUCTURING_DECLARATION ->
                    node.dispatch<KtDestructuringDeclaration> { element -> beforeDestructuringDeclaration(element, autoCorrect, emit) }
                DESTRUCTURING_DECLARATION_ENTRY ->
                    node.dispatch<KtDestructuringDeclarationEntry> { element ->
                        beforeDestructuringDeclarationEntry(element, autoCorrect, emit)
                    }
                FILE ->
                    node.dispatch<KtFile> { element -> beforeFile(element, autoCorrect, emit) }
                SCRIPT ->
                    node.dispatch<KtScript> { element -> beforeScript(element, autoCorrect, emit) }
                IMPORT_ALIAS ->
                    node.dispatch<KtImportAlias> { element -> beforeImportAlias(element, autoCorrect, emit) }
                IMPORT_DIRECTIVE ->
                    node.dispatch<KtImportDirective> { element -> beforeImportDirective(element, autoCorrect, emit) }
                IMPORT_LIST ->
                    node.dispatch<KtImportList> { element -> beforeImportList(element, autoCorrect, emit) }
                CLASS_BODY ->
                    node.dispatch<KtClassBody> { element -> beforeClassBody(element, autoCorrect, emit) }
                MODIFIER_LIST ->
                    node.dispatch<KtModifierList> { element -> beforeModifierList(element, autoCorrect, emit) }
                ANNOTATION ->
                    node.dispatch<KtAnnotation> { element -> beforeAnnotation(element, autoCorrect, emit) }
                ANNOTATION_ENTRY ->
                    node.dispatch<KtAnnotationEntry> { element -> beforeAnnotationEntry(element, autoCorrect, emit) }
                CONSTRUCTOR_CALLEE ->
                    node.dispatch<KtConstructorCalleeExpression> { element ->
                        beforeConstructorCalleeExpression(element, autoCorrect, emit)
                    }
                TYPE_PARAMETER_LIST ->
                    node.dispatch<KtTypeParameterList> { element -> beforeTypeParameterList(element, autoCorrect, emit) }
                TYPE_PARAMETER ->
                    node.dispatch<KtTypeParameter> { element -> beforeTypeParameter(element, autoCorrect, emit) }
                ENUM_ENTRY ->
                    node.dispatch<KtEnumEntry> { element -> beforeEnumEntry(element, autoCorrect, emit) }
                VALUE_PARAMETER_LIST ->
                    node.dispatch<KtParameterList> { element -> beforeParameterList(element, autoCorrect, emit) }
                VALUE_PARAMETER ->
                    node.dispatch<KtParameter> { element -> beforeParameter(element, autoCorrect, emit) }
                SUPER_TYPE_LIST ->
                    node.dispatch<KtSuperTypeList> { element -> beforeSuperTypeList(element, autoCorrect, emit) }
                DELEGATED_SUPER_TYPE_ENTRY ->
                    node.dispatch<KtDelegatedSuperTypeEntry> { element -> beforeDelegatedSuperTypeEntry(element, autoCorrect, emit) }
                SUPER_TYPE_CALL_ENTRY ->
                    node.dispatch<KtSuperTypeCallEntry> { element -> beforeSuperTypeCallEntry(element, autoCorrect, emit) }
                SUPER_TYPE_ENTRY ->
                    node.dispatch<KtSuperTypeEntry> { element -> beforeSuperTypeEntry(element, autoCorrect, emit) }
                CONTEXT_RECEIVER_LIST ->
                    node.dispatch<KtContextReceiverList> { element -> beforeContextReceiverList(element, autoCorrect, emit) }
                CONSTRUCTOR_DELEGATION_CALL ->
                    node.dispatch<KtConstructorDelegationCall> { element -> beforeConstructorDelegationCall(element, autoCorrect, emit) }
                PROPERTY_DELEGATE ->
                    node.dispatch<KtPropertyDelegate> { element -> beforePropertyDelegate(element, autoCorrect, emit) }
                TYPE_REFERENCE ->
                    node.dispatch<KtTypeReference> { element -> beforeTypeReference(element, autoCorrect, emit) }
                VALUE_ARGUMENT_LIST ->
                    node.dispatch<KtValueArgumentList> { element -> beforeValueArgumentList(element, autoCorrect, emit) }
                VALUE_ARGUMENT ->
                    node.dispatch<KtValueArgument> { element -> beforeValueArgument(element, autoCorrect, emit) }
                REFERENCE_EXPRESSION ->
                    node.dispatch<KtReferenceExpression> { element -> beforeReferenceExpression(element, autoCorrect, emit) }
                LABELED_EXPRESSION ->
                    node.dispatch<KtLabeledExpression> { element -> beforeLabeledExpression(element, autoCorrect, emit) }
                PREFIX_EXPRESSION ->
                    node.dispatch<KtPrefixExpression> { element -> beforePrefixExpression(element, autoCorrect, emit) }
                POSTFIX_EXPRESSION ->
                    node.dispatch<KtPostfixExpression> { element -> beforePostfixExpression(element, autoCorrect, emit) }
                BINARY_EXPRESSION ->
                    node.dispatch<KtBinaryExpression> { element -> beforeBinaryExpression(element, autoCorrect, emit) }
                THROW ->
                    node.dispatch<KtThrowExpression> { element -> beforeThrowExpression(element, autoCorrect, emit) }
                BREAK ->
                    node.dispatch<KtBreakExpression> { element -> beforeBreakExpression(element, autoCorrect, emit) }
                CONTINUE ->
                    node.dispatch<KtContinueExpression> { element -> beforeContinueExpression(element, autoCorrect, emit) }
                IF ->
                    node.dispatch<KtIfExpression> { element -> beforeIfExpression(element, autoCorrect, emit) }
                WHEN ->
                    node.dispatch<KtWhenExpression> { element -> beforeWhenExpression(element, autoCorrect, emit) }
                COLLECTION_LITERAL_EXPRESSION ->
                    node.dispatch<KtCollectionLiteralExpression> { element ->
                        beforeCollectionLiteralExpression(element, autoCorrect, emit)
                    }
                TRY ->
                    node.dispatch<KtTryExpression> { element -> beforeTryExpression(element, autoCorrect, emit) }
                FOR ->
                    node.dispatch<KtForExpression> { element -> beforeForExpression(element, autoCorrect, emit) }
                WHILE ->
                    node.dispatch<KtWhileExpression> { element -> beforeWhileExpression(element, autoCorrect, emit) }
                DO_WHILE ->
                    node.dispatch<KtDoWhileExpression> { element -> beforeDoWhileExpression(element, autoCorrect, emit) }
                LAMBDA_EXPRESSION ->
                    node.dispatch<KtLambdaExpression> { element -> beforeLambdaExpression(element, autoCorrect, emit) }
                ANNOTATED_EXPRESSION ->
                    node.dispatch<KtAnnotatedExpression> { element -> beforeAnnotatedExpression(element, autoCorrect, emit) }
                CALL_EXPRESSION ->
                    node.dispatch<KtCallExpression> { element -> beforeCallExpression(element, autoCorrect, emit) }
                ARRAY_ACCESS_EXPRESSION ->
                    node.dispatch<KtArrayAccessExpression> { element -> beforeArrayAccessExpression(element, autoCorrect, emit) }
                CALLABLE_REFERENCE_EXPRESSION ->
                    node.dispatch<KtCallableReferenceExpression> { element ->
                        beforeCallableReferenceExpression(element, autoCorrect, emit)
                    }
                CLASS_LITERAL_EXPRESSION ->
                    node.dispatch<KtClassLiteralExpression> { element -> beforeClassLiteralExpression(element, autoCorrect, emit) }
                DOT_QUALIFIED_EXPRESSION ->
                    node.dispatch<KtDotQualifiedExpression> { element -> beforeDotQualifiedExpression(element, autoCorrect, emit) }
                SAFE_ACCESS_EXPRESSION ->
                    node.dispatch<KtSafeQualifiedExpression> { element -> beforeSafeQualifiedExpression(element, autoCorrect, emit) }
                OBJECT_LITERAL ->
                    node.dispatch<KtObjectLiteralExpression> { element -> beforeObjectLiteralExpression(element, autoCorrect, emit) }
                BLOCK ->
                    node.dispatch<KtBlockExpression> { element -> beforeBlockExpression(element, autoCorrect, emit) }
                CATCH ->
                    node.dispatch<KtCatchClause> { element -> beforeCatchClause(element, autoCorrect, emit) }
                FINALLY ->
                    node.dispatch<KtFinallySection> { element -> beforeFinallySection(element, autoCorrect, emit) }
                TYPE_ARGUMENT_LIST ->
                    node.dispatch<KtTypeArgumentList> { element -> beforeTypeArgumentList(element, autoCorrect, emit) }
                THIS_EXPRESSION ->
                    node.dispatch<KtThisExpression> { element -> beforeThisExpression(element, autoCorrect, emit) }
                SUPER_EXPRESSION ->
                    node.dispatch<KtSuperExpression> { element -> beforeSuperExpression(element, autoCorrect, emit) }
                PARENTHESIZED ->
                    node.dispatch<KtParenthesizedExpression> { element -> beforeParenthesizedExpression(element, autoCorrect, emit) }
                INITIALIZER_LIST ->
                    node.dispatch<KtInitializerList> { element -> beforeInitializerList(element, autoCorrect, emit) }
                SCRIPT_INITIALIZER ->
                    node.dispatch<KtScriptInitializer> { element -> beforeScriptInitializer(element, autoCorrect, emit) }
                CLASS_INITIALIZER ->
                    node.dispatch<KtClassInitializer> { element -> beforeClassInitializer(element, autoCorrect, emit) }
                PROPERTY_ACCESSOR ->
                    node.dispatch<KtPropertyAccessor> { element -> beforePropertyAccessor(element, autoCorrect, emit) }
                TYPE_CONSTRAINT_LIST ->
                    node.dispatch<KtTypeConstraintList> { element -> beforeTypeConstraintList(element, autoCorrect, emit) }
                TYPE_CONSTRAINT ->
                    node.dispatch<KtTypeConstraint> { element -> beforeTypeConstraint(element, autoCorrect, emit) }
                USER_TYPE ->
                    node.dispatch<KtUserType> { element -> beforeUserType(element, autoCorrect, emit) }
                DYNAMIC_TYPE ->
                    node.dispatch<KtDynamicType> { element -> beforeDynamicType(element, autoCorrect, emit) }
                FUNCTION_TYPE ->
                    node.dispatch<KtFunctionType> { element -> beforeFunctionType(element, autoCorrect, emit) }
                BINARY_WITH_TYPE ->
                    node.dispatch<KtBinaryExpressionWithTypeRHS> { element ->
                        beforeBinaryExpressionWithTypeRHS(element, autoCorrect, emit)
                    }
                STRING_TEMPLATE ->
                    node.dispatch<KtStringTemplateExpression> { element -> beforeStringTemplateExpression(element, autoCorrect, emit) }
                NULLABLE_TYPE ->
                    node.dispatch<KtNullableType> { element -> beforeNullableType(element, autoCorrect, emit) }
                TYPE_PROJECTION ->
                    node.dispatch<KtTypeProjection> { element -> beforeTypeProjection(element, autoCorrect, emit) }
                WHEN_ENTRY ->
                    node.dispatch<KtWhenEntry> { element -> beforeWhenEntry(element, autoCorrect, emit) }
                IS_EXPRESSION ->
                    node.dispatch<KtIsExpression> { element -> beforeIsExpression(element, autoCorrect, emit) }
                WHEN_CONDITION_IS_PATTERN ->
                    node.dispatch<KtWhenConditionIsPattern> { element -> beforeWhenConditionIsPattern(element, autoCorrect, emit) }
                WHEN_CONDITION_IN_RANGE ->
                    node.dispatch<KtWhenConditionInRange> { element -> beforeWhenConditionInRange(element, autoCorrect, emit) }
                WHEN_CONDITION_WITH_EXPRESSION ->
                    node.dispatch<KtWhenConditionWithExpression> { element ->
                        beforeWhenConditionWithExpression(element, autoCorrect, emit)
                    }
                OBJECT_DECLARATION ->
                    node.dispatch<KtObjectDeclaration> { element -> beforeObjectDeclaration(element, autoCorrect, emit) }
                LONG_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtBlockStringTemplateEntry> { element -> beforeBlockStringTemplateEntry(element, autoCorrect, emit) }
                SHORT_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtSimpleNameStringTemplateEntry> { element ->
                        beforeSimpleNameStringTemplateEntry(element, autoCorrect, emit)
                    }
                LITERAL_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtLiteralStringTemplateEntry> { element -> beforeLiteralStringTemplateEntry(element, autoCorrect, emit) }
                ESCAPE_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtEscapeStringTemplateEntry> { element -> beforeEscapeStringTemplateEntry(element, autoCorrect, emit) }
                PACKAGE_DIRECTIVE ->
                    node.dispatch<KtPackageDirective> { element -> beforePackageDirective(element, autoCorrect, emit) }
                else -> false
            }
        if (!dispatched) {
            // In case no mapping exists for the element type, or if the PSI of the node could not safely be cast to the Kt-type, then fall back
            // on the generic handler.
            beforeVisitChildNodes(node, autoCorrect, emit)
        }
    }

    /**
     * This method is called on a node in the AST after visiting the child nodes. If the node can be transformed to a known Kt-type (for
     * example [KtIfExpression], the call is dispatched to the visit method [afterIfExpression] for that specific type. Otherwise, the node
     * is dispatched to the default handler [afterVisitChildNodes].
     *
     * The called dispatch function can process other nodes in the AST. But do not that rule suppressions on other nodes are not guaranteed
     * to be taken into account.
     *
     * @param node AST node
     * @param autoCorrect indicates whether rule should attempt autocorrection
     * @param emit a way for rule to notify about a violation (lint error)
     */
    public fun afterNode(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        val dispatched =
            when (node.elementType) {
                CLASS ->
                    node.dispatch<KtClass> { element -> afterClass(element, autoCorrect, emit) }

                SECONDARY_CONSTRUCTOR ->
                    node.dispatch<KtSecondaryConstructor> { element -> afterSecondaryConstructor(element, autoCorrect, emit) }

                PRIMARY_CONSTRUCTOR ->
                    node.dispatch<KtPrimaryConstructor> { element -> afterPrimaryConstructor(element, autoCorrect, emit) }

                PROPERTY ->
                    node.dispatch<KtProperty> { element -> afterProperty(element, autoCorrect, emit) }

                TYPEALIAS ->
                    node.dispatch<KtTypeAlias> { element -> afterTypeAlias(element, autoCorrect, emit) }

                DESTRUCTURING_DECLARATION ->
                    node.dispatch<KtDestructuringDeclaration> { element -> afterDestructuringDeclaration(element, autoCorrect, emit) }

                DESTRUCTURING_DECLARATION_ENTRY ->
                    node.dispatch<KtDestructuringDeclarationEntry> { element ->
                        afterDestructuringDeclarationEntry(element, autoCorrect, emit)
                    }

                FILE ->
                    node.dispatch<KtFile> { element -> afterFile(element, autoCorrect, emit) }

                SCRIPT ->
                    node.dispatch<KtScript> { element -> afterScript(element, autoCorrect, emit) }

                IMPORT_ALIAS ->
                    node.dispatch<KtImportAlias> { element -> afterImportAlias(element, autoCorrect, emit) }

                IMPORT_DIRECTIVE ->
                    node.dispatch<KtImportDirective> { element -> afterImportDirective(element, autoCorrect, emit) }

                IMPORT_LIST ->
                    node.dispatch<KtImportList> { element -> afterImportList(element, autoCorrect, emit) }

                CLASS_BODY ->
                    node.dispatch<KtClassBody> { element -> afterClassBody(element, autoCorrect, emit) }

                MODIFIER_LIST ->
                    node.dispatch<KtModifierList> { element -> afterModifierList(element, autoCorrect, emit) }

                ANNOTATION ->
                    node.dispatch<KtAnnotation> { element -> afterAnnotation(element, autoCorrect, emit) }

                ANNOTATION_ENTRY ->
                    node.dispatch<KtAnnotationEntry> { element -> afterAnnotationEntry(element, autoCorrect, emit) }

                CONSTRUCTOR_CALLEE ->
                    node.dispatch<KtConstructorCalleeExpression> { element -> afterConstructorCalleeExpression(element, autoCorrect, emit) }

                TYPE_PARAMETER_LIST ->
                    node.dispatch<KtTypeParameterList> { element -> afterTypeParameterList(element, autoCorrect, emit) }

                TYPE_PARAMETER ->
                    node.dispatch<KtTypeParameter> { element -> afterTypeParameter(element, autoCorrect, emit) }

                ENUM_ENTRY ->
                    node.dispatch<KtEnumEntry> { element -> afterEnumEntry(element, autoCorrect, emit) }

                VALUE_PARAMETER_LIST ->
                    node.dispatch<KtParameterList> { element -> afterParameterList(element, autoCorrect, emit) }

                VALUE_PARAMETER ->
                    node.dispatch<KtParameter> { element -> afterParameter(element, autoCorrect, emit) }

                SUPER_TYPE_LIST ->
                    node.dispatch<KtSuperTypeList> { element -> afterSuperTypeList(element, autoCorrect, emit) }

                DELEGATED_SUPER_TYPE_ENTRY ->
                    node.dispatch<KtDelegatedSuperTypeEntry> { element -> afterDelegatedSuperTypeEntry(element, autoCorrect, emit) }

                SUPER_TYPE_CALL_ENTRY ->
                    node.dispatch<KtSuperTypeCallEntry> { element -> afterSuperTypeCallEntry(element, autoCorrect, emit) }

                SUPER_TYPE_ENTRY ->
                    node.dispatch<KtSuperTypeEntry> { element -> afterSuperTypeEntry(element, autoCorrect, emit) }

                CONTEXT_RECEIVER_LIST ->
                    node.dispatch<KtContextReceiverList> { element -> afterContextReceiverList(element, autoCorrect, emit) }

                CONSTRUCTOR_DELEGATION_CALL ->
                    node.dispatch<KtConstructorDelegationCall> { element -> afterConstructorDelegationCall(element, autoCorrect, emit) }

                PROPERTY_DELEGATE ->
                    node.dispatch<KtPropertyDelegate> { element -> afterPropertyDelegate(element, autoCorrect, emit) }

                TYPE_REFERENCE ->
                    node.dispatch<KtTypeReference> { element -> afterTypeReference(element, autoCorrect, emit) }

                VALUE_ARGUMENT_LIST ->
                    node.dispatch<KtValueArgumentList> { element -> afterValueArgumentList(element, autoCorrect, emit) }

                VALUE_ARGUMENT ->
                    node.dispatch<KtValueArgument> { element -> afterValueArgument(element, autoCorrect, emit) }

                REFERENCE_EXPRESSION ->
                    node.dispatch<KtReferenceExpression> { element -> afterReferenceExpression(element, autoCorrect, emit) }

                LABELED_EXPRESSION ->
                    node.dispatch<KtLabeledExpression> { element -> afterLabeledExpression(element, autoCorrect, emit) }

                PREFIX_EXPRESSION ->
                    node.dispatch<KtPrefixExpression> { element -> afterPrefixExpression(element, autoCorrect, emit) }

                POSTFIX_EXPRESSION ->
                    node.dispatch<KtPostfixExpression> { element -> afterPostfixExpression(element, autoCorrect, emit) }

                BINARY_EXPRESSION ->
                    node.dispatch<KtBinaryExpression> { element -> afterBinaryExpression(element, autoCorrect, emit) }

                THROW ->
                    node.dispatch<KtThrowExpression> { element -> afterThrowExpression(element, autoCorrect, emit) }

                BREAK ->
                    node.dispatch<KtBreakExpression> { element -> afterBreakExpression(element, autoCorrect, emit) }

                CONTINUE ->
                    node.dispatch<KtContinueExpression> { element -> afterContinueExpression(element, autoCorrect, emit) }

                IF ->
                    node.dispatch<KtIfExpression> { element -> afterIfExpression(element, autoCorrect, emit) }

                WHEN ->
                    node.dispatch<KtWhenExpression> { element -> afterWhenExpression(element, autoCorrect, emit) }

                COLLECTION_LITERAL_EXPRESSION ->
                    node.dispatch<KtCollectionLiteralExpression> { element -> afterCollectionLiteralExpression(element, autoCorrect, emit) }

                TRY ->
                    node.dispatch<KtTryExpression> { element -> afterTryExpression(element, autoCorrect, emit) }

                FOR ->
                    node.dispatch<KtForExpression> { element -> afterForExpression(element, autoCorrect, emit) }

                WHILE ->
                    node.dispatch<KtWhileExpression> { element -> afterWhileExpression(element, autoCorrect, emit) }

                DO_WHILE ->
                    node.dispatch<KtDoWhileExpression> { element -> afterDoWhileExpression(element, autoCorrect, emit) }

                LAMBDA_EXPRESSION ->
                    node.dispatch<KtLambdaExpression> { element -> afterLambdaExpression(element, autoCorrect, emit) }

                ANNOTATED_EXPRESSION ->
                    node.dispatch<KtAnnotatedExpression> { element -> afterAnnotatedExpression(element, autoCorrect, emit) }

                CALL_EXPRESSION ->
                    node.dispatch<KtCallExpression> { element -> afterCallExpression(element, autoCorrect, emit) }

                ARRAY_ACCESS_EXPRESSION ->
                    node.dispatch<KtArrayAccessExpression> { element -> afterArrayAccessExpression(element, autoCorrect, emit) }

                CALLABLE_REFERENCE_EXPRESSION ->
                    node.dispatch<KtCallableReferenceExpression> { element -> afterCallableReferenceExpression(element, autoCorrect, emit) }

                CLASS_LITERAL_EXPRESSION ->
                    node.dispatch<KtClassLiteralExpression> { element -> afterClassLiteralExpression(element, autoCorrect, emit) }

                DOT_QUALIFIED_EXPRESSION ->
                    node.dispatch<KtDotQualifiedExpression> { element -> afterDotQualifiedExpression(element, autoCorrect, emit) }

                SAFE_ACCESS_EXPRESSION ->
                    node.dispatch<KtSafeQualifiedExpression> { element -> afterSafeQualifiedExpression(element, autoCorrect, emit) }

                OBJECT_LITERAL ->
                    node.dispatch<KtObjectLiteralExpression> { element -> afterObjectLiteralExpression(element, autoCorrect, emit) }

                BLOCK ->
                    node.dispatch<KtBlockExpression> { element -> afterBlockExpression(element, autoCorrect, emit) }

                CATCH ->
                    node.dispatch<KtCatchClause> { element -> afterCatchClause(element, autoCorrect, emit) }

                FINALLY ->
                    node.dispatch<KtFinallySection> { element -> afterFinallySection(element, autoCorrect, emit) }

                TYPE_ARGUMENT_LIST ->
                    node.dispatch<KtTypeArgumentList> { element -> afterTypeArgumentList(element, autoCorrect, emit) }

                THIS_EXPRESSION ->
                    node.dispatch<KtThisExpression> { element -> afterThisExpression(element, autoCorrect, emit) }

                SUPER_EXPRESSION ->
                    node.dispatch<KtSuperExpression> { element -> afterSuperExpression(element, autoCorrect, emit) }

                PARENTHESIZED ->
                    node.dispatch<KtParenthesizedExpression> { element -> afterParenthesizedExpression(element, autoCorrect, emit) }

                INITIALIZER_LIST ->
                    node.dispatch<KtInitializerList> { element -> afterInitializerList(element, autoCorrect, emit) }

                SCRIPT_INITIALIZER ->
                    node.dispatch<KtScriptInitializer> { element -> afterScriptInitializer(element, autoCorrect, emit) }

                CLASS_INITIALIZER ->
                    node.dispatch<KtClassInitializer> { element -> afterClassInitializer(element, autoCorrect, emit) }

                PROPERTY_ACCESSOR ->
                    node.dispatch<KtPropertyAccessor> { element -> afterPropertyAccessor(element, autoCorrect, emit) }

                TYPE_CONSTRAINT_LIST ->
                    node.dispatch<KtTypeConstraintList> { element -> afterTypeConstraintList(element, autoCorrect, emit) }

                TYPE_CONSTRAINT ->
                    node.dispatch<KtTypeConstraint> { element -> afterTypeConstraint(element, autoCorrect, emit) }

                USER_TYPE ->
                    node.dispatch<KtUserType> { element -> afterUserType(element, autoCorrect, emit) }

                DYNAMIC_TYPE ->
                    node.dispatch<KtDynamicType> { element -> afterDynamicType(element, autoCorrect, emit) }

                FUNCTION_TYPE ->
                    node.dispatch<KtFunctionType> { element -> afterFunctionType(element, autoCorrect, emit) }

                BINARY_WITH_TYPE ->
                    node.dispatch<KtBinaryExpressionWithTypeRHS> { element -> afterBinaryExpressionWithTypeRHS(element, autoCorrect, emit) }

                STRING_TEMPLATE ->
                    node.dispatch<KtStringTemplateExpression> { element -> afterStringTemplateExpression(element, autoCorrect, emit) }

                NULLABLE_TYPE ->
                    node.dispatch<KtNullableType> { element -> afterNullableType(element, autoCorrect, emit) }

                TYPE_PROJECTION ->
                    node.dispatch<KtTypeProjection> { element -> afterTypeProjection(element, autoCorrect, emit) }

                WHEN_ENTRY ->
                    node.dispatch<KtWhenEntry> { element -> afterWhenEntry(element, autoCorrect, emit) }

                IS_EXPRESSION ->
                    node.dispatch<KtIsExpression> { element -> afterIsExpression(element, autoCorrect, emit) }

                WHEN_CONDITION_IS_PATTERN ->
                    node.dispatch<KtWhenConditionIsPattern> { element -> afterWhenConditionIsPattern(element, autoCorrect, emit) }

                WHEN_CONDITION_IN_RANGE ->
                    node.dispatch<KtWhenConditionInRange> { element -> afterWhenConditionInRange(element, autoCorrect, emit) }

                WHEN_CONDITION_WITH_EXPRESSION ->
                    node.dispatch<KtWhenConditionWithExpression> { element -> afterWhenConditionWithExpression(element, autoCorrect, emit) }

                OBJECT_DECLARATION ->
                    node.dispatch<KtObjectDeclaration> { element -> afterObjectDeclaration(element, autoCorrect, emit) }

                LONG_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtBlockStringTemplateEntry> { element -> afterBlockStringTemplateEntry(element, autoCorrect, emit) }

                SHORT_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtSimpleNameStringTemplateEntry> { element ->
                        afterSimpleNameStringTemplateEntry(element, autoCorrect, emit)
                    }

                LITERAL_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtLiteralStringTemplateEntry> { element -> afterLiteralStringTemplateEntry(element, autoCorrect, emit) }

                ESCAPE_STRING_TEMPLATE_ENTRY ->
                    node.dispatch<KtEscapeStringTemplateEntry> { element -> afterEscapeStringTemplateEntry(element, autoCorrect, emit) }

                PACKAGE_DIRECTIVE ->
                    node.dispatch<KtPackageDirective> { element -> afterPackageDirective(element, autoCorrect, emit) }
                else -> false
            }
        if (!dispatched) {
            // In case no mapping exists for the element type, or if the PSI of the node could not safely be cast to the Kt-type, then fall back
            // on the generic handler.
            afterVisitChildNodes(node, autoCorrect, emit)
        }
    }

    private inline fun <reified T : KtElement> ASTNode.dispatch(ktElement: (T) -> Unit): Boolean =
        psi
            .safeAs<T>()
            ?.let {
                ktElement(it)
                true
            }
            ?: false
}
