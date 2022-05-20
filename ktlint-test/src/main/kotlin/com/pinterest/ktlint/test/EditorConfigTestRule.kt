package com.pinterest.ktlint.test

import com.pinterest.ktlint.core.api.FeatureInAlphaState
import java.io.File
import org.ec4j.core.model.PropertyType

@Deprecated(
    message = "Marked for removal in Ktlint 0.46. See KDoc on method call for more information."
)
@FeatureInAlphaState
public class EditorConfigTestRule {

    @Deprecated(
        message = "Implementation has been removed as it depended on a JUnit 4 implementation which no longer is " +
            "being used by Ktlint.",
        replaceWith = ReplaceWith(""),
        level = DeprecationLevel.ERROR
    )
    /**
     * Unit tests should not use the filesystem if that can be avoided. Unit tests that depend on an `.editorconfig`
     * property should now be written as follows:
     * ```
     * MaxLineLengthRule().lint(
     *     "some-code",
     *     EditorConfigOverride.from(
     *         maxLineLengthProperty to 40,
     *         ignoreBackTickedIdentifierProperty to true
     *     )
     * )
     * ```
     * All helper methods in [com.pinterest.ktlint.test.RuleExtensionKt] class now support the EditorConfigOverride
     * values. Please, be aware that this class also contains a lot of deprecated methods which you should not use
     * either.
     */
    public fun writeToEditorConfig(content: Map<PropertyType<*>, String>, lintedFileExtension: String = ".kt"): File {
        throw UnsupportedOperationException("This functionality has been removed. See deprecation notice and KDoc.")
    }
}
