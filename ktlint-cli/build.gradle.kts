import org.gradle.crypto.checksum.Checksum

plugins {
    id("ktlint-publication-library")
    alias(libs.plugins.shadow)
    alias(libs.plugins.checksum)
    signing
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.pinterest.ktlint.Main")
        attributes("Implementation-Version" to version)
    }
}

tasks.shadowJar {
    mergeServiceFiles()
}

dependencies {
    implementation(projects.ktlintCore)
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintCliReporterBaseline)
    implementation(projects.ktlintCliReporterCore)
    implementation(projects.ktlintCliReporterPlain)
    implementation(projects.ktlintRuleEngine)
    implementation(projects.ktlintRulesetStandard)
    implementation(libs.picocli)
    implementation(libs.logback)

    runtimeOnly(projects.ktlintCliReporterCheckstyle)
    runtimeOnly(projects.ktlintCliReporterJson)
    runtimeOnly(projects.ktlintCliReporterFormat)
    runtimeOnly(projects.ktlintCliReporterHtml)
    runtimeOnly(projects.ktlintCliReporterPlainSummary)
    runtimeOnly(projects.ktlintCliReporterSarif)

    testImplementation(projects.ktlintTest)
}

// Implements https://github.com/brianm/really-executable-jars-maven-plugin maven plugin behaviour.
// To check details how it works, see https://skife.org/java/unix/2011/06/20/really_executable_jars.html.
val shadowJarExecutable by tasks.registering(DefaultTask::class) {
    description = "Creates self-executable file, that runs generated shadow jar"
    group = "Distribution"

    inputs.files(tasks.shadowJar.map { it.outputs.files })
    outputs.files("$buildDir/run/ktlint")
    if (!version.toString().endsWith("SNAPSHOT")) {
        outputs.files("$buildDir/run/ktlint.asc")
    }

    doLast {
        val execFile = outputs.files.first()
        execFile.appendText(
            """
            #!/bin/sh

            # From this SO answer: https://stackoverflow.com/a/56243046

            # First we get the major Java version as an integer, e.g. 8, 11, 16. It has special handling for the leading 1 of older java
            # versions, e.g. 1.8 = Java 8
            JV=$(java -version 2>&1 | sed -E -n 's/.* version "([^.-]*).*".*/\1/p')

            # Add --add-opens for java version 16 and above
            X=$( [ "${"$"}JV" -ge "16" ] && echo "--add-opens java.base/java.lang=ALL-UNNAMED" || echo "")

            exec java ${"$"}X -Xmx512m -jar "$0" "$@"

            """.trimIndent(),
        )

        execFile.appendBytes(inputs.files.singleFile.readBytes())
        execFile.setExecutable(true, false)
    }
    finalizedBy(tasks.named("shadowJarExecutableChecksum"))
}

tasks.signMavenPublication {
    dependsOn(shadowJarExecutable)
    if (!version.toString().endsWith("SNAPSHOT")) {
        sign(*shadowJarExecutable.map { it.outputs.files.files }.get().toTypedArray())
    }
}

tasks.register<Checksum>("shadowJarExecutableChecksum") {
    description = "Generates MD5 checksum for ktlint executable"
    group = "Distribution"

    inputFiles.setFrom(shadowJarExecutable.map { it.outputs.files })
    // put the checksums in the same folder with the executable itself
    outputDirectory.fileProvider(shadowJarExecutable.map { it.outputs.files.files.first().parentFile })
    checksumAlgorithm.set(Checksum.Algorithm.MD5)
}

tasks.withType<Test>().configureEach {
    dependsOn(shadowJarExecutable)

    // TODO: Use providers directly after https://github.com/gradle/gradle/issues/12247 is fixed.
    val executableFilePath =
        providers.provider { shadowJarExecutable.get().outputs.files.first { it.name == "ktlint" }.absolutePath }.get()
    val ktlintVersion = providers.provider { version }.get()
    doFirst {
        systemProperty(
            "ktlint-cli",
            executableFilePath,
        )
        systemProperty("ktlint-version", ktlintVersion)
    }
}
