plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

// Pass '-PkotlinDev' to command line to enable kotlin-in-development version
val kotlinVersion = if (project.hasProperty("kotlinDev")) {
    logger.warn("Enabling kotlin dev version!")
    "1.7.0-Beta"
} else {
    "1.6.21"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.6.0")
}
