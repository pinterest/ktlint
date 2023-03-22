plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    // Any SLF4J compatible logging framework can be used. The "slf4j-simple" logging provider is configured in file
    // ktlint-api-consumer/src/main/resources/simplelogger.properties
    runtimeOnly("org.slf4j:slf4j-simple:2.0.7")

    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngine)
    // This example API Consumer also depends on ktlint-ruleset-standard as it mixes custom rules and rules from ktlint-ruleset-standard
    // into a new rule set.
    implementation(projects.ktlintRulesetStandard)

    testImplementation(projects.ktlintTest)
}
