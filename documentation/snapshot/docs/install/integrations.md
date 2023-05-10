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
                  <!-- Note: the JVM arg below is only required when running ktlint with Java 16+ in format mode.
                  <jvmarg value="--add-opens=java.base/java.lang=ALL-UNNAMED"/>
                  -->
                  <!-- see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information -->
                  <arg value="src/**/*.kt"/>
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
                    <!-- Note: the JVM args below is only required when running ktlint with Java 16+ in format mode -->
                    <jvmarg value="--add-opens=java.base/java.lang=ALL-UNNAMED"/>
                    <!-- see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information -->
                    <arg value="-F"/>
                    <arg value="src/**/*.kt"/>
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
            <version>0.49.0</version>
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

* The `ktlintCheck` is bound to the *Gradle check* task. It can also be executed with command `./gradlew ktlintCheck`.
* The `ktlintFormat` task is not bound to any other task. It can be executed with command `./gradlew ktlintFormat`.

```groovy title="build.gradle"
// kotlin-gradle-plugin must be applied for configuration below to work
// (see https://kotlinlang.org/docs/reference/using-gradle.html)

plugins {
    id 'java'
}

repositories {
    mavenCentral()
}

configurations {
    ktlint
}

dependencies {
    ktlint("com.pinterest:ktlint:0.49.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, getObjects().named(Bundling, Bundling.EXTERNAL))
        }
    }
    // additional 3rd party ruleset(s) can be specified here
    // just add them to the classpath (e.g. ktlint 'groupId:artifactId:version') and 
    // ktlint will pick them up
}

tasks.register("ktlintCheck", JavaExec) {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = configurations.ktlint
    mainClass = "com.pinterest.ktlint.Main"
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args "src/**/*.kt", "**.kts", "!**/build/**"
}

tasks.named("check") {
    dependsOn tasks.named("ktlintCheck")
}

tasks.register("ktlintFormat", JavaExec) {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = configurations.ktlint
    mainClass = "com.pinterest.ktlint.Main"
    jvmArgs "--add-opens=java.base/java.lang=ALL-UNNAMED"
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args "-F", "src/**/*.kt", "**.kts", "!**/build/**"
}
```

See [Making your Gradle tasks incremental](https://proandroiddev.com/making-your-gradle-tasks-incremental-7f26e4ef09c3) by [Niklas Baudy](https://github.com/vanniktech) on how to make tasks above incremental.

#### Custom Gradle integration with Kotlin DSL 

!!! Warning
    It is recommended to use one of the Gradle plugins mentioned before.

The configuration below, defines following task:

* The `ktlintCheck` is bound to the *Gradle check* task. It can also be executed with command `./gradlew ktlintCheck`.
* The `ktlintFormat` task is not bound to any other task. It can be executed with command `./gradlew ktlintFormat`.

```kotlin title="build.gradle.kts"
val ktlint by configurations.creating

dependencies {
    ktlint("com.pinterest:ktlint:0.49.0") {
        attributes {
            attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        }
    }
    // ktlint(project(":custom-ktlint-ruleset")) // in case of custom ruleset
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
    )
}

tasks.check {
    dependsOn(ktlintCheck)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
    )
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
