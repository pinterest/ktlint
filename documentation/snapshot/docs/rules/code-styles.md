Starting from version `1.0`, `ktlint_official` is the default code style. If you want to revert to another code style, then set the `.editorconfig` property `ktlint_code_style`.

```ini
[*.{kt,kts}]
ktlint_code_style = intellij_idea # or android_studio or ktlint_official (default)
```

The `ktlint_official` code style combines the best elements from the [Kotlin Coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android's Kotlin styleguide](https://developer.android.com/kotlin/style-guide). This code style also provides additional formatting on topics which are not (explicitly) mentioned in those conventions and style guide.

!!! note
    Be aware that this code style in some cases formats code in a way which is not accepted by the default code formatters in IntelliJ IDEA and Android Studio. The formatters of those editors produce nicely formatted code in the vast majority of cases. But in a number of edge cases, the formatting contains bugs which are waiting to be fixed for several years. The new code style formats code in a way which is compatible with the default formatting of the editors whenever possible. When using this codestyle, it is best to disable (e.g. not use) code formatting in the editor.

* The `intellij_idea` (formerly `official`) code style aims to be compatible with default formatter of IntelliJ IDEA. This code style is based on [Kotlin Coding conventions](https://kotlinlang.org/docs/coding-conventions.html).

* The `android_studio` (formerly `android`) aims to be compatible with default formatter of Android Studio. This code style is based on [Android's Kotlin styleguide](https://developer.android.com/kotlin/style-guide).
