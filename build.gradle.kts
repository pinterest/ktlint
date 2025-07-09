import java.net.URI

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.checksum) apply false
    alias(libs.plugins.shadow) apply false
    alias(
        libs
            .plugins
            .kotlinx
            .binary
            .compatibiltiy
            .validator,
    )
}

val internalNonPublishableProjects by extra(
    setOf(
        "ktlint-api-consumer",
        "ktlint-bom",
        "ktlint-ruleset-template",
    ),
)

apiValidation {
    ignoredProjects += internalNonPublishableProjects
}

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(projects.ktlintCli)
}

tasks.register<JavaExec>("ktlintCheck") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
        // Do not run with option "--log-level=debug" or "--log-level=trace" as the lint violations will be difficult
        // to spot between the amount of output lines.
    )
}

tasks.register<JavaExec>("ktlintFormat") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    // Suppress "sun.misc.Unsafe::objectFieldOffset" on Java24 (warning) (https://github.com/pinterest/ktlint/issues/2973)
    // jvmArgs("--sun-misc-unsafe-memory-access=allow") // Java 24+
    args(
        "-F",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
        // Do not run with option "--log-level=debug" or "--log-level=trace" as the lint violations will be difficult
        // to spot between the amount of output lines.
    )
}

tasks.wrapper {
    distributionSha256Sum =
        URI
            .create("$distributionUrl.sha256")
            .toURL()
            .openStream()
            .use { it.reader().readText().trim() }
}
