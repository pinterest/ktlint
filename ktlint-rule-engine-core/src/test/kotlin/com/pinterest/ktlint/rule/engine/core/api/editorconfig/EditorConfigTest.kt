package com.pinterest.ktlint.rule.engine.core.api.editorconfig

import com.pinterest.ktlint.rule.engine.internal.toPropertyWithValue
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatNoException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.ec4j.core.model.PropertyType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EditorConfigTest {
    @Test
    fun `Given an EditorConfig from which a non existing property is retrieved then an exception is thrown`() {
        val editorConfig = EditorConfig()

        assertThatThrownBy { editorConfig[SOME_EDITOR_CONFIG_PROPERTY] }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageStartingWith("Property '$SOME_PROPERTY_NAME' can not be retrieved from this EditorConfig.")
    }

    @Test
    fun `Given an EditorConfig from which an existing property is retrieved then return the value of that property`() {
        val editorConfig = EditorConfig(SOME_EDITOR_CONFIG_PROPERTY.toPropertyWithValue(SOME_PROPERTY_VALUE))

        val actual = editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE)
    }

    @Test
    fun `Given an empty EditorConfig and add a property with default value then the default value can be retrieved for the default code style`() {
        val editorConfig = EditorConfig().addPropertiesWithDefaultValueIfMissing(SOME_EDITOR_CONFIG_PROPERTY)

        val actual = editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        // Note that IntelliJ IDEA is the default code style for now, this will change to KTLINT_OFFICIAL in the future
        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE_INTELLIJ_IDEA)
    }

    @ParameterizedTest(name = "Code style: {0}, expected result: {1}")
    @CsvSource(
        value = [
            "android, $SOME_PROPERTY_VALUE_ANDROID",
            "android_studio, $SOME_PROPERTY_VALUE_ANDROID",
            "official, $SOME_PROPERTY_VALUE_INTELLIJ_IDEA",
            "intellij_idea, $SOME_PROPERTY_VALUE_INTELLIJ_IDEA",
            "ktlint_official, $SOME_PROPERTY_VALUE_OFFICIAL",
        ],
    )
    fun `Given an EditorConfig with a defined code style and add a property with default value then the default value can be retrieved for the default code style`(
        codeStyleValue: CodeStyleValue,
        expectedValue: String,
    ) {
        val editorConfig =
            EditorConfig(CODE_STYLE_PROPERTY.toPropertyWithValue(codeStyleValue.name))
                .addPropertiesWithDefaultValueIfMissing(SOME_EDITOR_CONFIG_PROPERTY)

        val actual = editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        assertThat(actual).isEqualTo(expectedValue)
    }

    @Test
    fun `Given an EditorConfig containing a certain property with a non-default value and add the same property again with default value then the non-default value is not overwritten`() {
        val editorConfig =
            EditorConfig(SOME_EDITOR_CONFIG_PROPERTY.toPropertyWithValue(SOME_PROPERTY_VALUE))
                .addPropertiesWithDefaultValueIfMissing(SOME_EDITOR_CONFIG_PROPERTY)

        val actual = editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE)
    }

    @Test
    fun `Given an EditorConfig from which a deprecated -error-level- property is retrieved then thrown an exception`() {
        val someDeprecationMessage = "some-deprecation-message"
        val someDeprecatedEditorConfigProperty = SOME_EDITOR_CONFIG_PROPERTY.copy(deprecationError = someDeprecationMessage)
        val editorConfig = EditorConfig().addPropertiesWithDefaultValueIfMissing(someDeprecatedEditorConfigProperty)

        assertThatThrownBy { editorConfig[someDeprecatedEditorConfigProperty] }
            .hasMessage("Property '$SOME_PROPERTY_NAME' is disallowed: $someDeprecationMessage")
    }

    @Test
    fun `Given an EditorConfig from which a deprecated -warning-level- property is retrieved then do not throw an exception`() {
        val someDeprecatedEditorConfigProperty = SOME_EDITOR_CONFIG_PROPERTY.copy(deprecationWarning = "some-deprecation-message")
        val editorConfig = EditorConfig().addPropertiesWithDefaultValueIfMissing(someDeprecatedEditorConfigProperty)

        editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        assertThatNoException()
    }

    @Test
    fun `Given an EditorConfig containing a property then 'contains' returns true when that property is retrieved`() {
        val editorConfig = EditorConfig(SOME_EDITOR_CONFIG_PROPERTY.toPropertyWithValue(SOME_PROPERTY_VALUE))

        val actual = editorConfig.contains(SOME_EDITOR_CONFIG_PROPERTY.name)

        assertThat(actual).isTrue
    }

    @Test
    fun `Given an EditorConfig then 'contains' returns false when a non-existent property is retrieved`() {
        val editorConfig = EditorConfig()

        val actual = editorConfig.contains(SOME_EDITOR_CONFIG_PROPERTY.name)

        assertThat(actual).isFalse
    }

    @Test
    fun `Given an EditorConfig containing some properties then mapping of all properties is possible`() {
        val propertyName1 = "property-1"
        val propertyName2 = "property-2"
        val propertyValue1 = "value-1"
        val propertyValue2 = "value-1"
        val editorConfig =
            EditorConfig()
                .addPropertiesWithDefaultValueIfMissing(
                    SOME_EDITOR_CONFIG_PROPERTY.copy(name = propertyName1, intellijIdeaCodeStyleDefaultValue = propertyValue1),
                    SOME_EDITOR_CONFIG_PROPERTY.copy(name = propertyName2, intellijIdeaCodeStyleDefaultValue = propertyValue2),
                )

        val actual = editorConfig.map { property -> property.name.uppercase() to property.sourceValue.uppercase() }

        assertThat(actual).containsExactly(
            propertyName1.uppercase() to propertyValue1.uppercase(),
            propertyName2.uppercase() to propertyValue2.uppercase(),
        )
    }

    @Test
    fun `Given an EditorConfig to which properties are added with the same name but different identities than those properties can not be loaded in the same EditorConfig`() {
        assertThatThrownBy {
            EditorConfig()
                .addPropertiesWithDefaultValueIfMissing(
                    SOME_EDITOR_CONFIG_PROPERTY.copy(defaultValue = SOME_PROPERTY_VALUE_ANDROID),
                    SOME_EDITOR_CONFIG_PROPERTY.copy(defaultValue = SOME_PROPERTY_VALUE_INTELLIJ_IDEA),
                )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageStartingWith(
                "Found multiple editorconfig properties with name '$SOME_PROPERTY_NAME' but having distinct identities:",
            )
    }

    @Test
    fun `Given two editorconfig properties with the same name but different identities than those properties can not be loaded in the same EditorConfig`() {
        assertThatThrownBy {
            EditorConfig()
                .filterBy(
                    setOf(
                        SOME_EDITOR_CONFIG_PROPERTY.copy(defaultValue = SOME_PROPERTY_VALUE_ANDROID),
                        SOME_EDITOR_CONFIG_PROPERTY.copy(defaultValue = SOME_PROPERTY_VALUE_INTELLIJ_IDEA),
                    ),
                )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageStartingWith(
                "Found multiple editorconfig properties with name '$SOME_PROPERTY_NAME' but having distinct identities:",
            )
    }

    @Test
    fun `Given an editorconfig containing a property and a filterBy for a property with the same name is given but with different identity then the existing property is not overwritten`() {
        val editorConfig =
            EditorConfig(SOME_EDITOR_CONFIG_PROPERTY.toPropertyWithValue(SOME_PROPERTY_VALUE_ANDROID))
                .filterBy(
                    setOf(SOME_EDITOR_CONFIG_PROPERTY.copy(defaultValue = SOME_PROPERTY_VALUE_INTELLIJ_IDEA)),
                )

        val actual = editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE_ANDROID)
    }

    @Test
    fun `Given an editorconfig containing a property for which the name of the property is not identical to the name of the property type then the property can be retrieved by name and type combination`() {
        val property1 = "property-1"
        val property2 = "property-2"
        val editorConfig =
            EditorConfig(
                SOME_EDITOR_CONFIG_PROPERTY
                    .copy(name = property1)
                    .toPropertyWithValue(SOME_PROPERTY_VALUE_ANDROID),
                SOME_EDITOR_CONFIG_PROPERTY
                    .copy(name = property2)
                    .toPropertyWithValue(SOME_PROPERTY_VALUE_INTELLIJ_IDEA),
            )

        val actual = editorConfig.getEditorConfigValue(SOME_EDITOR_CONFIG_PROPERTY.type, property2)

        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE_INTELLIJ_IDEA)
    }

    @Test
    fun `Given an editorconfig containing a property for which a property mapper is defined then the property mapper is called`() {
        val editorConfigPropertyWithPropertyMapper =
            SOME_EDITOR_CONFIG_PROPERTY
                .copy(
                    propertyMapper = { _, _ -> SOME_PROPERTY_VALUE_OFFICIAL },
                    defaultValue = SOME_PROPERTY_VALUE,
                    androidStudioCodeStyleDefaultValue = null,
                    intellijIdeaCodeStyleDefaultValue = null,
                    ktlintOfficialCodeStyleDefaultValue = null,
                )
        val editorConfig = EditorConfig().addPropertiesWithDefaultValueIfMissing(editorConfigPropertyWithPropertyMapper)

        val actual = editorConfig[editorConfigPropertyWithPropertyMapper]

        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE_OFFICIAL)
    }

    @Test
    fun `Given an editorconfig containing a property for which he value is unset then return its default value`() {
        val editorConfig = EditorConfig(
            SOME_EDITOR_CONFIG_PROPERTY.toPropertyWithValue("unset"),
        )

        val actual = editorConfig[SOME_EDITOR_CONFIG_PROPERTY]

        assertThat(actual).isEqualTo(SOME_PROPERTY_VALUE_INTELLIJ_IDEA)
    }

    private companion object {
        const val SOME_PROPERTY_NAME = "some-property-name"
        const val SOME_PROPERTY_VALUE = "some-property-value"
        const val SOME_PROPERTY_VALUE_ANDROID = "some-property-value-android"
        const val SOME_PROPERTY_VALUE_DEFAULT = "some-property-value-default"
        const val SOME_PROPERTY_VALUE_INTELLIJ_IDEA = "some-property-value-intellij-idea"
        const val SOME_PROPERTY_VALUE_OFFICIAL = "some-property-value-official"
        val SOME_EDITOR_CONFIG_PROPERTY = EditorConfigProperty(
            name = SOME_PROPERTY_NAME,
            type = PropertyType(
                SOME_PROPERTY_NAME,
                "",
                PropertyType.PropertyValueParser.IDENTITY_VALUE_PARSER,
                setOf(SOME_PROPERTY_VALUE_ANDROID, SOME_PROPERTY_VALUE_INTELLIJ_IDEA),
            ),
            defaultValue = SOME_PROPERTY_VALUE_DEFAULT,
            androidStudioCodeStyleDefaultValue = SOME_PROPERTY_VALUE_ANDROID,
            ktlintOfficialCodeStyleDefaultValue = SOME_PROPERTY_VALUE_OFFICIAL,
            intellijIdeaCodeStyleDefaultValue = SOME_PROPERTY_VALUE_INTELLIJ_IDEA,
        )
    }
}
