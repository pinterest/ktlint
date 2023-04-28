pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}

rootProject.name = "ktlint-root"

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    // ktlint-core module is no longer used internally in the ktlint project except for backwards compatibility
    ":ktlint-core",
    ":ktlint-api-consumer",
    ":ktlint-baseline",
    ":ktlint-bom",
    ":ktlint-cli",
    ":ktlint-cli-reporter-baseline",
    ":ktlint-cli-reporter-checkstyle",
    ":ktlint-cli-reporter-core",
    ":ktlint-cli-reporter-format",
    ":ktlint-cli-reporter-json",
    ":ktlint-cli-reporter-sarif",
    ":ktlint-cli-reporter-html",
    ":ktlint-cli-reporter-plain",
    ":ktlint-cli-reporter-plain-summary",
    ":ktlint-cli-ruleset-core",
    ":ktlint-logger",
    ":ktlint-rule-engine",
    ":ktlint-rule-engine-core",
    ":ktlint-ruleset-standard",
    ":ktlint-ruleset-template",
    ":ktlint-test",
    ":ktlint-test-ruleset-provider-v2-deprecated",
)
