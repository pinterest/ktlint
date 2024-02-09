plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngine)
    implementation(projects.ktlintCliRulesetCore)
    api(libs.assertj)
    api(libs.junit5)
    api(libs.jimfs)
}
