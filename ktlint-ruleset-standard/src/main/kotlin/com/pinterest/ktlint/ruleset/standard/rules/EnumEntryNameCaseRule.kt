package com.pinterest.ktlint.ruleset.standard.rules

import com.pinterest.ktlint.rule.engine.core.api.AutocorrectDecision
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.EXPERIMENTAL
import com.pinterest.ktlint.rule.engine.core.api.SinceKtlint.Status.STABLE
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfigProperty
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.SafeEnumValueParser
import com.pinterest.ktlint.ruleset.standard.StandardRule
import com.pinterest.ktlint.ruleset.standard.rules.internal.regExIgnoringDiacriticsAndStrokesOnLetters
import org.ec4j.core.model.PropertyType
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.psi.KtEnumEntry

/**
 * https://kotlinlang.org/docs/coding-conventions.html#property-names
 */
@SinceKtlint("0.36", EXPERIMENTAL)
@SinceKtlint("0.46", STABLE)
public class EnumEntryNameCaseRule :
    StandardRule(
        id = "enum-entry-name-case",
        usesEditorConfigProperties = setOf(ENUM_ENTRY_NAME_CASING_PROPERTY),
    ) {
    private lateinit var enumEntryCasingRegex: Regex
    private lateinit var enumEntryCasingViolation: String
    private var x = ENUM_ENTRY_NAME_CASING_PROPERTY.defaultValue

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        x = editorConfig[ENUM_ENTRY_NAME_CASING_PROPERTY]
        when (editorConfig[ENUM_ENTRY_NAME_CASING_PROPERTY]) {
            EnumEntryNameCasing.upper_cases -> {
                enumEntryCasingRegex = "[A-Z][A-Z_\\d]*".regExIgnoringDiacriticsAndStrokesOnLetters()
                enumEntryCasingViolation = "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\""
            }

            EnumEntryNameCasing.camel_cases -> {
                enumEntryCasingRegex = "[A-Z]([A-Za-z\\d]*)".regExIgnoringDiacriticsAndStrokesOnLetters()
                enumEntryCasingViolation = "Enum entry name should be upper camel-case like \"EnumEntry\""
            }

            EnumEntryNameCasing.upper_or_camel_cases -> {
                enumEntryCasingRegex = "[A-Z]([A-Za-z\\d]*|[A-Z_\\d]*)".regExIgnoringDiacriticsAndStrokesOnLetters()
                enumEntryCasingViolation =
                    "Enum entry name should be uppercase underscore-separated names like \"ENUM_ENTRY\" or upper camel-case like \"EnumEntry\""
            }
        }
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> AutocorrectDecision,
    ) {
        if (node !is CompositeElement) {
            return
        }
        val enumEntry = node.psi as? KtEnumEntry ?: return
        val name = enumEntry.name ?: return

        if (!name.matches(enumEntryCasingRegex)) {
            emit(node.startOffset, enumEntryCasingViolation, false)
        }
    }

    public companion object {
        @Suppress("EnumEntryName")
        public enum class EnumEntryNameCasing {
            /**
             * Enforce all enum entry names to be uppercase underscore-separated names like "ENUM_ENTRY". Digits, diacritics and strokes are
             * allowed.
             */
            upper_cases,

            /**
             * Enforce all enum entry names to be upper camel-case like "EnumEntry". Digits, diacritics and strokes are allowed.
             */
            camel_cases,

            /**
             * Enforce all enum entry names to be uppercase underscore-separated names like "ENUM_ENTRY" or upper camel-case like
             * "EnumEntry". Digits, diacritics and strokes are allowed.
             */
            upper_or_camel_cases,
        }

        public val ENUM_ENTRY_NAME_CASING_PROPERTY_TYPE: PropertyType.LowerCasingPropertyType<EnumEntryNameCasing> =
            PropertyType.LowerCasingPropertyType(
                "ktlint_enum_entry_name_casing",
                "Enforce all enum entry names to be uppercase underscore-separated names like \"ENUM_ENTRY\" and/or upper " +
                    "camel-case like \"EnumEntry\". Digits, diacritics and strokes are always allowed.",
                SafeEnumValueParser(EnumEntryNameCasing::class.java),
                EnumEntryNameCasing.entries.map { it.name.lowercase() }.toSet(),
            )

        public val ENUM_ENTRY_NAME_CASING_PROPERTY: EditorConfigProperty<EnumEntryNameCasing> =
            EditorConfigProperty(
                type = ENUM_ENTRY_NAME_CASING_PROPERTY_TYPE,
                defaultValue = EnumEntryNameCasing.upper_or_camel_cases,
            )
    }
}

public val ENUM_ENTRY_NAME_CASE_RULE_ID: RuleId = EnumEntryNameCaseRule().ruleId
