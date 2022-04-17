import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
    }

    // Do not enable explicit api for cli project
    if (project.name != "ktlint") {
        explicitApiWarning()
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        @Suppress("SuspiciousCollectionReassignment")
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }
}

addAdditionalJdkVersionTests()
