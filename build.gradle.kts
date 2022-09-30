plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.checksum)
    alias(libs.plugins.shadow)
    alias(libs.plugins.githubRelease)
}

val isKotlinDev = project.hasProperty("isKotlinDev")

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

// Deployment tasks
fun getGithubToken(): String {
    return if (project.hasProperty("servers.github.privKey")) {
        project.property("servers.github.privKey").toString()
    } else {
        logger.warn("No github token specified")
        ""
    }
}

// Explicitly adding dependency on "shadowJarExecutable" as Gradle does not it set via "releaseAssets" property
tasks.named("githubRelease") {
    dependsOn(provider { projects.ktlint.dependencyProject.tasks.named("shadowJarExecutable") })
}

githubRelease {
    token(getGithubToken())
    owner("pinterest")
    repo("ktlint")
    tagName(project.properties["VERSION_NAME"].toString())
    releaseName(project.properties["VERSION_NAME"].toString())
    targetCommitish("master")
    releaseAssets(
        project.files(
            provider {
                // "shadowJarExecutableChecksum" task does not declare checksum files
                // as output, only the whole output directory. As it uses the same directory
                // as "shadowJarExecutable" - just pass all the files from that directory
                projects.ktlint.dependencyProject.tasks.named("shadowJarExecutable").get()
                    .outputs.files.files.first().parentFile.listFiles()
            },
        ),
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
    subprojects.filter {
        !it.name.contains("ktlint-ruleset-template")
    }.forEach { subproject ->
        dependsOn(subproject.tasks.named("publishMavenPublicationToMavenCentralRepository"))
    }

    commandLine("./.announce", "-y")
    environment("VERSION" to "${project.property("VERSION_NAME")}")
    environment("GITHUB_TOKEN" to getGithubToken())
}

val homebrewBumpFormula by tasks.registering(Exec::class) {
    group = "Help"
    description = "Runs brew bump-forumula-pr"
    commandLine("./.homebrew")
    environment("VERSION" to "${project.property("VERSION_NAME")}")
    dependsOn(tasks.named("githubRelease"))
}

tasks.register("publishNewRelease", DefaultTask::class) {
    group = "Help"
    description = "Triggers uploading new archives and publish announcements"
    dependsOn(announceRelease, homebrewBumpFormula, tasks.named("githubRelease"))
}

tasks.wrapper {
    gradleVersion = libs.versions.gradle.get()
    distributionSha256Sum = libs.versions.gradleSha256.get()
}
