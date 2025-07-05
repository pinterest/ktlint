import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named

abstract class PublicationLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("ktlint-kotlin-common")
            pluginManager.apply("ktlint-dokka")
            pluginManager.apply("ktlint-publication")

            extensions.configure<PublishingExtension> {
                publications.named<MavenPublication>("maven") {
                    from(components["java"])
                }
            }
        }
}
