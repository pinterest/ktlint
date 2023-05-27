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

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val FOO_1 = Foo()
    val FOO_BAR_1 = "FOO-BAR"

    var foo1: Foo = Foo()

    class Bar {
        const val FOO_2 = "foo"
        const val FOO_BAR_2 = "FOO-BAR"

        val foo2 = "foo"
        val fooBar2 = "foo-bar"
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val Foo1 = Foo()
    val FooBar1 = "FOO-BAR"

    var FOO_1: Foo = Foo()

    class Bar {
        const val foo2 = "foo"
        const val fooBar2 = "FOO-BAR"

        val FOO2 = "foo"
        val FOO_BAR_2 = "foo-bar"
    }
    ```

!!! note
    Top level `val` properties and `const val` properties have to be written in screaming snake notation. Local `val` and `const val` are written in lower camel case.

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

    /* ktlint-disable foo-rule-id bar-rule-id */
    val foo = "foo"
    /* ktlint-enable foo-rule-id bar-rule-id */
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /* Some comment */
    val foo = "foo" /* Some comment */
    ```

Rule id: `no-single-line-block-comment` (`standard` rule set)

## Spacing

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
