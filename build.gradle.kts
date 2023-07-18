import java.net.URL

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.checksum) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.kotlinx.binary.compatibiltiy.validator)
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
    mainClass.set("com.pinterest.ktlint.Main")
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
    mainClass.set("com.pinterest.ktlint.Main")
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
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
        URL("$distributionUrl.sha256")
            .openStream().use { it.reader().readText().trim() }
}
