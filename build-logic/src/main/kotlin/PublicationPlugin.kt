import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin

abstract class PublicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            val definedVersion: String = providers.gradleProperty("VERSION_NAME").get()

            project.version =
                if (hasProperty("kotlinDev")) {
                    "${definedVersion.removeSuffix("-SNAPSHOT")}-kotlin-dev-SNAPSHOT"
                } else {
                    definedVersion
                }

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral()
                coordinates(
                    groupId = localGradleProperty("POM_GROUP_ID").get(),
                    artifactId = localGradleProperty("POM_ARTIFACT_ID").get(),
                    version = version.toString(),
                )

                // This property allows OS package maintainers to disable signing
                val enableSigning = providers.gradleProperty("ktlint.publication.signing.enable").orNull != "false"

                signAllPublications()
                the<SigningExtension>().isRequired = enableSigning && !version.toString().endsWith("SNAPSHOT")

                configureBasedOnAppliedPlugins()

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

    // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
    fun Project.localGradleProperty(name: String): Provider<String> =
        provider {
            if (hasProperty(name)) property(name)?.toString() else null
        }
}
