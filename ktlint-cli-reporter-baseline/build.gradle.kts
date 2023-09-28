plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintLogger)
    implementation(projects.ktlintRuleEngineCore)
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
}

publishing {
    publications {
        // TODO: Remove in release after ktlint 1.0.1
        create<MavenPublication>("relocation-ktlint-cli-reporter-baseline") {
            pom {
                // Old artifact coordinates
                groupId = "com.pinterest.ktlint"
                artifactId = "ktlint-reporter-baseline"
                version = "0.51.0-FINAL"

                distributionManagement {
                    relocation {
                        // New artifact coordinates
                        artifactId.set("ktlint-cli-reporter-baseline")
                        version.set("1.0.0")
                        message.set("artifactId has been changed")
                    }
                }
            }
        }
    }
}
