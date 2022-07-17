!!! Tip
    See [Writing your first ktlint rule](https://medium.com/@vanniktech/writing-your-first-ktlint-rule-5a1707f4ca5b) by [Niklas Baudy](https://github.com/vanniktech).

In a nutshell: a "rule set" is a JAR containing one or more [Rule](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/Rule.kt)s gathered together in a [RuleSet](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/RuleSet.kt). `ktlint` is relying on the
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to discover all available "RuleSet"s
on the classpath (as a ruleset author, all you need to do is to include a `META-INF/services/com.pinterest.ktlint.core.RuleSetProvider` file
containing a fully qualified name of your [RuleSetProvider](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/RuleSetProvider.kt) implementation).

## ktlint-ruleset-template

A complete sample project (with tests and build files) is included in this repo under the [ktlint-ruleset-template](ktlint-ruleset-template) directory (make sure to check [NoVarRuleTest](ktlint-ruleset-template/src/test/kotlin/yourpkgname/NoVarRuleTest.kt) as it contains some useful information).

```shell title="Building the ktlint-ruleset-template"
$ cd ktlint-ruleset-template/
$ ../gradlew build
```

```shell title="Provide code sample that violates rule `custom:no-var"
$ echo 'var v = 0' > test.kt
```

```shell title="Running the ktlint-ruleset-template" hl_lines="1 40 43"
$ ktlint -R build/libs/ktlint-ruleset-template.jar --debug --relative test.kt

18:13:21.026 [main] DEBUG com.pinterest.ktlint.internal.RuleSetsLoader - JAR ruleset provided with path "/../ktlint/ktlint-ruleset-template/build/libs/ktlint-ruleset-template.jar"
18:13:21.241 [main] DEBUG com.pinterest.ktlint.Main - Discovered reporter with "baseline" id.
18:13:21.241 [main] DEBUG com.pinterest.ktlint.Main - Discovered reporter with "checkstyle" id.
18:13:21.241 [main] DEBUG com.pinterest.ktlint.Main - Discovered reporter with "json" id.
18:13:21.242 [main] DEBUG com.pinterest.ktlint.Main - Discovered reporter with "html" id.
18:13:21.242 [main] DEBUG com.pinterest.ktlint.Main - Discovered reporter with "plain" id.
18:13:21.242 [main] DEBUG com.pinterest.ktlint.Main - Discovered reporter with "sarif" id.
18:13:21.242 [main] DEBUG com.pinterest.ktlint.Main - Initializing "plain" reporter with {verbose=false, color=false, color_name=DARK_GRAY}
[DEBUG] Rule with id 'standard:max-line-length' should run after the rule with id 'experimental:trailing-comma'. However, the latter rule is not loaded and is allowed to be ignored. For best results, it is advised load the rule.
[DEBUG] Rules will be executed in order below (unless disabled):
           - standard:filename, 
           - standard:final-newline, 
           - standard:chain-wrapping, 
           - standard:colon-spacing, 
           - standard:comma-spacing, 
           - standard:comment-spacing, 
           - standard:curly-spacing, 
           - standard:dot-spacing, 
           - standard:import-ordering, 
           - standard:keyword-spacing, 
           - standard:modifier-order, 
           - standard:no-blank-line-before-rbrace, 
           - standard:no-consecutive-blank-lines, 
           - standard:no-empty-class-body, 
           - standard:no-line-break-after-else, 
           - standard:no-line-break-before-assignment, 
           - standard:no-multi-spaces, 
           - standard:no-semi, 
           - standard:no-trailing-spaces, 
           - standard:no-unit-return, 
           - standard:no-unused-imports, 
           - standard:no-wildcard-imports, 
           - standard:op-spacing, 
           - standard:parameter-list-wrapping, 
           - standard:paren-spacing, 
           - standard:range-spacing, 
           - standard:string-template, 
           - custom:no-var, 
           - standard:indent, 
           - standard:max-line-length
`text test.kt:1:1: Unexpected var, use val instead (cannot be auto-corrected)`
18:13:21.893 [main] DEBUG com.pinterest.ktlint.Main - 872ms / 1 file(s) / 1 error(s)
```

!!! tip
    Multiple custom rule sets can be loaded at the same time.

## AST

While writing/debugging [Rule](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/Rule.kt)s it's often helpful to have an AST
printed out to see the structure rules have to work with. ktlint >= 0.15.0 has a `printAST` subcommand (or `--print-ast` flag for ktlint < 0.34.0) specifically for this purpose
(usage: `ktlint --color printAST <file>`).
An example of the output is shown below.

```sh
$ printf "fun main() {}" | ktlint --color printAST --stdin

1: ~.psi.KtFile (~.psi.stubs.elements.KtFileElementType.kotlin.FILE)
1:   ~.psi.KtPackageDirective (~.psi.stubs.elements.KtPlaceHolderStubElementType.PACKAGE_DIRECTIVE) ""
1:   ~.psi.KtImportList (~.psi.stubs.elements.KtPlaceHolderStubElementType.IMPORT_LIST) ""
1:   ~.psi.KtScript (~.psi.stubs.elements.KtScriptElementType.SCRIPT)
1:     ~.psi.KtBlockExpression (~.KtNodeType.BLOCK)
1:       ~.psi.KtNamedFunction (~.psi.stubs.elements.KtFunctionElementType.FUN)
1:         ~.c.i.p.impl.source.tree.LeafPsiElement (~.lexer.KtKeywordToken.fun) "fun"
1:         ~.c.i.p.impl.source.tree.PsiWhiteSpaceImpl (~.c.i.p.tree.IElementType.WHITE_SPACE) " "
1:         ~.c.i.p.impl.source.tree.LeafPsiElement (~.lexer.KtToken.IDENTIFIER) "main"
1:         ~.psi.KtParameterList 
  (~.psi.stubs.elements.KtPlaceHolderStubElementType.VALUE_PARAMETER_LIST)
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (~.lexer.KtSingleValueToken.LPAR) "("
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (~.lexer.KtSingleValueToken.RPAR) ")"
1:         ~.c.i.p.impl.source.tree.PsiWhiteSpaceImpl (~.c.i.p.tree.IElementType.WHITE_SPACE) " "
1:         ~.psi.KtBlockExpression (~.KtNodeType.BLOCK)
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (~.lexer.KtSingleValueToken.LBRACE) "{"
1:           ~.c.i.p.impl.source.tree.LeafPsiElement (~.lexer.KtSingleValueToken.RBRACE) "}"

   format: <line_number:> <node.psi::class> (<node.elementType>) "<node.text>"
   legend: ~ = org.jetbrains.kotlin, c.i.p = com.intellij.psi
```
