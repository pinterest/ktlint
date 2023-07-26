package com.pinterest.ktlint.rule.engine.core.api.visitor.internal

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
import java.io.File

/**
 * This script generates the classes [com.pinterest.ktlint.rule.engine.core.api.visitor.AfterNodeVisitor],
 * [com.pinterest.ktlint.rule.engine.core.api.visitor.BeforeNodeVisitor] and
 * [com.pinterest.ktlint.rule.engine.core.api.visitor.NodeVisitor].
 *
 * The configuration table of this script needs to be updated whenever new `Kt` classes are introduced with new Kotlin versions. As this
 * does not happen regularly, it suffices to run this script manually.
 */
internal fun main() {
    val targetDir = "ktlint-rule-engine-core/src/main/kotlin/com/pinterest/ktlint/rule/engine/core/api/visitor"

    elementTypeMap
        .generateNodeVisitor()
        .let { content ->
            File("$targetDir/NodeVisitor.kt")
                .also { println("Generate ${it.absolutePath}") }
                .writeText(content)
        }
    elementTypeMap
        .generateBeforeNodeVisitor()
        .let { content ->
            File("$targetDir/beforeNodeVisitor.kt")
                .also { println("Generate ${it.absolutePath}") }
                .writeText(content)
        }
    elementTypeMap
        .generateAfterNodeVisitor()
        .let { content ->
            File("$targetDir/afterNodeVisitor.kt")
                .also { println("Generate ${it.absolutePath}") }
                .writeText(content)
        }
}

/*
 * Mapping is based on function available in https://github.com/JetBrains/kotlin/blob/master/compiler/psi/src/org/jetbrains/kotlin/psi/KtVisitorVoid.java
 * Not all function available in ktVisitorVoid can be mapped to ElementTypes.
 */
private val elementTypeMap =
    mapOf(
        /*
         * KtVisitorVoid contains methods below which could not be mapped to a single ElementType. As of that not overridable functions will
         * be generated:
         *  - visitAnonymousInitializer
         *  - visitClassOrObject
         *  - visitConstantExpression
         *  - visitDeclaration
         *  - visitDoubleColonExpression
         *  - visitExpression
         *  - visitExpressionWithLabel
         *  - visitIntersectionType
         *  - visitKtElement
         *  - visitLoopExpression
         *  - visitNamedDeclaration
         *  - visitNamedFunction
         *  - visitQualifiedExpression
         *  - visitSelfType
         *  - visitSimpleNameExpression
         *  - visitStringTemplateEntry
         *  - visitStringTemplateEntryWithExpression
         *  - visitSuperTypeListEntry
         *  - visitUnaryExpression
         */
        ANNOTATED_EXPRESSION to KtAnnotatedExpression::class.java,
        ANNOTATION to KtAnnotation::class.java,
        ANNOTATION_ENTRY to KtAnnotationEntry::class.java,
        ARRAY_ACCESS_EXPRESSION to KtArrayAccessExpression::class.java,
        BINARY_EXPRESSION to KtBinaryExpression::class.java,
        BINARY_WITH_TYPE to KtBinaryExpressionWithTypeRHS::class.java,
        BLOCK to KtBlockExpression::class.java,
        BREAK to KtBreakExpression::class.java,
        CALL_EXPRESSION to KtCallExpression::class.java,
        CALLABLE_REFERENCE_EXPRESSION to KtCallableReferenceExpression::class.java,
        CATCH to KtCatchClause::class.java,
        CLASS to KtClass::class.java,
        CLASS_BODY to KtClassBody::class.java,
        CLASS_INITIALIZER to KtClassInitializer::class.java,
        CLASS_LITERAL_EXPRESSION to KtClassLiteralExpression::class.java,
        COLLECTION_LITERAL_EXPRESSION to KtCollectionLiteralExpression::class.java,
        CONSTRUCTOR_CALLEE to KtConstructorCalleeExpression::class.java,
        CONSTRUCTOR_DELEGATION_CALL to KtConstructorDelegationCall::class.java,
        CONTEXT_RECEIVER_LIST to KtContextReceiverList::class.java,
        CONTINUE to KtContinueExpression::class.java,
        DELEGATED_SUPER_TYPE_ENTRY to KtDelegatedSuperTypeEntry::class.java,
        DESTRUCTURING_DECLARATION to KtDestructuringDeclaration::class.java,
        DESTRUCTURING_DECLARATION_ENTRY to KtDestructuringDeclarationEntry::class.java,
        DO_WHILE to KtDoWhileExpression::class.java,
        DOT_QUALIFIED_EXPRESSION to KtDotQualifiedExpression::class.java,
        DYNAMIC_TYPE to KtDynamicType::class.java,
        ENUM_ENTRY to KtEnumEntry::class.java,
        ESCAPE_STRING_TEMPLATE_ENTRY to KtEscapeStringTemplateEntry::class.java,
        FILE to KtFile::class.java,
        FINALLY to KtFinallySection::class.java,
        FOR to KtForExpression::class.java,
        FUNCTION_TYPE to KtFunctionType::class.java,
        IF to KtIfExpression::class.java,
        IMPORT_ALIAS to KtImportAlias::class.java,
        IMPORT_DIRECTIVE to KtImportDirective::class.java,
        IMPORT_LIST to KtImportList::class.java,
        INITIALIZER_LIST to KtInitializerList::class.java,
        IS_EXPRESSION to KtIsExpression::class.java,
        LABELED_EXPRESSION to KtLabeledExpression::class.java,
        LAMBDA_EXPRESSION to KtLambdaExpression::class.java,
        LITERAL_STRING_TEMPLATE_ENTRY to KtLiteralStringTemplateEntry::class.java,
        LONG_STRING_TEMPLATE_ENTRY to KtBlockStringTemplateEntry::class.java,
        MODIFIER_LIST to KtModifierList::class.java,
        NULLABLE_TYPE to KtNullableType::class.java,
        OBJECT_DECLARATION to KtObjectDeclaration::class.java,
        OBJECT_LITERAL to KtObjectLiteralExpression::class.java,
        PACKAGE_DIRECTIVE to KtPackageDirective::class.java,
        PARENTHESIZED to KtParenthesizedExpression::class.java,
        POSTFIX_EXPRESSION to KtPostfixExpression::class.java,
        PREFIX_EXPRESSION to KtPrefixExpression::class.java,
        PRIMARY_CONSTRUCTOR to KtPrimaryConstructor::class.java,
        PROPERTY to KtProperty::class.java,
        PROPERTY_ACCESSOR to KtPropertyAccessor::class.java,
        PROPERTY_DELEGATE to KtPropertyDelegate::class.java,
        REFERENCE_EXPRESSION to KtReferenceExpression::class.java,
        SAFE_ACCESS_EXPRESSION to KtSafeQualifiedExpression::class.java,
        SCRIPT to KtScript::class.java,
        SCRIPT_INITIALIZER to KtScriptInitializer::class.java,
        SECONDARY_CONSTRUCTOR to KtSecondaryConstructor::class.java,
        SHORT_STRING_TEMPLATE_ENTRY to KtSimpleNameStringTemplateEntry::class.java,
        STRING_TEMPLATE to KtStringTemplateExpression::class.java,
        SUPER_EXPRESSION to KtSuperExpression::class.java,
        SUPER_TYPE_CALL_ENTRY to KtSuperTypeCallEntry::class.java,
        SUPER_TYPE_ENTRY to KtSuperTypeEntry::class.java,
        SUPER_TYPE_LIST to KtSuperTypeList::class.java,
        THIS_EXPRESSION to KtThisExpression::class.java,
        THROW to KtThrowExpression::class.java,
        TRY to KtTryExpression::class.java,
        TYPE_ARGUMENT_LIST to KtTypeArgumentList::class.java,
        TYPE_CONSTRAINT to KtTypeConstraint::class.java,
        TYPE_CONSTRAINT_LIST to KtTypeConstraintList::class.java,
        TYPE_PARAMETER to KtTypeParameter::class.java,
        TYPE_PARAMETER_LIST to KtTypeParameterList::class.java,
        TYPE_PROJECTION to KtTypeProjection::class.java,
        TYPE_REFERENCE to KtTypeReference::class.java,
        TYPEALIAS to KtTypeAlias::class.java,
        USER_TYPE to KtUserType::class.java,
        VALUE_ARGUMENT to KtValueArgument::class.java,
        VALUE_ARGUMENT_LIST to KtValueArgumentList::class.java,
        VALUE_PARAMETER to KtParameter::class.java,
        VALUE_PARAMETER_LIST to KtParameterList::class.java,
        WHEN to KtWhenExpression::class.java,
        WHEN_CONDITION_IN_RANGE to KtWhenConditionInRange::class.java,
        WHEN_CONDITION_IS_PATTERN to KtWhenConditionIsPattern::class.java,
        WHEN_CONDITION_WITH_EXPRESSION to KtWhenConditionWithExpression::class.java,
        WHEN_ENTRY to KtWhenEntry::class.java,
        WHILE to KtWhileExpression::class.java,
    )
