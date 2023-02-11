import java.net.URL

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.checksum) apply false
    alias(libs.plugins.shadow) apply false
}

allprojects {
    if (project.hasProperty("isKotlinDev")) {
        val definedVersion = ext["VERSION_NAME"].toString().removeSuffix("-SNAPSHOT")
        ext["VERSION_NAME"] = "$definedVersion-kotlin-dev-SNAPSHOT"
    }

    val skipTests: String = System.getProperty("skipTests", "false")
    tasks.withType<Test>().configureEach {
        if (skipTests == "false") {
            useJUnitPlatform()
        } else {
            logger.warn("Skipping tests for task '$name' as system property 'skipTests=$skipTests'")
        }

        val args = mutableSetOf<String>()
        if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_16)) {
            // https://docs.gradle.org/7.5/userguide/upgrading_version_7.html#removes_implicit_add_opens_for_test_workers
            args += listOf(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
            )
        }
        if (JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_18)) {
            // https://openjdk.org/jeps/411
            args += "-Djava.security.manager=allow"
        }
        jvmArgs(args)
    }
}

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(projects.ktlint)
}

tasks.register<JavaExec>("ktlint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style including experimental rules."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
        // Exclude sources which contain lint violations for the purpose of testing.
        "!ktlint/src/test/resources/**",
        "--baseline=ktlint/src/test/resources/test-baseline.xml",
        // Experimental rules run by default run on the ktlint code base itself. Experimental rules should not be released if
        // we are not pleased ourselves with the results on the ktlint code base.
        "--experimental",
        // Do not run with option "--verbose" or "-v" as the lint violations are difficult to spot between the amount of
        // debug output lines.
    )
}

tasks.wrapper {
    distributionSha256Sum = URL("$distributionUrl.sha256")
        .openStream().use { it.reader().readText().trim() }
}
