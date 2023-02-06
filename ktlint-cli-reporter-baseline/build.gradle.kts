plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
}
