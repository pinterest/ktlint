plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    api(projects.ktlintCliReporterCore)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
