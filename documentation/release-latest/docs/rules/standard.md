## Annotation formatting

Multiple annotations should be on a separate line than the annotated declaration; annotations with parameters should each be on separate lines; annotations should be followed by a space

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // A single annotation (without parameters) is allowed on same line as annotated construct
    @FunctionalInterface class FooBar {
        @JvmField var foo: String
        @Test fun bar() {}
    }
    // A class or function parameter may have a single annotation with parameter(s) on the same line
    class Foo(@Path("fooId") val fooId: String)
    class Bar(
        @NotNull("fooId") val fooId: String,
        @NotNull("bar") bar: String
    )
    // Multiple annotations (without parameters) are allowed on the same line
    @Foo @Bar
    class FooBar {
        @Foo @Bar
        var foo: String
        @Foo @Bar
        fun bar() {}
    }
    // An array of annotations (without parameters) is allowed on same line as annotated construct
    @[Foo Bar] class FooBar2 {
        @[Foo Bar] var foo: String
        @[Foo Bar] fun bar() {}
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // An annotation with parameter(s) is not allowed on same line as annotated construct
    @Suppress("Unused") class FooBar {
        @Suppress("Unused") var foo: String
        @Suppress("Unused") fun bar() {}
    }
    // Multiple annotation on same line as annotated construct are not allowed
    @Foo @Bar class FooBar {
        @Foo @Bar var foo: String
        @Foo @Bar fun bar() {}
    }
    ```

Rule-id: `annotation` (`standard` rule set)

## Argument list wrapping

All arguments should be on the same line, or every argument should be on a separate line.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val x = f(
        a,
        b,
        c
    )
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val x = f(
        a,
        b, c
    )
    ```

Rule-id: `argument-list-wrapping` (`standard` rule set)

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

Rule id: `block-comment-initial-star-alignment` (`standard` rule set)

## Chain wrapping

When wrapping chained calls `.`, `?.` and `?:` should be placed on the next line

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo = listOf(1, 2, 3)
        .filter { it > 2 }!!
        .takeIf { it.count() > 100 }
        ?.sum()
    val foobar = foo() ?:
        bar
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo = listOf(1, 2, 3).
        filter { it > 2 }!!.
        takeIf { it.count() > 100 }?.
        sum()
    val foobar = foo()
        ?: bar
    ```

Rule id: `chain-wrapping` (`standard` rule set)

## Class/object naming

Enforce naming of class.

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

!!! note
    Functions in files which import a class from package `org.junit.jupiter.api` are considered to be test functions and are allowed to have a name specified between backticks and do not need to adhere to the normal naming convention. Although, the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) does not allow this explicitly for class identifiers, `ktlint` does allow it.

This rule can also be suppressed with the IntelliJ IDEA inspection suppression `ClassName`.

Rule id: `class-naming` (`standard` rule set)

## Enum entry

Enum entry names should be uppercase underscore-separated names.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    enum class Bar {
        FOO,
        Foo,
        FOO_BAR,
        Foo_Bar
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    enum class Bar {
        foo,
        bAr,
        Foo_Bar,
    }
    ```

Rule id: `enum-entry-name-case` (`standard` rule set)

## File name

A file containing only one visible (e.g. non-private) class, and visible declarations related to that class only, should be named according to that element. The same applies if the file does not contain a visible class but exactly one type alias or one object declaration. Otherwise, the PascalCase notation should be used.

Rule id: `filename` (`standard` rule set)

## Final newline

Ensures consistent usage of a newline at the end of each file. 

This rule can be configured with `.editorconfig` property [`insert_final_newline`](../configuration-ktlint/#final-newline).

Rule id: `final-newline` (`standard` rule set)

## Import ordering

Ensures that imports are ordered consistently (see [Import Layouts](../configuration-ktlint/#import-layouts) for configuration).

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    import com.bar.Bar
    import com.foo.Foo
    import org.foo.bar.FooBar
    import java.util.concurrent.ConcurrentHashMap
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    import com.bar.Bar
    import java.util.concurrent.ConcurrentHashMap
    import org.foo.bar.FooBar
    import com.foo.Foo
    ```

Rule id: `import-ordering` (`standard` rule set)

## Indentation

Indentation formatting - respects `.editorconfig` `indent_size` with no continuation indent (see [EditorConfig](../configuration-ktlint/) section for more).

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun main() {
        foobar(
            a,
            b,
            c
        )
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun main() {
        foobar(
              a,
              b,
              c
              )
    }
    ```

!!! note
    This rule handles indentation for many different language constructs which can not be summarized with a few examples. See the [unit tests](https://github.com/pinterest/ktlint/blob/master/ktlint-ruleset-standard/src/test/kotlin/com/pinterest/ktlint/ruleset/standard/rules/IndentationRuleTest.kt) for more details.

Rule id: `indent` (`standard` rule set)

## Max line length

Ensures that lines do not exceed the given length of `.editorconfig` property `max_line_length` (see [EditorConfig](../configuration-ktlint/) section for more). This rule does not apply in a number of situations. For example, in the case a line exceeds the maximum line length due to a comment that disables ktlint rules then that comment is being ignored when validating the length of the line. The `.editorconfig` property `ktlint_ignore_back_ticked_identifier` can be set to ignore identifiers which are enclosed in backticks, which for example is very useful when you want to allow longer names for unit tests.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    // Lines below are accepted although the max
    // line length is exceeded.
    package com.toooooooooooooooooooooooooooo.long
    import com.tooooooooooooooooooooooooooooo.long
    val foo =
        """
        fooooooooooooooooooooooooooooooooooooooooo
        """
    @Test
    fun `Test description which is toooooooooooo long`() {
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    val fooooooooooooooo = "fooooooooooooooooooooo"
    val foooooooooooooo = "foooooooooooooooooooo" // some comment
    val fooooooooooooo =
        "foooooooooooooooooooooooooooooooooooooooo"
    ```

Rule id: `max-line-length` (`standard` rule set)

## Modifier order

Consistent order of modifiers

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    abstract class A {
        protected open val v = ""
        internal open suspend fun f(v: Any): Any = ""
        protected lateinit var lv: String
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    abstract class A {
        open protected val v = ""
        open suspend internal fun f(v: Any): Any = ""
        lateinit protected var lv: String
    }
    ```

Rule id: `modifier-order` (`standard` rule set)

## Multiline if-else

Braces required for multiline if/else statements.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo =
        if (true) {
            return 0
        } else {
            return 1
        }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo =
        if (true)
            return 0
        else
            return 1
    ```

Rule id: `multiline-if-else` (`standard` rule set)

## No blank lines before `}`

No blank lines before `}`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun main() {
        fun a() {
        }
        fun b()
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun main() {
        fun a() {

        }
        fun b()

    }
    ```

Rule id: `no-blank-line-before-rbrace` (`standard` rule set)

## No blank lines in chained method calls

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo(inputText: String) {
        inputText
            .lowercase(Locale.getDefault())
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo(inputText: String) {
        inputText

            .lowercase(Locale.getDefault())
    }
    ```

Rule id: `no-blank-lines-in-chained-method-calls` (`standard` rule set)

## No consecutive blank lines

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    package com.test

    import com.test.util

    val a = "a"

    fun b() {
    }

    fun c()
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    package com.test


    import com.test.util


    val a = "a"


    fun b() {
    }


    fun c()
    ```
 
Rule id: `no-consecutive-blank-lines` (`standard` rule set)

## No empty (`{}`) class bodies

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class C
    data class DC(val v: Any)
    interface I
    object O
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class C {}
    data class DC(val v: Any) { }
    interface I {
    }
    object O{}
    ```

Rule id: `no-empty-class-body` (`standard` rule set)

## No leading empty lines in method blocks

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun bar() {
       val a = 2
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun bar() {

       val a = 2
    }
    ```

Rule id: `no-empty-first-line-in-method-block` (`standard` rule set)

## No line break after else

Disallows line breaks after the else keyword if that could lead to confusion, for example:

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun funA() {
        if (conditionA()) {
            doSomething()
        } else if (conditionB()) {
            doAnotherThing()
        }
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun funA() {
        if (conditionA()) {
            doSomething()
        } else
        if (conditionB()) {
            doAnotherThing()
        }
    }
    ```

Rule id: `no-line-break-after-else` (`standard` rule set)

## No line break before assignment 

When a line is broken at an assignment (`=`) operator the break comes after the symbol.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val valA =
        ""
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val valA
        = ""
    ```

Rule id: `no-line-break-before-assignment` (`standard` rule set)

## No multi spaces

Except in indentation and in KDoc's it is not allowed to have multiple consecutive spaces.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun main() {
        x(1, 3)
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun  main()  {
        x(1,  3)
    }
    ```

Rule id: `no-multi-spaces` (`standard` rule set)

## No semicolons

No semicolons (unless used to separate multiple statements on the same line).

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo() {
        bar()

        bar()
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo() {
        ;
        bar()
        ;

        bar()

        ;
    }
    ```

Rule id: `no-semi` (`standard` rule set)

## No trailing whitespaces

Rule id: `no-trailing-spaces` (`standard` rule set)

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

Rule id: `no-unit-return` (`standard` rule set)

## No unused imports

!!! warning
    This rule is not able to detect *all* unused imports as mentioned in this [issue comment](https://github.com/pinterest/ktlint/issues/1754#issuecomment-1368201667).

Rule id: `no-unused-imports` (`standard` rule set)

## No wildcard imports

No wildcard imports except imports listed in `.editorconfig` property `ij_kotlin_packages_to_use_import_on_demand`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    import foobar.Bar
    import foobar.Foo
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    import foobar.*
    ```

!!! warning
    In case property `ij_kotlin_packages_to_use_import_on_demand` is not explicitly set, it allows wildcards imports like `java.util.*` by default to keep in sync with IntelliJ IDEA behavior. To disallow *all* wildcard imports, add property below to your `.editorconfig`:
    ```editorconfig
    [*.{kt,kts}]
    ij_kotlin_packages_to_use_import_on_demand = unset
    ```

Rule id: `no-wildcard-imports` (`standard` rule set)

## Package name

Validates that the package name matches the regular expression `[a-z][a-zA-Z\d]*(\.[a-z][a-zA-Z\d]*)*`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    package foo
    package foo.bar
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    package Foo
    package foo.Foo
    package `foo bar`
    package foo.`foo bar`
    ```

Rule id: `package-name` (`standard` rule set)

## Parameter list wrapping

When class/function signature doesn't fit on a single line, each parameter must be on a separate line

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class ClassA(paramA: String, paramB: String, paramC: String)
    class ClassA(
        paramA: String,
        paramB: String,
        paramC: String
    )
    fun f(a: Any, b: Any, c: Any)
    fun f(
        a: Any,
        b: Any,
        c: Any
    )
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class ClassA(
        paramA: String, paramB: String,
        paramC: String
    )
    fun f(
        a: Any,
        b: Any, c: Any
    )
    ```

Rule id: `parameter-list-wrapping` (`standard` rule set)

## Parameter wrapping

When a function or class parameter doesn't fit on a single line, wrap the type or value to a separate line

=== "[:material-heart:](#) Ktlint (ktlint_official)"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    class Bar(
        val fooooooooooooooooooooooooTooLong:
            Foo,
    )
    fun bar(
        fooooooooooooooooooooooooTooLong:
            Foo,
    )
    ```
=== "[:material-heart:](#) Ktlint (non ktlint_official)"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    class Bar(
        val fooooooooooooooooooooooooTooLong:
        Foo,
    )
    fun bar(
        fooooooooooooooooooooooooTooLong:
        Foo,
    )
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    class Bar(
        val fooooooooooooooooooooooooTooLong: Foo,
    )
    fun bar(
        fooooooooooooooooooooooooooooTooLong: Foo,
    )
    ```

Rule id: `parameter-wrapping` (`standard` rule set)

## Property wrapping

When a property doesn't fit on a single line, wrap the type or value to a separate line

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    val aVariableWithALooooooooooooongName:
        String
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // Assume that the last allowed character is
    // at the X character on the right           X
    val aVariableWithALooooooooooooongName: String
    ```

Rule id: `property-wrapping` (`standard` rule set)

## String template

Consistent string templates (`$v` instead of `${v}`, `${p.v}` instead of `${p.v.toString()}`)

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo = "$foo hello"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo = "${foo} hello"
    ```

Rule id: `string-template` (`standard` rule set)

## Trailing comma on call site

Consistent removal (default) or adding of trailing commas on call site.

!!! important
    KtLint uses the IntelliJ IDEA `.editorconfig` property `ij_kotlin_allow_trailing_comma_on_call_site` to configure the rule. When this property is enabled, KtLint *enforces* the usage of the trailing comma at call site while IntelliJ IDEA default formatter only *allows* to use the trailing comma but leaves it to the developer's discretion to actually use it (or not). KtLint values *consistent* formatting more than a per-situation decision.

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

!!! note
    In KtLint 0.48.x the default value for using the trailing comma on call site has been changed to `true` except when codestyle `android` is used.
    
    Although the [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#trailing-commas) leaves it to the developer's discretion to use trailing commas on the call site, it also states that usage of trailing commas has several benefits:
    
     * It makes version-control diffs cleaner – as all the focus is on the changed value.
     * It makes it easy to add and reorder elements – there is no need to add or delete the comma if you manipulate elements.
     * It simplifies code generation, for example, for object initializers. The last element can also have a comma.

!!! note
    Trailing comma on call site is automatically disabled if the [Wrapping](#wrapping) rule (or, before version `0.45.0`, the [Indentation](#indentation) rule) is disabled or not loaded. Because it cannot provide proper formatting with unwrapped calls. (see [dependencies](./dependencies.md)).

Rule id: `trailing-comma-on-call-site` (`standard` rule set)

## Trailing comma on declaration site

Consistent removal (default) or adding of trailing commas on declaration site.

!!! important
    KtLint uses the IntelliJ IDEA `.editorconfig` property `ij_kotlin_allow_trailing_comma` to configure the rule. When this property is enabled, KtLint *enforces* the usage of the trailing comma at declaration site while IntelliJ IDEA default formatter only *allows* to use the trailing comma but leaves it to the developer's discretion to actually use it (or not). KtLint values *consistent* formatting more than a per-situation decision.

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

!!! note
    In KtLint 0.48.x the default value for using the trailing comma on declaration site has been changed to `true` except when codestyle `android` is used.

    The [Kotlin coding conventions](https://kotlinlang.org/docs/reference/coding-conventions.html#trailing-commas) encourages the usage of trailing commas on the declaration site, but leaves it to the developer's discretion to use trailing commas on the call site. But next to this, it also states that usage of trailing commas has several benefits:
    
     * It makes version-control diffs cleaner – as all the focus is on the changed value.
     * It makes it easy to add and reorder elements – there is no need to add or delete the comma if you manipulate elements.
     * It simplifies code generation, for example, for object initializers. The last element can also have a comma.

!!! note
    Trailing comma on declaration site is automatically disabled if the [Wrapping](#wrapping) rule (or, before version `0.45.0`, the [Indentation](#indentation) rule) is disabled or not loaded. Because it cannot provide proper formatting with unwrapped declarations. (see [dependencies](./dependencies.md)).

Rule id: `trailing-comma-on-declaration-site` (`standard` rule set)

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

Rule id: `unnecessary-parentheses-before-trailing-lambda` (`standard` rule set)

## Wrapping

### Wrapping

Inserts missing newlines (for example between parentheses of a multi-line function call).

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val x = f(
        a,
        b,
        c
    )
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val x = f(
        a,
        b,
        c)
    ```

Rule id: `wrapping` (`standard` rule set)

### Comment wrapping

A block comment should start and end on a line that does not contain any other element.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    /* Some comment 1 */
    val foo1 = "foo1"
    val foo2 = "foo" // Some comment
    val foo3 = { /* no-op */ } 
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    /* Some comment 1 */ val foo1 = "foo1"
    val foo2 = "foo" /* Block comment instead of end-of-line comment */
    val foo3 = "foo" /* Some comment
                      * with a newline
                      */
    ```

Rule id: `comment-wrapping` (`standard` rule set)

## Spacing

### Angle bracket spacing

No spaces around angle brackets when used for typing.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val a: Map<Int, String> = mapOf()
    val b: Map<Int, String> = mapOf()
    val c: Map<Int, String> = mapOf()
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val a: Map< Int, String> = mapOf()
    val b: Map<Int, String > = mapOf()
    val c: Map <Int, String> = mapOf()
    ```

Rule id: `spacing-around-angle-brackets` (`standard` rule set)

### Annotation spacing

Annotations should be separated by a single line break.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    @JvmField
    fun foo() {}

    /**
     * block comment
     */
    @Foo @Bar
    class FooBar {
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    @JvmField

    fun foo() {}

    @Foo @Bar
    /**
     * block comment
     */
    class FooBar {
    }
    ```

Rule id: `annotation-spacing` (`standard` rule set)

### Blank line between declarations with annotations

Declarations with annotations should be separated by a blank line.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun a()

    @Bar
    fun b()
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun a()
    @Bar
    fun b()
    ```

Rule id: `spacing-between-declarations-with-annotations` (`standard` rule set)

### Blank line between declaration with comments

Declarations with comments should be separated by a blank line.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // some comment 1
    bar()

    /*
     * some comment 2
     */
    foo()
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    // some comment 1
    bar()
    /*
     * some comment 2
     */
    foo()
    ```

Rule id: `spacing-between-declarations-with-comments` (`standard` rule set)

### Colon spacing

Consistent spacing around colon.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class A : B
    class A2 : B2
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class A:B
    class A2  :  B2
    ```

Rule id: `colon-spacing` (`standard` rule set)

### Comma spacing

Consistent spacing around comma.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = Foo(1, 3)
    val foo2 = Foo(1, 3)
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 = Foo(1 ,3)
    val foo2 = Foo(1,3)
    ```

Rule id: `comma-spacing` (`standard` rule set)

### Comment spacing

The end of line comment sign `//` should be preceded and followed by exactly a space.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    // comment
    var debugging = false // comment
    var debugging = false // comment
    var debugging = false // comment
    fun main() {
        System.out.println( // 123
            "test"
        )
    }
        // comment
    ```

=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    //comment
    var debugging = false// comment
    var debugging = false //comment
    var debugging = false//comment
    fun main() {
        System.out.println(//123
            "test"
        )
    }
        //comment
    ```

Rule id: `comment-spacing` (`standard` rule set)

### Curly spacing

Consistent spacing around curly braces.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo = if (true) { 0 } else { 1 }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo = if (true){0}else{1}
    ```

Rule id: `curly-spacing` (`standard` rule set)

### Dot spacing

Consistent spacing around dots.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun String.foo() = "foo"
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun String . foo() = "foo"
    ```

Rule id: `dot-spacing` (`standard` rule set)

### Double colon spacing

No spaces around `::`.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo = Foo::class
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 = Foo ::class
    val foo2 = Foo:: class
    val foo3 = Foo :: class
    val foo4 = Foo::
        class
    ```

Rule id: `double-colon-spacing` (`standard` rule set)

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

Rule id: `function-return-type-spacing` (`standard` rule set)

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

Rule id: `function-start-of-body-spacing` (`standard` rule set)

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

Rule id: `function-type-reference-spacing` (`standard` rule set)

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

Rule id: `fun-keyword-spacing` (`standard` rule set)

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

Rule id: `kdoc-wrapping` (`standard` rule set)

### Keyword spacing

Consistent spacing around keywords.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun main() {
        if (true) {}
    }
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun main() {
        if(true){}
    }
    ```

Rule id: `keyword-spacing` (`standard` rule set)

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

Rule id: `modifier-list-spacing` (`standard` rule set)

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

Rule id: `nullable-type-spacing` (`standard` rule set)

### Operator spacing

Consistent spacing around operators.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = 1 + 2
    val foo2 = 1 - 2
    val foo3 = 1 * 2
    val foo4 = 1 / 2
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 = 1+2
    val foo2 = 1- 2
    val foo3 = 1 *2
    val foo4 = 1  /  2
    ```

Rule id: `op-spacing` (`standard` rule set)

### Parenthesis spacing

Consistent spacing around parenthesis.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    class Foo : Bar {
        constructor(string: String) : super()
    }
    val foo1 = ((1 + 2) / 3)
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    class Foo : Bar {
        constructor(string: String) : super ()
    }
    val foo1 = ( (1 + 2 ) / 3)
    ```

Rule id: `paren-spacing` (`standard` rule set)

### Range spacing

Consistent spacing around range operators.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    val foo1 = (1..12 step 2).last
    val foo2 = (1..12 step 2).last
    val foo3 = (1..12 step 2).last
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    val foo1 = (1.. 12 step 2).last
    val foo2 = (1 .. 12 step 2).last
    val foo3 = (1 ..12 step 2).last
    ```

Rule id: `range-spacing` (`standard` rule set)

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

Rule id: `spacing-between-function-name-and-opening-parenthesis` (`standard` rule set)

### Unary operator spacing

No spaces around unary operators.

=== "[:material-heart:](#) Ktlint"

    ```kotlin
    fun foo1(i: Int) = i++
    fun foo2(i: Int) = ++i
    fun foo3(i: Int) = ++i
    ```
=== "[:material-heart-off-outline:](#) Disallowed"

    ```kotlin
    fun foo1(i: Int) = i ++
    fun foo2(i: Int) = ++ i
    fun foo3(i: Int) = ++
        i
    ```

Rule id: `unary-op-spacing` (`standard` rule set)
