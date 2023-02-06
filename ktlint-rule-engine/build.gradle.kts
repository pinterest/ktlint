plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    implementation(projects.ktlintLogger)

    api(projects.ktlintRulesetCore)
    api(libs.kotlin.compiler)
    api(libs.ec4j)

    testImplementation(projects.ktlintTest)
    testImplementation(projects.ktlintRulesetStandard)
    testImplementation(libs.jimfs)
}
