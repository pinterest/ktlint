plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    // FIXME: DO NOT MERGE WITH MASTER. THIS IS SOLELY NEEDED FOR INVESTIGATION OF KOTLIN 1.9 IMPACT
    // https://github.com/pinterest/ktlint/issues/1981
    maven {
        url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        artifactUrls("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
    // END OF FIXME: DO NOT MERGE WITH MASTER. THIS IS SOLELY NEEDED FOR INVESTIGATION OF KOTLIN 1.9 IMPACT
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
