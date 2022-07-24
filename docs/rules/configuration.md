Ktlint uses a limited set of `.editorconfig` properties for additional configuration. A sensible default value is provided for each property when not explicitly defined. Properties can be overridden, provided they are specified under `[*.{kt,kts}]`. Ktlint uses some properties defined by [.editorconfig](https://editorconfig.org/), IntelliJ IDEA and custom properties.

!!! danger

    Unfortunately [IntelliJ IDEA](https://www.jetbrains.com/idea/) has an [autoformat issue regarding `.editorconfig`](https://youtrack.jetbrains.com/issue/IDEA-242506). Due to this error an additional space is added between glob statements, resulting in `[*{kt, kts}]` instead of `[*{kt,kts}]`. The `.editorconfig` library used by `ktlint` [ignores sections after encountering a space in the list](https://github.com/editorconfig/editorconfig/issues/148). As a result, the rule is not applied on all files as documented in the [original ktlint issue](https://github.com/pinterest/ktlint/issues/762).

## Code style

By default, the `offical` Kotlin code style is applied. Alternatively, the code style can be set to `android`.

```ini
[*.{kt,kts}]
ktlint_code_style = official
```

## Disabled rules

By default, no rules are disabled. The property `disabled_rules` holds a comma separated list (without spaces). Rules which are not defined in the `standard` ruleset have to be prefixed. Rules defined in the `standard` ruleset may optionally be prefixed.

Example:
```ini
[*.{kt,kts}]
disabled_rules = some-standard-rule,experimental:some-experimental-rule,my-custom-ruleset:my-custom-rule
```

## Final newline

By default, a final newline is required at the end of the file.

```ini
[*.{kt,kts}]
insert_final_newline = true
```

This setting only takes effect when rule `final-newline` is enabled.

## Force multiline function signature based on number of parameters

By default, the number of parameters in a function signature is not relevant when rewriting the function signature. Only the maximum line length determines when a function signature should be written on a single line or with multiple lines. Setting `ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than` can be used, to force a multiline function signature in case the function contain at least a number of parameters even in case the function signature would fit on a single line. Use value `-1` (default) to disable this setting.

```ini
[*.{kt,kts}]
ktlint_function_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than= -1
```

This setting only takes effect when rule `experimental:function-signature` is enabled.

## Wrapping the expression body of a function

Setting `ktlint_function_signature_body_expression_wrapping` determines if and when the expression body of a function is wrapped to a new line. This setting can be set to value `default`, `multiline` or `always`. 

When set to `default`, the first line of a body expression is appended to the function signature as long as the max line length is not exceeded.

```kotlin title="ktlint_function_signature_body_expression_wrapping=default (or when not set)"
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

This setting only takes effect when rule `experimental:function-signature` is enabled.

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

By default, indenting is done with 4 spaces per indent level in `official` Kotlin code style while a single tab is used by default in the `android` code style.

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
max_line_length = -1 # Use "off" (or -1) to ignore max line length or a positive number to set max line length
```

This setting is used by multiple rules of which rule `max-line-length` is the most important.

## Trailing comma

Trailing comma's (both on call and declaration site) are disabled (e.g. not allowed) by. When enabling the properties, the trailing becomes mandatory where applicable.

Example:
```ini
[*.{kt,kts}]
ij_kotlin_allow_trailing_comma = false
ij_kotlin_allow_trailing_comma_on_call_site = false
```

This setting only takes effect when rule `trailing-comma` is enabled.

## Overriding Editorconfig properties for specific directories

You can [override](https://editorconfig.org/#file-format-details) properties for specific directories inside your project:
```ini
[*.{kt,kts}]
disabled_rules=import-ordering

 Note that in this case 'import-ordering' rule will be active and 'indent' will be disabled
[api/*.{kt,kts}]
disabled_rules=indent
```
