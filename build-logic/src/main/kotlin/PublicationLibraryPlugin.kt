import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class PublicationLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit =
        with(target) {
            pluginManager.apply("ktlint-kotlin-common")
            pluginManager.apply("ktlint-dokka")
            pluginManager.apply("ktlint-publication")
        }
}
