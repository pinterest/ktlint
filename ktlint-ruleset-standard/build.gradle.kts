plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(libs.logging)

    api(projects.ktlintRulesetCore)

    testImplementation(projects.ktlintTest)
}
