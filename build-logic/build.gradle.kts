plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlinPlugin =
        if (providers.gradleProperty("kotlinDev").orNull.toBoolean()) {
            // Pass '-PkotlinDev' to command line to enable kotlin-in-development version
            logger.warn("Enabling kotlin dev version!")
            libs.kotlin.plugin.dev
        } else {
            libs.kotlin.plugin
        }
    implementation(kotlinPlugin)
    implementation(libs.dokka)
}
