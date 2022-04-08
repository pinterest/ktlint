plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    api(libs.kotlin.compiler)
    api(libs.ec4j)
    api(libs.logging)

    // Standard ruleset is required for EditConfigLoaderTest only
    testImplementation(projects.ktlintRulesetStandard)
    testImplementation(projects.ktlintTestLogging)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
    testImplementation(libs.jimfs)
}
