package com.pinterest.ktlint.ruleset.standard

import com.pinterest.ktlint.core.Rule
import com.pinterest.ktlint.core.ast.ElementType
import org.jetbrains.kotlin.KtNodeTypes.FLOAT_CONSTANT
import org.jetbrains.kotlin.KtNodeTypes.INTEGER_CONSTANT
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import java.util.Locale

class UnderscoreNumericLiteralRule : Rule("underscore-numeric-literal") {

    private val DELIMITER = "_"
    private val acceptableLength = 4
    private val chunkLength = 3

    override fun visit(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType != INTEGER_CONSTANT && node.elementType != FLOAT_CONSTANT) {
            return
        }

        val normalizedText = normalizeForMatching(node.text)
        if (isNotDecimalNumber(normalizedText) || KtConstantExpression(node).isSerialUidProperty()) {
            return
        }

        val numberString = normalizedText.split('.').first()
        if (numberString.length < acceptableLength) {
            return
        }

        if (!numberString.matches(UNDERSCORE_NUMBER_REGEX)) {
            emit(
                node.startOffset,
                "Numeric literals should be delimited with '$DELIMITER'",
                true
            )

            if (autoCorrect) {
                val decimalPoints: String? = normalizedText.split('.').getOrNull(1)
                val typeModifier = node.text.filter { it.isLetter() }
                val cleanDigits = numberString.replace(DELIMITER, "").reversed()

                val newDigits = cleanDigits.chunked(chunkLength).joinToString(DELIMITER).reversed() +
                    if (decimalPoints != null) {
                        ".$decimalPoints"
                    } else {
                        ""
                    }

                val newNode = LeafPsiElement(ElementType.INTEGER_CONSTANT, newDigits + typeModifier)
                node.replaceChild(node.firstChildNode, newNode)
            }
        }
    }

    private fun isNotDecimalNumber(rawText: String): Boolean =
        rawText.replace(DELIMITER, "").toDoubleOrNull() == null || rawText.startsWith(HEX_PREFIX) ||
            rawText.startsWith(BIN_PREFIX)

    private fun KtConstantExpression.isSerialUidProperty(): Boolean {
        val propertyElement = if (parent is KtPrefixExpression) parent?.parent else parent
        val property = propertyElement as? KtProperty
        return property != null && property.name == SERIAL_UID_PROPERTY_NAME && isSerializable(property)
    }

    private fun isSerializable(property: KtProperty): Boolean {
        var containingClassOrObject = property.containingClassOrObject
        if (containingClassOrObject is KtObjectDeclaration && containingClassOrObject.isCompanion()) {
            containingClassOrObject = containingClassOrObject.containingClassOrObject
        }
        return containingClassOrObject
            ?.superTypeListEntries
            ?.any { it.text == SERIALIZABLE } == true
    }

    private fun normalizeForMatching(text: String): String = text.trim()
        .lowercase(Locale.ROOT)
        .removeSuffix("l")
        .removeSuffix("d")
        .removeSuffix("f")
        .removeSuffix("u")

    companion object {
        private val UNDERSCORE_NUMBER_REGEX = Regex("[0-9]{1,3}(_[0-9]{3})*")
        private const val HEX_PREFIX = "0x"
        private const val BIN_PREFIX = "0b"
        private const val SERIALIZABLE = "Serializable"
        private const val SERIAL_UID_PROPERTY_NAME = "serialVersionUID"
    }
}
