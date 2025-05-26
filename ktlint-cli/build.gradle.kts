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
val shadowJarExecutable by tasks.registering(ShadowJarExecutableTask::class) {
    dependsOn(tasks.shadowJar)
    selfExecutable.fileProvider(tasks.shadowJar.map { it.outputs.files.singleFile })
    windowsBatchFileInputPath.value(layout.projectDirectory.file("src/main/scripts/ktlint.bat"))
    signing.sign(selfExecutableKtlintSignatureOutputPath.get())
}

tasks.register<Checksum>("shadowJarExecutableChecksum") {
    description = "Generates MD5 checksum for ktlint executable"
    group = "Distribution"

    dependsOn(shadowJarExecutable)

    inputFiles.setFrom(shadowJarExecutable.map { it.outputs.files })
    // put the checksums in the same folder with the executable itself
    outputDirectory.fileProvider(shadowJarExecutable.flatMap { it.selfExecutable }.map { it.asFile.parentFile })
    checksumAlgorithm = Checksum.Algorithm.MD5
}

publishing {
    // option 1: append to existing publication with classifier
    publications.named<MavenPublication>("maven") {
        artifact(tasks.named<ShadowJarExecutableTask>("shadowJarExecutable").map { it.selfExecutable }) {
            classifier = "exec"
        }
    }

    // option 2: create separate publication
    publications.register<MavenPublication>("fatJar") {
        artifactId = "ktlint-cli-fatjar"

        artifact(tasks.named("sourcesJar"))
        artifact(tasks.named("javadocJar"))
        artifact(tasks.named<ShadowJarExecutableTask>("shadowJarExecutable").map { it.selfExecutable })
        signing.sign(this)
    }
}

tasks.withType<Test>().configureEach {
    dependsOn(shadowJarExecutable)

    // TODO: Use providers directly after https://github.com/gradle/gradle/issues/12247 is fixed.
    val executableFilePath = shadowJarExecutable.flatMap { it.selfExecutableKtlintOutputPath }.get()
    val ktlintVersion = providers.provider { version }.get()
    doFirst {
        systemProperty(
            "ktlint-cli",
            executableFilePath.absolutePath,
        )
        systemProperty("ktlint-version", ktlintVersion)
    }
}
