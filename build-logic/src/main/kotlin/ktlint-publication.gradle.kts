import java.net.URI

plugins {
    `maven-publish`
    signing
}

if (providers.gradleProperty("isKotlinDev").orNull.toBoolean()) {
    val definedVersion = ext["VERSION_NAME"].toString().removeSuffix("-SNAPSHOT")
    ext["VERSION_NAME"] = "$definedVersion-kotlin-dev-SNAPSHOT"
}

project.version = providers.gradleProperty("VERSION_NAME").orNull
    ?: throw GradleException("Project version property is missing")
project.group = providers.gradleProperty("GROUP").orNull
    ?: throw GradleException("Project group property is missing")

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            version = version.toString()
            artifactId = providers.gradleProperty("POM_ARTIFACT_ID").get()

            pom {
                name.set(providers.gradleProperty("POM_NAME"))
                description.set(providers.gradleProperty("POM_DESCRIPTION"))
                url.set(providers.gradleProperty("POM_URL"))
                licenses {
                    license {
                        name.set(providers.gradleProperty("POM_LICENSE_NAME"))
                        url.set(providers.gradleProperty("POM_LICENSE_URL"))
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set(providers.gradleProperty("POM_DEVELOPER_ID"))
                        name.set(providers.gradleProperty("POM_DEVELOPER_NAME"))
                    }
                }
                scm {
                    url.set(providers.gradleProperty("POM_SCM_URL"))
                    connection.set(providers.gradleProperty("POM_SCM_CONNECTION"))
                    developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
                }
            }
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
                    ?: providers.systemProperty("SONATYPE_NEXUS_USERNAME").orNull
                password = providers.gradleProperty("SONATYPE_NEXUS_PASSWORD").orNull
                    ?: providers.systemProperty("SONATYPE_NEXUS_PASSWORD").orNull
            }
        }
    }
}

/**
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

    val signingKeyId = providers.systemProperty("ORG_GRADLE_PROJECT_signingKeyId").orNull
    val signingKey = providers.systemProperty("ORG_GRADLE_PROJECT_signingKey").orNull
    val signingPassword = providers.systemProperty("ORG_GRADLE_PROJECT_signingKeyPassword").orNull
    useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

    // This property allows OS package maintainers to disable signing
    val enableSigning = providers.gradleProperty("ktlint.publication.signing.enable").orNull != "false"
    sign(publishing.publications["maven"])
    isRequired = enableSigning && !version.toString().endsWith("SNAPSHOT")
}

tasks.withType<Sign>().configureEach {
    notCompatibleWithConfigurationCache("https://github.com/gradle/gradle/issues/13470")
}
