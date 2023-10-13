plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
}

// TODO: Remove in release after ktlint 1.0.1
publishing {
    publications {
        create<MavenPublication>("relocation-ktlint-cli-reporter-plain") {
            pom {
                // Old artifact coordinates
                groupId = "com.pinterest.ktlint"
                artifactId = "ktlint-reporter-plain"
                version = "0.51.0-FINAL"

                distributionManagement {
                    relocation {
                        // New artifact coordinates
                        artifactId.set("ktlint-cli-reporter-plain")
                        version.set("1.0.0")
                        message.set("artifactId has been changed")
                    }
                }
            }
        }
    }
}

// TODO: Remove in release after ktlint 1.0.1
signing {
    // Uncomment following line to use gpg-agent for signing
    // See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent how to configure it
    // useGpgCmd()

    val signingKeyId = System.getenv("ORG_GRADLE_PROJECT_signingKeyId")
    val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey")
    val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingKeyPassword")
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

    // This property allows OS package maintainers to disable signing
    val enableSigning = providers.gradleProperty("ktlint.publication.signing.enable").orNull != "false"

    sign(publishing.publications["relocation-ktlint-cli-reporter-plain"])

    isRequired = enableSigning && !version.toString().endsWith("SNAPSHOT")
}
