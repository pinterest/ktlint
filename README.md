<h1 align="center">
<a href="https://ktlint.github.io/">
  <img src="https://cloud.githubusercontent.com/assets/370176/26518284/38b680da-4262-11e7-8d27-2b9e849fb55f.png"/>
</a>
</h1>

<p align="center">
<a href="https://travis-ci.org/shyiko/ktlint"><img src="https://travis-ci.org/shyiko/ktlint.svg?branch=master" alt="Build Status"></a>
<a href="https://ci.appveyor.com/project/shyiko/ktlint"><img src="https://ci.appveyor.com/api/projects/status/9dtlak3cj5rum48g?svg=true&passingText=passing" alt="Build Status"></a>
<a href="https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22ktlint%22"><img src="https://img.shields.io/maven-central/v/com.github.shyiko/ktlint.svg" alt="Maven Central"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="ktlint"></a>
</p>

<p align="center">
<a href="https://kotlinlang.org/">Kotlin</a> linter in spirit of <a href="https://github.com/feross/standard">feross/standard</a> (JavaScript) and <a href="https://golang.org/cmd/gofmt/">gofmt</a> (Go).  
</p>

Features:
- **No configuration.** Which means no decisions to make, nothing to argue about and no special files to manage.   
While this might sound extreme, keep in mind that `ktlint` tries to capture (reflect) **official code style** from [kotlinlang.org](https://kotlinlang.org/docs/reference/coding-conventions.html) and [Android Kotlin Style Guide](https://android.github.io/kotlin-guides/style.html)
(+ [we respect you .editorconfig](#editorconfig) and support additional [ruleset](#creating-a-ruleset)|s).
- **Built-in formatter.** So that you wouldn't have to fix all style violations by hand.
- **Customizable output.** `plain` (+ `plain?group_by_file`), `json` and `checkstyle` reporters are available out-of-the-box. 
It's also [easy to create your own](#creating-a-reporter).
- **A single executable jar with all dependencies included.**

<p align="center">
<a href="#installation">Installation</a> | <a href="#usage">Usage</a> | <a href="#integration">Integration</a> with <a href="#-with-maven">Maven</a> / <a href="#-with-gradle">Gradle</a> / <a href="#-with-intellij-idea">IntelliJ IDEA</a> / <a href="#-with-emacs">Emacs</a> | Creating <a href="#creating-a-ruleset">a ruleset</a> | <a href="#creating-a-reporter">a reporter</a> | <a href="#badge">Badge</a> | <a href="#faq">FAQ</a>
</p>

## Standard rules

- 4 spaces for indentation  
(unless a different `indent_size` value is set in .editorconfig (see [EditorConfig](#editorconfig) section for more));
- No semicolons (unless used to separate multiple statements on the same line);
- No wildcard / unused `import`s;
- No consecutive blank lines;
- No blank lines before `}`;
- No trailing whitespaces;
- No `Unit` returns (`fun fn {}` instead of `fun fn: Unit {}`);
- No empty (`{}`) class bodies;
- No spaces around range (`..`) operator;
- No newline before (binary) `+` & `-`, `*`, `/`, `%`, `&&`, `||`; 
- When wrapping chained calls `.`, `?.` and `?:` should be placed on the next line;
- When a line is broken at an assignment (`=`) operator the break comes after the symbol;
- When class/function signature doesn't fit on a single line, each parameter must be on a separate line;
- Consistent string templates (`$v` instead of `${v}`, `${p.v}` instead of `${p.v.toString()}`);
- Consistent order of modifiers;
- Consistent spacing after keywords, commas; around colons, curly braces, infix operators, etc;
- Newline at the end of each file (not enabled by default, but recommended)  
(set `insert_final_newline=true` in .editorconfig to enable (see [EditorConfig](#editorconfig) section for more)).

## EditorConfig

ktlint recognizes the following [.editorconfig](http://editorconfig.org/) properties (provided they are specified under `[*.{kt,kts}]`):  
(values shown below are the defaults and do not need to be specified explicitly)
```ini
[*.{kt,kts}]
# possible values: number (e.g. 2), "unset" (makes ktlint ignore indentation completely)  
indent_size=4
# possible values: number (e.g. 2), "unset"
continuation_indent_size=4
# true (recommended) / false
insert_final_newline=unset
# possible values: number (e.g. 120) (package name, imports & comments are ignored), "off"
# it's automatically set to 100 on `ktlint --android ...` (per Android Kotlin Style Guide)
max_line_length=off
```

## Installation

> Skip all the way to the "Integration" section if you don't plan to use `ktlint`'s command line interface.

```sh
curl -sSLO https://github.com/shyiko/ktlint/releases/download/0.22.0/ktlint &&
  chmod a+x ktlint &&
  sudo mv ktlint /usr/local/bin/
```

... or just download `ktlint` from the [releases](https://github.com/shyiko/ktlint/releases) page  (`ktlint.asc` contains PGP signature which you can verify with `curl -sS https://keybase.io/shyiko/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc`).  

On macOS ([or Linux](http://linuxbrew.sh/)) you can also use [brew](https://brew.sh/) - `brew install shyiko/ktlint/ktlint`.

> If you don't have curl installed - replace `curl -sL` with `wget -qO-`.

> If you are behind a proxy see -
[curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / 
[wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. 
Usually simple `http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...` is enough. 

## Usage

```bash
# check the style of all Kotlin files inside the current dir (recursively)
# (hidden folders will be skipped)
$ ktlint --color
  src/main/kotlin/Main.kt:10:10: Unused import
  
# check only certain locations (prepend ! to negate the pattern) 
$ ktlint "src/**/*.kt" "!src/**/*Test.kt"

# auto-correct style violations
# (if some errors cannot be fixed automatically they will be printed to stderr) 
$ ktlint -F "src/**/*.kt"

# print style violations grouped by file
$ ktlint --reporter=plain?group_by_file
# print style violations as usual + create report in checkstyle format 
$ ktlint --reporter=plain --reporter=checkstyle,output=ktlint-report-in-checkstyle-format.xml

# install git hook to automatically check files for style violations on commit
$ ktlint --install-git-pre-commit-hook
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
                    classpathref="maven.plugin.classpath" classname="com.github.shyiko.ktlint.Main">
                    <arg value="src/**/*.kt"/>
                    <!-- to generate report in checkstyle format prepend following args: -->
                    <!-- 
                    <arg value="--reporter=plain"/>
                    <arg value="--reporter=checkstyle,output=${project.build.directory}/ktlint.xml"/>
                    -->
                    <!-- see https://github.com/shyiko/ktlint#usage for more -->                    
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
                    classpathref="maven.plugin.classpath" classname="com.github.shyiko.ktlint.Main">
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
            <groupId>com.github.shyiko</groupId>
            <artifactId>ktlint</artifactId>
            <version>0.22.0</version>
        </dependency>
        <!-- additional 3rd party ruleset(s) can be specified here -->
    </dependencies>
</plugin>
...
```

To check code style - `mvn antrun:run@ktlint` (it's also bound to `mvn verify`).  
To run formatter - `mvn antrun:run@ktlint-format`.   

#### ... with [Gradle](https://gradle.org/)

#### (without a plugin)

> build.gradle

```groovy
// kotlin-gradle-plugin must be applied for configuration below to work
// (see https://kotlinlang.org/docs/reference/using-gradle.html)

repositories {
    jcenter()
}

configurations {
    ktlint
}

dependencies {
    ktlint "com.github.shyiko:ktlint:0.22.0"
    // additional 3rd party ruleset(s) can be specified here
    // just add them to the classpath (e.g. ktlint 'groupId:artifactId:version') and 
    // ktlint will pick them up
}

task ktlint(type: JavaExec, group: "verification") {
    description = "Check Kotlin code style."
    classpath = configurations.ktlint
    main = "com.github.shyiko.ktlint.Main"
    args "src/**/*.kt"
    // to generate report in checkstyle format prepend following args:
    // "--reporter=plain", "--reporter=checkstyle,output=${buildDir}/ktlint.xml"
    // see https://github.com/shyiko/ktlint#usage for more
}
check.dependsOn ktlint

task ktlintFormat(type: JavaExec, group: "formatting") {
    description = "Fix Kotlin code style deviations."
    classpath = configurations.ktlint
    main = "com.github.shyiko.ktlint.Main"
    args "-F", "src/**/*.kt"
}
```

To check code style - `gradle ktlint` (it's also bound to `gradle check`).  
To run formatter - `gradle ktlintFormat`.

See [Making your Gradle tasks incremental](https://proandroiddev.com/making-your-gradle-tasks-incremental-7f26e4ef09c3) by [Niklas Baudy](https://github.com/vanniktech) on how to make tasks above incremental. 

#### (with a plugin)

Gradle plugins (in order of appearance):
- [jlleitschuh/ktlint-gradle](https://github.com/jlleitschuh/ktlint-gradle)  
The very first ktlint gradle plugin.

- [jeremymailen/kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle)  
Gradle plugin featuring incremental build, `*.kts` support.

You might also want to take a look at [diffplug/spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-ktlint-to-kotlin-files) which has a built-in support for ktlint. In addition to linting/formatting kotlin code it allows you to keep license headers, markdown documentation, etc. in check.

#### ... with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

> While this is not strictly necessary it makes Intellij IDEA's built-in formatter produce 100% ktlint-compatible 
 code. 

##### Option #1 (recommended)

> (inside project's root directory)  

```sh
ktlint --apply-to-idea-project
# or if you want to be compliant with Android Kotlin Style Guide
ktlint --apply-to-idea-project --android 
```

##### Option #2

Go to <kbd>File</kbd> -> <kbd>Settings...</kbd> -> <kbd>Editor</kbd>
- <kbd>General</kbd> -> <kbd>Auto Import</kbd>
  - check `Optimize imports on the fly (for current project)`.
- <kbd>Code Style</kbd> -> <kbd>Kotlin</kbd>
  - <kbd>Set from...</kbd> -> <kbd>Predefined style</kbd> -> <kbd>Kotlin style guide</kbd> (Kotlin plugin 1.2.20+).
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

> Integrated with something else? Send a PR.

## Creating a ruleset

In a nutshell: "ruleset" is a JAR containing one or more [Rule](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/Rule.kt)s gathered together in a [RuleSet](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/RuleSet.kt). `ktlint` is relying on 
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to discover all available "RuleSet"s
on the classpath (as a ruleset author, all you need to do is to include a `META-INF/services/com.github.shyiko.ktlint.core.RuleSetProvider` file 
containing a fully qualified name of your [RuleSetProvider](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/RuleSetProvider.kt) implementation).    

Once packaged in a JAR you can load it with

```sh
# enable additional 3rd party ruleset by pointing ktlint to its location on the file system
$ ktlint -R /path/to/custom/rulseset.jar "src/test/**/*.kt"

# you can also use <groupId>:<artifactId>:<version> triple in which case artifact is
# downloaded from Maven Central, JCenter or JitPack (depending on where it's located and 
# whether or not it's already present in local Maven cache)
$ ktlint -R com.github.username:rulseset:master-SNAPSHOT
```

A complete sample project (with tests and build files) is included in this repo under the [ktlint-ruleset-template](ktlint-ruleset-template) directory 
(make sure to check [NoVarRuleTest](ktlint-ruleset-template/src/test/kotlin/yourpkgname/NoVarRuleTest.kt) as it contains some useful information). 

#### AST

While writing/debugging [Rule](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/Rule.kt)s it's often helpful to have an AST
printed out to see the structure rules have to work with. ktlint >= 0.15.0 has `--print-ast` flag specifically for this purpose
(usage: `ktlint --color --print-ast <file>`).  
An example of the output is shown below. 

```sh
$ printf "fun main() {}" | ktlint --color --print-ast --stdin

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
[Reporter](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/Reporter.kt) and make it available by registering 
a custom [ReporterProvider](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/ReporterProvider.kt) using
`META-INF/services/com.github.shyiko.ktlint.core.ReporterProvider`. Pack all of that into a JAR and you're done.

To load a custom (3rd party) reporter use `ktlint --reporter=name,artifact=groupId:artifactId:version` / `ktlint --reporter=name,artifact=/path/to/custom-ktlint-reporter.jar`
(see `ktlint --help` for more).
 
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

### How do I suppress an error?

> This is meant primarily as an escape latch for the rare cases when **ktlint** is not able
to produce the correct result (please report any such instances using [GitHub Issues](https://github.com/shyiko/ktlint/issues)).

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

## Development

> Make sure to read [CONTRIBUTING.md](CONTRIBUTING.md).

```sh
git clone https://github.com/shyiko/ktlint && cd ktlint
./mvnw # shows how to build, test, etc. project
```

#### Access to the latest `master` snapshot

Whenever a commit is added to the `master` branch `0.0.0-SNAPSHOT` is automatically uploaded to [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/com/github/shyiko/ktlint/).
If you are eager to try upcoming changes (that might or might not be included in the next stable release) you can do 
so by changing version of ktlint to `0.0.0-SNAPSHOT` + adding a repo: 

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

## Legal

This project is not affiliated with or endorsed by the Jetbrains.  
All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2016 Stanley Shyiko.
