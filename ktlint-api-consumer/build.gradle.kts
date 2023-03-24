plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngine)
    // This example API Consumer also depends on ktlint-ruleset-standard as it mixes custom rules and rules from ktlint-ruleset-standard
    // into a new rule set.
    implementation(projects.ktlintRulesetStandard)

    testImplementation(projects.ktlintTest)
}
