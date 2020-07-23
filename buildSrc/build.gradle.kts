plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    jcenter()
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:0.10.1")
}
