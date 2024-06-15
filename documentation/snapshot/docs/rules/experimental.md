!!! important
    Up and until Ktlint version `0.47`, experimental were located in a separate `experimental` rule set. As of Ktlint version `0.48`, each rule set can optionally define experimental rules.

All experimental rules described below are part of the `standard` rule set of Ktlint. To enable all experimental rules (from all rule sets), set `editorconfig` property below:
```ini
[*.{kt,kts}]
ktlint_experimental=enabled
```
Also see [enable/disable specific rules](../configuration-ktlint/#disabled-rules).

## Blank lines between when-conditions

Consistently add or remove blank lines between when-conditions in a when-statement. A blank line is only added between when-conditions if the when-statement contains at lease one multiline when-condition. If a when-statement only contains single line when-conditions, then the blank lines between the when-conditions are removed.

!!! note
    Ktlint uses `.editorconfig` property `ij_kotlin_line_break_after_multiline_when_entry` but applies it also on single line entries to increase consistency.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 =
        when (bar) {
            BAR1 -> "bar1"
            BAR2 -> "bar2"
            else -> null
        }

    // ij_kotlin_line_break_after_multiline_when_entry = true
    val foo2 =
        when (bar) {
            BAR1 -> "bar1"

            BAR2 -> {
                "bar2"
            }

            else -> null
        }

    // ij_kotlin_line_break_after_multiline_when_entry = true
    val foo3 =
        when (bar) {
            BAR1 -> "bar1"

            // BAR2 comment
            BAR2 -> "bar2"

            else -> null
        }

    // ij_kotlin_line_break_after_multiline_when_entry = false
    val foo4 =
        when (bar) {
            BAR1 -> "bar1"
            BAR2 -> {
                "bar2"
            }
            else -> null
        }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // ij_kotlin_line_break_after_multiline_when_entry = true | false (no blank lines in simple when-statement)
    val foo1 =
        when (bar) {
            BAR1 -> "bar1"

            BAR2 -> "bar2"

            else -> null
        }

    // ij_kotlin_line_break_after_multiline_when_entry = true (missing newline after BAR1)
    val foo2 =
        when (bar) {
            BAR1 -> "bar1"
            BAR2 -> {
                "bar2"
            }

            else -> null
        }

    // ij_kotlin_line_break_after_multiline_when_entry = true (missing newline after BAR1, and BAR2)
    val foo3 =
        when (bar) {
            BAR1 -> "bar1"
            // BAR2 comment
            BAR2 -> "bar2"
            else -> null
        }

    // ij_kotlin_line_break_after_multiline_when_entry = false (unexpected newline after BAR2)
    val foo4 =
        when (bar) {
            BAR1 -> "bar1"
            BAR2 -> {
                "bar2"
            }

            else -> null
        }
    ```

| Configuration setting                                                                                                                                                                                                     | ktlint_official | intellij_idea | android_studio |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------:|:-------------:|:--------------:|
| `ij_kotlin_line_break_after_multiline_when_entry`<br/><i>Despite its name, forces a blank line between single line and multiline when-entries when at least one multiline when-entry is found in the when-statement.</i> |      `true`       |    `true`     |     `true`     |

Rule id: `standard:blank-lines-between-when-conditions`

## KDoc

KDoc's should only be used on elements for which KDoc is to be transformed to documentation. Normal block comments should be used in other cases.

!!! note:
Access modifiers are ignored. Strictly speaking, one could argue that private declarations should not have a KDoc as no documentation will be generated for it. However, for internal use of developers the KDoc still serves documentation purposes.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    /** some KDoc */
    class FooBar(
        /** some KDoc */
        val foo: Foo
    ) {
        /**
         * Some bar KDoc
         */
        constructor() : this()

        /** some KDoc */
        val bar: Bar
    }

    enum class Foo {
        /** some KDoc */
        BAR
    }

    /** some KDoc */
    interface Foo
    /** some KDoc */
    fun foo()
    /** some KDoc */
    val foo: Foo
    /** some KDoc */
    object foo: Foo
    /** some KDoc */
    typealias FooBar = (Foo) -> Bar
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /**
     * Some dangling Kdoc (e.g. not followed by a declaration)
     */

    val foo /** Some KDoc */ = "foo"

    class Foo(
        /** some dangling KDoc inside a parameter list */
    )
    ```

Rule id: `standard:kdoc`

## Mixed condition operators

Conditions should not use a both `&&` and `||` operators between operators at the same level. By using parenthesis the expression is to be clarified.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo = bar1 && (bar2 || bar3) && bar4
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo = bar1 &&
        bar2 ||
        bar3
    val foo = bar1 && (bar2 || bar3 && bar4) && bar5
    ```

Rule id: `standard:mixed-condition-operators`

## Square brackets spacing

Check for spacing around square brackets.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = bar[1]
    val foo2 =
       bar[
           1,
           2,
       ]

    @Foo(
        fooBar = ["foo", "bar"],
        fooBaz = [
            "foo",
            "baz",
        ],
    )
    fun foo() {}
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 = bar [1]
    val foo2 = bar[ 1]
    val foo3 = bar[1 ]
    ```

Rule id: `standard:square-brackets-spacing`
