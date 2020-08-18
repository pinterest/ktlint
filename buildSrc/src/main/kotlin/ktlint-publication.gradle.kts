import java.net.URI
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    `maven-publish`
    signing
    id("org.jetbrains.dokka")
}

// Disabling dokka on Java 10+ versions
// See https://github.com/Kotlin/dokka/issues/294
val shouldEnableJavadoc = JavaVersion.current() <= JavaVersion.VERSION_1_9

java {
    withSourcesJar()
    if (shouldEnableJavadoc) withJavadocJar()
}

if (shouldEnableJavadoc) {
    val dokkaJavadocTask = tasks.register<DokkaTask>("dokkaJavadoc") {
        outputFormat = "javadoc"
        outputDirectory = "$buildDir/javadoc"
    }

    tasks.named<Jar>("javadocJar") {
        dependsOn(dokkaJavadocTask)
        archiveClassifier.set("javadoc")
        from(dokkaJavadocTask)
    }
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
    sign(publishing.publications["maven"])
    setRequired({
        !version.toString().endsWith("SNAPSHOT")
    })
}
