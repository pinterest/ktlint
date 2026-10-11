// Standalone settings for ktlint-ruleset-template. Ignored by Gradle when this module is built as part of the ktlint project.
// When copying this template into your own project, update the fallback kotlin version below to the latest stable release.
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // Read the kotlin version from the parent ktlint project's libs.versions.toml when available, so the version stays
        // in sync without a Gradle dependency on the root project's build logic.
        val tomlFile = file("../gradle/libs.versions.toml")
        val kotlinVersion =
            if (tomlFile.exists()) {
                tomlFile
                    .readLines()
                    .firstOrNull { it.startsWith("kotlin =") }
                    ?.substringAfter('"')
                    ?.substringBefore('"')
                    ?: "2.4.21"
            } else {
                "2.4.21"
            }
        kotlin("jvm") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ktlint-ruleset-template"
