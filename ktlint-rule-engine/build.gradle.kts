plugins {
    id("ktlint-publication")
    id("ktlint-kotlin-common")
}

dependencies {
    api(projects.ktlintCore)
    api(libs.kotlin.compiler)
    api(libs.ec4j)
    api(libs.logging)

    testImplementation(projects.ktlintRulesetCore)
    testImplementation(projects.ktlintTestLogging)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
    testImplementation(libs.jimfs)
}
