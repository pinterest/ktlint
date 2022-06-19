import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }

    // Do not enable explicit api for cli project
    if (project.name != "ktlint") {
        explicitApiWarning()
    }
}

addAdditionalJdkVersionTests()
