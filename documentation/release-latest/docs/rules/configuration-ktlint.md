Ktlint uses a limited set of `.editorconfig` properties for additional configuration. A sensible default value is provided for each property when not explicitly defined. Properties can be overridden, provided they are specified under `[*.{kt,kts}]`. Ktlint uses some properties defined by [.editorconfig](https://editorconfig.org/), IntelliJ IDEA and custom properties.

!!! danger

    Unfortunately [IntelliJ IDEA](https://www.jetbrains.com/idea/) has an [autoformat issue regarding `.editorconfig`](https://youtrack.jetbrains.com/issue/IDEA-242506). Due to this error an additional space is added between glob statements, resulting in `[*{kt, kts}]` instead of `[*{kt,kts}]`. The `.editorconfig` library used by `ktlint` [ignores sections after encountering a space in the list](https://github.com/editorconfig/editorconfig/issues/148). As a result, the rule is not applied on all files as documented in the [original ktlint issue](https://github.com/pinterest/ktlint/issues/762).

## Code style

By default, the `ktlint_official` code style is applied. Alternatively, the code style can be set to `intellij_idea` or `android_studio`.

```ini
[*.{kt,kts}]
ktlint_code_style = ktlint_official
```

## Disabled rules

Rule sets and individual rules can be disabled / enabled with a separate property per rule (set).

All rules in a rule set can be enabled or disabled with a rule set property. The name of the rule set property consists of the `ktlint_` prefix followed by the rule set id. Examples:
```editorconfig
ktlint_standard = disabled # Disable all rules from the `standard` rule set provided by KtLint
ktlint_experimental = enabled # Enable all `experimental` rules from all rule sets provided by KtLint or other rule providers
ktlint_custom-rule-set = enabled # Enable all rules in the `custom-rule-set` rule set (not provided by KtLint)
```

Rules that are marked as experimental will not be run, unless explicitly enabled:
```editorconfig
ktlint_experimental = enabled # Enable rules marked as experimental for all rule sets that are enabled
```

An individual rule can be enabled or disabled with a rule property. The name of the rule property consists of the `ktlint_` prefix followed by the rule set id followed by a `_` and the rule id. Examples:
```editorconfig
ktlint_standard_final-newline = disabled # Disables the `final-newline` rule provided by KtLint
ktlint_standard_some-experimental-rule = enabled # Enables the (experimental) `some-experimental-rule` in the `standard` rule set provided by KtLint
ktlint_custom-rule-set_custom-rule = disabled # Disables the `custom-rule` rule in the `custom-rule-set` rule set (not provided by KtLint)
```

!!! note
    The *rule* properties are applied after applying the *rule set* properties and take precedence. So if a rule set is disabled but a specific rule of that rule set is enabled, then the rule will be executed.


## Final newline

By default, a final newline is required at the end of the file.

```ini
[*.{kt,kts}]
insert_final_newline = true
```

This setting only takes effect when rule `final-newline` is enabled.

## Force multiline chained methods based on number of chain operators

Setting `ktlint_chain_method_rule_force_multiline_when_chain_operator_count_greater_or_equal_than` forces a chained method to be wrapped at each chain operator (`.` or `?.`) in case it contains the specified minimum number of chain operators even in case the entire chained method fits on a single line. Use value `unset` (default) to disable this setting.

!!! note
    By default, chained methods are wrapped when an expression contains 4 or more chain operators in an expression. Note that if a chained method contains nested expressions the chain operators of the inner expression are not taken into account.

```ini
[*.{kt,kts}]
ktlint_chain_method_rule_force_multiline_when_chain_operator_count_greater_or_equal_than=unset
```

This setting only takes effect when rule `chain-method-continution` is enabled.

## Force multiline function signature based on number of parameters

Setting `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than` forces a multiline function signature in case the function contains the specified minimum number of parameters even in case the function signature would fit on a single line. Use value `unset` (default) to disable this setting.

!!! note
    By default, the `ktlint_official` code style wraps parameters of functions having at least 2 parameters. For other code styles, this setting is disabled by default. 

```ini
[*.{kt,kts}]
ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than=unset
```

This setting only takes effect when rule `function-signature` is enabled.

## Wrapping the expression body of a function

Setting `ktlint_function_signature_body_expression_wrapping` determines if and when the expression body of a function is wrapped to a new line. This setting can be set to value `default`, `multiline` or `always`. 

!!! note
    In this context `default` means the default behavior of the IntelliJ IDEA formatter. If not set explicitly, this style is used by code styles `intellij_idea` and `android_studio`. Code style `ktlint_official` uses style `multiline` when this setting has no value.

When set to `default`, the first line of a body expression is appended to the function signature as long as the max line length is not exceeded.

```kotlin title="ktlint_function_signature_body_expression_wrapping=default"
// Given that the function signature has to be written as a single line function signature
fun someFunction(a: Any, b: Any): String = "some-result"
    .uppercase()

// Given that the function signature has to be written as a multiline function signature
fun someFunction(
    a: Any,
    b: Any
): String = "some-result"
    .uppercase()
```

When set to `multiline`, the body expression starts on a separate line in case it is a multiline expression. A single line body expression is wrapped only when it does not fit on the same line as the function signature.

```kotlin title="ktlint_function_signature_body_expression_wrapping=multiline"
// Given a single line body expression and
// a the function signature that has to be written as a single line function signature and
// it does not exceed the max line length
fun someFunction(a: Any, b: Any): String = "some-result".uppercase()

// Given a single line body expression and
// a the function signature that has to be written as a multiline function signature and
// it does not exceed the max line length
fun someFunction(
    a: Any,
    b: Any
): String = "some-result".uppercase()

// Given a single line body expression then always wrap it to a separate line
fun someFunction(a: Any, b: Any): String =
    "some-result"
         .uppercase()
fun someFunction(
    a: Any,
    b: Any
): String =
    "some-result"
       .uppercase()
```

When set to `always` the body expression is always wrapped to a separate line.

```kotlin title="ktlint_function_signature_body_expression_wrapping=always"
fun someFunction(a: Any, b: Any): String =
    "some-result".uppercase()
fun functionWithAVeryLongName(
    a: Any,
    b: Any
): String =
    "some-result"
        .uppercase()
```

This setting only takes effect when rule `function-signature` is enabled.

## Ignore identifiers enclosed in backticks

By default, the identifiers enclosed in backticks are not ignored.

According to [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#names-for-test-methods) it is acceptable to write method names in natural language. When using natural language, the description tends to be longer. This property allows lines containing an identifier between backticks to be longer than the maximum line length. (Since 0.41.0)

```kotlin
@Test
fun `Given a test with a very loooooooooooooooooooooong test description`() {
    
}
```

```ini
[*.{kt,kts}]
ktlint_ignore_back_ticked_identifier = false
```

This setting only takes effect when rule `max-line-length` is enabled.

## Import layouts

By default, the same imports are allowed as in IntelliJ IDEA. The import path can be a full path, e.g. "java.util.List.*" as well as wildcard path, e.g. "kotlin.**".

The layout can be composed by the following symbols:

*  `*` - wildcard. There must be at least one entry of a single wildcard to match all other imports. Matches anything after a specified symbol/import as well.
* `|` - blank line. Supports only single blank lines between imports. No blank line is allowed in the beginning or end of the layout.
* `^` - alias import, e.g. "^android.*" will match all android alias imports, "^" will match all other alias imports.

Examples:
```kotlin
ij_kotlin_imports_layout=* # alphabetical with capital letters before lower case letters (e.g. Z before a), no blank lines
ij_kotlin_imports_layout=*,java.**,javax.**,kotlin.**,^ # default IntelliJ IDEA style, same as alphabetical, but with "java", "javax", "kotlin" and alias imports in the end of the imports list
ij_kotlin_imports_layout=android.**,|,^org.junit.**,kotlin.io.Closeable.*,|,*,^ # custom imports layout
```

Wildcard imports can be allowed for specific import paths (Comma-separated list, use "**" as wildcard for package and all subpackages). This setting overrides the no-wildcard-imports rule. This setting is best be used for allowing wildcard imports from libraries like Ktor where extension functions are used in a way that creates a lot of imports.

```ini
[*.{kt,kts}]
ij_kotlin_packages_to_use_import_on_demand = java.util.*,kotlinx.android.synthetic.**
```

This setting only takes effect when rule `no-wildcard-imports` is enabled.

## Indent size & style

By default, indenting is done with 4 spaces per indent level. Code style `android_studio` uses a tab per indent level.

```ini
[*.{kt,kts}]
indent_size = 4 # possible values: number (e.g. 2), "unset" (makes ktlint ignore indentation completely)  
indent_style = space # or "tab"
```

Those settings are used by multiple rules of which rule `indent` is the most important.

## Max line length

By default, the maximum line length is not set. The `android` code style sets the max line length to 100 (per Android Kotlin Style Guide).

```ini
[*.{kt,kts}]
max_line_length = off # Use "off" to ignore max line length or a positive number to set max line length
```

This setting is used by multiple rules of which rule `max-line-length` is the most important.

## Trailing comma on call site

KtLint uses the IntelliJ IDEA `.editorconfig` property `ij_kotlin_allow_trailing_comma_on_call_site` to *enforce* the usage of the trailing comma at call site when enabled. IntelliJ IDEA uses this property to *allow* the use of trailing comma but leaves it to the developer's discretion to actually use it (or not). KtLint values *consistent* formatting more than a per-situation decision.

!!! note
    In KtLint 0.48.x the default value for using the trailing comma on call site has been changed to `true` except when codestyle `android` is used.

    Although the [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#trailing-commas) leaves it to the developer's discretion to use trailing commas on the call site, it also states that usage of trailing commas has several benefits:
    
     * It makes version-control diffs cleaner – as all the focus is on the changed value.
     * It makes it easy to add and reorder elements – there is no need to add or delete the comma if you manipulate elements.
     * It simplifies code generation, for example, for object initializers. The last element can also have a comma.

Example:
```ini
[*.{kt,kts}]
ij_kotlin_allow_trailing_comma_on_call_site = false
```

This setting only takes effect when rule `trailing-comma-on-call-site` is enabled.

## Trailing comma on declaration site

KtLint uses the IntelliJ IDEA `.editorconfig` property `ij_kotlin_allow_trailing_comma` to *enforce* the usage of the trailing comma at declaration site when enabled. IntelliJ IDEA uses this property to *allow* the use of trailing comma but leaves it to the developer's discretion to actually use it (or not). KtLint values *consistent* formatting more than a per-situation decision.

!!! note
    In KtLint 0.48.x the default value for using the trailing comma on declaration site has been changed to `true` except when codestyle `android` is used.

    The [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#trailing-commas) encourages the usage of trailing commas on the declaration site, but leaves it to the developer's discretion to use trailing commas on the call site. But next to this, it also states that usage of trailing commas has several benefits:
    
     * It makes version-control diffs cleaner – as all the focus is on the changed value.
     * It makes it easy to add and reorder elements – there is no need to add or delete the comma if you manipulate elements.
     * It simplifies code generation, for example, for object initializers. The last element can also have a comma.

Example:
```ini
[*.{kt,kts}]
ij_kotlin_allow_trailing_comma = false # Only used for declaration site 
```

This setting only takes effect when rule `trailing-comma-on-declaration-site` is enabled.

## Overriding Editorconfig properties for specific directories

You can [override](https://editorconfig.org/#file-format-details) properties for specific directories inside your project:
```ini
[*.{kt,kts}]
ktlint_standard_import-ordering = disabled

[api/*.{kt,kts}]
ktlint_standard_indent = disabled
```

Note that the `import-ordering` rule is disabled for *all* packages including the `api` sub package. Next to this the `indent` rule is disabled for the `api` package and its sub packages.
