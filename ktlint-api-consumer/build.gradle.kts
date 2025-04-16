plugins {
    id("ktlint-kotlin-common")
}

dependencies {
    // Any SLF4J compatible logging framework can be used. The "slf4j-simple" logging provider is configured in file
    // ktlint-api-consumer/src/main/resources/simplelogger.properties
    runtimeOnly(libs.slf4j)

    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngine)

    // If the API consumer depends on a fixed set of ruleset, it might be best to provide those dependencies at compile time. In this way
    // statically typing can be used when defining the EditorConfigOverride for the KtlintRuleEngine. However, in this example, the
    // dependencies are provided at runtime.
    // implementation(projects.ktlintRulesetStandard)

    // For advanced use cases, the API consumer might prefer to provide the ruleset dependencies at runtime and load them dynamically using
    // the RuleSetProvider of ktlint-cli-ruleset-core.
    implementation(projects.ktlintCliRulesetCore)
    runtimeOnly(projects.ktlintRulesetStandard)

    // The standard ruleset is also provided as test dependency to demonstrate that rules that are provided at compile time can also be unit
    // tested.
    testImplementation(projects.ktlintRulesetStandard)

    testImplementation(projects.ktlintTest)

    testImplementation(libs.junit5.jupiter)
    // Since Gradle 8 the platform launcher needs explicitly be defined as runtime dependency to avoid classpath problems
    // https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
    testRuntimeOnly(libs.junit5.platform.launcher)
}
