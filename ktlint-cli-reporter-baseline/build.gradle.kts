plugins {
    id("ktlint-publication-library")
}

dependencies {
    constraints {
        implementation("org.apache.logging.log4j:log4j-to-slf4j:2.26.1") {
            //  +--- io.github.hakky54:logcaptor:2.12.6
            //  |    +--- org.slf4j:slf4j-api:2.0.17 -> 2.0.18
            //  |    +--- ch.qos.logback:logback-classic:1.3.15 -> 1.6.3 (*)
            //  |    +--- org.apache.logging.log4j:log4j-to-slf4j:2.25.3 -> 2.26.1
            because("logcaptor 2.12.6 provides an outdated version of slf4j")
        }
        implementation("org.apache.logging.log4j:log4j-api:2.26.1") {
            //  +--- io.github.hakky54:logcaptor:2.12.6
            //  |    +--- org.slf4j:slf4j-api:2.0.17 -> 2.0.18
            //  |    +--- ch.qos.logback:logback-classic:1.3.15 -> 1.6.3 (*)
            //  |    +--- org.apache.logging.log4j:log4j-to-slf4j:2.25.3 -> 2.26.1
            because("logcaptor 2.12.6 provides an outdated version of slf4j")
        }
    }
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngineCore)
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
    testImplementation(libs.logback)
    testImplementation(libs.logcaptor)

    testImplementation(libs.junit5.jupiter)
    // Since Gradle 8 the platform launcher needs explicitly be defined as runtime dependency to avoid classpath problems
    // https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
    testRuntimeOnly(libs.junit5.platform.launcher)
}
