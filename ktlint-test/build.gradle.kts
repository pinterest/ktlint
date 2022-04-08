plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    api(projects.ktlintCore)
    api(projects.ktlintRulesetTest)
    api(projects.ktlintTestLogging)

    implementation(libs.junit5)
    implementation(libs.assertj)
}
