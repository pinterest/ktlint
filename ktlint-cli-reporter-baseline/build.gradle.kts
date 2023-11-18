plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngineCore)
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
    testImplementation(libs.logback)
    testImplementation(libs.logcaptor)
}
