import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register

val Project.javaToolchains: JavaToolchainService
    get() = extensions.getByType(JavaToolchainService::class.java)

fun Project.addAdditionalJdkVersionTests() {
    // Tests should be run on all supported LTS versions of the JDK. For example, see https://endoflife.date/java
    arrayOf(8, 11, 17).forEach(::addJdkVersionTests)
}

private fun Project.addJdkVersionTests(jdkVersion: Int) {
    val jdkVersionTests = tasks.register<Test>("testOnJdk$jdkVersion") {
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
