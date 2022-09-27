plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCore)
    implementation(libs.sarif4k)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
