import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register

const val testsOnJDK11TaskName: String = "testOnJdk11"

val Project.javaToolchains: JavaToolchainService
    get() = (this as ExtensionAware).extensions.getByName("javaToolchains") as JavaToolchainService

fun Project.addJdk11Tests() {
    val testTask = tasks.register<Test>(testsOnJDK11TaskName) {
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(11))
            }
        )
    }

    tasks.named("check") {
        dependsOn(testTask)
    }
}
