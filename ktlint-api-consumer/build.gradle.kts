plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngine)
    implementation(projects.ktlintRulesetStandard)

    testImplementation(projects.ktlintTest)
}
