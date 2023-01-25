plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCore)
    implementation(projects.ktlintRuleEngine)
    implementation(projects.ktlintRulesetStandard)
    implementation(libs.logback)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
