!!! important
    Up and until Ktlint version `0.47`, experimental were located in a separate `experimental` rule set. As of Ktlint version `0.48`, each rule set can optionally define experimental rules.

All experimental rules described below are part of the `standard` rule set of Ktlint. To enable all experimental rules (from all rule sets), set `editorconfig` property below:
```ini
[*.{kt,kts}]
ktlint_experimental=enabled
```
Also see [enable/disable specific rules](configuration-ktlint.md#disable-rules).

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
    @Suppress("ktlint:expression-operand-wrapping")
    ```
   Enable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_expression-operand-wrapping = enabled
    ```
   Disable rule via `.editorconfig`
    ```editorconfig
    ktlint_standard_expression-operand-wrapping = disabled
    ```
