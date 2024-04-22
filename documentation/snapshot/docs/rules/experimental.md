!!! important
    Up and until Ktlint version `0.47`, experimental were located in a separate `experimental` rule set. As of Ktlint version `0.48`, each rule set can optionally define experimental rules.

All experimental rules described below are part of the `standard` rule set of Ktlint. To enable all experimental rules (from all rule sets), set `editorconfig` property below:
```ini
[*.{kt,kts}]
ktlint_experimental=enabled
```
Also see [enable/disable specific rules](../configuration-ktlint/#disabled-rules).

### Backing property naming

Allows property names to start with `_` in case the property is a backing property. `ktlint_official` and `android_studio` code styles require the correlated property/function to be defined as `public`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class Bar {
        // Backing property
        private val _elementList = mutableListOf<Element>()
        val elementList: List<Element>
            get() = _elementList
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class Bar {
        // Incomplete backing property as public property 'elementList1' is missing
        private val _elementList1 = mutableListOf<Element>()

        // Invalid backing property as '_elementList2' is not a private property
        val _elementList2 = mutableListOf<Element>()
        val elementList2: List<Element>
            get() = _elementList2
    }
    ```

Rule id: `backing-property-naming` (`standard` rule set)

## Binary expression wrapping

Wraps binary expression at the operator reference whenever the binary expression does not fit on the line. In case the binary expression is nested, the expression is evaluated from outside to inside. If the left and right hand sides of the binary expression, after wrapping, fit on a single line then the inner binary expressions will not be wrapped. If one or both inner binary expression still do not fit on a single after wrapping of the outer binary expression, then each of those inner binary expressions will be wrapped.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() {
        // Assume that the last allowed character is
        // at the X character on the right                       X
        if ((leftHandSideExpression && rightHandSideExpression) ||
            (
                leftHandSideLongExpression &&
                    rightHandSideExpression
            )
        ) {
            // do something
        }
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo() {
        // Assume that the last allowed character is
        // at the X character on the right                       X
        if ((leftHandSideExpression && rightHandSideExpression) ||
            (leftHandSideLongExpression && rightHandSideExpression)
        ) {
            // do something
        }
    }
    ```

Rule id: `binary-expression-wrapping` (`standard` rule set)

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

Rule id: `blank-lines-between-when-conditions` (`standard` rule set)

## Chain method continuation

In a multiline method chain, the chain operators (`.` or `?.`) have to be aligned with each other. 

Multiple chained methods on a single line are allowed as long as the maximum line length, and the maximum number of chain operators are not exceeded. Under certain conditions, it is allowed that the expression before the first and/or the expression after the last chain operator is a multiline expression.

The `.` in `java.class` is ignored when wrapping on chain operators.

This rule can be configured with `.editorconfig` property [`ktlint_chain_method_rule_force_multiline_when_chain_operator_count_greater_or_equal_than`](../configuration-ktlint/#force-multiline-chained-methods-based-on-number-of-chain-operators).

!!! warning
    Binary expression for which the left and/or right operand consist of method chain are currently being ignored by this rule. Please reach out, if you can help to determine what the best strategy is to deal with such kind of expressions.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 =
        listOf(1, 2, 3)
            .filter { it > 2 }!!
            .takeIf { it > 2 }
            .map {
                it * it
            }?.map {
                it * it
            }
    val foo2 =
        listOf(1, 2, 3)
            .filter {
                it > 2
            }.map {
                2 * it
            }?.map {
                2 * it
            }
    val foo3 =
        foo().bar().map {
            it.foobar()
        }
    val foo4 =
        """
        Some text
        """.trimIndent().foo().bar()
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 =
        listOf(1, 2, 3).
            filter { it > 2 }!!.
            takeIf { it > 2 }.
            map {
                it * it
            }?.
            map {
                it * it
            }
    val foo2 =
        listOf(1, 2, 3)
            .filter {
                it > 2
            }
            .map {
                2 * it
            }
            ?.map {
                2 * it
            }
    val foo3 = 
        foo()
        .bar().map {
            it.foobar()
        }
    val foo4 =
        """
        Some text
        """.trimIndent().foo()
            .bar()
    ```

| Configuration setting                                                                                                                                                                                                                                                                                                                                                                                  | ktlint_official | intellij_idea | android_studio |
|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------:|:-------------:|:--------------:|
| `ktlint_chain_method_rule_force_multiline_when_chain_operator_count_greater_or_equal_than`<br/><i>Force wrapping of chained methods in case an expression contains at least the specified number of chain operators. If a chained method contains nested expressions, the chain operators of the inner expression are not taken into account. Use value `unset` (default) to disable this setting.</i> |        4        |       4       |       4        |


Rule id: `chain-method-continuation` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Class signature

Rewrites the class signature to a consistent format respecting the `.editorconfig` property `max_line_length` if set. In the `ktlint_official` code style all class parameters are wrapped by default. Set `.editorconfig` property `ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than` to a value greater than 1 to allow class with a few parameters to be placed on a single line.
The other code styles allow an infinite amount of parameters on the same line (as long as the `max_line_length` is not exceeded) unless `.editorconfig` property `ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than` is set explicitly.

=== "[:material-heart:](#) Ktlint (ktlint_official)"

    ```kotlin
    // Assume that max_line_length is not exceeded when written as single line
    class Foo0
    
    class Foo1(
        a: Any,
    )
    
    class Foo2(
        a: Any,
        b: Any,
    )
    
    class Foo3(
        @Foo a: Any,
        b: Any,
        c: Any,
    )
    
    class Foo4(
        a: Any,
        b: Any,
        c: Any,
    ) : FooBar(a, c)
    
    class Foo5 :
        FooBar(
            "bar1",
            "bar2",
        ) {
        // body
    }
    
    class Foo6(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(
            bar1,
            bar2,
        ) {
        // body
    }
    
    class Foo7(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(
            bar1,
            bar2,
        ),
        BarFoo1,
        BarFoo2 {
        // body
    }
    
    class Foo8
        constructor(
            val bar1: Bar,
            val bar2: Bar,
        ) : FooBar(bar1, bar2),
            BarFoo1,
            BarFoo2 {
            // body
        }
    ```

=== "[:material-heart-off-outline:](#) Disallowed (ktlint_official)"

    ```kotlin
    // Assume that max_line_length is not exceeded when written as single line
    class Foo0()

    class Foo1(a: Any)

    class Foo2(a: Any, b: Any)

    class Foo3(@Foo a: Any, b: Any, c: Any)

    class Foo4(a: Any, b: Any, c: Any) : FooBar(a, c)

    class Foo5 : FooBar(
        "bar1",
        "bar2",
    ) {
        // body
    }

    class Foo6(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(
        bar1,
        bar2,
    ) {
        // body
    }

    class Foo7(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(
        bar1,
        bar2,
    ),
        BarFoo1,
        BarFoo2 {
        // body
    }

    class Foo8
    constructor(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(bar1, bar2),
        BarFoo1,
        BarFoo2 {
        // body
    }
    ```

=== "[:material-heart:](#) Ktlint (non ktlint_official)"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    class Foo0

    class Foo1(
        a: Any,
    )

    class Foo2(a: Any)

    class Foo3(
        a: Any,
        b: Any,
    )

    class Foo4(a: Any, b: Any)

    class Foo5(@Foo a: Any, b: Any, c: Any)

    class Foo6(a: Any, b: Any, c: Any) :
        FooBar(a, c)

    class Foo7 : FooBar(
        "bar1",
        "bar2",
    ) {
        // body
    }
    class Foo8(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(
        bar1,
        bar2
    ) {
        // body
    }

    class Foo9(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(
        bar1,
        bar2
    ),
        BarFoo1,
        BarFoo2 {
        // body
    }

    class Foo10
    constructor(
        val bar1: Bar,
        val bar2: Bar,
    ) : FooBar(bar1, bar2),
        BarFoo1,
        BarFoo2 {
        // body
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed (non ktlint_official)"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    class Foo0()

    class Foo6(a: Any, b: Any, c: Any) : FooBar(a, c)
    ```

| Configuration setting                                                                                                                                                                                                                                                                                                                       | ktlint_official | intellij_idea | android_studio |
|:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------:|:-------------:|:--------------:|
| `ktlint_class_signature_rule_force_multiline_when_parameter_count_greater_or_equal_than`<br/><i>Force wrapping of the parameters of the class signature in case it contains at least the specified number of parameters, even in case the entire class signature would fit on a single line. Use value `unset` to disable this setting.</i> |        1        |    `unset`    |    `unset`     |

Rule id: `class-signature` (`standard` rule set)

## Condition wrapping

Wraps each operand in a multiline condition to a separate line.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo = bar || baz
    if (bar1 ||
        bar2 ||
        baz1 ||
        (baz2 && baz3)
    ) {
       // do something
    }
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

Rule id: `condition-wrapping` (`standard` rule set)

## Function expression body

Rewrites a function body only containing a `return` or `throw` expression to an expression body.

!!! note
    If the function body contains a comment, it is not rewritten to an expression body.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo1() = "foo"

    fun foo2(): String = "foo"

    fun foo3(): Unit = throw IllegalArgumentException("some message")

    fun foo4(): Foo = throw IllegalArgumentException("some message")

    fun foo5() {
        return "foo" // some comment
    }

    fun foo6(): String {
        /* some comment */
        return "foo"
    }

    fun foo7() {
        throw IllegalArgumentException("some message")
        /* some comment */
    }

    fun foo8(): Foo {
        throw IllegalArgumentException("some message")
        // some comment
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo1() {
        return "foo"
    }

    fun foo2(): String {
        return "foo"
    }

    fun foo3() {
        throw IllegalArgumentException("some message")
    }

    fun foo4(): Foo {
        throw IllegalArgumentException("some message")
    }
    ```

Rule id: `function-expression-body` (`standard` rule set)

## Function literal

Enforces the parameters of a function literal and the arrow to be written on the same line as the opening brace if the maximum line length is not exceeded. In case the parameters are wrapped to multiple lines then this is respected.

If the function literal contains multiple parameter and at least one parameter other than the first parameter starts on a new line than all parameters and the arrow are wrapped to separate lines.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foobar1 = { foo + bar }
    val foobar2 =
        {
            foo + bar
        }
    val foobar3 =
        { foo: Foo ->
            foo.repeat(2)
        }
    val foobar4 =
        { foo: Foo, bar: Bar ->
            foo + bar
        }
    val foobar5 = { foo: Foo, bar: Bar -> foo + bar }
    val foobar6 =
        {
                foo: Foo,
                bar: Bar,
            ->
            foo + bar
        }
    
    // Assume that the last allowed character is
    // at the X character on the right           X
    val foobar7 =
        barrrrrrrrrrrrrr { 
              fooooooooooooooo: Foo
            ->
            foo.repeat(2)
        }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foobar3 =
        {
            foo: Foo ->
            foo.repeat(2)
        }
    val foobar6 =
        { foo: Foo,
          bar: Bar ->
            foo + bar
        }
    // Assume that the last allowed character is
    // at the X character on the right           X
    val foobar7 =
        barrrrrrrrrrrrrr { fooooooooooooooo: Foo ->
            foo.repeat(2)
        }
    ```

Rule id: `function-literal` (`standard` rule set)

## Function type modifier spacing

Enforce a single whitespace between the modifier list and the function type.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo: suspend () -> Unit = {}

    suspend fun bar(baz: suspend () -> Unit) = baz()
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo: suspend() -> Unit = {}

    suspend fun bar(baz: suspend   () -> Unit) = baz()
    ```

Rule id: `function-type-modifier-spacing` (`standard` rule set)

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

Rule id: `mixed-condition-operators` (`standard` rule set)

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

Rule id: `kdoc` (`standard` rule set)

## Multiline loop

Braces required for multiline for, while, and do statements.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    for (i in 1..10) {
        println(i)
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    for (i in 1..10)
        println(i)
    ```

Rule id: `multiline-loop` (`standard` rule set)

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

Rule id: `square-brackets-spacing` (`standard` rule set)
