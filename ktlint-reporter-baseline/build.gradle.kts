plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    implementation(projects.ktlintCore)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
