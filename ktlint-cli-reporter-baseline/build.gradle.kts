plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    api(projects.ktlintCliReporterCore)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
