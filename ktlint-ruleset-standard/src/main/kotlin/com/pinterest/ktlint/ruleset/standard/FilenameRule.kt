package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.KtLint
import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType.CLASS
import com.pinterest.ktlint.core.ast.ElementType.DOT
import com.pinterest.ktlint.core.ast.ElementType.FUN
import com.pinterest.ktlint.core.ast.ElementType.IDENTIFIER
import com.pinterest.ktlint.core.ast.ElementType.INTERFACE_KEYWORD
import com.pinterest.ktlint.core.ast.ElementType.OBJECT_DECLARATION
import com.pinterest.ktlint.core.ast.ElementType.PROPERTY
import com.pinterest.ktlint.core.ast.ElementType.TYPEALIAS
import com.pinterest.ktlint.core.ast.ElementType.TYPE_REFERENCE
import com.pinterest.ktlint.core.ast.prevCodeSibling
import java.nio.file.Paths
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.lang.FileASTNode

/**
 * If a Kotlin file contains a single class (potentially with related top-level declarations), its name should be
 * the same as the name of the class, with the .kt extension appended. If a file contains multiple classes,
 * or only top-level declarations, choose a name describing what the file contains, and name the file accordingly.
 * Use upper camel case with an uppercase first letter (also known as Pascal case),
 * for example, ProcessDeclarations.kt.
 *
 * Exceptions to this rule:
 * * file without .kt extension
 * * file with name package.kt
 * * file containing only top-level declarations on same receiver type ([see Android example](https://github.com/android/android-ktx/blob/51005889235123f41492eaaecde3c623473dfe95/src/main/java/androidx/core/graphics/Path.kt))
 *
 * **See Also:** [Kotlin lang documentation](https://kotlinlang.org/docs/coding-conventions.html#source-file-names)
 */
public class FilenameRule : Rule(
    id = "filename",
    visitorModifiers = setOf(
        VisitorModifier.RunOnRootNodeOnly
    )
) {
    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
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

        val declarations = topLevelDeclarations(node)
        if (declarations.size == 1) {
            // we have only one top-level declaration, name should be same as filename
            val element = declarations.first()
            if (fileName != element.name) {
                emit(
                    0,
                    "${element.type} ${element.escapedName} should be declared in a file named ${element.name}.kt",
                    false
                )
                return
            }
        } else {
            val receiverDeclarations = declarations.filterIsInstance<TopLevelDeclarationWithReceiverElement>()
            val allElementsHaveReceiver =
                receiverDeclarations.size >= 2 && receiverDeclarations.size == declarations.size
            if (allElementsHaveReceiver) {
                val allOfSameReceiver = receiverDeclarations.map { it.receiverTypeName }.distinct().size == 1
                if (allOfSameReceiver) {
                    // in case of top-level declarations on same receiver type
                    val element = receiverDeclarations.first()
                    if (fileName != element.receiverTypeName) {
                        emit(
                            0,
                            "All elements with receiver ${element.receiverTypeName} should be declared in a file named ${element.receiverTypeName}.kt",
                            false
                        )
                        return
                    }
                } else {
                    hasToMatchPascalCase(fileName, emit)
                }
            } else {
                hasToMatchPascalCase(fileName, emit)
            }
        }
    }

    private fun hasToMatchPascalCase(
        fileName: String,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
    ) {
        if (!pascalCase.matches(fileName)) {
            emit(0, "File name $fileName.kt should conform PascalCase", false)
        }
    }

    // https://kotlinlang.org/docs/reference/grammar.html#topLevelObject
    // https://kotlinlang.org/docs/reference/grammar.html#declaration
    private fun topLevelDeclarations(fileNode: ASTNode): List<TopLevelDeclarationElement> {
        return fileNode.getChildren(null)
            .filterNotNull()
            .filter { topLevelDeclarationSet.contains(it.elementType) }
            .map {
                when (it.elementType) {
                    CLASS ->
                        it.createTopLevelClassInterfaceElement()
                    OBJECT_DECLARATION ->
                        it.createTopLevelDeclarationElement("Object")
                    FUN ->
                        it.createTopLevelDeclarationElementWithReceiver("Extension function")
                            ?: it.createTopLevelDeclarationElement("Function")
                    PROPERTY ->
                        it.createTopLevelDeclarationElementWithReceiver("Extension property")
                            ?: it.createTopLevelDeclarationElement("Property")
                    TYPEALIAS ->
                        it.createTopLevelDeclarationElement("Typealias")
                    else -> error("Unsupported top-level type ${it.elementType}")
                }
            }
            .toList()
    }

    private open class TopLevelDeclarationElement(
        open val type: String,
        open val name: String,
        open val escapedName: String
    )

    private class TopLevelDeclarationWithReceiverElement(
        override val type: String,
        override val name: String,
        override val escapedName: String,
        val receiverTypeName: String
    ) : TopLevelDeclarationElement(type, name, escapedName)

    private companion object {
        private val topLevelDeclarationSet = setOf(
            CLASS,
            OBJECT_DECLARATION,
            FUN,
            PROPERTY,
            TYPEALIAS
        )

        private val pascalCase = """^[A-Z][A-Za-z0-9]*$""".toRegex()

        private fun ASTNode.name() = text.removeSurrounding("`")

        private fun ASTNode.createTopLevelDeclarationElement(
            type: String
        ): TopLevelDeclarationElement {
            return createTopLevelDeclarationElement(type) { null }
        }

        private fun ASTNode.createTopLevelDeclarationElement(
            type: String,
            typeFinder: (ASTNode) -> String?
        ): TopLevelDeclarationElement {
            val id = findChildByType(IDENTIFIER) ?: error("Unable to find identifier in $this")
            return TopLevelDeclarationElement(typeFinder(this) ?: type, id.name(), id.text)
        }

        private fun ASTNode.createTopLevelClassInterfaceElement(
        ): TopLevelDeclarationElement {
            return createTopLevelDeclarationElement("Class") {
                val id = findChildByType(IDENTIFIER) ?: error("Unable to find identifier in $this")
                if (id.prevCodeSibling()?.elementType == INTERFACE_KEYWORD) "Interface" else null
            }
        }

        private fun ASTNode.createTopLevelDeclarationElementWithReceiver(
            type: String
        ): TopLevelDeclarationWithReceiverElement? {
            val id = findChildByType(IDENTIFIER) ?: error("Unable to find identifier in $this")
            val prevCodeSibling = id.prevCodeSibling()
            return if (prevCodeSibling?.elementType == DOT) {
                val receiverType = prevCodeSibling.prevCodeSibling()
                if (receiverType?.elementType == TYPE_REFERENCE) {
                    TopLevelDeclarationWithReceiverElement(type, id.name(), id.text, receiverType.name())
                } else {
                    error("Unable to find Extension type-receiver at $this")
                }
            } else {
                return null
            }
        }
    }
}
