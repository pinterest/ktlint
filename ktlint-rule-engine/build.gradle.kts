plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)

    api(projects.ktlintRuleEngineCore)
    api(libs.kotlin.compiler)
    api(libs.ec4j)

    testImplementation(projects.ktlintTest)
    testImplementation(projects.ktlintRulesetStandard)
    testRuntimeOnly(libs.logback)
}
