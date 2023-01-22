plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCore)
    implementation(projects.ktlintRulesetCore)
    implementation(libs.logging)

    testImplementation(projects.ktlintTest)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}
