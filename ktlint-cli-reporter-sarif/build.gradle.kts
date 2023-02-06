plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCliReporterCore)
    implementation(libs.sarif4k)

    testImplementation(projects.ktlintTest)
}
