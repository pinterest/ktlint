<h1 align="center">
<a href="https://ktlint.github.io/">
  <img src="https://cloud.githubusercontent.com/assets/370176/26518284/38b680da-4262-11e7-8d27-2b9e849fb55f.png"/>
</a>
</h1>

<p align="center">
<a href="https://travis-ci.org/pinterest/ktlint"><img src="https://travis-ci.org/pinterest/ktlint.svg?branch=master" alt="Build Status"></a>
<a href="https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.pinterest%22%20AND%20a%3A%22ktlint%22"><img src="https://img.shields.io/maven-central/v/com.pinterest/ktlint.svg" alt="Maven Central"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="ktlint"></a>
</p>

<p align="center">
<a href="https://kotlinlang.org/">Kotlin</a> linter in spirit of <a href="https://github.com/feross/standard">feross/standard</a> (JavaScript) and <a href="https://golang.org/cmd/gofmt/">gofmt</a> (Go).  
</p>

## Features
- **No configuration.**[*](https://github.com/pinterest/ktlint#how-do-i-globally-disable-a-rule) Which means no decisions to make, nothing to argue about and no special files to manage.   
While this might sound extreme, keep in mind that `ktlint` tries to capture (reflect) **official code style**[*](https://github.com/pinterest/ktlint/issues/284#issuecomment-425177186) from [kotlinlang.org](https://kotlinlang.org/docs/reference/coding-conventions.html) and [Android Kotlin Style Guide](https://android.github.io/kotlin-guides/style.html)
(+ [we respect your .editorconfig](#editorconfig) and support additional [ruleset](#creating-a-ruleset)|s).
- **Built-in formatter.** So that you wouldn't have to fix all style violations by hand.
- **Customizable output.** `plain` (+ `plain?group_by_file`), `json` and `checkstyle` reporters are available out-of-the-box. 
It's also [easy to create your own](#creating-a-reporter).
- **A single executable jar with all dependencies included.**

<p align="center">
<a href="#installation">Installation</a> | <a href="#usage">Usage</a> | <a href="#integration">Integration</a> with <a href="#-with-maven">Maven</a> / <a href="#-with-gradle">Gradle</a> / <a href="#-with-intellij-idea">IntelliJ IDEA</a> / <a href="#-with-emacs">Emacs</a> | Creating <a href="#creating-a-ruleset">a ruleset</a> | <a href="#creating-a-reporter">a reporter</a> | <a href="#badge">Badge</a> | <a href="#faq">FAQ</a>
</p>

## Standard rules

- Indentation formatting - respects `.editorconfig` `indent_size` with no continuation indent (see [EditorConfig](#editorconfig) section for more)
- No semicolons (unless used to separate multiple statements on the same line)
- No unused `import`s
- No consecutive blank lines
- No blank lines before `}`
- No trailing whitespaces
- No `Unit` returns (`fun fn {}` instead of `fun fn: Unit {}`)
- No empty (`{}`) class bodies
- No spaces around range (`..`) operator
- No newline before (binary) `+` & `-`, `*`, `/`, `%`, `&&`, `||` 
- No wildcard `import`s
- When wrapping chained calls `.`, `?.` and `?:` should be placed on the next line
- When a line is broken at an assignment (`=`) operator the break comes after the symbol
- When class/function signature doesn't fit on a single line, each parameter must be on a separate line
- Consistent string templates (`$v` instead of `${v}`, `${p.v}` instead of `${p.v.toString()}`)
- Consistent order of modifiers
- Consistent spacing after keywords, commas; around colons, curly braces, parens, infix operators, comments, etc
- Newline at the end of each file (enabled by default)
(set `insert_final_newline=false` in .editorconfig to disable (see [EditorConfig](#editorconfig) section for more)).
- Imports ordered consistently (see [Custom ktlint EditorConfig properties](#custom-ktlint-specific-editorconfig-properties) for more)

## Experimental rules
New rules will be added into the [experimental ruleset](https://github.com/pinterest/ktlint/tree/master/ktlint-ruleset-experimental), which can be enabled
by passing the `--experimental` flag to `ktlint`.

- Annotation formatting - multiple annotations should be on a separate line than the annotated declaration; annotations with parameters should each be on separate lines; annotations should be followed by a space; declarations with annotations should be separated by a blank line
- Comment space formatting - declarations with comments should be separated by a blank line
- No underscores in package names
- Braces required for multiline if/else statements
- Enum entry names should be uppercase underscore-separated names
- No spaces around `::`
- No spaces around angle brackets


## EditorConfig

ktlint recognizes the following [.editorconfig](http://editorconfig.org/) properties (provided they are specified under `[*.{kt,kts}]`):  
(values shown below are the defaults and do not need to be specified explicitly)
```ini
[*.{kt,kts}]
# possible values: number (e.g. 2), "unset" (makes ktlint ignore indentation completely)  
indent_size=4
# true (recommended) / false
insert_final_newline=true
# possible values: number (e.g. 120) (package name, imports & comments are ignored), "off"
# it's automatically set to 100 on `ktlint --android ...` (per Android Kotlin Style Guide)
max_line_length=off
```

### Custom Ktlint specific EditorConfig properties

```ini
# Comma-separated list of rules to disable (Since 0.34.0)
# Note that rules in any ruleset other than the standard ruleset will need to be prefixed 
# by the ruleset identifier.
disabled_rules=no-wildcard-imports,experimental:annotation,my-custom-ruleset:my-custom-rule

# Defines the imports layout. There are predefined layouts like "ascii" or "idea", as well as a custom layout.
# The predefined layouts are temporary and will be deprecated in the future, once Kotlin plugin supports EditorConfig property for imports layout.
# The custom layout can be composed by the following symbols:
# "*" - wildcard. There must be at least one entry of a single wildcard to match all other imports. Matches anything after a specified symbol/import as well.
# "|" - blank line. Supports only single blank lines between imports. No blank line is allowed in the beginning or end of the layout.
# "^" - alias import, e.g. "^android.*" will match all android alias imports, "^*" will match all other alias imports.
# import paths - these can be full paths, e.g. "java.util.List" as well as wildcard paths, e.g. "kotlin.*"
# Examples:
kotlin_imports_layout=ascii # alphabetical with capital letters before lower case letters (e.g. Z before a), no blank lines
kotlin_imports_layout=idea # default IntelliJ IDEA style, same as "ascii", but with "java", "javax", "kotlin" and alias imports in the end of the imports list
kotlin_imports_layout=android.*,|,^org.junit.*,kotlin.io.Closeable,|,*,^* # custom imports layout
# Alternatively ij_kotlin_imports_layout name can be used, in order to set an imports layout for both ktlint and IDEA via a single property
# Note: this is not yet implemented on IDEA side, so it only takes effect for ktlint
ij_kotlin_imports_layout=*
```

### Overriding Editorconfig properties for specific directories

You could [override](https://editorconfig.org/#file-format-details) properties for specific directories inside your project:
```ini
[*.{kt,kts}]
disabled_rules=import-ordering

# Note that in this case 'import-ordering' rule will be active and 'indent' will be disabled
[api/*.{kt,kts}]
disabled_rules=indent
```

## Installation

> Skip all the way to the "Integration" section if you don't plan to use `ktlint`'s command line interface.

```sh
curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.38.1/ktlint &&
  chmod a+x ktlint &&
  sudo mv ktlint /usr/local/bin/
```

... or just download `ktlint` from the [releases](https://github.com/pinterest/ktlint/releases) page  (`ktlint.asc` contains PGP signature which you can verify with `curl -sS https://keybase.io/pinterestandroid/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc`).  

On macOS ([or Linux](http://linuxbrew.sh/)) you can also use [brew](https://brew.sh/) - `brew install ktlint`.

> If you don't have curl installed - replace `curl -sL` with `wget -qO-`.

> If you are behind a proxy see -
[curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / 
[wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. 
Usually simple `http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...` is enough. 

## Usage

```bash
# check the style of all Kotlin files inside the current dir (recursively)
# (hidden folders will be skipped)
$ ktlint --color [--color-name="RED"]
  src/main/kotlin/Main.kt:10:10: Unused import
  
# check only certain locations (prepend ! to negate the pattern,
# Ktlint uses .gitignore pattern style syntax)
$ ktlint "src/**/*.kt" "!src/**/*Test.kt"

# auto-correct style violations
# (if some errors cannot be fixed automatically they will be printed to stderr) 
$ ktlint -F "src/**/*.kt"

# print style violations grouped by file
$ ktlint --reporter=plain?group_by_file
# print style violations as usual + create report in checkstyle format 
$ ktlint --reporter=plain --reporter=checkstyle,output=ktlint-report-in-checkstyle-format.xml

# install git hook to automatically check files for style violations on commit
# Run "ktlint installGitPrePushHook" if you wish to run ktlint on push instead
$ ktlint installGitPreCommitHook
```

> on Windows you'll have to use `java -jar ktlint ...`. 

`ktlint --help` for more.

### Integration 

#### ... with [Maven](https://github.com/shyiko/mvnw)

> pom.xml

```xml
...
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>1.8</version>
    <executions>
        <execution>
            <id>ktlint</id>
            <phase>verify</phase>
            <configuration>
            <target name="ktlint">
                <java taskname="ktlint" dir="${basedir}" fork="true" failonerror="true"
                    classpathref="maven.plugin.classpath" classname="com.pinterest.ktlint.Main">
                    <arg value="src/**/*.kt"/>
                    <!-- to generate report in checkstyle format prepend following args: -->
                    <!-- 
                    <arg value="--reporter=plain"/>
                    <arg value="--reporter=checkstyle,output=${project.build.directory}/ktlint.xml"/>
                    -->
                    <!-- see https://github.com/pinterest/ktlint#usage for more -->                    
                </java>
            </target>
            </configuration>
            <goals><goal>run</goal></goals>
        </execution>
        <execution>
            <id>ktlint-format</id>
            <configuration>
            <target name="ktlint">
                <java taskname="ktlint" dir="${basedir}" fork="true" failonerror="true"
                    classpathref="maven.plugin.classpath" classname="com.pinterest.ktlint.Main">
                    <arg value="-F"/>
                    <arg value="src/**/*.kt"/>
                </java>
            </target>
            </configuration>
            <goals><goal>run</goal></goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.pinterest</groupId>
            <artifactId>ktlint</artifactId>
            <version>0.38.1</version>
        </dependency>
        <!-- additional 3rd party ruleset(s) can be specified here -->
    </dependencies>
</plugin>
...
```

> If you want ktlint to run before code compilation takes place - change `<phase>verify</phase>` to `<phase>validate</phase>` (see [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) for more).

To check code style - `mvn antrun:run@ktlint` (it's also bound to `mvn verify`).  
To run formatter - `mvn antrun:run@ktlint-format`.   

**Another option** is to use a dedicated Maven plugin - [gantsign/ktlint-maven-plugin](https://github.com/gantsign/ktlint-maven-plugin). 

#### ... with [Gradle](https://gradle.org/)

#### (with a plugin - Recommended)

Gradle plugins (in order of appearance):
- [jlleitschuh/ktlint-gradle](https://github.com/jlleitschuh/ktlint-gradle)  
Gradle plugin that automatically creates check and format tasks for project Kotlin sources,
supports different kotlin plugins and Gradle build caching.

- [jeremymailen/kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle)  
Gradle plugin featuring incremental build support, file reports, and `*.kts` source support.

You might also want to take a look at [diffplug/spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-ktlint-to-kotlin-files) or [autostyle/autostyle](https://github.com/autostyle/autostyle/tree/master/plugin-gradle#applying-ktlint-to-kotlin-files) that have a built-in support for ktlint. In addition to linting/formatting kotlin code it allows you to keep license headers, markdown documentation, etc. in check.

#### (without a plugin)

> build.gradle

```groovy
// kotlin-gradle-plugin must be applied for configuration below to work
// (see https://kotlinlang.org/docs/reference/using-gradle.html)

apply plugin: 'java'

repositories {
    jcenter()
}

configurations {
    ktlint
}

dependencies {
    ktlint "com.pinterest:ktlint:0.38.1"
    // additional 3rd party ruleset(s) can be specified here
    // just add them to the classpath (e.g. ktlint 'groupId:artifactId:version') and 
    // ktlint will pick them up
}

task ktlint(type: JavaExec, group: "verification") {
    description = "Check Kotlin code style."
    classpath = configurations.ktlint
    main = "com.pinterest.ktlint.Main"
    args "src/**/*.kt"
    // to generate report in checkstyle format prepend following args:
    // "--reporter=plain", "--reporter=checkstyle,output=${buildDir}/ktlint.xml"
    // see https://github.com/pinterest/ktlint#usage for more
}
check.dependsOn ktlint

task ktlintFormat(type: JavaExec, group: "formatting") {
    description = "Fix Kotlin code style deviations."
    classpath = configurations.ktlint
    main = "com.pinterest.ktlint.Main"
    args "-F", "src/**/*.kt"
}
```

To check code style - `gradle ktlint` (it's also bound to `gradle check`).  
To run formatter - `gradle ktlintFormat`.

See [Making your Gradle tasks incremental](https://proandroiddev.com/making-your-gradle-tasks-incremental-7f26e4ef09c3) by [Niklas Baudy](https://github.com/vanniktech) on how to make tasks above incremental. 


#### (without a plugin) for Gradle Kotlin DSL (build.gradle.kts)

> build.gradle.kts

```kotlin
val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest:ktlint:0.38.1")
    // ktlint(project(":custom-ktlint-ruleset")) // in case of custom ruleset
}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlintCheck by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F", "src/**/*.kt")
}
```

#### ... with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

> While this is not strictly necessary it makes Intellij IDEA's built-in formatter produce 100% ktlint-compatible 
 code. 

##### Option #1 (recommended)

> (inside project's root directory)  

```sh
ktlint applyToIDEAProject
# or if you want to be compliant with Android Kotlin Style Guide
ktlint --android applyToIDEAProject
```

##### Option #2

Apply to all IDEA projects:
```sh
ktlint applyToIDEA
```
Or if you want to use android specific code style:
```sh
ktlint --android applyToIDEA
```

##### Option #3

Go to <kbd>File</kbd> -> <kbd>Settings...</kbd> -> <kbd>Editor</kbd>
- <kbd>General</kbd> -> <kbd>Auto Import</kbd>
  - check `Optimize imports on the fly (for current project)`.
- <kbd>Code Style</kbd> -> <kbd>Kotlin</kbd>
  - <kbd>Set from...</kbd> -> <kbd>Predefined style</kbd> -> <kbd>Kotlin style guide</kbd> (Kotlin plugin 1.2.20+).
  - open <kbd>Code Generation</kbd> tab
    - uncheck `Line comment at first column`;
    - select `Add a space at comment start`.
  - open <kbd>Imports</kbd> tab
    - select `Use single name import` (all of them);
    - remove `import java.util.*` from `Packages to Use Import with '*'`.
  - open <kbd>Blank Lines</kbd> tab
    - change `Keep Maximum Blank Lines` / `In declarations` & `In code` to 1 and `Before '}'` to 0.
  - (optional but recommended) open <kbd>Wrapping and Braces</kbd> tab
    - uncheck `Method declaration parameters` / `Align when multiline`.     
  - (optional but recommended) open <kbd>Tabs and Indents</kbd> tab
    - change `Continuation indent` to the same value as `Indent` (4 by default).   
- <kbd>Inspections</kbd> 
  - change `Severity` level of `Unused import directive` and `Redundant semicolon` to `ERROR`.

#### ... with [GNU Emacs](https://www.gnu.org/software/emacs/)

See [whirm/flycheck-kotlin](https://github.com/whirm/flycheck-kotlin).

#### ... with [Vim](https://www.vim.org/)

See [w0rp/ale](https://github.com/w0rp/ale).

> Integrated with something else? Send a PR.

## Creating a ruleset

> See also [Writing your first ktlint rule](https://medium.com/@vanniktech/writing-your-first-ktlint-rule-5a1707f4ca5b) by [Niklas Baudy](https://github.com/vanniktech). 

In a nutshell: "ruleset" is a JAR containing one or more [Rule](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/Rule.kt)s gathered together in a [RuleSet](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/RuleSet.kt). `ktlint` is relying on 
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to discover all available "RuleSet"s
on the classpath (as a ruleset author, all you need to do is to include a `META-INF/services/com.pinterest.ktlint.core.RuleSetProvider` file 
containing a fully qualified name of your [RuleSetProvider](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/RuleSetProvider.kt) implementation).    

Once packaged in a JAR <sup>[e.g. via `./gradlew build`](https://github.com/pinterest/ktlint/issues/300#issuecomment-432408753)</sup> you can load it with

```sh
# enable additional 3rd party ruleset by pointing ktlint to its location on the file system
$ ktlint -R /path/to/custom/rulseset.jar "src/test/**/*.kt"
```

Loading custom (3rd party) ruleset via built-in maven dependency resolver is deprecated,
see https://github.com/pinterest/ktlint/issues/451.

A complete sample project (with tests and build files) is included in this repo under the [ktlint-ruleset-template](ktlint-ruleset-template) directory 
(make sure to check [NoVarRuleTest](ktlint-ruleset-template/src/test/kotlin/yourpkgname/NoVarRuleTest.kt) as it contains some useful information). 

#### AST

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

## Creating a reporter

Take a look at [ktlint-reporter-plain](ktlint-reporter-plain). 

In short, all you need to do is to implement a 
[Reporter](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/Reporter.kt) and make it available by registering 
a custom [ReporterProvider](ktlint-core/src/main/kotlin/com/pinterest/ktlint/core/ReporterProvider.kt) using
`META-INF/services/com.pinterest.ktlint.core.ReporterProvider`. Pack all of that into a JAR and you're done.

To load a custom (3rd party) reporter use `ktlint --reporter=name,artifact=/path/to/custom-ktlint-reporter.jar`
(see `ktlint --help` for more).

Loading custom (3rd party) reporter via built-in maven dependency resolver is deprecated,
see https://github.com/pinterest/ktlint/issues/451.

Third-party:
* [kryanod/ktlint-junit-reporter](https://github.com/kryanod/ktlint-junit-reporter)

## Badge

[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)

```md
[![ktlint](https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg)](https://ktlint.github.io/)
```

## FAQ

### Why should I use ktlint?

**Simplicity**.

Spending time on configuration (& maintenance down the road) of hundred-line long style config file(s) is counter-productive. Instead of wasting your energy on something that has no business value - focus on what really matters (not debating whether to use tabs or spaces).

By using ktlint you put the importance of code clarity and community conventions over personal preferences. This makes things easier for people reading your code as well as frees you from having to document & explain what style potential contributor(s) have to follow.

ktlint is a single binary with both linter & formatter included. All you need is to drop it in (no need to get [overwhelmed](https://en.wikipedia.org/wiki/Decision_fatigue) while choosing among [dozens of code style options](http://checkstyle.sourceforge.net/checks.html)).

### Can I have my own rules on top of ktlint?

Absolutely, "no configuration" doesn't mean "no extensibility". You can add your own ruleset(s) to discover potential bugs, check for anti-patterns, etc.

See [Creating A Ruleset](#creating-a-ruleset).

### How do I suppress an error for a line/block/file?

> This is meant primarily as an escape latch for the rare cases when **ktlint** is not able
to produce the correct result (please report any such instances using [GitHub Issues](https://github.com/pinterest/ktlint/issues)).

To disable a specific rule you'll need to turn on the verbose mode (`ktlint --verbose ...`). At the end of each line
you'll see an error code. Use it as an argument for `ktlint-disable` directive (shown below).  

```kotlin
import package.* // ktlint-disable no-wildcard-imports

/* ktlint-disable no-wildcard-imports */
import package.a.*
import package.b.*
/* ktlint-enable no-wildcard-imports */
```

To disable all checks:

```kotlin
import package.* // ktlint-disable
```

### How do I globally disable a rule?
See the [EditorConfig section](https://github.com/pinterest/ktlint#editorconfig) for details on how to use the `disabled_rules` property.

You may also pass a list of disabled rules via the `--disabled_rules` command line flag. It has the same syntax as the EditorConfig property.

## Development

> Make sure to read [CONTRIBUTING.md](CONTRIBUTING.md).

```sh
git clone https://github.com/pinterest/ktlint && cd ktlint
./mvnw # shows how to build, test, run, etc. project
```

> To open ktlint in Intellij IDEA:  
<kbd>File</kbd> -> <kbd>Open...</kbd> (you may need to right-click on `pom.xml` (in the project dir) and then <kbd>Maven</kbd> -> <kbd>Reimport</kbd>).  
You'll also need to set "Project SDK" to [1.8](https://github.com/shyiko/jabba#usage), "Project language level" to 8 in "Project Settings" (<kbd>File</kbd> -> <kbd>Project Structure...</kbd>).  
To run `ktlint` - right-click on `ktlint/src/main/kotlin/com/pinterest/ktlint/Main.kt` -> <kbd>Run</kbd>.       

#### Access to the latest `master` snapshot

Whenever a commit is added to the `master` branch a snapshot build is automatically uploaded to [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/com/pinterest/ktlint/).
If you are eager to try upcoming changes (that might or might not be included in the next stable release) you can do 
so by changing version of ktlint to `<latest-version>-SNAPSHOT` + adding a repo: 

##### Maven

```xml
...
<repository>
    <id>sonatype-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
    <releases>
        <enabled>false</enabled>
    </releases>
</repository>
...
```

##### Gradle

```groovy
repositories {
  maven {
    url "https://oss.sonatype.org/content/repositories/snapshots"
  }
}
```

##### Kotlin development version snapshot

Additionally, project publishes snapshots build against latest kotlin development version. To use them, change version
of ktlint to `<latest-version>-kotlin-dev-SNAPSHOT`.

## Legal

This project is not affiliated with nor endorsed by JetBrains.  
All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2019 Pinterest, Inc.  
Copyright (c) 2016-2019 Stanley Shyiko.
