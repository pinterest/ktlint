package com.pinterest.ktlint.core.api.editorconfig

import org.ec4j.core.model.PropertyType

@Deprecated(
    // Keep postponing the deprecation period until around 0.50. Some projects irregular update to newer KtLint
    // version and skipping intermediate version. As of such they might have missed the deprecation warning in
    // KtLint 0.47.
    "Marked for removal in KtLint 0.49. Update all your all '.editorconfig' files. See " +
        "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
        "for more information",
)
public val DISABLED_RULES_PROPERTY: EditorConfigProperty<String> =
    createDisabledRulesEditorConfigProperty("disabled_rules")

@Deprecated(
    // Keep postponing the deprecation period until around 0.50. Some projects irregular update to newer KtLint
    // version and skipping intermediate version.
    "Marked for removal in KtLint 0.49. Update all your all '.editorconfig' files. See " +
        "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
        "for more information",
)
// This property was never exposed in an official release. However, it should be kept until property
// UsesEditorConfigProperties.ktlintDisabledRulesProperty has been removed.
internal val KTLINT_DISABLED_RULES_PROPERTY: EditorConfigProperty<String> =
    createDisabledRulesEditorConfigProperty("ktlint_disabled_rules")

private fun createDisabledRulesEditorConfigProperty(name: String) = EditorConfigProperty(
    name = name,
    type = PropertyType.LowerCasingPropertyType(
        "disabled_rules",
        "A comma separated list of rule ids which should not be run. For rules not defined in the 'standard' ruleset, the qualified rule-id should be used.",
        PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
        emptySet(),
    ),
    defaultValue = "",
    propertyMapper = { property, _ ->
        when {
            property?.isUnset == true -> ""
            property?.getValueAs<String>() != null -> {
                // Remove spaces (most likely they occur only around the comma) as they otherwise will be seen
                // as part of the rule-id which is to be disabled. But as the space is not allowed in the id's
                // of rule sets and rule ids, they are just removed all.
                property.getValueAs<String>().replace(" ", "")
            }

            else -> property?.getValueAs()
        }
    },
    // TODO: Mark this property with a deprecationError starting from KtLint 0.49
    deprecationWarning = "Update all your all '.editorconfig' files. See " +
        "https://pinterest.github.io/ktlint/faq/#why-is-editorconfig-property-disabled_rules-deprecated-and-how-do-i-resolve-this " +
        "for more information",
)
