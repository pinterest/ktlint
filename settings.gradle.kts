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

        // FIXME: DO NOT MERGE WITH MASTER. THIS IS SOLELY NEEDED FOR INVESTIGATION OF KOTLIN 1.9 IMPACT
        // https://github.com/pinterest/ktlint/issues/1981
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
            artifactUrls("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
        }
        // END OF FIXME: DO NOT MERGE WITH MASTER. THIS IS SOLELY NEEDED FOR INVESTIGATION OF KOTLIN 1.9 IMPACT
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
