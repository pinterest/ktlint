plugins {
    id("ktlint-kotlin-common")
//    id("ktlint-publication") No need to publish? If so, also remove from gradle.properties
}

dependencies {
    api(projects.ktlintCliRulesetCore)
    api(projects.ktlintRuleEngineCore)
}
