package io.github.ktlint.core.rule.engine.core.api.editorconfig

/**
 * Code style to be used while linting and formatting.
 */
public enum class CodeStyleValue {
    /**
     * Code formatting based on Android's Kotlin styleguide (https://developer.android.com/kotlin/style-guide). This
     * code style aims to be compatible with code formatting in Android Studio.
     */
    ANDROID_STUDIO,

    /**
     * Code formatting based on Kotlin Coding conventions (https://kotlinlang.org/docs/coding-conventions.html). This
     * code style aims to be compatible with code formatting in IntelliJ IDEA.
     */
    INTELLIJ_IDEA,

    /**
     * Code formatting based on the best of both the Kotlin Coding conventions
     * (https://kotlinlang.org/docs/coding-conventions.html) and Android's Kotlin styleguide
     * (https://developer.android.com/kotlin/style-guide). This codestyle also provides additional formatting on topics
     * which are not (explicitly) mentioned in the before mentioned styleguide. Also, this code style sometimes formats
     * code in a way which is not compatible with the default code formatters in IntelliJ IDEA and Android Studio. When
     * using this codestyle, it is best to disable (e.g. not use) automatic code formatting in the editor. Mean reason
     * for deviating from the code formatting provided by the editor is that those contain bugs which after some years
     * are still not fixed.
     * In the long run, this code style becomes the default code style provided by KtLint.
     */
    KTLINT_OFFICIAL,
}
