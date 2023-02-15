plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    api(libs.kotlin.compiler)
    api(libs.ec4j)
}
