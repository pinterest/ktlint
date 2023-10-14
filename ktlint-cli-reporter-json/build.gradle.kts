import java.net.URI

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
        create<MavenPublication>("relocation-ktlint-cli-reporter-json") {
            pom {
                // Old artifact coordinates
                groupId = "com.pinterest.ktlint"
                artifactId = "ktlint-reporter-json"
                version = "0.51.0-FINAL"

                name = artifactId
                description = providers.gradleProperty("POM_DESCRIPTION")
                url = providers.gradleProperty("POM_URL")
                licenses {
                    license {
                        name = providers.gradleProperty("POM_LICENSE_NAME")
                        url = providers.gradleProperty("POM_LICENSE_URL")
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = providers.gradleProperty("POM_DEVELOPER_ID")
                        name = providers.gradleProperty("POM_DEVELOPER_NAME")
                    }
                }
                scm {
                    url = providers.gradleProperty("POM_SCM_URL")
                    connection = providers.gradleProperty("POM_SCM_CONNECTION")
                    developerConnection = providers.gradleProperty("POM_SCM_DEV_CONNECTION")
                }

                distributionManagement {
                    relocation {
                        // New artifact coordinates
                        artifactId.set("ktlint-cli-reporter-json")
                        version.set("1.0.0")
                        message.set("artifactId has been changed")
                    }
                }

                repositories {
                    maven {
                        val releasesRepoUrl = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
                        val snapshotsRepoUrl = URI.create("https://oss.sonatype.org/content/repositories/snapshots/")
                        name = "mavenCentral"
                        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                        credentials {
                            username = providers.gradleProperty("SONATYPE_NEXUS_USERNAME").orNull
                                ?: System.getenv("SONATYPE_NEXUS_USERNAME")
                            password = providers.gradleProperty("SONATYPE_NEXUS_PASSWORD").orNull
                                ?: System.getenv("SONATYPE_NEXUS_PASSWORD")
                        }
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

    sign(publishing.publications["relocation-ktlint-cli-reporter-json"])

    isRequired = true
}
