plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(libs.logging)

    api(projects.ktlintCliRulesetCore)
    api(projects.ktlintRuleEngineCore)

    testImplementation(projects.ktlintTest)
}
