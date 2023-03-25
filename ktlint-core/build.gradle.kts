plugins {
    id("ktlint-publication-library")
}

dependencies {
    api(libs.kotlin.compiler)
    api(libs.ec4j)
}
