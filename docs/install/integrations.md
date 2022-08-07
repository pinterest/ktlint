## [Maven](https://github.com/shyiko/mvnw) integration

By adding the plugin definition below to the `<plugins>` section in the `pom.xml`:

* The `ktlint` task is bound to the *Maven verify* lifecycle and will be executed each time the `mvn verify` is executed. It can also be executed with command `mvn antrun:run@ktlint`.
* The `ktlint-format` task is not bound to any other maven lifecycle. It can be executed with command `mvn antrun:run@ktlint-format`.

See [cli usage](../cli) for arguments that can be supplied to `ktlint`.

```xml title="Adding plugin to pom.xml"
...
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-antrun-plugin</artifactId>
    <version>3.1.0</version>
    <executions>
        <execution>
            <id>ktlint</id>
            <phase>verify</phase>
            <configuration>
            <target name="ktlint">
                <java taskname="ktlint" dir="${basedir}" fork="true" failonerror="true"
                    classpathref="maven.plugin.classpath" classname="com.pinterest.ktlint.Main">
                    <arg value="src/**/*.kt"/>
                    <!-- see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information -->
                </java>
            </target>
            </configuration>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
        <execution>
            <id>ktlint-format</id>
            <configuration>
            <target name="ktlint">
                <java taskname="ktlint" dir="${basedir}" fork="true" failonerror="true"
                    classpathref="maven.plugin.classpath" classname="com.pinterest.ktlint.Main">
                    <arg value="-F"/>
                    <arg value="src/**/*.kt"/>
                    <!-- see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information -->
                </java>
            </target>
            </configuration>
            <goals>
                <goal>run</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>com.pinterest</groupId>
            <artifactId>ktlint</artifactId>
            <version>0.46.1</version>
        </dependency>
        <!-- additional 3rd party ruleset(s) can be specified here -->
    </dependencies>
</plugin>
...
```

!!! Tip
    If you want ktlint to run before code compilation takes place - change `<phase>verify</phase>` to `<phase>validate</phase>` (see [Maven Build Lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html) for more).

!!! Info "ktlint-maven-plugin"
    You might be interested to use the dedicated [gantsign/ktlint-maven-plugin](https://github.com/gantsign/ktlint-maven-plugin).

## [Gradle](https://gradle.org/) integration

### jlleitschuh/ktlint-gradle

The [jlleitschuh/ktlint-gradle](https://github.com/jlleitschuh/ktlint-gradle) Gradle plugin automatically creates check and format tasks for project Kotlin sources. It supports different kotlin plugins and Gradle build caching.

### jeremymailen/kotlinter-gradle

The [jeremymailen/kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle) Gradle plugin features incremental build support, file reports, and `*.kts` source support.

### diffplug/spotless

The [diffplug/spotless](https://github.com/diffplug/spotless/tree/master/plugin-gradle#applying-ktlint-to-kotlin-files) Gradle plugin is a general-purpose formatting plugin which amongst many others also supports `ktlint`.

### autostyle/autostyle

The [autostyle/autostyle](https://github.com/autostyle/autostyle/tree/master/plugin-gradle#applying-ktlint-to-kotlin-files) Gradle plugin is a general-purpose formatting plugin which amongst others also supports `ktlint`. 

### Custom Gradle integration

#### Custom Gradle integration with Groovy

!!! Warning
    It is recommended to use one of the Gradle plugins mentioned before.

The configuration below, defines following task:

* The `ktlint` is bound to the *Gradle check* task. It can also be executed with command `gradle ktlint`.
* The `ktlint-format` task is not bound to any other task. It can be executed with command `gradle ktlintFormat`.

```groovy title="build.gradle"
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
    ktlint("com.pinterest:ktlint:0.46.1") {
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
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
}
check.dependsOn ktlint

task ktlintFormat(type: JavaExec, group: "formatting") {
    description = "Fix Kotlin code style deviations."
    classpath = configurations.ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args "-F", "src/**/*.kt"
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
}
```

See [Making your Gradle tasks incremental](https://proandroiddev.com/making-your-gradle-tasks-incremental-7f26e4ef09c3) by [Niklas Baudy](https://github.com/vanniktech) on how to make tasks above incremental.

#### Custom Gradle integration with Kotlin DSL 

!!! Warning
    It is recommended to use one of the Gradle plugins mentioned before.

The configuration below, defines following task:

* The `ktlint` is bound to the *Gradle check* task. It can also be executed with command `gradle ktlint`.
* The `ktlint-format` task is not bound to any other task. It can be executed with command `gradle ktlintFormat`.

```kotlin title="build.gradle.kts"
val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest:ktlint:0.46.1") {
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
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Fix Kotlin code style deviations."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args = listOf("-F", "src/**/*.kt")
}
```

## [GNU Emacs](https://www.gnu.org/software/emacs/) integration

See [whirm/flycheck-kotlin](https://github.com/whirm/flycheck-kotlin).

## [Vim](https://www.vim.org/) integration

See [w0rp/ale](https://github.com/w0rp/ale).

## [Mega-Linter](https://nvuillam.github.io/mega-linter/) integration

The [Mega-Linter](https://nvuillam.github.io/mega-linter/) integrates 70+ linters in a single tool for CI, including **ktlint** activated out of the box

## Other integration

Do you know any other integration with `ktlint` then please create a PR to add this integration to our documentation.
