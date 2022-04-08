plugins {
    `ktlint-kotlin-common`
    `ktlint-publication`
}

dependencies {
    implementation(projects.ktlintCore)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
