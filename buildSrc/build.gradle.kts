plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// Pass '-PkotlinDev' to command line to enable kotlin-in-development version
val kotlinVersion = if (project.hasProperty("kotlinDev")) {
    logger.warn("Enabling kotlin dev version!")
    "1.7.20-Beta"
} else {
    "1.7.10" // Keep in sync with 'gradle/libs.versions.toml and check whether dokka-gradle-plugin has been updated as well
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.7.10")
}
