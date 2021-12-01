import gradle.kotlin.dsl.accessors._80e422bfb44acd38519c1b2ed4303c90.javaToolchains
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.register

const val testsOnJDK11TaskName: String = "testOnJdk11"

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
