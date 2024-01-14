pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

plugins {
    `gradle-enterprise`
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        if (System.getenv("CI") != null) {
            publishAlways()
        }
    }
}

rootProject.name = "ktlint-root"

enableFeaturePreview("STABLE_CONFIGURATION_CACHE")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":ktlint-api-consumer",
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
)
