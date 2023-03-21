Experimental rules in ktlint are part of the [standard ruleset](https://github.com/pinterest/ktlint/tree/master/ktlint-ruleset-standard). Enabling `.editorconfig` property `ktlint_experimental` to enable all experimental rules. Or, enable a specific experimental rule by setting `.editorconfig` property `ktlint_<rule-id>` where `<rule-id>` is replaced with the id of the rule.

## Block comment initial star alignment

Lines in a block comment which (exclusive the indentation) start with a `*` should have this `*` aligned with the `*` in the opening of the block comment.

Rule id: `block-comment-initial-star-alignment`

## Discouraged comment location

Detect discouraged comment locations (no autocorrect).

Rule id: `discouraged-comment-location`

## Disallow empty lines at start of class body

Detect blank lines at start of a class body.

Rule id: `no-empty-first-line-in-class-body`

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Disallow consecutive comments

Disallow consecutive comments (EOL comments, block comments or KDoc) except EOL comments. Comments need to be separated by at least one code element.

=== "[:material-heart:](#) Ktlint (ktlint_official code style)"

    ```kotlin
    // An EOL comment
    // may be followed by another EOL comment
    val foo = "foo"

    // An EOL comment
    /* followed by a block comment */
    /** or a KDoc
     * will be reported as a violation when '.editorconfig' property 'ktlint_code_style = ktlint_official` is set
     */
    val bar = "bar" 
    ```

=== "[:material-heart:](#) Ktlint (non ktlint_official code style)"

    ```kotlin
    // An EOL comment
    /* followed by a block comment */
    /** or a KDoc
     * will not be reported as a violation
     */
    val bar = "bar" 
    ```


Rule id: `no-consecutive-comments`

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

## Unnecessary parenthesis before trailing lambda

An empty parentheses block before a lambda is redundant.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    "some-string".count { it == '-' }
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    "some-string".count() { it == '-' }
    ```

Rule id: `unnecessary-parentheses-before-trailing-lambda`

## Function signature

Rewrites the function signature to a single line when possible (e.g. when not exceeding the `max_line_length` property) or a multiline signature otherwise. In case of function with a body expression, the body expression is placed on the same line as the function signature when not exceeding the `max_line_length` property. Optionally the function signature can be forced to be written as a multiline signature in case the function has more than a specified number of parameters (`.editorconfig' property `ktlint_function_signature_wrapping_rule_always_with_minimum_parameters`)

Rule id: `function-signature`

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

## Naming

### Class/object naming

Enforce naming of class.

!!! note
    Functions in files which import a class from package `org.junit.jupiter.api` are considered to be test functions and are allowed to have a name specified between backticks and do not need to adhere to the normal naming convention. Although, the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) does not allow this explicitly for class identifiers, `ktlint` does allow it as this makes it possible to write code like below:
     ```kotlin
     @Nested
     inner class `Some descriptive class name` {
         @Test
         fun `Some descriptive test name`() {
             // do something
         }
     }
     ```

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `ClassName`.

Rule id: `class-naming`

### Function naming

Enforce naming of function. 

!!! note
    Functions in files which import a class from package `org.junit`, `org.testng` or `kotlin.test` are considered to be test functions. Functions in such classes are allowed to have underscores in the name. Or function names can be specified between backticks and do not need to adhere to the normal naming convention.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `FunctionName`.

Rule id: `function-naming`

### Package naming

Enforce naming of package.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `PackageName`.

Rule id: `package-naming`

### Property naming

Enforce naming of property.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `PropertyName`.

Rule id: `property-naming`

## Spacing

### Fun keyword spacing

Consistent spacing after the fun keyword.

Rule id: `fun-keyword-spacing`

### Function return type spacing

Consistent spacing around the function return type.

Rule id: `function-return-type-spacing`

### Function start of body spacing

Consistent spacing before start of function body.

Rule id: `function-start-of-body-spacing`:

### Function type reference spacing

Consistent spacing in the type reference before a function.

Rule id: `function-type-reference-spacing`

### Modifier list spacing

Consistent spacing between modifiers in and after the last modifier in a modifier list.

Rule id: `modifier-list-spacing`

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

Rule id: `no-blank-line-in-list`

### Nullable type spacing

No spaces in a nullable type.

Rule id: `nullable-type-spacing`

### Parameter list spacing

Consistent spacing inside the parameter list.

Rule id: `parameter-list-spacing`

### Spacing between function name and opening parenthesis

Consistent spacing between function name and opening parenthesis.

Rule id: `spacing-between-function-name-and-opening-parenthesis`

### String template indent

Enforce consistent string template indentation for multiline string templates which are post-fixed with `.trimIndent()`. The opening and closing `"""` are placed on separate lines and the indentation of the content of the template is aligned with the `"""`.

=== "[:material-heart:](#) Ktlint (ktlint_official code style)"

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
=== "[:material-heart:](#) Ktlint (non ktlint_official code style)"

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

Rule id: `string-template-indent`

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Try catch finally spacing

Enforce consistent spacing in `try { .. } catch { .. } finally { .. }`.

=== "[:material-heart:](#) Ktlint (ktlint_official code style)"

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
=== "[:material-heart:](#) Ktlint (non ktlint_official code style)"

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

Rule id: `try-catch-finally-spacing`

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.

### Type argument list spacing

Spacing before and after the angle brackets of a type argument list.

Rule id: `type-argument-list-spacing`

### Type parameter list spacing

Spacing after a type parameter list in function and class declarations.

Rule id: `type-parameter-list-spacing`

## Wrapping

### Comment wrapping

A block comment should start and end on a line that does not contain any other element. A block comment should not be used as end of line comment.

Rule id: `comment-wrapping`

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

Rule id: `context-receiver-wrapping`

### Kdoc wrapping

A KDoc comment should start and end on a line that does not contain any other element.

Rule id: `kdoc-wrapping`

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

Rule id: `multiline-expression-wrapping`

!!! Note
    This rule is only run when `ktlint_code_style` is set to `ktlint_official` or when the rule is enabled explicitly.
