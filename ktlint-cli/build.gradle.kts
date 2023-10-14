import org.gradle.crypto.checksum.Checksum
import java.net.URI

plugins {
    id("ktlint-publication-library")
    alias(libs.plugins.shadow)
    alias(libs.plugins.checksum)
    alias(libs.plugins.sdkman)
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

sdkman {
    val sdkmanVersion = providers.environmentVariable("SDKMAN_VERSION").orElse(project.version.toString())
    candidate = "ktlint"
    version = sdkmanVersion
    url = "https://github.com/pinterest/ktlint/releases/download/$sdkmanVersion/ktlint-$sdkmanVersion.zip"
    hashtag = "ktlint"
}

// TODO: Remove in release after ktlint 1.0.1
publishing {
    publications {
        create<MavenPublication>("relocation-ktlint-cli") {
            pom {
                // Old artifact coordinates ktlint-cli
                groupId = "com.pinterest"
                artifactId = "ktlint"
                version = "0.51.0-FINAL"

                name = artifactId
                description = providers.gradleProperty("POM_DESCRIPTION")
                url = providers.gradleProperty("POM_URL")
                licenses {
                    license {
                        name = providers.gradleProperty("POM_LICENSE_NAME")
                        url = providers.gradleProperty("POM_LICENSE_URL")
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = providers.gradleProperty("POM_DEVELOPER_ID")
                        name = providers.gradleProperty("POM_DEVELOPER_NAME")
                    }
                }
                scm {
                    url = providers.gradleProperty("POM_SCM_URL")
                    connection = providers.gradleProperty("POM_SCM_CONNECTION")
                    developerConnection = providers.gradleProperty("POM_SCM_DEV_CONNECTION")
                }

                distributionManagement {
                    relocation {
                        // New artifact coordinates
                        groupId.set("com.pinterest.ktlint")
                        artifactId.set("ktlint-cli")
                        version.set("1.0.0")
                        message.set("groupId and artifactId have been changed")
                    }
                }

                repositories {
                    maven {
                        name = "mavenCentral"
                        url = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

                        logger.lifecycle("Create relocation pom.xml for ktlint-cli and upload to $url")

                        credentials {
                            username = providers.gradleProperty("SONATYPE_NEXUS_USERNAME").orNull
                                ?: System.getenv("SONATYPE_NEXUS_USERNAME")
                            password = providers.gradleProperty("SONATYPE_NEXUS_PASSWORD").orNull
                                ?: System.getenv("SONATYPE_NEXUS_PASSWORD")
                        }
                    }
                }
            }
        }
    }
}

// TODO: Remove in release after ktlint 1.0.1
signing {
    logger.lifecycle("Sign relocation pom.xml for ktlint-cli")

    // Uncomment following line to use gpg-agent for signing
    // See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent how to configure it
    // useGpgCmd()
    val signingKeyId = System.getenv("ORG_GRADLE_PROJECT_signingKeyId")
    val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingKeyPassword")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

    sign(publishing.publications["relocation-ktlint-cli"])

    isRequired = true
}
