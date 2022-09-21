plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    if (project.hasProperty("kotlinDev")) {
        // Pass '-PkotlinDev' to command line to enable kotlin-in-development version
        logger.warn("Enabling kotlin dev version!")
        implementation(libs.kotlin.plugin.dev)
    } else {
        implementation(libs.kotlin.plugin)
    }
    implementation(libs.dokka)
}
