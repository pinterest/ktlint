New rules will be added into the [experimental ruleset](https://github.com/pinterest/ktlint/tree/master/ktlint-ruleset-experimental), which can be enabled
by passing the `--experimental` flag to `ktlint`.

## Block comment initial star alignment

Lines in a block comment which (exclusive the indentation) start with a `*` should have this `*` aligned with the `*` in the opening of the block comment.

Rule id: `experimental:block-comment-initial-star-alignment`

## Discouraged comment location

Detect discouraged comment locations (no autocorrect).

Rule id: `experimental:discouraged-comment-location`

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

Rule id: `experimental:unnecessary-parentheses-before-trailing-lambda`

## Function signature

Rewrites the function signature to a single line when possible (e.g. when not exceeding the `max_line_length` property) or a multiline signature otherwise. In case of function with a body expression, the body expression is placed on the same line as the function signature when not exceeding the `max_line_length` property. Optionally the function signature can be forced to be written as a multiline signature in case the function has more than a specified number of parameters (`.editorconfig' property `ktlint_function_signature_wrapping_rule_always_with_minimum_parameters`)

Rule id: `function-signature`

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

Rule id: `experimental:class-naming`

### Function naming

Enforce naming of function. 

!!! note
    Functions in files which import a class from package `org.junit`, `org.testng` or `kotlin.test` are considered to be test functions. Functions in such classes are allowed to have underscores in the name. Or function names can be specified between backticks and do not need to adhere to the normal naming convention.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `FunctionName`.

Rule id: `experimental:function-naming`

### Package naming

Enforce naming of package.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `PackageName`.

Rule id: `experimental:package-naming`

### Property naming

Enforce naming of property.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `PropertyName`.

Rule id: `experimental:property-naming`

## Spacing

### Fun keyword spacing

Consistent spacing after the fun keyword.

Rule id: `experimental:fun-keyword-spacing`

### Function return type spacing

Consistent spacing around the function return type.

Rule id: `experimental:function-return-type-spacing`

### Function start of body spacing

Consistent spacing before start of function body.

Rule id: `experimental:function-start-of-body-spacing`:

### Function type reference spacing

Consistent spacing in the type reference before a function.

Rule id: `experimental:function-type-reference-spacing`

### Modifier list spacing

Consistent spacing between modifiers in and after the last modifier in a modifier list.

Rule id: `experimental:modifier-list-spacing`

### Nullable type spacing

No spaces in a nullable type.

Rule id: `experimental:nullable-type-spacing`

### Parameter list spacing

Consistent spacing inside the parameter list.

Rule id: `experimental:parameter-list-spacing`

### Spacing between function name and opening parenthesis

Consistent spacing between function name and opening parenthesis.

Rule id: `experimental:spacing-between-function-name-and-opening-parenthesis`

### Type argument list spacing

Spacing before and after the angle brackets of a type argument list.

Rule id: `experimental:type-argument-list-spacing`

### Type parameter list spacing

Spacing after a type parameter list in function and class declarations.

Rule id: `experimental:type-parameter-list-spacing`

## Wrapping

### Comment wrapping

A block comment should start and end on a line that does not contain any other element. A block comment should not be used as end of line comment.

Rule id: `experimental:comment-wrapping`

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

Rule id: `experimental:context-receiver-wrapping`

### Kdoc wrapping

A KDoc comment should start and end on a line that does not contain any other element.

Rule id: `experimental:kdoc-wrapping`
