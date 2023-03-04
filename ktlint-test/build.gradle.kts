plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngine)
    implementation(projects.ktlintRulesetTestTooling)
    api(libs.assertj)
    api(libs.junit5)
    api(libs.logback)
    api(libs.janino)
    api(libs.jimfs)
}
