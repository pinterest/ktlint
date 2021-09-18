import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.dokkaJavadoc.configure {
    outputDirectory.set(buildDir.resolve("javadoc"))
}

tasks.named<Jar>("javadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

project.version = project.property("VERSION_NAME")
    ?: throw GradleException("Project version property is missing")
project.group = project.property("GROUP")
    ?: throw GradleException("Project group property is missing")

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.property("GROUP") as String?
            artifactId = project.property("POM_ARTIFACT_ID") as String?
            version = project.property("VERSION_NAME") as String?

            pom {
                name.set(project.property("POM_NAME") as String?)
                description.set(project.property("POM_DESCRIPTION") as String?)
                url.set(project.property("POM_URL") as String?)
                licenses {
                    license {
                        name.set(project.property("POM_LICENSE_NAME") as String?)
                        url.set(project.property("POM_LICENSE_URL") as String?)
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set(project.property("POM_DEVELOPER_ID") as String?)
                        name.set(project.property("POM_DEVELOPER_NAME") as String?)
                    }
                }
                scm {
                    url.set(project.property("POM_SCM_URL") as String?)
                    connection.set(project.property("POM_SCM_CONNECTION") as String?)
                    developerConnection.set(project.property("POM_SCM_DEV_CONNECTION") as String?)
                }
            }

            from(components["java"])
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = URI.create("https://oss.sonatype.org/content/repositories/snapshots/")
            name = "mavenCentral"
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                username = project.findProperty("SONATYPE_NEXUS_USERNAME")?.toString()
                    ?: System.getenv("SONATYPE_NEXUS_USERNAME")
                password = project.findProperty("SONATYPE_NEXUS_PASSWORD")?.toString()
                    ?: System.getenv("SONATYPE_NEXUS_PASSWORD")
            }
        }
    }
}

/**
 * Following signing parameters must be configured in `$HOME/.gradle/gradle.properties`:
 * ```
 * signing.keyId=12345678
 * signing.password=some_password
 * signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
 * ```
 */
signing {
    // Uncomment following line to use gpg-agent for signing
    // See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent how to configure it
    // useGpgCmd()

    sign(publishing.publications["maven"])
    setRequired({
        !version.toString().endsWith("SNAPSHOT")
    })
}
