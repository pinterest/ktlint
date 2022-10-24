plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinPlugin = if (project.hasProperty("kotlinDev")) {
        // Pass '-PkotlinDev' to command line to enable kotlin-in-development version
        logger.warn("Enabling kotlin dev version!")
        libs.kotlin.plugin.dev
    } else {
        libs.kotlin.plugin
    }
    implementation(kotlinPlugin)
    implementation(libs.dokka)
}
