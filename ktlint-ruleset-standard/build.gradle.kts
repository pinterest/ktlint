plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCore)
    implementation(libs.logging)

    api(projects.ktlintRulesetCore)
    api(projects.ktlintRuleEngine)

    testImplementation(projects.ktlintTest)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
