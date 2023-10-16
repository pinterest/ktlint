plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintCliReporterCore)
    implementation(libs.sarif4k)

    testImplementation(projects.ktlintTest)
}
