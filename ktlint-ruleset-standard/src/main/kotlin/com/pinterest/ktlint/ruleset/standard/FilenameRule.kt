package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.MODIFIER_LIST
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.TYPEALIAS
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.children
import com.pinterest.ktlint.core.ast.isRoot
import com.pinterest.ktlint.ruleset.standard.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import java.nio.file.Paths
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType

/**
 * [Kotlin lang documentation](https://kotlinlang.org/docs/coding-conventions.html#source-file-names):
 * If a Kotlin file contains a single class (potentially with related top-level declarations), its name should be
 * the same as the name of the class, with the `.kt` extension appended. If a file contains multiple classes,
 * or only top-level declarations, choose a name describing what the file contains, and name the file accordingly.
 * Use upper camel case with an uppercase first letter (also known as Pascal case),
 * for example, `ProcessDeclarations.kt`.
 *
 * According to issue https://youtrack.jetbrains.com/issue/KTIJ-21897/Kotlin-coding-convention-file-naming-for-class,
 * "class" above should be read as any type of class (data class, enum class, sealed class) and interfaces.
 *
 * A strict implementation of guideline above had unwanted consequences:
 *   - If the file contains a single top level private class, it does not make sense to force the name of file to be
 *     identical to that class.
 *   - If the file contains a coherent set of functions and one of those function returns an instance of a public class
 *     which happens to be the only top level class in that file, it might not always be best to force the file to be
 *     named after that class.
 *   - Existing functionality regarding files containing a single top level object/typealias was lost.
 *
 * Exceptions to this rule:
 * - file without `.kt` extension
 * - file with name `package.kt`
 */
public class FilenameRule : Rule("filename") {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (node.isRoot()) {
            node as FileASTNode? ?: error("node is not ${FileASTNode::class} but ${node::class}")

            val filePath = node.getUserData(KtLint.FILE_PATH_USER_DATA_KEY)
            if (filePath?.endsWith(".kt") != true) {
                // ignore all non ".kt" files (including ".kts")
                return
            }

            val fileName = Paths.get(filePath).fileName.toString().substringBefore(".")
            if (fileName == "package") {
                // ignore package.kt filename
                return
            }

            val topLevelClassDeclarations = node.topLevelDeclarations(CLASS)
            if (topLevelClassDeclarations.size == 1) {
                val topLevelClassDeclaration = topLevelClassDeclarations.first()
                if (node.hasTopLevelDeclarationNotExtending(topLevelClassDeclaration.identifier)) {
                    fileName.shouldMatchPascalCase(emit)
                } else {
                    // If the file only contains one (non private) top level class and possibly some extension functions of
                    // that class, then its filename should be identical to the class name.
                    fileName.shouldMatchClassName(topLevelClassDeclaration.identifier, emit)
                }
            } else {
                val topLevelDeclarations = node.topLevelDeclarations()
                if (topLevelDeclarations.size == 1) {
                    val topLevelDeclaration = topLevelDeclarations.first()
                    if (topLevelDeclaration.elementType == OBJECT_DECLARATION ||
                        topLevelDeclaration.elementType == TYPEALIAS
                    ) {
                        val pascalCaseIdentifier =
                            topLevelDeclaration
                                .identifier
                                .toPascalCase()
                        fileName.shouldMatchFileName(pascalCaseIdentifier, emit)
                    } else {
                        fileName.shouldMatchPascalCase(emit)
                    }
                } else {
                    fileName.shouldMatchPascalCase(emit)
                }
            }
        }
    }

    private fun ASTNode.topLevelDeclarations(elementType: IElementType? = null): List<TopLevelDeclaration> =
        children()
            .filter { elementType == null || it.elementType == elementType }
            .filter { it.doesNotHavePrivateModifier() }
            .mapNotNull { it.toTopLevelDeclaration() }
            .distinct()
            .toList()

    private fun ASTNode.doesNotHavePrivateModifier(): Boolean =
        findChildByType(MODIFIER_LIST)
            ?.children()
            ?.none { it.text == "private" }
            ?: true

    private fun ASTNode.hasTopLevelDeclarationNotExtending(className: String) =
        children()
            .filter { it.doesNotHavePrivateModifier() }
            .any { it.isNotClassRelatedTopLevelDeclaration() || it.isFunctionNotExtending(className) }

    private fun ASTNode.isNotClassRelatedTopLevelDeclaration() =
        elementType in NON_CLASS_RELATED_TOP_LEVEL_DECLARATION_TYPES

    private fun ASTNode.isFunctionNotExtending(className: String) =
        elementType == FUN &&
            findChildByType(TYPE_REFERENCE)?.text?.let { !it.contains(className) } ?: true

    private fun String.shouldMatchClassName(
        className: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (this != className) {
            emit(
                0,
                "File '$this.kt' contains a single class and possibly also extension functions for that class and should be named same after that class '$className.kt'",
                false
            )
        }
    }

    private fun String.toPascalCase() =
        replaceFirstChar { it.uppercaseChar() }

    private fun String.shouldMatchFileName(
        filename: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (this != filename) {
            emit(
                0,
                "File '$this.kt' contains a single top level declaration and should be named '$filename.kt'",
                false
            )
        }
    }

    private fun String.shouldMatchPascalCase(
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (!this.matches(pascalCaseRegEx)) {
            emit(0, "File name '$this.kt' should conform PascalCase", false)
        }
    }

    private data class TopLevelDeclaration(
        val elementType: IElementType,
        val identifier: String
    )

    private fun ASTNode.toTopLevelDeclaration(): TopLevelDeclaration? =
        findChildByType(IDENTIFIER)
            ?.text
            ?.removeSurrounding("`")
            ?.let { TopLevelDeclaration(elementType, it) }

    private companion object {
        val pascalCaseRegEx = "^[A-Z][A-Za-z\\d]*$".regExIgnoringDiacriticsAndStrokesOnLetters()
        val NON_CLASS_RELATED_TOP_LEVEL_DECLARATION_TYPES = listOf(OBJECT_DECLARATION, TYPEALIAS, PROPERTY)
    }
}
