plugins {
    id("ktlint-publication-library")
    alias(libs.plugins.shadow)
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

val ktlintCliFiles by tasks.registering(KtlintCliTask::class) {
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

    finalizedBy("signktlintCliFiles")
}

val signktlintCliFiles by tasks.registering(Sign::class) {
    dependsOn(ktlintCliFiles)

    sign(ktlintCliFiles.flatMap { it.ktlintCliExecutable }.get())
}

tasks.withType<Test>().configureEach {
    dependsOn(ktlintCliFiles)

    // TODO: Use providers directly after https://github.com/gradle/gradle/issues/12247 is fixed.
    val ktlintCliExecutableFilePath = ktlintCliFiles.flatMap { it.ktlintCliExecutable }.map { it.absolutePath }.get()
    val ktlintVersion = providers.provider { version }.get()
    doFirst {
        systemProperty(
            "ktlint-cli",
            ktlintCliExecutableFilePath,
        )
        systemProperty("ktlint-version", ktlintVersion)
    }
}
