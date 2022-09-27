plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    api(libs.logback)
    api(libs.janino)
}
