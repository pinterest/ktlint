!!! important
    Up and until Ktlint version `0.47`, experimental were located in a separate `experimental` rule set. As of Ktlint version `0.48`, each rule set can optionally define experimental rules.

All experimental rules described below are part of the `standard` rule set of Ktlint. To enable all experimental rules (from all rule sets), set `editorconfig` property below:
```ini
[*.{kt,kts}]
ktlint_experimental=enabled
```
Also see [enable/disable specific rules](../configuration-ktlint/#disabled-rules).

## Binary expression wrapping

Wraps binary expression at the operator reference whenever the binary expression does not fit on the line. In case the binary expression is nested, the expression is evaluated from outside to inside. If the left and right hand sides of the binary expression, after wrapping, fit on a single line then the inner binary expressions will not be wrapped. If one or both inner binary expression still do not fit on a single after wrapping of the outer binary expression, then each of those inner binary expressions will be wrapped.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() {
        // Assume that the last allowed character is
        // at the X character on the right                       X
        if ((leftHandSideExpression && rightHandSideExpression) ||
            (leftHandSideLongExpression &&
                rightHandSideLongExpression)) {
            // do something
        }
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo() {
        // Assume that the last allowed character is
        // at the X character on the right                       X
        if ((leftHandSideExpression && rightHandSideExpression) || (leftHandSideLongExpression && rightHandSideLongExpression)) {
            // do something
        }
    }
    ```

Rule id: `binary-expression-wrapping` (`standard` rule set)

## Blank line before declarations

Requires a blank line before any class or function declaration. No blank line is required between the class signature and the first declaration in the class. In a similar way, a blank line is required before any list of top level or class properties. No blank line is required before local properties or between consecutive properties.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    const val foo1 = "foo1"

    class FooBar {
        val foo2 = "foo2"
        val foo3 = "foo3"

        fun bar1() {
           val foo4 = "foo4"
           val foo5 = "foo5"
        }

        fun bar2() = "bar"

        val foo6 = "foo3"
        val foo7 = "foo4"

        enum class Foo {}
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    const val foo1 = "foo1"
    class FooBar {
        val foo2 = "foo2"
        val foo3 = "foo3"
        fun bar1() {
           val foo4 = "foo4"
           val foo5 = "foo5"
        }
        fun bar2() = "bar"
        val foo6 = "foo3"
        val foo7 = "foo4"
        enum class Foo {}
    }
    ```

Rule id: `blank-line-before-declaration` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Chain method continuation

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
    val foo3 = foo().bar().map {
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
    val foo3 = foo()
        .bar().map {
            it.foobar()
        }
    val foo4 =
        """
        Some text
        """.trimIndent().foo()
            .bar()
    ```

Rule id: `chain-method-continuation` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Class signature

Rewrites the class signature to a consistent format respecting the `.editorconfig` property `max_line_length` if set. In the `ktlint_official` code style all class parameters are wrapped by default. Set `.editorconfig` property `ktlint_class_signature_wrapping_rule_always_with_minimum_parameters` to a value greater than 1 to allow class with a few parameters to be placed on a single line.
The other code styles allow an infinite amount of parameters on the same line (as long as the `max_line_length` is not exceeded) unless `.editorconfig` property `ktlint_class_signature_wrapping_rule_always_with_minimum_parameters` is set explicitly.

=== "[:material-heart:](#) Ktlint (ktlint_official)"

    ```kotlin
    // Assume that max_line_length is not exceeded when written as single line
    class Foo0
    class Foo1(
        a: Any
    )
    class Foo2(
        a: Any,
        b: Any
    )
    class Foo3(
        @Foo a: Any,
        b: Any,
        c: Any
    )
    class Foo4(
        a: Any,
        b: Any,
        c: Any
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
        val bar2: Bar
    ) : FooBar(
            bar1,
            bar2
        ) {
        // body
    }
    class Foo7(
        val bar1: Bar,
        val bar2: Bar
    ) : FooBar(
            bar1,
            bar2
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
        val bar2: Bar
    ) : FooBar(
        bar1,
        bar2
    ) {
        // body
    }
    class Foo7(
        val bar1: Bar,
        val bar2: Bar
    ) : FooBar(
        bar1,
        bar2
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
        a: Any
    )
    class Foo2(a: Any)
    class Foo3(
        a: Any,
        b: Any
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
        val bar2: Bar
    ) : FooBar(
        bar1,
        bar2
    ) {
        // body
    }
    class Foo9(
        val bar1: Bar,
        val bar2: Bar
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

Rule id: `class-signature` (`standard` rule set)

## Discouraged comment location

Detect discouraged comment locations (no autocorrect).

!!! note
    Kotlin allows comments to be placed almost everywhere. As this can lead to code which is hard to read, most of them will never be used in practice. Ideally each rule takes comments at all possible locations into account. Sometimes this is really hard and not worth the effort. By explicitly forbidding such comment locations, the development of those rules becomes a bit easier. 

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun <T> /* some comment */ foo(t: T) = "some-result"

    fun foo() {
        if (true)
            // some comment
            bar()
    }
    ```

Rule id: `discouraged-comment-location` (`standard` rule set)

## Disallow empty lines at start of class body

Detect blank lines at start of a class body.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class Foo {
        val foo = "foo"
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class Foo {

        val foo = "foo"
    }
    ```

Rule id: `no-empty-first-line-in-class-body` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Disallow consecutive comments

Consecutive comments are disallowed in following cases:
- Any mix of a consecutive kdoc, a block comment or an EOL comment unless separated by a blank line in between
- Consecutive KDocs (even when separated by a blank line)
- Consecutive block comments (even when separated by a blank line)

Consecutive EOL comments are always allowed as they are often used instead of a block comment.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // An EOL comment
    // may be followed by another EOL comment
    val foo = "foo"

    // Different comment types (including KDoc) may be consecutive ..

    /*
     * ... but do need to be separated by a blank line ...
     */

    /**
      * ... but a KDoc can not be followed by an EOL or a block comment or another KDoc
      */
    fun bar() = "bar"
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /*
     * Block comments can not be consecutive ...
     */
    /*
     * ... even not when separated by a new line.
     */
    val bar = "bar" 

    /**
      * A KDoc can not be followed by a block comment or an EOL comment or another KDOC
      */

    // ... even not when separated by a new line.
    ```

Rule id: `no-consecutive-comments` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

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
                        bar: Bar
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

!!! Note
This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Function signature

Rewrites a function body only containing a `return` or `throw` expression to an expression body.

!!! note:
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

## Function signature

Rewrites the function signature to a single line when possible (e.g. when not exceeding the `max_line_length` property) or a multiline signature otherwise. In case of function with a body expression, the body expression is placed on the same line as the function signature when not exceeding the `max_line_length` property. Optionally the function signature can be forced to be written as a multiline signature in case the function has more than a specified number of parameters (`.editorconfig` property `ktlint_function_signature_wrapping_rule_always_with_minimum_parameters`)

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    fun foooooooo(
        a: Any,
        b: Any,
        c: Any
    ): String {
        // body
    }

    // Assume that the last allowed character is
    // at the X character on the right           X
    fun bar(a: Any, b: Any, c: Any): String {
        // body
    }

    // When wrapping of body is set to 'default'.
    // Assume that the last allowed character is
    // at the X character on the right           X
    fun f(a: Any, b: Any): String = "some-result"
        .uppercase()

    // When wrapping of body is set to 'multiline'
    // or 'always'.
    // Assume that the last allowed character is
    // at the X character on the right           X
    fun f(a: Any, b: Any): String =
        "some-result"
            .uppercase()
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    fun foooooooo(a: Any, b: Any, c: Any): String {
        // body
    }

    // Assume that the last allowed character is
    // at the X character on the right           X
    fun bar(
        a: Any,
        b: Any,
        c: Any
    ): String {
        // body
    }

    // When wrapping of body is set to 'default'.
    // Assume that the last allowed character is
    // at the X character on the right           X
    fun f(a: Any, b: Any): String =
        "some-result"
            .uppercase()

    // When wrapping of body is set to 'multiline'
    // or 'always'.
    // Assume that the last allowed character is
    // at the X character on the right           X
    fun f(a: Any, b: Any): String = "some-result"
        .uppercase()
    ```

Rule id: `function-signature` (`standard` rule set)

## If else bracing

If at least one branch of an if-else statement or an if-else-if statement is wrapped between curly braces then all branches should be wrapped between braces.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo(value: int) {
        if (value > 0) {
            doSomething()
        } else if (value < 0) {
            doSomethingElse()
        } else {
            doSomethingElse2()
        }
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo(value: int) {
        if (value > 0)
            doSomething()
        else if (value < 0) {
            doSomethingElse()
        } else
            doSomethingElse2()
    }
    ```

Rule id: `if-else-bracing` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## If else wrapping

A single line if-statement should be kept simple. It may contain no more than one else-branch. The branches may not be wrapped in a block.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foobar() {
        if (true) foo()
        if (true) foo() else bar()
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foobar() {
        if (true) if (false) foo() else bar()
        if (true) bar() else if (false) foo() else bar()
        if (true) { foo() } else bar()
        if (true) bar() else { if (false) foo() else bar() }
    }
    ```

Rule id: `if-else-wrapping` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Naming

### Function naming

Enforce naming of function. 

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() {}
    fun fooBar() {}
    ```
=== "[:material-heart:](#) Ktlint Test"

    ```kotlin
    @Test
    fun `Some name`() {}

    @Test
    fun do_something() {}
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun Foo() {}
    fun Foo_Bar() {}
    fun `Some name`() {}
    fun do_something() {}
    ```

!!! note
    Functions in files which import a class from package `org.junit`, `org.testng` or `kotlin.test` are considered to be test functions. Functions in such classes are allowed to have underscores in the name. Or function names can be specified between backticks and do not need to adhere to the normal naming convention.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `FunctionName`.

Rule id: `function-naming` (`standard` rule set)

### Package naming

Enforce naming of package.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `PackageName`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    package foo
    package foo.foo
    package foo_bar
    package foo.foo_bar
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    package Foo
    package foo.Foo
    package `foo bar`
    package foo.`foo bar`
    ```

Rule id: `package-naming` (`standard` rule set)

### Property naming

Enforce naming of property.

!!! note
    This rule can not reliably detect all situations in which incorrect property naming is used. So it only detects in which it is certain that naming is incorrect.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = Foo() // In case developer want to communicate that Foo is mutable
    val FOO1 = Foo() // In case developer want to communicate that Foo is deeply immutable

    const val FOO_BAR = "FOO-BAR" // By definition deeply immutable

    var foo2: Foo = Foo() // By definition not immutable

    class Bar {
        val foo1 = Foo() // In case developer want to communicate that Foo is mutable
        val FOO1 = Foo() // In case developer want to communicate that Foo is deeply immutable

        const val FOO_BAR = "FOO-BAR" // By definition deeply immutable

        var foo2: Foo = Foo() // By definition not immutable

        // Backing property
        private val _elementList = mutableListOf<Element>()
        val elementList: List<Element>
            get() = _elementList
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    const val fooBar = "FOO-BAR" // By definition deeply immutable

    var FOO2: Foo = Foo() // By definition not immutable

    class Bar {
        val FOO_BAR = "FOO-BAR" // Class properties always start with lowercase, const is not allowed

        // Incomplete backing property as public property 'elementList1' is missing
        private val _elementList1 = mutableListOf<Element>()

        // Invalid backing property as '_elementList2' is not a private property
        val _elementList2 = mutableListOf<Element>()
        val elementList2: List<Element>
            get() = _elementList2
    }
    ```

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `PropertyName`.

Rule id: `property-naming` (`standard` rule set)

## No empty file

A kotlin (script) file should not be empty. It needs to contain at least one declaration. Files only contain a package and/or import statements are as of that disallowed.

Rule id: `no-empty-file`

## No single line block comments

A single line block comment should be replaced with an EOL comment when possible.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    /*
     * Some comment
     */
    val foo = "foo" // Some comment
    val foo = { /* no-op */ }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /* Some comment */
    val foo = "foo" /* Some comment */
    ```

Rule id: `no-single-line-block-comment` (`standard` rule set)

## Spacing

### Function type modifier spacing

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

### No blank lines in list

Disallow blank lines to be used in lists before the first element, between elements, and after the last element.

*Super type*

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class FooBar:
        Foo,
        Bar {
        // body
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class FooBar:

        Foo,

        Bar

    {
        // body
    }
    ```

*Type argument list*

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foobar: FooBar<
        Foo,
        Bar,
        > = FooBar(Foo(), Bar())
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foobar: FooBar<

        Foo,

        Bar,

        > = FooBar(Foo(), Bar())
    ```

*Type constraint list*

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
        val adapter1: A1,
        val adapter2: A2
    ) : RecyclerView.Adapter<C>()
        where A1 : RecyclerView.Adapter<V1>, A1 : ComposableAdapter.ViewTypeProvider,
              A2 : RecyclerView.Adapter<V2>, A2 : ComposableAdapter.ViewTypeProvider {
        // body
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class BiAdapter<C : RecyclerView.ViewHolder, V1 : C, V2 : C, out A1, out A2>(
        val adapter1: A1,
        val adapter2: A2
    ) : RecyclerView.Adapter<C>()
        where
              A1 : RecyclerView.Adapter<V1>, A1 : ComposableAdapter.ViewTypeProvider,

              A2 : RecyclerView.Adapter<V2>, A2 : ComposableAdapter.ViewTypeProvider
    {
        // body
    }
    ```

*Type parameter list*

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun <
        Foo,
        Bar,
        > foobar()
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun <

        Foo,

        Bar,

        > foobar()
    ```

*Value argument list*

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foobar = foobar(
        "foo",
        "bar",
    )
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foobar = foobar(

        "foo",

        "bar",

    )
    ```

*Value parameter list*

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foobar(
        foo: String,
        bar: String,
    )
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foobar(

        foo: String,

        bar: String,

    )
    ```

Rule id: `no-blank-line-in-list` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Parameter list spacing

Consistent spacing inside the parameter list.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo(a: Any ) = "some-result"
    fun foo() = "some-result"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo( a : Any ) = "some-result"
    fun foo(
    ) = "some-result"
    ```

Rule id: `parameter-list-spacing` (`standard` rule set)

### String template indent

Enforce consistent string template indentation for multiline string templates which are post-fixed with `.trimIndent()`. The opening and closing `"""` are placed on separate lines and the indentation of the content of the template is aligned with the `"""`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo =
        """
        line1
        line2
        """.trimIndent()
    fun foo() {
        // The opening """ can not be wrapped to next line as that would result in a compilation error
        return """
            line1
            line2
            """.trimIndent()
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo = """
              line1
              line2
              """.trimIndent()
    fun foo() {
        return """
            line1
            line2
        """.trimIndent()
    }
    ```

Rule id: `string-template-indent` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Try catch finally spacing

Enforce consistent spacing in `try { .. } catch { .. } finally { .. }`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() =
        try {
            // do something
        } catch (exception: Exception) {
            // handle exception
        } finally {
            // clean up
        }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo1() = try { /* ... */ } catch (exception: Exception) { /* ... */ } finally { /* ... */ }
    fun foo2() = 
        try {
            // do something
        }
        catch (exception: Exception) {
            // handle exception
        }
        finally {
            // clean up
        }
    ```

Rule id: `try-catch-finally-spacing` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Type argument list spacing

Spacing before and after the angle brackets of a type argument list.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val res = ArrayList<LintError>()
    class B<T> : A<T>() {
        override fun x() = super<A>.x()
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val res = ArrayList < LintError > ()
    class B<T> : A< T >() {
        override fun x() = super< A >.x()
    }
    ```

Rule id: `type-argument-list-spacing` (`standard` rule set)

### Type parameter list spacing

Spacing after a type parameter list in function and class declarations.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun <T> foo1(t: T) = "some-result"
    fun <T> foo2(t: T) = "some-result"
    fun <T> foo3(t: T) = "some-result"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun<T> foo1(t: T) = "some-result"
    fun <T>foo2(t: T) = "some-result"
    fun<T>foo3(t: T) = "some-result"
    ```

Rule id: `type-parameter-list-spacing` (`standard` rule set)

## Wrapping

### Content receiver wrapping

Wraps the content receiver list to a separate line regardless of maximum line length. If the maximum line length is configured and is exceeded, wrap the context receivers and if needed its projection types to separate lines.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // ALways wrap regardless of whether max line length is set
    context(Foo)
    fun fooBar()

    // Wrap each context receiver to a separate line when the
    // entire context receiver list does not fit on a single line
    context(
        Fooooooooooooooooooo1,
        Foooooooooooooooooooooooooooooo2
    )
    fun fooBar()

    // Wrap each context receiver to a separate line when the
    // entire context receiver list does not fit on a single line.
    // Also, wrap each of it projection types in case a context
    // receiver does not fit on a single line after it has been
    // wrapped.
    context(
        Foooooooooooooooo<
            Foo,
            Bar
            >
    )
    fun fooBar()
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // Should be wrapped regardless of whether max line length is set
    context(Foo) fun fooBar()

    // Should be wrapped when the entire context receiver list does not
    // fit on a single line
    context(Fooooooooooooooooooo1, Foooooooooooooooooooooooooooooo2)
    fun fooBar()

    // Should be wrapped when the entire context receiver list does not
    // fit on a single line. Also, it should wrap each of it projection
    // type in case a context receiver does not fit on a single line 
    // after it has been wrapped.
    context(Foooooooooooooooo<Foo, Bar>)
    fun fooBar()
    ```

Rule id: `context-receiver-wrapping` (`standard` rule set)

### Enum wrapping

An enum should be a single line, or each enum entry has to be placed on a separate line. In case the enumeration contains enum entries and declarations those are to be separated by a blank line.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    enum class Foo { A, B, C, D }

    enum class Foo {
        A,
        B,
        C,
        D,
        ;

        fun foo() = "foo"
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    enum class Foo {
        A,
        B, C,
        D
    }

    enum class Foo {
        A;
        fun foo() = "foo"
    }
    ```

Rule id: `enum-wrapping` (`standard` rule set)

### Multiline expression wrapping

Multiline expression on the right hand side of an expression are forced to start on a separate line. Expressions in return statement are excluded as that would result in a compilation error. 

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo =
        foo(
            parameterName =
                "The quick brown fox "
                    .plus("jumps ")
                    .plus("over the lazy dog"),
        )
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo = foo(
        parameterName = "The quick brown fox "
            .plus("jumps ")
            .plus("over the lazy dog"),
    )
    ```

Rule id: `multiline-expression-wrapping` (`standard` rule set)

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Statement wrapping

A function, class/object body or other block body statement has to be placed on different line than the braces of the body block.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() {
        if (true) {
            // do something
        }
    }
    class A {
        val a = 0
        val b = 1
    }
    enum class FooBar1 { FOO, BAR }
    enum class FooBar2 {
        FOO,
        BAR,
    }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo() { if (true) {
            // do something
        }
    }
    class A { val a = 0
        val b = 1 }
    ```

Rule id: `statement-wrapping`
