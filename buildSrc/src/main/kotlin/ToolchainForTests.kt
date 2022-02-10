import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register

val Project.javaToolchains: JavaToolchainService
    get() = extensions.getByType(JavaToolchainService::class.java)

fun Project.addJdk11Tests() {
    val testTask = tasks.register<Test>("testOnJdk11") {
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
