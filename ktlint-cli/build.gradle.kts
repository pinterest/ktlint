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

// Output is the self-executable file, the signature file and a batch file for Windows OS.
// Folder content is published as GitHub release artifacts
val ktlintOutputRoot = layout.buildDirectory.dir("run")

val shadowJarExecutable by tasks.registering(ShadowJarExecutableTask::class) {
    dependsOn(tasks.shadowJar)

    // Find the "ktlint-cli-<version>-all.jar" file
    val ktlintCliAllJarFile =
        tasks.shadowJar
            .get()
            .archiveFile
            .get()
    allJarFile.set(ktlintCliAllJarFile)
    windowsBatchScriptSource.set(layout.projectDirectory.file("src/main/scripts/ktlint.bat"))
    outputDirectory.set(ktlintOutputRoot)

    finalizedBy("signShadowJarExecutable", "shadowJarExecutableChecksum")
}

val signShadowJarExecutable by tasks.registering(Sign::class) {
    dependsOn(shadowJarExecutable)

    sign(shadowJarExecutable.flatMap { it.selfExecutable }.get())
}

tasks.register<Checksum>("shadowJarExecutableChecksum") {
    description = "Generates MD5 checksum for ktlint executable"
    group = "Distribution"

    dependsOn(shadowJarExecutable)
    dependsOn(signShadowJarExecutable)

    inputFiles.from(shadowJarExecutable.map { it.selfExecutable })
    inputFiles.from(shadowJarExecutable.map { it.windowsBatchScript })

    // put the checksums in the same folder with the executable itself
    outputDirectory.set(ktlintOutputRoot)
    checksumAlgorithm = Checksum.Algorithm.MD5
}

tasks.withType<Test>().configureEach {
    dependsOn(shadowJarExecutable)

    // TODO: Use providers directly after https://github.com/gradle/gradle/issues/12247 is fixed.
    val executableFilePath = shadowJarExecutable.flatMap { it.selfExecutable }.map { it.absolutePath }.get()
    val ktlintVersion = providers.provider { version }.get()
    doFirst {
        systemProperty(
            "ktlint-cli",
            executableFilePath,
        )
        systemProperty("ktlint-version", ktlintVersion)
    }
}
