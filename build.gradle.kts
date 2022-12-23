import java.net.URL

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
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

    val skipTests: String = System.getProperty("skipTests", "false")
    tasks
        .withType<Test>()
        .configureEach {
            if (skipTests == "false") {
                useJUnitPlatform()
            } else {
                logger.warn("Skipping tests for task '$name' as system property 'skipTests=$skipTests'")
            }
        }
}

val ktlint: Configuration = configurations.create("ktlint")

dependencies {
    ktlint(projects.ktlint)
}

tasks.register<JavaExec>("ktlint") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style"
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
        // Do not run with option "--log-level=debug" or "--log-level=trace" as the lint violations will be difficult
        // to spot between the amount of output lines.
    )
}

tasks.register<JavaExec>("ktlint format") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Check Kotlin code style and format"
    classpath = ktlint
    jvmArgs = listOf("--add-opens=java.base/java.lang=ALL-UNNAMED")
    mainClass.set("com.pinterest.ktlint.Main")
    args(
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**",
        "--format",
        // Do not run with option "--log-level=debug" or "--log-level=trace" as the lint violations will be difficult
        // to spot between the amount of output lines.
    )
}

// Deployment tasks
val githubToken: String = if (project.hasProperty("servers.github.privKey")) {
    project.property("servers.github.privKey").toString()
} else {
    logger.warn("No github token specified")
    ""
}

val shadowJarExecutable: TaskProvider<Task> by lazy {
    projects.ktlint.dependencyProject.tasks.named("shadowJarExecutable")
}

val shadowJarExecutableChecksum: TaskProvider<Task> by lazy {
    projects.ktlint.dependencyProject.tasks.named("shadowJarExecutableChecksum")
}

// Explicitly adding dependency on "shadowJarExecutable" as Gradle does not it set via "releaseAssets" property
tasks.githubRelease {
    dependsOn(provider { shadowJarExecutable })
    dependsOn(provider { shadowJarExecutableChecksum })
}

githubRelease {
    token(githubToken)
    owner("pinterest")
    repo("ktlint")
    tagName(project.property("VERSION_NAME").toString())
    releaseName(project.property("VERSION_NAME").toString())
    targetCommitish("master")
    releaseAssets.from(
        // Temp: Hardcode the list of files to upload
        project.file("ktlint/build/run/ktlint"),
        project.file("ktlint/build/run/ktlint.md5"),
        project.file("ktlint/build/run/ktlint.asc"),
        project.file("ktlint/build/run/ktlint.asc.md5"),
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
        dependsOn(subproject.tasks.named("publishMavenPublicationToMavenCentralRepository"))
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
    dependsOn(tasks.named("githubRelease"))
}

tasks.register<DefaultTask>("publishNewRelease") {
    group = "Help"
    description = "Triggers uploading new archives and publish announcements"
    dependsOn(announceRelease, homebrewBumpFormula, tasks.named("githubRelease"))
}

tasks.wrapper {
    distributionSha256Sum = URL("$distributionUrl.sha256")
        .openStream().use { it.reader().readText().trim() }
}
