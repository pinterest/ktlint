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

    dependsOn(tasks.shadowJar)

    // Find the "ktlint-cli-<version>-all.jar" file
    val ktlintCliAllJarFile =
        tasks
            .shadowJar
            .orNull
            ?.outputs
            ?.files
            ?.singleFile
            ?: throw GradleException("Can not locate the jar file for building the self-executable ktlint-cli")
    logger.lifecycle("ktlint-cli: Base jar to build self-executable file: ${ktlintCliAllJarFile.absolutePath}")
    inputs.files(ktlintCliAllJarFile)

    // Output is the self-executable file
    val selfExecutableKtlintPath = "$buildDir/run/ktlint"
    outputs.files(selfExecutableKtlintPath)
    if (!version.toString().endsWith("SNAPSHOT")) {
        // And for releases also the signature file
        outputs.files("$buildDir/run/ktlint.asc")
    }

    doLast {
        logger.lifecycle("Creating the self-executable ktlint-cli")
        File(selfExecutableKtlintPath).apply {
            // writeText effective replaces the entire content if the file already exists. If appendText is used, the file keeps on growing
            // with each build if the clean target is not used.
            writeText(
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
            // Add the jar
            appendBytes(ktlintCliAllJarFile.readBytes())

            setExecutable(true, false)
        }
        logger.lifecycle("Finished creating the self-executable ktlint-cli")
    }
}

tasks.register<Checksum>("shadowJarExecutableChecksum") {
    description = "Generates MD5 checksum for ktlint executable"
    group = "Distribution"

    dependsOn(shadowJarExecutable)

    inputFiles.setFrom(shadowJarExecutable.map { it.outputs.files })
    // put the checksums in the same folder with the executable itself
    outputDirectory.fileProvider(
        shadowJarExecutable
            .also { logger.lifecycle("registerChecksum - Set output files on shadowJarExecutableChecksum:") }
            .map {
                it
                    .outputs
                    .also { logger.lifecycle("registerChecksum - TasksOutputInternal contains ${it.files.count()} fileCollections") }
                    .files
                    .also {
                        logger.lifecycle(
                            it.joinToString(prefix = "registerChecksum - Files [${it.asPath}]: ", separator = ", ") { it.path },
                        )
                    }.files
                    .also { logger.lifecycle("registerChecksum - File set contains ${it.count()} files") }
                    .first()
                    .also { logger.lifecycle("registerChecksum - First file: ${it.path}") }
                    .parentFile
                    .also { logger.lifecycle("registerChecksum - Parent file: ${it.path}") }
            },
    )
    checksumAlgorithm.set(Checksum.Algorithm.MD5)
}

tasks.signMavenPublication {
    dependsOn(shadowJarExecutable)
    if (!version.toString().endsWith("SNAPSHOT")) {
        // Just need to sign execFile.
        val ktlintSelfExecutable =
            shadowJarExecutable
                .orNull
                ?.outputs
                ?.files
                ?.files
                ?.filterNot {
                    // Ignore the signature file
                    it.path.endsWith(".asc")
                }?.single()
                ?: throw GradleException("Can not locate the self-executable ktlint-cli to be signed")
        logger.lifecycle("Before sign of ${ktlintSelfExecutable.path} in signMavenPublication")
        sign(ktlintSelfExecutable)
        logger.lifecycle("After sign of ${ktlintSelfExecutable.path} in signMavenPublication")
    }
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
