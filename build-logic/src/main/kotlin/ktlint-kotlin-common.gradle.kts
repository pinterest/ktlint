import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

kotlin {
    // All modules, the CLI included, must have an explicit API
    explicitApi()
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

// compileJava task and compileKotlin task jvm target compatibility should be set to the same Java version.
// For some reason, we fallback from toolchain using, see https://github.com/pinterest/ktlint/pull/1787
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
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

    if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
        // https://docs.gradle.org/7.5/userguide/upgrading_version_7.html#removes_implicit_add_opens_for_test_workers
        jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    }
}
