<h1 align="center">
ktlint
</h1>
<p align="center">
[![Build Status](https://travis-ci.org/shyiko/ktlint.svg?branch=master)](https://travis-ci.org/shyiko/ktlint) [![Maven Central](http://img.shields.io/badge/maven_central-0.2.1-blue.svg?style=flat)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko%22%20AND%20a%3A%22ktlint%22)

<p align="center">
[Kotlin](https://kotlinlang.org/) linter in spirit of <a href="https://github.com/feross/standard">feross/standard</a> (JavaScript) and <a href="https://golang.org/cmd/gofmt/">gofmt</a> (Go).  

Features:
- **No configuration.** Which means no decisions to make, nothing to argue about and no special files to manage.   
While this might sound extreme, keep in mind that `ktlint` tries to capture (reflect) **official code style** from [kotlinlang.org](https://kotlinlang.org/docs/reference/)
(+ we support **additional** [3rd party rulesets](#creating-a-ruleset)).
- **Built-in formatter.** So that you wouldn't have to fix all style violations by hand.
- **A single executable jar with all dependencies included.**

<p align="center">
[Standard rules](#standard-rules) | [Installation](#installation) | 
[Usage](#usage) | [Integration](#integration) with [Maven](#-with-maven) / [Gradle](#-with-gradle) / [IntelliJ IDEA](#-with-intellij-idea) | 
[Creating a ruleset](#creating-a-ruleset) | [FAQ](#faq)

## Standard rules

- 4 spaces for indentation.
- No semicolons (unless used to separate multiple statements on the same line).
- No wildcard / unused imports.
- No consecutive blank lines.
- No trailing whitespaces.
- Consistent spacing after keywords, commas; around colons, curly braces, infix operators, etc.

> [More coming](https://github.com/shyiko/ktlint/labels/rule).

## Installation

> Skip all the way to the "Integration" section if you don't plan to use `ktlint`'s command line interface.

```sh
curl -sL https://github.com/shyiko/ktlint/releases/download/0.2.1/ktlint > ktlint &&
  chmod a+x ktlint
```

> If you don't have curl installed - replace `curl -sL` with `wget -qO-`.

> If you are behind a proxy see -
[curl](https://curl.haxx.se/docs/manpage.html#ENVIRONMENT) / 
[wget](https://www.gnu.org/software/wget/manual/wget.html#Proxies) manpage. 
Usually simple `http_proxy=http://proxy-server:port https_proxy=http://proxy-server:port curl -sL ...` is enough. 

... or just download `ktlint` from the ["release(s)"](https://github.com/shyiko/ktlint/releases) page.  

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

# enable additional 3rd party ruleset by pointing ktlint to its location on the file system
$ ktlint -R /path/to/custom/rulseset.jar "src/test/**/*.kt"

# you can also use <groupId>:<artifactId>:<version> triple in which case artifact is
# downloaded from Maven Central, JCenter or JitPack (depending on where it's located and 
# whether or not it's already present in local Maven cache)
$ ktlint -R com.github.username:ktlint-rulseset-magical:master-SNAPSHOT
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
            <version>0.2.1</version>
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
repositories {
    mavenCentral()
}

configurations {
    ktlint
}

dependencies {
    ktlint 'com.github.shyiko:ktlint:0.2.1'
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

#### ... with [IntelliJ IDEA](https://www.jetbrains.com/idea/)

Go to `File -> Settings... -> Editor`
- `Code Style -> Manage... -> Import -> Intellij IDEA code style XML`,
select [integration/intellij-idea/configs/codestyles/ktlint.xml](integration/intellij-idea/configs/codestyles/ktlint.xml).
- `Inspections -> Manage -> Import`,
select [integration/intellij-idea/configs/inspection/ktlint.xml](integration/intellij-idea/configs/inspection/ktlint.xml).

> Integrated with something else? Send a PR.

## Creating a ruleset

In a nutshell: "ruleset" is a JAR containing one or more [Rule](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/Rule.kt)s gathered together in a [RuleSet](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/RuleSet.kt). `ktlint` is relying on 
[ServiceLoader](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to discover all available "RuleSet"s
on the classpath (as a ruleset author, all you need to do is to include a `META-INF/services/com.github.shyiko.ktlint.core.RuleSetProvider` file 
containing a fully qualified name of your [RuleSetProvider](ktlint-core/src/main/kotlin/com/github/shyiko/ktlint/core/RuleSetProvider.kt) implementation).    

A complete sample project (with tests and build files) is included in this repo under the [ktlint-ruleset-template](ktlint-ruleset-template) directory 
(make sure to check [NoVarRuleTest](ktlint-ruleset-template/src/test/kotlin/yourpkgname/NoVarRuleTest.kt) as it contains some useful information). 

## FAQ

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

```sh
git clone https://github.com/shyiko/ktlint && cd ktlint
./mvnw # shows how to build, test, etc. project
```

## Legal

This project is not affiliated with or endorsed by the Jetbrains.  
All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2016 Stanley Shyiko.
