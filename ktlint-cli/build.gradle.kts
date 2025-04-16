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
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintCliReporterBaseline)
    implementation(projects.ktlintCliReporterCore)
    implementation(projects.ktlintCliReporterPlain)
    implementation(projects.ktlintRuleEngine)
    implementation(projects.ktlintRulesetStandard)
    implementation(libs.clikt)
    implementation(libs.logback)

    runtimeOnly(projects.ktlintCliReporterCheckstyle)
    runtimeOnly(projects.ktlintCliReporterJson)
    runtimeOnly(projects.ktlintCliReporterFormat)
    runtimeOnly(projects.ktlintCliReporterHtml)
    runtimeOnly(projects.ktlintCliReporterPlainSummary)
    runtimeOnly(projects.ktlintCliReporterSarif)

    testImplementation(projects.ktlintTest)

    testImplementation(libs.junit5.jupiter)
    // Since Gradle 8 the platform launcher needs explicitly be defined as runtime dependency to avoid classpath problems
    // https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
    testRuntimeOnly(libs.junit5.platform.launcher)
}

// Implements https://github.com/brianm/really-executable-jars-maven-plugin maven plugin behaviour.
// To check details how it works, see https://skife.org/java/unix/2011/06/20/really_executable_jars.html.
val shadowJarExecutable by tasks.registering(DefaultTask::class) {
    description = "Creates self-executable file, that runs generated shadow jar"
    group = "Distribution"

    dependsOn(tasks.shadowJar)

    val isReleaseBuild = !version.toString().endsWith("SNAPSHOT")

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

    val windowsBatchFileInputPath = "$projectDir/src/main/scripts/ktlint.bat"
    inputs.files(windowsBatchFileInputPath)

    // Output is the self-executable file
    val outputDirectoryPath = "$buildDir/run"
    val selfExecutableKtlintOutputPath = "$outputDirectoryPath/ktlint"
    val selfExecutableKtlintSignatureOutputPath = "$outputDirectoryPath/ktlint.asc"
    val windowsBatchFileOutputPath = "$outputDirectoryPath/ktlint.bat"
    outputs.files(selfExecutableKtlintOutputPath)
    if (isReleaseBuild) {
        // And for releases also the signature file and a batch file for Windows OS
        outputs.files(selfExecutableKtlintSignatureOutputPath)
        outputs.files(windowsBatchFileOutputPath)
    }

    doLast {
        File(selfExecutableKtlintOutputPath).apply {
            logger.lifecycle("Creating the self-executable file: $selfExecutableKtlintOutputPath")

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

            if (isReleaseBuild) {
                logger.lifecycle("Creating the signature file: $selfExecutableKtlintSignatureOutputPath")
                signing.sign(this)
            }
        }
        if (isReleaseBuild) {
            logger.lifecycle("Creating the batch file for Windows OS: $windowsBatchFileOutputPath")
            File(windowsBatchFileOutputPath).apply {
                writeText(File(windowsBatchFileInputPath).readText())
            }
        }
        logger.lifecycle("Finished creating output files ktlint-cli")
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
            .map {
                it
                    .outputs
                    .files
                    .files
                    .first()
                    .parentFile
            },
    )
    checksumAlgorithm = Checksum.Algorithm.MD5
}

tasks.withType<Test>().configureEach {
    dependsOn(shadowJarExecutable)

    // TODO: Use providers directly after https://github.com/gradle/gradle/issues/12247 is fixed.
    val executableFilePath =
        providers
            .provider {
                shadowJarExecutable
                    .get()
                    .outputs
                    .files
                    .first { it.name == "ktlint" }
                    .absolutePath
            }.get()
    val ktlintVersion = providers.provider { version }.get()
    doFirst {
        systemProperty(
            "ktlint-cli",
            executableFilePath,
        )
        systemProperty("ktlint-version", ktlintVersion)
    }
}
