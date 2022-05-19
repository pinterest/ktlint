<h1 align="center">
<a href="https://ktlint.github.io/">
  <img src="https://cloud.githubusercontent.com/assets/370176/26518284/38b680da-4262-11e7-8d27-2b9e849fb55f.png"/>
</a>
</h1>

<p align="center">
<a href="https://kotlinlang.slack.com/messages/CKS3XG0LS"><img src="https://img.shields.io/badge/slack-@kotlinlang/ktlint-yellow.svg?logo=slack" alt="Join the chat at https://kotlinlang.slack.com"/></a>
<a href="https://github.com/pinterest/ktlint/actions?query=workflow%3A%22Snapshot+Publish%22"><img src="https://github.com/pinterest/ktlint/workflows/Snapshot%20Publish/badge.svg" alt="Build status"></a>
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
- **Customizable output.** `plain` (+ `plain?group_by_file`), `json`, `html` and `checkstyle` reporters are available out-of-the-box. 
It's also [easy to create your own](#creating-a-reporter).
- **A single executable jar with all dependencies included.**

<p align="center">
<a href="#installation">Installation</a> | <a href="#usage">Usage</a> | <a href="#integration">Integration</a> with <a href="#-with-maven">Maven</a> / <a href="#-with-gradle">Gradle</a> / <a href="#-with-intellij-idea">IntelliJ IDEA</a> / <a href="#-with-emacs">Emacs</a> / <a href="#-with-continuous-integration">Continuous Integration</a> | Creating <a href="#creating-a-ruleset">a ruleset</a> | <a href="#creating-a-reporter">a reporter</a> | <a href="#badge">Badge</a> | <a href="#faq">FAQ</a>
</p>

## Standard rules

- `chain-wrapping`: When wrapping chained calls `.`, `?.` and `?:` should be placed on the next line
- `comment-spacing`: The end of line comment sign `//` should be preceded and followed by exactly a space
- `filename`: Files containing only one toplevel domain should be named according to that element.
- `final-newline`: Newline at the end of each file (enabled by default)
  (set `insert_final_newline=false` in .editorconfig to disable (see [EditorConfig](#editorconfig) section for more)).
- `import-ordering`: Imports ordered consistently (see [Custom ktlint EditorConfig properties](#custom-ktlint-specific-editorconfig-properties) for more)
- `indent`: Indentation formatting - respects `.editorconfig` `indent_size` with no continuation indent (see [EditorConfig](#editorconfig) section for more)
- `max-line-length`: Ensures that lines do not exceed the given length of `.editorconfig` property `max_line_length` (see [EditorConfig](#editorconfig) section for more). This rule does not apply in a number of situations. For example, in the case a line exceeds the maximum line length due to and comment that disables ktlint rules than that comment is being ignored when validating the length of the line. The `.editorconfig` property `ktlint_ignore_back_ticked_identifier` can be set to ignore identifiers which are enclosed in backticks, which for example is very useful when you want to allow longer names for unit tests.  
- `modifier-order`: Consistent order of modifiers
- `no-blank-line-before-rbrace`: No blank lines before `}` 
- `no-consecutive-blank-lines`: No consecutive blank lines
- `no-empty-class-body`: No empty (`{}`) class bodies
- `no-line-break-after-else`: Disallows line breaks after the else keyword if that could lead to confusion, for example:
    ```kotlin
    if (conditionA()) {
        doSomething()
    } else
    if (conditionB()) {
        doAnotherThing()
    }
    ```
- `no-line-break-before-assignment`: When a line is broken at an assignment (`=`) operator the break comes after the symbol
- `no-multi-spaces`: Except in indentation and in KDoc's it is not allowed to have multiple consecutive spaces
- `no-semi`: No semicolons (unless used to separate multiple statements on the same line)
- `no-trailing-spaces`: No trailing whitespaces
- `no-unit-return`: No `Unit` returns (`fun fn {}` instead of `fun fn: Unit {}`)
- `no-unused-imports`: No unused `import`s
- `no-wildcard-imports`: No wildcard `import`s expect imports listed in `.editorconfig` property `ij_kotlin_packages_to_use_import_on_demand`
- `parameter-list-wrapping`: When class/function signature doesn't fit on a single line, each parameter must be on a separate line
- `string-template`: Consistent string templates (`$v` instead of `${v}`, `${p.v}` instead of `${p.v.toString()}`)

### Spacing
- `colon-spacing`: Consistent spacing around colon
- `comma-spacing`: Consistent spacing around comma
- `curly-spacing`: Consistent spacing around curly braces
- `dot-spacing`: Consistent spacing around dots
- `keyword-spacing`: Consistent spacing around keywords
- `op-spacing`: Consistent spacing around operators
- `paren-spacing`: Consistent spacing around parenthesis
- `range-spacing`: Consistent spacing around range operators

## Experimental rules
New rules will be added into the [experimental ruleset](https://github.com/pinterest/ktlint/tree/master/ktlint-ruleset-experimental), which can be enabled
by passing the `--experimental` flag to `ktlint`.

- `experimental:annotation`: Annotation formatting - multiple annotations should be on a separate line than the annotated declaration; annotations with parameters should each be on separate lines; annotations should be followed by a space
- `experimental:block-comment-initial-star-alignment`: Lines in a block comment which (exclusive the indentation) start with a `*` should have this `*` aligned with the `*` in the opening of the block comment.
- `experimental:discouraged-comment-location`: Detect discouraged comment locations (no autocorrect)
- `experimental:enum-entry-name-case`: Enum entry names should be uppercase underscore-separated names
- `experimental:multiline-if-else`: Braces required for multiline if/else statements
- `experimental:no-empty-first-line-in-method-block`: No leading empty lines in method blocks
- `experimental:package-name`: No underscores in package names
- `experimental:parameter-list-spacing`: Consistent spacing inside the parameter list
- `experimental:unnecessary-parentheses-before-trailing-lambda`: An empty parentheses block before a lambda is redundant. For example `some-string".count() { it == '-' }`
- `function-signature`: rewrites the function signature to a single line when possible (e.g. when not exceeding the `max_line_length` property) or a multiline signature otherwise. In case of function with a body expression, the body expression is placed on the same line as the function signature when not exceeding the `max_line_length` property. Optionally the function signature can be forced to be written as a multiline signature in case the function has more than a specified number of parameters (`.editorconfig' property `ktlint_function_signature_wrapping_rule_always_with_minimum_parameters`)

### Spacing
- `experimental:annotation-spacing`: Annotations should be separated by the annotated declaration by a single line break
- `experimental:double-colon-spacing`: No spaces around `::`
- `experimental:fun-keyword-spacing`: Consistent spacing after the fun keyword
- `experimental:function-return-type-spacing`: Consistent spacing around the function return type
- `experimental:function-start-of-body-spacing`: Consistent spacing before start of function body
- `experimental:function-type-reference-spacing`: Consistent spacing in the type reference before a function
- `experimental:modifier-list-spacing`: Consistent spacing between modifiers in and after the last modifier in a modifier list
- `experimental:nullable-type-spacing`: No spaces in a nullable type
- `experimental:spacing-around-angle-brackets`: No spaces around angle brackets
- `experimental:spacing-between-declarations-with-annotations`: Declarations with annotations should be separated by a blank line
- `experimental:spacing-between-declarations-with-comments`: Declarations with comments should be separated by a blank line
- `experimental:spacing-between-function-name-and-opening-parenthesis`: Consistent spacing between function name and opening parenthesis
- `experimental:type-parameter-list-spacing`: Spacing after a type parameter list in function and class declarations
- `experimental:unary-op-spacing`: No spaces around unary operators

### Wrapping
- `experimental:argument-list-wrapping`: Argument list wrapping
- `experimental:comment-wrapping`: A block comment should start and end on a line that does not contain any other element. A block comment should not be used as end of line comment.
- `experimental:kdoc-wrapping`: A KDoc comment should start and end on a line that does not contain any other element.

## EditorConfig

ktlint recognizes the following [.editorconfig](https://editorconfig.org/) properties (provided they are specified under `[*.{kt,kts}]`):  
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

### IntelliJ IDEA `.editorconfig` autoformat issue

Unfortunately [IntelliJ IDEA](https://www.jetbrains.com/idea/) has `.editorconfig` [autoformat issue](https://youtrack.jetbrains.com/issue/IDEA-242506) 
that adds additional space into glob statements.
For example, `[*{kt,kts}]` is formatted into `[*{kt, kts}]` ([original ktlint issue](https://github.com/pinterest/ktlint/issues/762)).
Such behaviour violates `.editorconfig` [specification](https://github.com/editorconfig/editorconfig/issues/148) and leads to ignoring this section when ktlint is parsing it.

### Custom Ktlint specific EditorConfig properties

```ini
# Comma-separated list of rules to disable (Since 0.34.0)
# Note that rules in any ruleset other than the standard ruleset will need to be prefixed 
# by the ruleset identifier.
disabled_rules=no-wildcard-imports,experimental:annotation,my-custom-ruleset:my-custom-rule

# Defines the imports layout. The layout can be composed by the following symbols:
# "*" - wildcard. There must be at least one entry of a single wildcard to match all other imports. Matches anything after a specified symbol/import as well.
# "|" - blank line. Supports only single blank lines between imports. No blank line is allowed in the beginning or end of the layout.
# "^" - alias import, e.g. "^android.*" will match all android alias imports, "^" will match all other alias imports.
# import paths - these can be full paths, e.g. "java.util.List.*" as well as wildcard paths, e.g. "kotlin.**"
# Examples (we use ij_kotlin_imports_layout to set an imports layout for both ktlint and IDEA via a single property):
ij_kotlin_imports_layout=* # alphabetical with capital letters before lower case letters (e.g. Z before a), no blank lines
ij_kotlin_imports_layout=*,java.**,javax.**,kotlin.**,^ # default IntelliJ IDEA style, same as alphabetical, but with "java", "javax", "kotlin" and alias imports in the end of the imports list
ij_kotlin_imports_layout=android.**,|,^org.junit.**,kotlin.io.Closeable.*,|,*,^ # custom imports layout

# According to https://kotlinlang.org/docs/reference/coding-conventions.html#names-for-test-methods it is acceptable to write method names
# in natural language. When using natural language, the description tends to be longer. Allow lines containing an identifier between
# backticks to be longer than the maximum line length. (Since 0.41.0)
[**/test/**.kt]
ktlint_ignore_back_ticked_identifier=true

# Comma-separated list of allowed wildcard imports that will override the no-wildcard-imports rule.
# This can be used for allowing wildcard imports from libraries like Ktor where extension functions are used in a way that creates a lot of imports.
# "**" applies to package and all subpackages
ij_kotlin_packages_to_use_import_on_demand=java.util.* # allow java.util.* as wildcard import
ij_kotlin_packages_to_use_import_on_demand=io.ktor.** # allow wildcard import from io.ktor.* and all subpackages 
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

## Online demo
You can try `ktlint` online [here](https://ktlint-demo.herokuapp.com/) using the standard or a custom ruleset without installing it to your PC. \
To contribute or get more info, please visit the [GitHub repository](https://github.com/akuleshov7/diKTat-demo).


## Installation

> Skip all the way to the "Integration" section if you don't plan to use `ktlint`'s command line interface.

```sh
curl -sSLO https://github.com/pinterest/ktlint/releases/download/0.45.2/ktlint &&
  chmod a+x ktlint &&
  sudo mv ktlint /usr/local/bin/
```

... or just download `ktlint` from the [releases](https://github.com/pinterest/ktlint/releases) page

* `ktlint.asc` contains PGP signature which you can verify with:
  * (Releases up through 0.31.0) `curl -sS https://keybase.io/shyiko/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc`
  * (Releases from 0.32.0 on) `curl -sS https://keybase.io/ktlint/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc`

On macOS ([or Linux](https://docs.brew.sh/Homebrew-on-Linux)) you can also use [brew](https://brew.sh/) - `brew install ktlint` - or [MacPorts](https://www.macports.org/) - `port install ktlint`.
On Arch Linux, you can install [ktlint](https://aur.archlinux.org/packages/ktlint/) <sup>AUR</sup>.

> If you don't have curl installed - replace `curl -sL` with `wget -qO-`.

> If you are behind a proxy see -
[curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / 
[wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. 
Usually simple `http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...` is enough. 

## Command line usage

```bash
# Get help about all available commands
$ ktlint --help

# Check the style of all Kotlin files (ending with '.kt' or '.kts') inside the current dir (recursively).
# Hidden folders will be skipped.
$ ktlint
  
# Check only certain locations starting from the current directory.
#
# Prepend ! to negate the pattern, KtLint uses .gitignore pattern style syntax.
# Globs are applied starting from the last one.
#
# Hidden folders will be skipped.
# Check all '.kt' files in 'src/' directory, but ignore files ending with 'Test.kt':
ktlint "src/**/*.kt" "!src/**/*Test.kt"
# Check all '.kt' files in 'src/' directory, but ignore 'generated' directory and its subdirectories:
ktlint "src/**/*.kt" "!src/**/generated/**"

# Auto-correct style violations.
# If some errors cannot be fixed automatically they will be printed to stderr. 
$ ktlint -F "src/**/*.kt"

# Print style violations grouped by file.
$ ktlint --reporter=plain?group_by_file

# Print style violations as usual + create report in checkstyle format, specifying report location. 
$ ktlint --reporter=plain --reporter=checkstyle,output=ktlint-report-in-checkstyle-format.xml

# Check against a baseline file.
$ ktlint --baseline=ktlint-baseline.xml

# Install git hook to automatically check files for style violations on commit.
# Run "ktlint installGitPrePushHook" if you wish to run ktlint on push instead.
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
            <version>0.45.2</version>
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
    mavenCentral()
}

configurations {
    ktlint
}

dependencies {
    ktlint("com.pinterest:ktlint:0.45.2") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, getObjects().named(Bundling, Bundling.EXTERNAL))
        }
    }
    // additional 3rd party ruleset(s) can be specified here
    // just add them to the classpath (e.g. ktlint 'groupId:artifactId:version') and 
    // ktlint will pick them up
}

task ktlint(type: JavaExec, group: "verification") {
    description = "Check Kotlin code style."
    classpath = configurations.ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args "src/**/*.kt"
    // to generate report in checkstyle format prepend following args:
    // "--reporter=plain", "--reporter=checkstyle,output=${buildDir}/ktlint.xml"
    // to add a baseline to check against prepend following args:
    // "--baseline=ktlint-baseline.xml"
    // see https://github.com/pinterest/ktlint#usage for more
}
check.dependsOn ktlint

task ktlintFormat(type: JavaExec, group: "formatting") {
    description = "Fix Kotlin code style deviations."
    classpath = configurations.ktlint
    mainClass.set("com.pinterest.ktlint.Main")
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
    ktlint("com.pinterest:ktlint:0.45.2") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
    // ktlint(project(":custom-ktlint-ruleset")) // in case of custom ruleset
}

val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlintCheck by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "src/**/*.kt")
}
```

#### ... with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

> It is recommended to align the settings of IntelliJ IDEA's built-in formatter with ktlint. This reduces the chance that code which is formatted by ktlint conflicts with formatting by the IntelliJ IDEA built-in formatter.  

Choose any of options below to align the formatting settings of IntelliJ IDEA.

##### Update code style of single project via ktlint (recommended)
Use ktlint to change the code style of a single project with any of the commands below.
```sh
# Run command below from root directory of project
ktlint applyToIDEAProject
```
Or if you want to use android specific code style:

```sh
# Run command below from root directory of project
ktlint --android applyToIDEAProject
```

##### Update global code style for all projects via ktlint
Use ktlint to change the code style of all projects with any of the commands below.
```sh
ktlint applyToIDEA
```
Or if you want to use android specific code style:
```sh
ktlint --android applyToIDEA
```

##### Manually update `.editorconfig`

Create or update the code style config with `.editorconfig` by setting properties below:
```ini
[{*.kt,*.kts}]
ij_kotlin_code_style_defaults = KOTLIN_OFFICIAL

ij_kotlin_line_comment_at_first_column = false
ij_kotlin_line_comment_add_space = true

# These options can keep to use single name import
ij_kotlin_name_count_to_use_star_import = 2147483647
ij_kotlin_name_count_to_use_star_import_for_members = 2147483647

ij_kotlin_keep_blank_lines_in_declarations = 1
ij_kotlin_keep_blank_lines_in_code = 1
ij_kotlin_keep_blank_lines_before_right_brace = 0

# optional but recommended
ij_kotlin_align_multiline_parameters = false

# optional but recommended
ij_continuation_indent_size = 4

# Android specific rules
ij_kotlin_import_nested_classes = false
ij_kotlin_imports_layout = *,^
```

##### Manually update the IntelliJ IDEA preferences

Go to <kbd>File</kbd> -> <kbd>Settings...</kbd> -> <kbd>Editor</kbd>
- <kbd>General</kbd> -> <kbd>Auto Import</kbd>
  - check `Kotlin` / `Optimize imports on the fly (for current project)`.
- <kbd>Code Style</kbd> -> <kbd>Kotlin</kbd>
  - <kbd>Set from...</kbd> on the right -> (<kbd>Predefined style</kbd>) -> <kbd>Kotlin style guide</kbd> (Kotlin plugin 1.2.20+).
  - open <kbd>Code Generation</kbd> tab
    - uncheck `Line comment at first column`;
    - select `Add a space at comment start`.
  - open <kbd>Imports</kbd> tab
    - select `Use single name import` (all of them);
    - remove `import java.util.*` from `Packages to Use Import with '*'`.
  - open <kbd>Blank Lines</kbd> tab
    - change `Keep Maximum Blank Lines` / `In declarations` & `In code` to 1 and `Before '}'` to 0.
  - (optional but recommended) open <kbd>Wrapping and Braces</kbd> tab
    - uncheck `Function declaration parameters` (OR `Methods declartion parameters` for older version) / `Align when multiline`.
  - (optional but recommended) open <kbd>Tabs and Indents</kbd> tab
    - change `Continuation indent` to the same value as `Indent` (4 by default).   
- <kbd>Inspections</kbd> 
  - change `Severity` level of `Unused import directive` and `Redundant semicolon` under `Kotlin` -> `Redundant constructs` to `ERROR`.

#### ... with [GNU Emacs](https://www.gnu.org/software/emacs/)

See [whirm/flycheck-kotlin](https://github.com/whirm/flycheck-kotlin).

#### ... with [Vim](https://www.vim.org/)

See [w0rp/ale](https://github.com/w0rp/ale).

> Integrated with something else? Send a PR.

#### ... with Continuous Integration

See [Mega-Linter](https://nvuillam.github.io/mega-linter/): 70+ linters aggregated in a single tool for CI, including **ktlint** activated out of the box

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
* [musichin/ktlint-github-reporter](https://github.com/musichin/ktlint-github-reporter)
* [tobi2k/ktlint-gitlab-reporter](https://github.com/tobi2k/ktlint-gitlab-reporter)

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

ktlint is a single binary with both linter & formatter included. All you need is to drop it in (no need to get [overwhelmed](https://en.wikipedia.org/wiki/Decision_fatigue) while choosing among [dozens of code style options](https://checkstyle.sourceforge.net/checks.html)).

### Can I have my own rules on top of ktlint?

Absolutely, "no configuration" doesn't mean "no extensibility". You can add your own ruleset(s) to discover potential bugs, check for anti-patterns, etc.

See [Creating A Ruleset](#creating-a-ruleset).

### How do I suppress an errors for a line/block/file?

> This is meant primarily as an escape latch for the rare cases when **ktlint** is not able
to produce the correct result (please report any such instances using [GitHub Issues](https://github.com/pinterest/ktlint/issues)).

To disable a specific rule you'll need the rule identifier which is displayed at the end of the lint error. Note that when the rule id is prefixed with a rule set id like `experimental`, you will need to use that fully qualified rule id.

An error can be suppressed using:

* EOL comments
* Block comments
* @Suppress annotations

From a consistency perspective seen, it might be best to **not** mix the (EOL/Block) comment style with the annotation style in the same project.

Important notice: some rules like the `indent` rule do not yet support disabling of the rule per line of block.

#### Disabling for one specific line using EOL comment

An error for a specific rule on a specific line can be disabled with an EOL comment on that line:

```kotlin
import package.* // ktlint-disable no-wildcard-imports
```

In case lint errors for different rules on the same line need to be ignored, then specify multiple rule ids (separated by a space):

```kotlin
import package.* // ktlint-disable no-wildcard-imports other-rule-id
```

In case all lint errors on a line need to be ignored, then do not specify the rule id at all:

```kotlin
import package.* // ktlint-disable
```

#### Disabling for a block of lines using Block comments

An error for a specific rule in a block of lines can be disabled with an block comment like:

```kotlin
/* ktlint-disable no-wildcard-imports */
import package.a.*
import package.b.*
/* ktlint-enable no-wildcard-imports */
```

In case lint errors for different rules in the same block of lines need to be ignored, then specify multiple rule ids (separated by a space):

```kotlin
/* ktlint-disable no-wildcard-imports other-rule-id */
import package.a.*
import package.b.*
/* ktlint-enable no-wildcard-imports,other-rule-id */
```

Note that the `ktlint-enable` directive needs to specify the exact same rule-id's and in the same order as the `ktlint-disable` directive.

In case all lint errors in a block of lines needs to be ignored, then do not specify the rule id at all:

```kotlin
/* ktlint-disable */
import package.a.*
import package.b.*
/* ktlint-enable */
```

#### Disabling for a statement using @Suppress

> As of ktlint version 0.46, it is possible to specify any ktlint rule id via the `@Suppress` annotation in order to suppress errors found by that rule. Note that some rules like `indent` still do not support disabling for parts of a file.

An error for a specific rule on a specific line can be disabled with a `@Suppress` annotation:

```kotlin
@Suppress("ktlint:max-line-length","ktlint:experimental:trailing-comma")
val foo = listOf(
    "some really looooooooooooooooong string exceeding the max line length",
  )
```

Note that when using `@Suppress` each qualified rule id needs to be prefixed with `ktlint:`.

To suppress the violations of all ktlint rules, use:
```kotlin
@Suppress("ktlint")
val foo = "some really looooooooooooooooong string exceeding the max line length"
```

Like with other `@Suppress` annotations, it can be placed on targets supported by the annotation.

### How do I globally disable a rule?
See the [EditorConfig section](https://github.com/pinterest/ktlint#editorconfig) for details on how to use the `disabled_rules` property.

You may also pass a list of disabled rules via the `--disabled_rules` command line flag. It has the same syntax as the EditorConfig property.

## Development

> Make sure to read [CONTRIBUTING.md](CONTRIBUTING.md).

```sh
git clone https://github.com/pinterest/ktlint && cd ktlint
./gradlew tasks # shows how to build, test, run, etc. project
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
