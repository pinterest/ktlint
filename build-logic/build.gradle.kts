import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(
        libs
            .versions
            .java
            .compilation
            .get()
            .toInt(),
    )
}

// Override java target for build-logic, to the latest version. Kotlin version forced by kotlin-dsl doesn't yet support targeting Java 24
val buildLogicJavaTarget = JvmTarget.JVM_22
tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(buildLogicJavaTarget)
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(buildLogicJavaTarget.target.toInt())
}

dependencies {
    val kotlinPlugin =
        if (hasProperty("kotlinDev")) {
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
