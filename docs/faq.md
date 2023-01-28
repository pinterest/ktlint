## Why should I use ktlint?

the short answer is **Simplicity**.

Spending time on configuration (and maintenance down the road) of hundred-line long style config file(s) is counter-productive. Instead of wasting your energy on something that has no business value - focus on what really matters (not debating whether to use tabs or spaces).

By using ktlint you put the importance of code clarity and community conventions over personal preferences. This makes things easier for people reading your code as well as frees you from having to document and explain what style potential contributor(s) have to follow.

ktlint is a single binary with both linter & formatter included. All you need is to drop it in (no need to get [overwhelmed](https://en.wikipedia.org/wiki/Decision_fatigue) while choosing among [dozens of code style options](https://checkstyle.sourceforge.net/checks.html)).

## Can I have my own rules on top of ktlint?

Absolutely, "no configuration" doesn't mean "no extensibility". You can add your own ruleset(s) to discover potential bugs, check for anti-patterns, etc.

See [adding a custom rule set](../extensions/custom-rule-set/) for more information.

## How do I suppress errors for a line/block/file?

!!! tip
    Suppressing a `ktlint` violation is meant primarily as an escape latch for the rare cases when **ktlint** is not able to produce the correct result. Please report any such instances using [GitHub Issues](https://github.com/pinterest/ktlint/issues)).

To disable a specific rule you'll need the rule identifier which is displayed at the end of the lint error. Note that when the rule id is prefixed with a rule set id like `experimental`, you will need to use that fully qualified rule id.

An error can be suppressed using:

* EOL comments
* Block comments
* @Suppress annotations

From a consistency perspective seen, it might be best to **not** mix the (EOL/Block) comment style with the annotation style in the same project.

!!! warning
    Some rules like the `indent` rule do not (yet) support disabling of the rule per line or block. It can be disabled for an entire file though. 

### Disabling for one specific line using EOL comment

An error for a specific rule on a specific line can be disabled with an EOL comment on that line:

```kotlin
import package.* // ktlint-disable no-wildcard-imports
```

In case lint errors for different rules on the same line need to be ignored, then specify multiple rule ids (separated by a space):

```kotlin
import package.* // ktlint-disable no-wildcard-imports other-rule-id
```

In case all lint errors on a line need to be ignored, then do not specify the rule id at all:

```kotlin
import package.* // ktlint-disable
```

### Disabling for a block of lines using Block comments

An error for a specific rule in a block of lines can be disabled with an block comment like:

```kotlin
/* ktlint-disable no-wildcard-imports */
import package.a.*
import package.b.*
/* ktlint-enable no-wildcard-imports */
```

In case lint errors for different rules in the same block of lines need to be ignored, then specify multiple rule ids (separated by a space):

```kotlin
/* ktlint-disable no-wildcard-imports other-rule-id */
import package.a.*
import package.b.*
/* ktlint-enable no-wildcard-imports,other-rule-id */
```

Note that the `ktlint-enable` directive needs to specify the exact same rule-id's and in the same order as the `ktlint-disable` directive.

In case all lint errors in a block of lines needs to be ignored, then do not specify the rule id at all:

```kotlin
/* ktlint-disable */
import package.a.*
import package.b.*
/* ktlint-enable */
```

### Disabling for a statement or an entire file using @Suppress

!!! tip
    As of ktlint version 0.46, it is possible to specify any ktlint rule id via the `@Suppress` annotation in order to suppress errors found by that rule. Note that some rules like `indent` still do not support disabling for parts of a file.

An error for a specific rule on a specific line can be disabled with a `@Suppress` annotation:

```kotlin
@Suppress("ktlint:max-line-length","ktlint:experimental:trailing-comma-on-call-site")
val foo = listOf(
    "some really looooooooooooooooong string exceeding the max line length",
  )
```

Note that when using `@Suppress` each qualified rule id needs to be prefixed with `ktlint:`.

To suppress the violations of all ktlint rules, use:
```kotlin
@Suppress("ktlint")
val foo = "some really looooooooooooooooong string exceeding the max line length"
```

Like with other `@Suppress` annotations, it can be placed on targets supported by the annotation. As of this it is possible to disable rules in the entire file with:
```kotlin
@file:Suppress("ktlint") // Suppressing all rules for the entire file
// or
@file:Suppress("ktlint:max-line-length","ktlint:experimental:trailing-comma") // Suppressing specific rules for the entire file
```


## How do I globally disable a rule?
With [`.editorConfig` property `disabled_rules`](../rules/configuration-ktlint#disabled-rules) a rule can be disabled globally.

You may also pass a list of disabled rules via the `--disabled_rules` command line flag. It has the same syntax as the EditorConfig property.


## Why is `.editorconfig` property `disabled_rules` deprecated and how do I resolve this?
The `.editorconfig` properties `disabled_rules` and `ktlint_disbaled_rules` are deprecated as of KtLint version `0.48` and are marked for removal in version `0.49`. Those properties contain a comma separated list of rules which are disabled. Using a comma separated list of values has some disadvantages.

A big disadvantage is that it is not possible to override the property partially in an `.editorconfig` file in a subpackage. Another disadvantage is that it is not possible to express explicitly that a rule is enabled. Lastly, (qualified) rule ids can be 20 characters or longer, which makes a list with multiple entries hard to read.

*Root `.editorconfig`*
```editorconfig
root = true

[*.kt]
disabled_rules=rule-1,rule-2,rule-3
```
This `.editorconfig` defines that all rules except `rule-1`, `rule-2` and `rule-3` should be run in all packages. Suppose that we want to enable `rule-1` but disable `rule-4` in certain subpackage, then we would need to define an `.editorconfig` file like below:

*Secondary `.editorconfig`*
```editorconfig
[*.kt]
disabled_rules=rule-2,rule-4,rule-3
```
Disabling another rule in the root `.editorconfig` file, does not have effect on this subpackage as long as that rule has not been added to the `.editorconfig` file in the subpackage. 

Starting with KtLint `0.48` entire rule sets and individual rules can be disabled / enabled with a separate property per rule (set). 

All rules in a rule set can be enabled or disabled with a rule set property. The name of the rule set property consists of the `ktlint_` prefix followed by the rule set id. Examples:
```editorconfig
ktlint_standard = disabled # Disable all rules from the `standard` rule set provided by KtLint
ktlint_experimental = enabled # Enable all rules from the `experimental` rule set provided by KtLint
ktlint_your-custom-rule-set_custom-rule = enabled # Enable all rules in the `custom-rule-set` rule set (not provided by KtLint)
```

!!! note
    All rules from the `standard` and custom rule sets are *enabled* by default and can optionally be disabled in the `.editorconfig`. All rules from the `experimental` rule set are *disabled* by default and can optionally be enabled in the `.editorconfig`.

An individual property can be enabled or disabled with a rule property. The name of the rule property consists of the `ktlint_` prefix followed by the rule set id followed by a `_` and the rule id. Examples:
```editorconfig
ktlint_standard_final-newline = disabled # Disables the `final-newline` rule in the `standard` rule set provided by KtLint
ktlint_experimental_type-argument-list-spacing = enabled # Enables the `type-argument-list-spacing` rule in the `experimental` rule set provided by KtLint
ktlint_custom-rule-set_custom-rule = disabled # Disables the `custom-rule` rule in the `custom-rule-set` rule set (not provided by KtLint)
```

!!! note
    The *rule* properties are applied after applying the *rule set* properties and take precedence. So if a rule set is disabled but a specific rule of that rule set is enabled, then the rule will be executed.  

## Why is wildcard import `java.util.*` not reported by the `no-wildcard-imports` rule?

The `no-wildcard-imports` rule forbids wildcard imports, except for imports defined in `.editorconfig` property `ij_kotlin_packages_to_use_import_on_demand`. If ths property is not set explicitly set, it allows wildcards imports like `java.util.*` by default to keep in sync with IntelliJ IDEA behavior.
