plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)

    api(libs.kotlin.compiler)
    api(libs.ec4j)

    testImplementation(projects.ktlintTest)
    testImplementation(projects.ktlintRuleEngine)
    testRuntimeOnly(libs.slf4j)
}
