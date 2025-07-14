import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

abstract class PublicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("maven-publish")
            pluginManager.apply("signing")

            val definedVersion: String = providers.gradleProperty("VERSION_NAME").get()

            project.version =
                if (hasProperty("kotlinDev")) {
                    "${definedVersion.removeSuffix("-SNAPSHOT")}-kotlin-dev-SNAPSHOT"
                } else {
                    definedVersion
                }

            extensions.configure<PublishingExtension> {
                publications {
                    register<MavenPublication>("maven") {
                        groupId = localGradleProperty("POM_GROUP_ID").get()
                        version = version.toString()
                        artifactId = localGradleProperty("POM_ARTIFACT_ID").get()

                        pom {
                            name.set(localGradleProperty("POM_NAME"))
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
                        val releasesRepoUrl =
                            URI.create(
                                "https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/",
                            )
                        val snapshotsRepoUrl = URI.create("https://central.sonatype.com/repository/maven-snapshots/")
                        name = "mavenCentral"
                        url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

                        logger.info("Set publication repository for version $version to $url")

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
            extensions.configure<SigningExtension> {
                // Uncomment following line to use gpg-agent for signing
                // See https://docs.gradle.org/current/userguide/signing_plugin.html#sec:using_gpg_agent how to configure it
                // useGpgCmd()

                val signingKeyId = System.getenv("ORG_GRADLE_PROJECT_signingKeyId")
                val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey")
                val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingKeyPassword")
                useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)

                // This property allows OS package maintainers to disable signing
                val enableSigning = providers.gradleProperty("ktlint.publication.signing.enable").orNull != "false"

                sign(the<PublishingExtension>().publications["maven"])
                isRequired = enableSigning && !version.toString().endsWith("SNAPSHOT")
            }

            project.createPlaceholderJarTasks()
        }

    // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
    fun Project.localGradleProperty(name: String): Provider<String> =
        provider {
            if (hasProperty(name)) property(name)?.toString() else null
        }

    // Create placeholder jars to satisfy Central Portal validation.
    // See https://central.sonatype.org/publish/requirements/#supply-javadoc-and-sources
    private fun Project.createPlaceholderJarTasks() {
        val readmePath = "$rootDir/gradle/"
        val sourcePlaceholderJar =
            tasks.register("sourcePlaceholderJar", Jar::class.java) {
                archiveBaseName.set("${localGradleProperty("POM_NAME").get()}")
                archiveVersion.set(this@createPlaceholderJarTasks.version.toString())
                archiveClassifier.set("sources")
                from(layout.projectDirectory.file("$readmePath/README-sources.md")) {
                    into("")
                    rename { "README.md" }
                }
            }

        val docsPlaceholderJar =
            tasks.register("docsPlaceholderJar", Jar::class.java) {
                archiveBaseName.set("${localGradleProperty("POM_NAME").get()}")
                archiveVersion.set(this@createPlaceholderJarTasks.version.toString())
                archiveClassifier.set("javadoc")
                from(layout.projectDirectory.file("$readmePath/README-javadoc.md")) {
                    into("")
                    rename { "README.md" }
                }
            }

        tasks.named("publishMavenPublicationToMavenCentralRepository") {
            dependsOn(sourcePlaceholderJar, docsPlaceholderJar)
        }
    }
}
