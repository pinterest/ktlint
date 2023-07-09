import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(20)
}

// TODO: Remove setting `options.release` and `compilerOptions.jvmTarget` after upgrade to Kotlin Gradle Plugin 1.9
//  build-logic is an internal project and given we know how the "actual" project is built - it's fine to target current java here as well.
//  @see https://github.com/pinterest/ktlint/pull/2120#discussion_r1260229055 for more details
val targetJavaVersion = JavaVersion.VERSION_17
tasks.withType<JavaCompile>().configureEach {
    options.release.set(targetJavaVersion.majorVersion.toInt())
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

dependencies {
    val kotlinPlugin =
        if (providers.gradleProperty("kotlinDev").orNull.toBoolean()) {
            // Pass '-PkotlinDev' to command line to enable kotlin-in-development version
            logger.warn("Enabling kotlin dev version!")
            libs.kotlin.plugin.dev
        } else {
            libs.kotlin.plugin.asProvider()
        }
    implementation(kotlinPlugin)
    implementation(libs.dokka)
    implementation(libs.poko)
}
