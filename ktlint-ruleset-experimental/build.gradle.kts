plugins {
    `ktlint-kotlin-common`
    `ktlint-publication`
}

dependencies {
    implementation(projects.ktlintCore)

    testImplementation(projects.ktlintTest)
    testImplementation(projects.ktlintRulesetStandard)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
