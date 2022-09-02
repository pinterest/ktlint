plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCore)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
