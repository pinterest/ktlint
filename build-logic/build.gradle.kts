import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.sam.with.receiver)
}

repositories {
    mavenCentral()
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
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

    compilerOptions {
        // Match language version enforced by current Gradle version https://docs.gradle.org/current/userguide/compatibility.html#kotlin
        @Suppress("DEPRECATION")
        apiVersion.set(KotlinVersion.KOTLIN_1_8)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xsuppress-version-warnings") // ignores deprecated kotlin language version
    }
}

gradlePlugin {
    plugins {
        register("ktlint-dokka") {
            id = "ktlint-dokka"
            implementationClass = "DokkaPlugin"
        }
        register("ktlint-kotlin-common") {
            id = "ktlint-kotlin-common"
            implementationClass = "KotlinCommonPlugin"
        }
        register("ktlint-publication") {
            id = "ktlint-publication"
            implementationClass = "PublicationPlugin"
        }
        register("ktlint-publication-library") {
            id = "ktlint-publication-library"
            implementationClass = "PublicationLibraryPlugin"
        }
    }
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
    implementation(gradleKotlinDsl())
    implementation(libs.dokka)
    implementation(libs.poko)
}
