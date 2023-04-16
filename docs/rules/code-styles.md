In ktlint `0.49` the new code style `ktlint_official` is introduced. This code style is work in progress but will become the default code style in the `1.0` release. Please try out the new code style and provide your feedback via the [issue tracker](https://github.com/pinterest/ktlint/issues).

```ini
[*.{kt,kts}]
ktlint_code_style = ktlint_official
```

This `ktlint_official` code style combines the best elements from the [Kotlin Coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Android's Kotlin styleguide](https://developer.android.com/kotlin/style-guide). This code style also provides additional formatting on topics which are not (explicitly) mentioned in those conventions and style guide.

!!! note
    Be aware that this code style in some cases formats code in a way which is not accepted by the default code formatters in IntelliJ IDEA and Android Studio. The formatters of those editors produce nicely formatted code in the vast majority of cases. But in a number of edge cases, the formatting contains bugs which are waiting to be fixed for several years. The new code style formats code in a way which is compatible with the default formatting of the editors whenever possible. When using this codestyle, it is best to disable (e.g. not use) code formatting in the editor.

The existing code styles have been renamed to make more clear what the basis of the code style is.

* The `official` code style has been renamed to `intellij_idea`. Code formatted with this code style aims to be compatible with default formatter of IntelliJ IDEA. This code style is based on [Kotlin Coding conventions](https://kotlinlang.org/docs/coding-conventions.html). If `.editorconfig` property `ktlint_code_style` has been set to `official` then do not forget to change the value of that property to `intellij_idea`. When not set, this is still the default code style of ktlint `0.49`. Be aware that the default code style will be changed to `ktlint_official` in the `1.0` release.

* Code style `android` has been renamed to `android_studio`. Code formatted with this code style aims to be compatible with default formatter of Android Studio. This code style is based on [Android's Kotlin styleguide](https://developer.android.com/kotlin/style-guide). If `.editorconfig` property `ktlint_code_style` has been set to `android` then do not forget to change the value of that property to `android_studio`.
