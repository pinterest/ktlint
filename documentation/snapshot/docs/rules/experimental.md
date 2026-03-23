!!! important
    Up and until Ktlint version `0.47`, experimental were located in a separate `experimental` rule set. As of Ktlint version `0.48`, each rule set can optionally define experimental rules.

All experimental rules described below are part of the `standard` rule set of Ktlint. To enable all experimental rules (from all rule sets), set `editorconfig` property below:
```ini
[*.{kt,kts}]
ktlint_experimental=enabled
```
Also see [enable/disable specific rules](configuration-ktlint.md#disable-rules).

## Call expression wrapping

In case a call expression does not fit on the line, the lambda expression, and/or the value argument list after a reference expression are wrapped. 

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // Assume that the last allowed
    // character is at the X character
    // on the right                  X
    val foo1 = bar() {
        "some message"
    }
    val foo2 = bar(
        "foobarrrrrrrrrrrr"
    ) { "some message" }
    val foo3 = bar(
        "foobarrrrrrrrrrrr"
    ) {
        "some longgggggggggg message"
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // Assume that the last allowed
    // character is at the X character
    // on the right                  X
    val foo1 = bar() { "some message" }
    val foo2 = bar("foobarrrrrrrrrrrr") { "some message" }
    val foo3 = bar("foobarrrrrrrrrrrr") { "some longgggggggggg message" }
    ```

Rule id: `standard:call-expression-wrapping`

Suppress or disable rule (1)
{ .annotate }

1. Suppress rule in code with annotation below:
    ```kotlin
    @Suppress("ktlint:standard:call-expression-wrapping")
    ```
   Enable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_call-expression-wrapping = enabled
    ```
   Disable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_call-expression-wrapping = disabled
    ```

## Expression operand wrapping

Wraps each operand in a multiline expression to a separate line.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = bar || baz
    val foo2 =
        bar1 ||
            bar2 ||
            baz1 ||
            (baz2 && baz3)
    val foo3 = bar + baz
    val foo4 =
        bar1 -
            bar2 -
            baz1 -
            (baz2 * baz3)
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo =
      multiLineOperand(
          "bar"
      ) || baz
    if (bar1 || bar2 ||
        baz1 || (baz2 && baz3)
    ) {
       // do something
    }
    ```

Rule id: `standard:expression-operand-wrapping`

Suppress or disable rule (1)
{ .annotate }

1. Suppress rule in code with annotation below:
    ```kotlin
    @Suppress("ktlint:standard:expression-operand-wrapping")
    ```
   Enable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_expression-operand-wrapping = enabled
    ```
   Disable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_expression-operand-wrapping = disabled
    ```

## Lambda return

Do not use a labeled return for the last statement in a lambda.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = bar { "value" }
    val foo2 = bar {
        if (baz()) return@bar "value"

        "value"
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 = bar { return@foo "value" }
    val foo2 = bar {
        if (baz()) return@bar "value" // This is OK

        return@bar "value" // This is disallowed
    }
    ```

Rule id: `standard:lambda-return`

Suppress or disable rule (1)
{ .annotate }

1. Suppress rule in code with annotation below:
    ```kotlin
    @Suppress("ktlint:standard:lambda-return")
    ```
   Enable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_lambda-return = enabled
    ```
   Disable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_lambda-return = disabled
    ```
