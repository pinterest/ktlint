plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId) apply false
    alias(libs.plugins.checksum)
    alias(libs.plugins.shadow)
    alias(libs.plugins.githubRelease)
}

val isKotlinDev: Boolean = project.hasProperty("isKotlinDev")

allprojects {
    if (isKotlinDev) {
        val definedVersion = ext["VERSION_NAME"].toString().removeSuffix("-SNAPSHOT")
        ext["VERSION_NAME"] = "$definedVersion-kotlin-dev-SNAPSHOT"
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

val ktlint: Configuration = configurations.create("ktlint")

dependencies {
    ktlint(projects.ktlint)
}

tasks.register<JavaExec>("ktlint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style including experimental rules."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    // Experimental rules run by default run on the ktlint code base itself. Experimental rules should not be released if
    // we are not pleased ourselves with the results on the ktlint code base.
    // Sources in "ktlint/src/test/resources" are excluded as those source contain lint errors that have to be detected by
    // unit tests and should not be reported/fixed.
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
        "!ktlint/src/test/resources/**",
        "--baseline=ktlint/src/test/resources/test-baseline.xml",
        "--experimental",
        "--verbose"
    )
}

// Deployment tasks
val githubToken: String = if (project.hasProperty("servers.github.privKey")) {
    project.property("servers.github.privKey").toString()
} else {
    logger.warn("No github token specified")
    ""
}

val shadowJarExecutable: Task by lazy {
    projects.ktlint.dependencyProject.tasks["shadowJarExecutable"]
}

// Explicitly adding dependency on "shadowJarExecutable" as Gradle does not it set via "releaseAssets" property
tasks.githubRelease {
    dependsOn({
        shadowJarExecutable
    })
}

githubRelease {
    token(githubToken)
    owner("pinterest")
    repo("ktlint")
    tagName(project.property("VERSION_NAME").toString())
    releaseName(project.property("VERSION_NAME").toString())
    releaseAssets(
        project.files(
            {
                // "shadowJarExecutableChecksum" task does not declare checksum files
                // as output, only the whole output directory. As it uses the same directory
                // as "shadowJarExecutable" - just pass all the files from that directory
                shadowJarExecutable.outputs.files.files.first().parentFile.listFiles()
            }
        )
    )
    overwrite(true)
    dryRun(false)
    body {
        var changelog = project.file("CHANGELOG.md").readText()
        changelog = changelog.substring(changelog.indexOf("## "))
        // 1 in indexOf here to skip first "## [" occurence
        changelog.substring(0, changelog.indexOf("## [", 1))
    }
}

// Put "servers.github.privKey" in "$HOME/.gradle/gradle.properties".
val announceRelease by tasks.registering(Exec::class) {
    group = "Help"
    description = "Runs .announce script"
    subprojects.filter { !it.name.contains("ktlint-ruleset-template") }.forEach { subproject ->
        dependsOn(subproject.tasks["publishMavenPublicationToMavenCentralRepository"])
    }

    commandLine("./.announce", "-y")
    environment("VERSION" to "${project.property("VERSION_NAME")}")
    environment("GITHUB_TOKEN" to githubToken)
}

val homebrewBumpFormula by tasks.registering(Exec::class) {
    group = "Help"
    description = "Runs brew bump-forumula-pr"
    commandLine("./.homebrew")
    environment("VERSION" to "${project.property("VERSION_NAME")}")
    dependsOn(tasks.githubRelease)
}

tasks.register<DefaultTask>("publishNewRelease") {
    group = "Help"
    description = "Triggers uploading new archives and publish announcements"
    dependsOn(announceRelease, homebrewBumpFormula, tasks.githubRelease)
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionSha256Sum = libs.versions.gradleSha256.get()
    distributionType = Wrapper.DistributionType.BIN
}
