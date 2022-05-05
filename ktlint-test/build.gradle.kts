plugins {
    `ktlint-kotlin-common`
    `ktlint-publication`
}

dependencies {
    api(projects.ktlintCore)
    api(projects.ktlintRulesetTest)
    api(projects.ktlintTestLogging)
    api(libs.assertj)

    implementation(libs.junit5)
}
