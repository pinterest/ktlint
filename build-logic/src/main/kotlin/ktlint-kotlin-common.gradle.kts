import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("dev.drewhamilton.poko")
}

val projectLibs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val javaCompilationVersion = JavaLanguageVersion.of(projectLibs.findVersion("java-compilation").get().requiredVersion)
val javaTargetVersion = JavaLanguageVersion.of(projectLibs.findVersion("java-target").get().requiredVersion)

kotlin {
    // All modules, the CLI included, must have an explicit API
    explicitApi()
    jvmToolchain(jdkVersion = javaCompilationVersion.asInt())
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaTargetVersion.asInt())
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(JavaVersion.toVersion(javaTargetVersion).toString()))
}

val requestedJdkVersion = project.findProperty("testJdkVersion")?.toString()?.toInt()
if (requestedJdkVersion != null) {
    tasks.register<Test>("testOnJdk") {
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(requestedJdkVersion) }

        // Copy inputs from normal Test task.
        val testTask = tasks.test.get()
        classpath = testTask.classpath
        testClassesDirs = testTask.testClassesDirs
    }
}

val skipTests: String = providers.systemProperty("skipTests").getOrElse("false")
tasks.withType<Test>().configureEach {
    if (skipTests == "false") {
        useJUnitPlatform()
    } else {
        logger.warn("Skipping tests for task '$name' as system property 'skipTests=$skipTests'")
    }

    maxParallelForks =
        if (System.getenv("CI") != null) {
            Runtime.getRuntime().availableProcessors()
        } else {
            // https://docs.gradle.org/8.0/userguide/performance.html#execute_tests_in_parallel
            (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
        }

    if (javaLauncher.get().metadata.languageVersion.canCompileOrRun(JavaLanguageVersion.of(11))) {
        // workaround for https://github.com/pinterest/ktlint/issues/1618. Java 11 started printing warning logs. Java 16 throws an error
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }
}
