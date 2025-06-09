import org.gradle.api.Project
import org.gradle.api.provider.Provider
import java.net.URI

plugins {
    `maven-publish`
    signing
}

val definedVersion: String = providers.gradleProperty("VERSION_NAME").get()

project.version =
    if (hasProperty("kotlinDev")) {
        "${definedVersion.removeSuffix("-SNAPSHOT")}-kotlin-dev-SNAPSHOT"
    } else {
        definedVersion
    }

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = localGradleProperty("POM_GROUP_ID").get()
            version = version.toString()
            artifactId = localGradleProperty("POM_ARTIFACT_ID").get()

            pom {
                name = localGradleProperty("POM_NAME")
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
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = URI.create("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = URI.create("https://central.sonatype.com/repository/maven-snapshots/")
            name = "mavenCentral"
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            logger.lifecycle("Set publication repository for version $version to $url")

            credentials {
                username = providers.gradleProperty("CENTRAL_PORTAL_USERNAME").orNull
                    ?: System.getenv("CENTRAL_PORTAL_USERNAME")
                password = providers.gradleProperty("CENTRAL_PORTAL_TOKEN").orNull
                    ?: System.getenv("CENTRAL_PORTAL_TOKEN")
            }
        }

        maven {
            name = "mavenCentralRelocation"
            url = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = providers.gradleProperty("CENTRAL_PORTAL_USERNAME").orNull
                    ?: System.getenv("CENTRAL_PORTAL_USERNAME")
                password = providers.gradleProperty("CENTRAL_PORTAL_TOKEN").orNull
                    ?: System.getenv("CENTRAL_PORTAL_TOKEN")
            }
        }
    }
}

/*
 * Following signing parameters must be configured in `$HOME/.gradle/gradle.properties`:
 * ```properties
 * signing.keyId=12345678
 * signing.password=some_password
 * signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
 * ```
 */
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

    sign(publishing.publications["maven"])
    isRequired = enableSigning && !version.toString().endsWith("SNAPSHOT")
}

// TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
fun Project.localGradleProperty(name: String): Provider<String> =
    provider {
        if (hasProperty(name)) property(name)?.toString() else null
    }
