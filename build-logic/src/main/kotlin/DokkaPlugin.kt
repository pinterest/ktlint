import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.dokka.gradle.DokkaExtension

abstract class DokkaPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("java-library")
            pluginManager.apply("org.jetbrains.dokka-javadoc")

            extensions.configure<DokkaExtension> {
                dokkaPublications.named("javadoc") {
                    outputDirectory.set(layout.buildDirectory.dir("javadoc"))
                }
            }
        }
}
