pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "ktlint-root"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":ktlint",
    ":ktlint-core",
    ":ktlint-reporter-baseline",
    ":ktlint-reporter-checkstyle",
    ":ktlint-reporter-json",
    ":ktlint-reporter-sarif",
    ":ktlint-reporter-html",
    ":ktlint-reporter-plain",
    ":ktlint-ruleset-experimental",
    ":ktlint-ruleset-standard",
    ":ktlint-ruleset-template",
    ":ktlint-ruleset-test",
    ":ktlint-test",
    ":ktlint-test-logging"
)
