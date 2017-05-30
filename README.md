<h1 align="center">
<a href="https://ktlint.github.io/">
  <img src="https://cloud.githubusercontent.com/assets/370176/26518284/38b680da-4262-11e7-8d27-2b9e849fb55f.png"/>
</a>
</h1>

<p align="center">
<a href="https://travis-ci.org/shyiko/ktlint"><img src="https://travis-ci.org/shyiko/ktlint.svg?branch=master" alt="Build Status"></a>
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22ktlint%22"><img src="http://img.shields.io/badge/maven_central-0.8.0-blue.svg?style=flat" alt="Maven Central"></a>
</p>

<p align="center">
<a href="https://kotlinlang.org/">Kotlin</a> linter in spirit of <a href="https://github.com/feross/standard">feross/standard</a> (JavaScript) and <a href="https://golang.org/cmd/gofmt/">gofmt</a> (Go).  
</p>

Features:
- **No configuration.** Which means no decisions to make, nothing to argue about and no special files to manage.   
While this might sound extreme, keep in mind that `ktlint` tries to capture (reflect) **official code style** from [kotlinlang.org](https://kotlinlang.org/docs/reference/)
(+ we support **additional** [3rd party rulesets](#creating-a-ruleset)).
- **Built-in formatter.** So that you wouldn't have to fix all style violations by hand.
- **A single executable jar with all dependencies included.**

<p align="center">
<a href="#standard-rules">Standard rules</a> | <a href="#installation">Installation</a> | <a href="#usage">Usage</a> | <a href="#integration">Integration</a> with <a href="#-with-maven">Maven</a> / <a href="#-with-gradle">Gradle</a> / <a href="#-with-intellij-idea">IntelliJ IDEA</a> | <a href="#creating-a-ruleset">Creating a ruleset</a> | <a href="#faq">FAQ</a>
</p>

## Standard rules

- 4 spaces for indentation*.
- No semicolons (unless used to separate multiple statements on the same line).
- No wildcard / unused imports.
- No consecutive blank lines.
- No trailing whitespaces.
- No Unit returns;
- Consistent order of modifiers;
- Consistent spacing after keywords, commas; around colons, curly braces, infix operators, etc.

> \* Starting from [0.8.0](https://github.com/shyiko/ktlint/releases/tag/0.8.0) value of `indent_size` specified under `[*{kt,kts}]` section in [.editorconfig](http://editorconfig.org/) takes precedence (if any). Official recommendation is to use 4 spaces, though. (see [#43](https://github.com/shyiko/ktlint/issues/43#issuecomment-304953280) for details)

> [More coming](https://github.com/shyiko/ktlint/labels/rule).

## Installation

> Skip all the way to the "Integration" section if you don't plan to use `ktlint`'s command line interface.

```sh
curl -sSLO https://github.com/shyiko/ktlint/releases/download/0.8.0/ktlint &&
  chmod a+x ktlint
```

> If you don't have curl installed - replace `curl -sL` with `wget -qO-`.

> If you are behind a proxy see -
[curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / 
[wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. 
Usually simple `http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...` is enough. 

... or just download `ktlint` from the ["release(s)"](https://github.com/shyiko/ktlint/releases) page  (`ktlint.asc` contains PGP signature which you can verify with `curl -sS https://keybase.io/shyiko/pgp_keys.asc | gpg --import && gpg --verify ktlint.asc`).  

> On Mac OS X ([or Linux](http://linuxbrew.sh/)) one can also use [brew](http://brew.sh/) - `brew install shyiko/ktlint/ktlint`.

## Usage

```bash
# check the style of all Kotlin files inside the current dir (recursively)
# (hidden folders will be skipped)
$ ktlint
  src/main/kotlin/Main.kt:10:10: Unused import
  
# check only certain locations (prepend ! to negate the pattern) 
$ ktlint "src/**/*.kt" "!src/**/*Test.kt"

# auto-correct style violations
# (if some errors cannot be fixed automatically they will be printed to stderr) 
$ ktlint -F "src/**/*.kt"
```

> on Windows you'll have to use `java -jar ktlint ...`. 

`ktlint --help` for more.

### Integration 

#### ... with [Maven]()

> pom.xml

```xml
...
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>1.7</version>
    <executions>
        <execution>
            <id>ktlint</id>
            <phase>verify</phase>
            <configuration>
            <target name="ktlint">
                <java taskname="ktlint" dir="${basedir}" fork="true" failonerror="true"
                    classname="com.github.shyiko.ktlint.Main" classpathref="maven.plugin.classpath">
                    <arg value="src/**/*.kt"/>
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
                    classname="com.github.shyiko.ktlint.Main" classpathref="maven.plugin.classpath">
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
            <version>0.8.0</version>
        </dependency>
        <!-- additional 3rd party ruleset(s) can be specified here -->
    </dependencies>
</plugin>
...
```

To check code style - `mvn antrun:run@ktlint` (it's also bound to `mvn verify`).  
To run formatter - `mvn antrun:run@ktlint-format`.   

#### ... with [Gradle]()

> build.gradle

```groovy
apply plugin: 'java'

repositories {
    mavenCentral()
}

configurations {
    ktlint
}

dependencies {
    ktlint 'com.github.shyiko:ktlint:0.8.0'
    // additional 3rd party ruleset(s) can be specified here
    // just add them to the classpath (ktlint 'groupId:artifactId:version') and 
    // ktlint will pick them up
}

task ktlint(type: JavaExec) {
    main = "com.github.shyiko.ktlint.Main"
    classpath = configurations.ktlint
    args "src/**/*.kt"
}

check.dependsOn ktlint

task ktlintFormat(type: JavaExec) {
    main = "com.github.shyiko.ktlint.Main"
    classpath = configurations.ktlint
    args "-F", "src/**/*.kt"
}
```

To check code style - `gradle ktlint` (it's also bound to `gradle check`).  
To run formatter - `gradle ktlintFormat`.

**Another option** is to use Gradle plugin (in order of appearance):
- [jlleitschuh/ktlint-gradle](https://github.com/jlleitschuh/ktlint-gradle)
- [jeremymailen/kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle)

Each plugin has some unique features (like incremental build support in case of [jeremymailen/kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle)) so check them out.

You might also want to take a look at [diffplug/spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-ktlint-to-kotlin-files) which has a built-in support for ktlint. In addition to linting/formatting kotlin code it allows you to keep license headers, markdown documentation, etc. in check.

#### ... with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

> While this is not strictly necessary it makes Intellij IDEA's built-in formatter produce 100% ktlint-compatible 
 code. 

##### Option #1 (recommended)

```sh
curl -sSLO https://github.com/shyiko/ktlint/releases/download/0.8.0/ktlint-intellij-idea-integration 
chmod a+x ktlint-intellij-idea-integration
# you can also download ktlint-intellij-idea-integration manually from 
# https://github.com/shyiko/ktlint/releases

# inside project's root directory  
ktlint-intellij-idea-integration apply 
```

##### Option #2

Go to `File -> Settings... -> Editor`
- `Code Style -> Manage... -> Import -> Intellij IDEA code style XML`,
select [codestyles/ktlint.xml](ktlint-intellij-idea-integration/src/main/resources/config/codestyles/ktlint.xml).
- `Inspections -> Manage -> Import`,
select [inspection/ktlint.xml](ktlint-intellij-idea-integration/src/main/resources/config/inspection/ktlint.xml).

##### Option #3

Go to `File -> Settings... -> Editor`
- `Code Style -> Kotlin`
  - open `Imports` tab, select all `Use single name import` options and remove `import java.util.*` from `Packages to Use Import with '*'`.
  - (optional but recommended) open `Wrapping and Braces` tab, uncheck `Method declaration parameters -> Align when multiline`. 
  - (optional but recommended) open `Tabs and Indents` tab, change `Continuation indent` to 4.
- `Inspections` 
  - change `Severity` level of `Unused import directive`, `Redundant semicolon` and (optional but recommended) `Unused symbol` to `ERROR`.

#### ... with [GNU Emacs](https://www.gnu.org/software/emacs/)

See [whirm/flycheck-kotlin](https://github.com/whirm/flycheck-kotlin).

> Integrated with something else? Send a PR.

## Creating a ruleset

In a nutshell: "ruleset" is a JAR containing one or more [Rule](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/Rule.kt)s gathered together in a [RuleSet](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/RuleSet.kt). `ktlint` is relying on 
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to discover all available "RuleSet"s
on the classpath (as a ruleset author, all you need to do is to include a `META-INF/services/com.github.shyiko.ktlint.core.RuleSetProvider` file 
containing a fully qualified name of your [RuleSetProvider](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/RuleSetProvider.kt) implementation).    

A complete sample project (with tests and build files) is included in this repo under the [ktlint-ruleset-template](ktlint-ruleset-template) directory 
(make sure to check [NoVarRuleTest](ktlint-ruleset-template/src/test/kotlin/yourpkgname/NoVarRuleTest.kt) as it contains some useful information). 

## FAQ

### Why should I use ktlint?

**Simplicity**.

Spending time on configuration (& maintenance down the road) of hundred-line long style config file(s) is counter-productive. Instead of wasting your energy on something that has no business value - focus on what really matters (not debating whether to use tabs or spaces).

By using ktlint you put the importance of code clarity and community conventions over personal preferences. This makes things easier for people reading your code as well as frees you from having to document & explain what style potential contributor(s) have to follow.

ktlint is a single binary with both linter & formatter included. All you need is to drop it in (no need to get [overwhelmed](https://en.wikipedia.org/wiki/Decision_fatigue) while choosing among [dozens of code style options](http://checkstyle.sourceforge.net/checks.html)).

### Can I have my own rules on top of ktlint?

Absolutely, "no configuration" doesn't mean "no extensibility". You can add your own ruleset(s) to discover potential bugs, check for anti-patterns, etc.

See [Creating A Ruleset](#creating-a-ruleset).

Once packaged in a JAR you can load it with

```sh
# enable additional 3rd party ruleset by pointing ktlint to its location on the file system
$ ktlint -R /path/to/custom/rulseset.jar "src/test/**/*.kt"

# you can also use <groupId>:<artifactId>:<version> triple in which case artifact is
# downloaded from Maven Central, JCenter or JitPack (depending on where it's located and 
# whether or not it's already present in local Maven cache)
$ ktlint -R com.github.username:rulseset:master-SNAPSHOT
```

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

## Legal

This project is not affiliated with or endorsed by the Jetbrains.  
All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2016 Stanley Shyiko.
