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
    options.release = javaTargetVersion.asInt()
}
tasks.withType<KotlinCompile>().configureEach {
    // Convert Java version (e.g. "1.8" or "11") to Kotlin JvmTarget ("8" resp. "11")
    compilerOptions.jvmTarget = JvmTarget.fromTarget(JavaVersion.toVersion(javaTargetVersion).toString())
}

val requestedJdkVersion = project.findProperty("testJdkVersion")?.toString()?.toInt()
// List all non-current Java versions the developers may want to run via IDE click
setOfNotNull(8, 11, 17, requestedJdkVersion).forEach { version ->
    tasks.register<Test>("testOnJdk$version") {
        javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(version) }

        description = "Runs the test suite on JDK $version"
        group = LifecycleBasePlugin.VERIFICATION_GROUP

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

    if (javaLauncher
            .get()
            .metadata
            .languageVersion
            .canCompileOrRun(JavaLanguageVersion.of(11))
    ) {
        // workaround for https://github.com/pinterest/ktlint/issues/1618. Java 11 started printing warning logs. Java 16 throws an error
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
        // Suppress warning "sun.misc.Unsafe::objectFieldOffset" on Java24+ (https://github.com/pinterest/ktlint/issues/2973)
        // jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }
}
