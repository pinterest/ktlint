plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
}
