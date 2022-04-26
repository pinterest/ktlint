import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register

val Project.javaToolchains: JavaToolchainService
    get() = extensions.getByType(JavaToolchainService::class.java)

fun Project.addAdditionalJdkVersionTests() {
    // Tests should be run on all supported LTS versions of the JDK. For example, see https://endoflife.date/java
    addJdkVersionTests("testOnJdk11", 11)
    addJdkVersionTests("testOnJdk17", 17)
}

private fun Project.addJdkVersionTests(taskName: String, jdkVersion: Int) {
    val jdkVersionTests = tasks.register<Test>(taskName) {
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(jdkVersion))
            }
        )
    }
    tasks.named("check") {
        dependsOn(jdkVersionTests)
    }
}
