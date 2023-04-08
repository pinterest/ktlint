## Annotation formatting

Multiple annotations should be on a separate line than the annotated declaration; annotations with parameters should each be on separate lines; annotations should be followed by a space

Rule-id: `annotation`

## Argument list wrapping

Rule-id: `argument-list-wrapping`

## Block comment initial star alignment

Lines in a block comment which (exclusive the indentation) start with a `*` should have this `*` aligned with the `*` in the opening of the block comment.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    /*
     * This comment is formatted well.
     */
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /*
          * This comment is not formatted well.
        */
    ```

Rule id: `block-comment-initial-star-alignment`

## Chain wrapping

When wrapping chained calls `.`, `?.` and `?:` should be placed on the next line

Rule id: `chain-wrapping`

## Class/object naming

Enforce naming of class.

!!! note
    Functions in files which import a class from package `org.junit.jupiter.api` are considered to be test functions and are allowed to have a name specified between backticks and do not need to adhere to the normal naming convention. Although, the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) does not allow this explicitly for class identifiers, `ktlint` does allow it as this makes it possible to write code like below:

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class Foo
    class Foo1
    ```
=== "[:material-heart:](#) Ktlint JUnit Test"

    ```kotlin
    @Nested
    inner class `Some descriptive class name` {
        @Test
        fun `Some descriptive test name`() {
            // do something
        }
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class foo
    class Foo_Bar
    class `Some class in the production code`
    ```

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `ClassName`.

Rule id: `class-naming`

## Enum entry

Enum entry names should be uppercase underscore-separated names.

Rule id: `enum-entry-name-case`

## File name

Files containing only one toplevel domain should be named according to that element.

Rule id: `filename`

## Final newline

Ensures consistent usage of a newline at the end of each file. 

This rule can be configured with `.editorconfig` property [`insert_final_newline`](../configuration-ktlint/#final-newline).

Rule id: `final-newline`

## Import ordering

Imports ordered consistently (see [Custom ktlint EditorConfig properties](#custom-ktlint-specific-editorconfig-properties) for more)

Rule id: `import-ordering`

## Indentation

Indentation formatting - respects `.editorconfig` `indent_size` with no continuation indent (see [EditorConfig](../configuration-ktlint/) section for more).

Rule id: `indent`

## Max line length

Ensures that lines do not exceed the given length of `.editorconfig` property `max_line_length` (see [EditorConfig](../configuration-ktlint/) section for more). This rule does not apply in a number of situations. For example, in the case a line exceeds the maximum line length due to and comment that disables ktlint rules than that comment is being ignored when validating the length of the line. The `.editorconfig` property `ktlint_ignore_back_ticked_identifier` can be set to ignore identifiers which are enclosed in backticks, which for example is very useful when you want to allow longer names for unit tests.

Rule id: `max-line-length`

## Modifier order

Consistent order of modifiers

Rule id: `modifier-order`

## Multiline if-else

Braces required for multiline if/else statements.

Rule id: `multiline-if-else`

## No blank lines before `}`

No blank lines before `}`.

Rule id: `no-blank-line-before-rbrace`

## No blank lines in chained method calls

Rule id: `no-blank-lines-in-chained-method-calls`

## No consecutive blank lines
 
Rule id: `no-consecutive-blank-lines`

## No empty (`{}`) class bodies

Rule id: `no-empty-class-body`

## No leading empty lines in method blocks
 
Rule id: `no-empty-first-line-in-method-block`

## No line break after else

Disallows line breaks after the else keyword if that could lead to confusion, for example:
```kotlin
if (conditionA()) {
    doSomething()
} else
if (conditionB()) {
    doAnotherThing()
}
```

Rule id: `no-line-break-after-else`

## No line break before assignment 

When a line is broken at an assignment (`=`) operator the break comes after the symbol.

Rule id: `no-line-break-before-assignment`

## No multi spaces

Except in indentation and in KDoc's it is not allowed to have multiple consecutive spaces.

Rule id: `no-multi-spaces`

## No semicolons

No semicolons (unless used to separate multiple statements on the same line).

Rule id: `no-semi`

## No trailing whitespaces

Rule id: `no-trailing-spaces`

## No `Unit` as return type 

The `Unit` type is not allowed as return type of a function.
returns (`fun fn {}` instead of `fun fn: Unit {}`)

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun fn() {}
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun fn(): Unit {}
    ```

Rule id: `no-unit-return`

## No unused imports

Rule id: `no-unused-imports`

## No wildcard imports

No wildcard imports except imports listed in `.editorconfig` property `ij_kotlin_packages_to_use_import_on_demand`.

!!! warning

    In case property `ij_kotlin_packages_to_use_import_on_demand` is not explicitly set, it allows wildcards imports like `java.util.*` by default to keep in sync with IntelliJ IDEA behavior. To disallow *all* wildcard imports, add property below to your `.editorconfig`:
    ```editorconfig
    [*.{kt,kts}]
    ij_kotlin_packages_to_use_import_on_demand = unset
    ```

Rule id: `no-wildcard-imports`

## Package name

Validates that the package name matches the regular expression `[a-z][a-zA-Z\d]*(\.[a-z][a-zA-Z\d]*)*`.

Rule id: `package-name`

## Parameter list wrapping

When class/function signature doesn't fit on a single line, each parameter must be on a separate line

Rule id: `parameter-list-wrapping`

## Parameter wrapping

When a function or class parameter doesn't fit on a single line, wrap the type or value to a separate line

Rule id: `parameter-wrapping`

## Property wrapping

When a property doesn't fit on a single line, wrap the type or value to a separate line

Rule id: `property-wrapping`

## String template

Consistent string templates (`$v` instead of `${v}`, `${p.v}` instead of `${p.v.toString()}`)

Rule id: `string-template`

## Trailing comma on call site

Consistent removal (default) or adding of trailing commas on call site.

!!! important
    KtLint uses the IntelliJ IDEA `.editorconfig` property `ij_kotlin_allow_trailing_comma_on_call_site` to configure the rule. When this property is enabled, KtLint *enforces* the usage of the trailing comma at call site while IntelliJ IDEA default formatter only *allows* to use the trailing comma but leaves it to the developer's discretion to actually use it (or not). KtLint values *consistent* formatting more than a per-situation decision.

!!! note
    In KtLint 0.48.x the default value for using the trailing comma on call site has been changed to `true` except when codestyle `android` is used.
    
    Although the [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#trailing-commas) leaves it to the developer's discretion to use trailing commas on the call site, it also states that usage of trailing commas has several benefits:
    
     * It makes version-control diffs cleaner – as all the focus is on the changed value.
     * It makes it easy to add and reorder elements – there is no need to add or delete the comma if you manipulate elements.
     * It simplifies code generation, for example, for object initializers. The last element can also have a comma.

!!! note
    Trailing comma on call site is automatically disabled if the [Wrapping](#wrapping) rule (or, before version `0.45.0`, the [Indentation](#indentation) rule) is disabled or not loaded. Because it cannot provide proper formatting with unwrapped calls. (see [dependencies](./dependencies.md)).

    === "[:material-heart:](#) Ktlint"

        ```kotlin
        FooWrapper(
            Foo(
                a = 3,
                b = 4,
            ),
        )
        ```
    === "[:material-heart-off-outline:](#) Disallowed"

        ```kotlin
        FooWrapper(Foo(
            a = 3,
            b = 4,
        ),) // it's weird to insert "," between unwrapped (continued) parenthesis
        ```


Rule id: `trailing-comma-on-call-site`

## Trailing comma on declaration site

Consistent removal (default) or adding of trailing commas on declaration site.

!!! important
    KtLint uses the IntelliJ IDEA `.editorconfig` property `ij_kotlin_allow_trailing_comma` to configure the rule. When this property is enabled, KtLint *enforces* the usage of the trailing comma at declaration site while IntelliJ IDEA default formatter only *allows* to use the trailing comma but leaves it to the developer's discretion to actually use it (or not). KtLint values *consistent* formatting more than a per-situation decision.

!!! note
    In KtLint 0.48.x the default value for using the trailing comma on declaration site has been changed to `true` except when codestyle `android` is used.

    The [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#trailing-commas) encourages the usage of trailing commas on the declaration site, but leaves it to the developer's discretion to use trailing commas on the call site. But next to this, it also states that usage of trailing commas has several benefits:
    
     * It makes version-control diffs cleaner – as all the focus is on the changed value.
     * It makes it easy to add and reorder elements – there is no need to add or delete the comma if you manipulate elements.
     * It simplifies code generation, for example, for object initializers. The last element can also have a comma.

!!! note
    Trailing comma on declaration site is automatically disabled if the [Wrapping](#wrapping) rule (or, before version `0.45.0`, the [Indentation](#indentation) rule) is disabled or not loaded. Because it cannot provide proper formatting with unwrapped declarations. (see [dependencies](./dependencies.md)).

    === "[:material-heart:](#) Ktlint"

        ```kotlin
        class FooWrapper(
            val foo = Foo(
                a = 3,
                b = 4,
            ),
        )
        ```
    === "[:material-heart-off-outline:](#) Disallowed"

        ```kotlin
        class FooWrapper(val foo = Foo(
            a = 3,
            b = 4,
        ),) // it's weird to insert "," between unwrapped (continued) parenthesis
        ```

Rule id: `trailing-comma-on-declaration-site`

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

## Wrapping

### Wrapping

Inserts missing newlines (for example between parentheses of a multi-line function call).

Rule id: `wrapping`

### Comment wrapping

A block comment should start and end on a line that does not contain any other element. A block comment should not be used as end of line comment.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    /* Some comment 1 */
    val foo1 = "foo1"
    val foo2 = "foo" // Some comment
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /* Some comment 1 */ val foo1 = "foo1"
    val foo2 = "foo" /* Block comment instead of end-of-line comment */
    val foo3 = "foo" /* Some comment
                      * with a newline
                      */
    ```

Rule id: `comment-wrapping`

## Spacing

### Angle bracket spacing

No spaces around angle brackets when used for typing.

Rule id: `spacing-around-angle-brackets`

### Annotation spacing

Annotations should be separated by a single line break.

Rule id: `annotation-spacing`

### Blank line between declarations with annotations

Declarations with annotations should be separated by a blank line.

Rule id: `spacing-between-declarations-with-annotations`

### Blank line between declaration with comments

Declarations with comments should be separated by a blank line.

Rule id: `spacing-between-declarations-with-comments`

### Colon spacing

Consistent spacing around colon.

Rule id: `colon-spacing`

### Comma spacing

Consistent spacing around comma.

Rule id: `comma-spacing`

### Comment spacing

The end of line comment sign `//` should be preceded and followed by exactly a space.

Rule id: `comment-spacing`

### Curly spacing

Consistent spacing around curly braces.

Rule id: `curly-spacing`

### Dot spacing

Consistent spacing around dots.

Rule id: `dot-spacing`

### Double colon spacing

No spaces around `::`.

Rule id: `double-colon-spacing`

### Function return type spacing

Consistent spacing around the function return type.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo(): String = "some-result"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo1() : String = "some-result"
    fun foo2():  String = "some-result"
    fun foo3():String = "some-result"
    fun foo4():
        String = "some-result"
    ```

Rule id: `function-return-type-spacing`

### Function start of body spacing

Consistent spacing before start of function body.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo1() = "some-result"
    fun foo2() =
        "some-result"
    fun foo3() {
        // do something
    }
    fun bar1(): String = "some-result"
    fun bar2(): String =
        "some-result"
    fun bar3(): String {
        return "some-result"
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo1()= "some-result"
    fun foo2()
        = "some-result"
    fun foo3()
    {
        // do something
    }
    fun bar1(): String= "some-result"
    fun bar2(): String
        = "some-result"
    fun bar3(): String
    {
        return "some-result"
    }
    ```

Rule id: `function-start-of-body-spacing`:

### Function type reference spacing

Consistent spacing in the type reference before a function.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun String.foo() = "some-result"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun String .foo() = "some-result"
    fun String
        .foo() = "some-result"
    fun String? .foo() = "some-result"
    fun String?
        .foo() = "some-result"
    ```

Rule id: `function-type-reference-spacing`

### Fun keyword spacing

Consistent spacing after the fun keyword.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() = "some-result"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun  foo() = "some-result"
    fun
    foo() = "some-result"
    ```

Rule id: `fun-keyword-spacing`

### Kdoc wrapping

A KDoc comment should start and end on a line that does not contain any other element.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    /** Some KDoc comment 1 */
    val foo1 = "foo1"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /** Some KDoc comment 1 */ val foo1 = "foo1"
    val foo2 = "foo2" /** Some KDoc comment
                       * with a newline
                       */
    ```

Rule id: `kdoc-wrapping`

### Keyword spacing

Consistent spacing around keywords.

Rule id: `keyword-spacing`

### Modifier list spacing

Consistent spacing between modifiers in and after the last modifier in a modifier list.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    abstract class Foo {
        protected abstract suspend fun execute()
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    abstract  class Foo {
        protected  abstract  suspend  fun execute()
    }
    abstract
    class Foo {
        protected
        abstract
        suspend
        fun execute()
    }
    ```

Rule id: `modifier-list-spacing`

### Nullable type spacing

No spaces in a nullable type.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo: String? = null
    val foo: List<String?> = listOf(null)
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo: String ? = null
    val foo: List<String ?> = listOf(null)
    ```

Rule id: `nullable-type-spacing`

### Operator spacing

Consistent spacing around operators.

Rule id: `op-spacing`

### Parenthesis spacing

Consistent spacing around parenthesis.

Rule id: `paren-spacing`

### Range spacing

Consistent spacing around range operators.

Rule id: `range-spacing`

### Spacing between function name and opening parenthesis

Consistent spacing between function name and opening parenthesis.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() = "foo"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo () = "foo"
    ```

Rule id: `spacing-between-function-name-and-opening-parenthesis`

### Unary operator spacing

No spaces around unary operators.

Rule id: `unary-op-spacing`
