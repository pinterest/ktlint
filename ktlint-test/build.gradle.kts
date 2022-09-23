plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    api(projects.ktlintCore)
    api(projects.ktlintRulesetTest)
    api(projects.ktlintTestLogging)
    api(libs.assertj)

    implementation(libs.junit5)
}
