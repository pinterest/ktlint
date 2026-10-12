import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.plugins.signing.SigningExtension

@Suppress("Unused") // Class is referenced by name in "build-logic/build.gradle.kts"
abstract class PublicationPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("com.vanniktech.maven.publish")

            project.version = projectVersion()

            configureMavenPublishBaseExtension()
        }

    context(project: Project)
    private fun projectVersion(): String {
        val definedVersion: String = project.providers.gradleProperty("VERSION_NAME").get()
        return if (project.hasProperty("kotlinDev")) {
            "${definedVersion.removeSuffix("-SNAPSHOT")}-kotlin-dev-SNAPSHOT"
        } else {
            definedVersion
        }
    }

    context(project: Project)
    private fun configureMavenPublishBaseExtension() {
        project.extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            coordinates(
                groupId = project.localGradleProperty("POM_GROUP_ID").get(),
                artifactId = project.localGradleProperty("POM_ARTIFACT_ID").get(),
                version = project.version.toString(),
            )
            configureSigning()
            @Suppress("UnstableApiUsage")
            configureBasedOnAppliedPlugins()
            configurePom()
        }
    }

    context(project: Project, mavenPublishBaseExtension: MavenPublishBaseExtension)
    private fun configureSigning() {
        // Pass `-PuseGpgCmd` to sign artifacts using local gpg agent
        val useGpgCmd = project.localGradleProperty("useGpgCmd").orNull

        val signingKeyId = project.localGradleProperty("signingInMemoryKeyId").orNull?.takeIf { it.isNotEmpty() }
        val signingKey = project.localGradleProperty("signingInMemoryKey").orNull?.takeIf { it.isNotEmpty() }
        val signingPassword = project.localGradleProperty("signingInMemoryKeyPassword").orNull?.takeIf { it.isNotEmpty() }
        val signingConfigPresent = signingKeyId != null && signingKey != null && signingPassword != null

        // Avoid setting empty strings as signing keys. This avoids breaking the build when PR is opened from a fork.
        // Also, due to https://github.com/gradle/gradle/issues/18477 signing tasks try to prematurely access the signatory.
        // This also improves error messages if something's misconfigured
        if (signingConfigPresent || useGpgCmd != null) {
            mavenPublishBaseExtension.signAllPublications()

            project.configure<SigningExtension> {
                this.isRequired = signingConfigPresent // for more reproducible builds, fail if SNAPSHOT builds are misconfigured too

                if (useGpgCmd != null && useGpgCmd != "false") {
                    useGpgCmd()
                }
            }
        } else {
            project.logger.info(
                listOfNotNull(
                    "signingKeyId".takeIf { signingKeyId.isNullOrBlank() },
                    "signingKey".takeIf { signingKey.isNullOrBlank() },
                    "signingPassword".takeIf { signingPassword.isNullOrBlank() },
                ).joinToString(prefix = "Signing info not complete. Field(s) should not be empty: "),
            )
        }
    }

    context(project: Project)
    private fun MavenPublishBaseExtension.configurePom() {
        pom {
            name.set(project.localGradleProperty("POM_NAME"))
            description.set(project.providers.gradleProperty("POM_DESCRIPTION"))
            url.set(project.providers.gradleProperty("POM_URL"))
            licenses {
                license {
                    name.set(project.providers.gradleProperty("POM_LICENSE_NAME"))
                    url.set(project.providers.gradleProperty("POM_LICENSE_URL"))
                    distribution.set("repo")
                }
            }
            developers {
                developer {
                    id.set(project.providers.gradleProperty("POM_DEVELOPER_ID"))
                    name.set(project.providers.gradleProperty("POM_DEVELOPER_NAME"))
                }
            }
            scm {
                url.set(project.providers.gradleProperty("POM_SCM_URL"))
                connection.set(project.providers.gradleProperty("POM_SCM_CONNECTION"))
                developerConnection.set(project.providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
            }
        }
    }

    // TODO: remove this once https://github.com/gradle/gradle/issues/23572 is fixed
    fun Project.localGradleProperty(name: String): Provider<String> =
        provider {
            if (hasProperty(name)) property(name)?.toString() else null
        }
}
