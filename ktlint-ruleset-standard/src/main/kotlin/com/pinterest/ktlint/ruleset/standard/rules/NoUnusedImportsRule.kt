package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.ElementType.BY_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT_QUALIFIED_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.FILE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IDENTIFIER
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PACKAGE_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.REFERENCE_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.IgnoreKtlintSuppressions
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.ifAutocorrectAllowed
import com.pinterest.ktlint.rule.engine.core.api.isPartOf
import com.pinterest.ktlint.rule.engine.core.api.isRoot20
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline20
import com.pinterest.ktlint.rule.engine.core.api.lastChildLeafOrSelf20
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.nextSibling
import com.pinterest.ktlint.rule.engine.core.api.nextSibling20
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevSibling20
import com.pinterest.ktlint.rule.engine.core.api.remove
import com.pinterest.ktlint.ruleset.standard.StandardRule
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens.MARKDOWN_LINK
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.resolve.ImportPath

@SinceKtlint("0.2", STABLE)
public class NoUnusedImportsRule :
    StandardRule("no-unused-imports"),
    // Prevent that imports which are only used inside code that is suppressed are (falsely) reported as unused.
    IgnoreKtlintSuppressions {
    private val ref =
        mutableSetOf(
            Reference("*", false),
        )
    private val parentExpressions = mutableSetOf<String>()
    private val imports = mutableMapOf<ImportPath, ASTNode>()
    private var packageName = ""
    private var rootNode: ASTNode? = null
    private var foundByKeyword = false

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.isRoot20) {
            rootNode = node
        }
        when (node.elementType) {
            PACKAGE_DIRECTIVE -> {
                val packageDirective = node.psi as KtPackageDirective
                packageName = packageDirective.qualifiedName
            }

            IMPORT_DIRECTIVE -> {
                val importPath = (node.psi as KtImportDirective).importPath!!
                if (imports.containsKey(importPath)) {
                    // Emit directly when same import occurs more than once
                    emit(node.startOffset, "Unused import", true)
                        .ifAutocorrectAllowed {
                            node.psi.delete()
                        }
                } else {
                    imports[importPath] = node
                }
            }

            DOT_QUALIFIED_EXPRESSION -> {
                if (node.isExpressionForStaticImportWithExistingParentImport()) {
                    parentExpressions.add(node.text.substringBeforeLast("("))
                }
            }

            MARKDOWN_LINK -> {
                val linkText =
                    node
                        .text
                        .removeSurrounding("[", "]")
                        .removeBackticksAndTrim()
                ref.add(Reference(linkText.split('.').first(), false))
                ref.add(Reference(linkText.split('.').last(), false))
            }

            REFERENCE_EXPRESSION, OPERATION_REFERENCE -> {
                if (!node.isPartOf(IMPORT_DIRECTIVE)) {
                    val identifier =
                        if (node is CompositeElement) {
                            node.findChildByType(IDENTIFIER)
                        } else {
                            node
                        }
                    identifier
                        ?.let { identifier.text }
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            ref.add(
                                Reference(
                                    it.removeBackticksAndTrim(),
                                    node.isParentDotQualifiedExpressionOrNull(),
                                ),
                            )
                        }
                }
            }

            BY_KEYWORD -> {
                foundByKeyword = true
            }
        }
    }

    override fun afterVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node.elementType == FILE) {
            val directCalls = ref.filter { !it.inDotQualifiedExpression }.map { it.text }
            parentExpressions.forEach { parent ->
                imports
                    .filterKeys { import ->
                        val importPath = import.pathStr.removeBackticksAndTrim()
                        importPath.endsWith(".$parent") && directCalls.none { importPath.endsWith(".$it") }
                    }.forEach { (importPath, importNode) ->
                        emit(importNode.startOffset, "Unused import", true)
                            .ifAutocorrectAllowed {
                                imports.remove(importPath, importNode)
                                importNode.removeImportDirective()
                            }
                    }
            }

            imports.forEach { (_, node) ->
                val importDirective = node.psi as KtImportDirective
                val name =
                    importDirective
                        .importPath
                        ?.importedName
                        ?.asString()
                        ?.removeBackticksAndTrim()
                val importPath = importDirective.importPath?.pathStr?.removeBackticksAndTrim()!!
                if (importDirective.aliasName == null &&
                    (packageName.isEmpty() || importPath.startsWith("$packageName.")) &&
                    importPath.substring(packageName.length + 1).indexOf('.') == -1
                ) {
                    // Allow imports without alias for which the fully qualified path is equal to the package name. See
                    // https://github.com/pinterest/ktlint/issues/2821 for an example in which marking an import from the same package
                    // led to compile failure.
                } else if (name != null &&
                    (!ref.map { it.text }.contains(name) || !isAValidImport(importPath)) &&
                    !OPERATOR_SET.contains(name) &&
                    !name.isComponentN() &&
                    !importPath.ignoreProvideDelegate()
                ) {
                    emit(node.startOffset, "Unused import", true)
                        .ifAutocorrectAllowed {
                            val nextSibling = node.nextSibling20
                            if (nextSibling == null) {
                                // Last import
                                node
                                    .lastChildLeafOrSelf20
                                    .nextLeaf
                                    ?.takeIf { it.isWhiteSpaceWithNewline20 }
                                    ?.let { whitespace ->
                                        if (node.prevLeaf == null) {
                                            // Also it was the first import, and it is not preceded by any other node containing some text. So
                                            // all whitespace until the next is redundant
                                            whitespace.remove()
                                        } else {
                                            val textAfterFirstNewline =
                                                whitespace
                                                    .text
                                                    .substringAfter("\n")
                                            if (textAfterFirstNewline.isNotBlank()) {
                                                (whitespace as LeafElement).rawReplaceWithText(textAfterFirstNewline)
                                            }
                                        }
                                    }
                            } else {
                                nextSibling
                                    .takeIf { it.isWhiteSpaceWithNewline20 }
                                    ?.remove()
                            }
                            importDirective.delete()
                        }
                }
            }
        }
    }

    private fun String.ignoreProvideDelegate() =
        if (endsWith(".provideDelegate")) {
            // Ignore provideDelegate if the `by` keyword is found anywhere in the file
            foundByKeyword
        } else {
            false
        }

    private fun ASTNode.removeImportDirective() {
        require(this.elementType == IMPORT_DIRECTIVE)
        when {
            treeParent.firstChildNode == this -> {
                nextSibling20
                    ?.takeIf { it.isWhiteSpaceWithNewline20 }
                    ?.let { it.treeParent.removeChild(it) }
            }

            treeParent.lastChildNode == this -> {
                prevSibling20
                    ?.takeIf { it.isWhiteSpaceWithNewline20 }
                    ?.remove()
            }

            else -> {
                nextLeaf
                    ?.takeIf { it.isWhiteSpaceWithNewline20 }
                    ?.let { it.treeParent.removeChild(it) }
            }
        }
        this.remove()
    }

    private fun ASTNode.isExpressionForStaticImportWithExistingParentImport(): Boolean {
        if (!containsMethodCall()) {
            return false
        }

        val methodCallExpression = text.substringBeforeLast("(")

        // Only check static imports; identified if they start with a capital letter indicating a
        // class name rather than a sub-package
        if (methodCallExpression.isNotEmpty() && methodCallExpression[0] !in 'A'..'Z') {
            return false
        }

        imports
            .filterKeys { it.pathStr.removeBackticksAndTrim().endsWith(".$methodCallExpression") }
            .forEach { import ->
                val count =
                    imports.count {
                        it.key.pathStr.removeBackticksAndTrim().startsWith(
                            import
                                .key
                                .pathStr
                                .removeBackticksAndTrim()
                                .substringBefore(methodCallExpression),
                        )
                    }
                // Parent import and static import both are present
                if (count > 1) {
                    return true
                }
            }
        return false
    }

    private fun ASTNode.containsMethodCall() = text.split(".").last().contains("(")

    // Check if the import being checked is present in the filtered import list
    private fun isAValidImport(importPath: String) =
        imports.any {
            it
                .key
                .pathStr
                .removeBackticksAndTrim()
                .contains(importPath)
        }

    private fun String.isComponentN() = COMPONENT_N_REGEX.matches(this)

    private fun ASTNode.isParentDotQualifiedExpressionOrNull(): Boolean {
        val callExpressionOrThis = parentCallExpressionOrNull() ?: this
        return callExpressionOrThis.isDotQualifiedExpression()
    }

    private fun ASTNode.parentCallExpressionOrNull() =
        treeParent
            .takeIf { it.elementType == ElementType.CALL_EXPRESSION }

    private fun ASTNode.isDotQualifiedExpression() =
        treeParent
            ?.takeIf { it.elementType == DOT_QUALIFIED_EXPRESSION }
            ?.let { it.psi as? KtDotQualifiedExpression }
            ?.takeIf { it.selectorExpression?.node == this }
            .let { it != null }

    private fun String.removeBackticksAndTrim() = replace("`", "").trim()

    private data class Reference(
        val text: String,
        val inDotQualifiedExpression: Boolean,
    )

    private companion object {
        val COMPONENT_N_REGEX = Regex("^component\\d+$")

        @Suppress("ktlint:standard:argument-list-wrapping")
        val OPERATOR_SET =
            setOf(
                // unary
                "unaryPlus", "unaryMinus", "not",
                // inc/dec
                "inc", "dec",
                // arithmetic
                "plus", "minus", "times", "div", "rem", "mod", "rangeTo", "rangeUntil",
                // in
                "contains",
                // indexed access
                "get", "set",
                // invoke
                "invoke",
                // (augmented) assignment
                "assign", "plusAssign", "minusAssign", "timesAssign", "divAssign", "modAssign",
                // (in)equality
                "equals",
                // comparison
                "compareTo",
                // iteration (https://github.com/shyiko/ktlint/issues/40)
                "iterator",
                // by (https://github.com/shyiko/ktlint/issues/54)
                "getValue", "setValue",
            )
    }
}

public val NO_UNUSED_IMPORTS_RULE_ID: RuleId = NoUnusedImportsRule().ruleId
