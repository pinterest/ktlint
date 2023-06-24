plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)

    api(projects.ktlintCliRulesetCore)
    api(projects.ktlintRuleEngineCore)

    testImplementation(projects.ktlintTest)
    testRuntimeOnly(libs.logback)
}
