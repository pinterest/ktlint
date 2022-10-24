## Annotation formatting

Multiple annotations should be on a separate line than the annotated declaration; annotations with parameters should each be on separate lines; annotations should be followed by a space

Rule-id: `annotation`

## Argument list wrapping

Rule-id: `argument-list-wrapping`

## Chain wrapping

When wrapping chained calls `.`, `?.` and `?:` should be placed on the next line

Rule id: `chain-wrapping`

## Enum entry

Enum entry names should be uppercase underscore-separated names.

Rule id: `enum-entry-name-case`

## File name

Files containing only one toplevel domain should be named according to that element.

Rule id: `filename`

## Final newline

Ensures consistent usage of a newline at the end of each file. 

This rule can be configured with `.editorconfig` property [`insert_final_newline`](../configuration/#final-newline).

Rule id: `final-newline`

## Import ordering

Imports ordered consistently (see [Custom ktlint EditorConfig properties](#custom-ktlint-specific-editorconfig-properties) for more)

Rule id: `import-ordering`

## Indentation

Indentation formatting - respects `.editorconfig` `indent_size` with no continuation indent (see [EditorConfig](#editorconfig) section for more).

Rule id: `indent`

## Max line length

Ensures that lines do not exceed the given length of `.editorconfig` property `max_line_length` (see [EditorConfig](#editorconfig) section for more). This rule does not apply in a number of situations. For example, in the case a line exceeds the maximum line length due to and comment that disables ktlint rules than that comment is being ignored when validating the length of the line. The `.editorconfig` property `ktlint_ignore_back_ticked_identifier` can be set to ignore identifiers which are enclosed in backticks, which for example is very useful when you want to allow longer names for unit tests.

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

Rule id: `no-wildcard-imports`

## No underscores in package names

Rule id: `package-name`

## Parameter list wrapping

When class/function signature doesn't fit on a single line, each parameter must be on a separate line

Rule id: `parameter-list-wrapping`

## String template

Consistent string templates (`$v` instead of `${v}`, `${p.v}` instead of `${p.v.toString()}`)

Rule id: `string-template`

## Trailing comma on call site

Consistent removal (default) or adding of trailing comma's on call site.

Rule id: `trailing-comma-on-call-site`

## Trailing comma on declaration site

Consistent removal (default) or adding of trailing comma's on declaration site.

Rule id: `trailing-comma-on-declaration-site`

## Wrapping

Inserts missing newlines (for example between parentheses of a multi-line function call).

Rule id: `wrapping`

## Spacing

### Annotation spacing

Annotations should be separated by a single line break.

Rule id: `annotation-spacing`

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

### Keyword spacing

Consistent spacing around keywords.

Rule id: `keyword-spacing`

### Operator spacing

Consistent spacing around operators.

Rule id: `op-spacing`

### Parenthesis spacing

Consistent spacing around parenthesis.

Rule id: `paren-spacing`

### Range spacing

Consistent spacing around range operators.

Rule id: `range-spacing`

### Angle bracket spacing

No spaces around angle brackets when used for typing.

Rule id: `spacing-around-angle-brackets`

### Blank line between declarations with annotations

Declarations with annotations should be separated by a blank line.

Rule id: `spacing-between-declarations-with-annotations`

### Blank line between declaration with comments

Declarations with comments should be separated by a blank line.

Rule id: `spacing-between-declarations-with-comments`

### Unary operator spacing

No spaces around unary operators.

Rule id: `unary-op-spacing`
