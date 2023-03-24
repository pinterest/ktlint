plugins {
    id("ktlint-kotlin-common")
}

dependencies {
    api(projects.ktlintCliRulesetCore)
    api(projects.ktlintRuleEngineCore)
}
