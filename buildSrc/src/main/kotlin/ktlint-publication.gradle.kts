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
            groupId = project.property("GROUP").toString()
            artifactId = project.property("POM_ARTIFACT_ID").toString()
            version = project.property("VERSION_NAME").toString()

            pom {
                name.set(project.property("POM_NAME").toString())
                description.set(project.property("POM_DESCRIPTION").toString())
                url.set(project.property("POM_URL").toString())
                licenses {
                    license {
                        name.set(project.property("POM_LICENSE_NAME").toString())
                        url.set(project.property("POM_LICENSE_URL").toString())
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set(project.property("POM_DEVELOPER_ID").toString())
                        name.set(project.property("POM_DEVELOPER_NAME").toString())
                    }
                }
                scm {
                    url.set(project.property("POM_SCM_URL").toString())
                    connection.set(project.property("POM_SCM_CONNECTION").toString())
                    developerConnection.set(project.property("POM_SCM_DEV_CONNECTION").toString())
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

    sign(publishing.publications["maven"])
    setRequired({
        !version.toString().endsWith("SNAPSHOT")
    })
}
