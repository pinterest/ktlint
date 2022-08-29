import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.register

val Project.javaToolchains: JavaToolchainService
    get() = extensions.getByType(JavaToolchainService::class.java)

fun Project.addAdditionalJdkVersionTests() {
    // Tests should be run on all supported LTS versions of the JDK. For example, see https://endoflife.date/java
    listOf(8, 11, 17, 18).forEach(::addJdkVersionTests)
}

private fun Project.addJdkVersionTests(jdkVersion: Int) {
    val jdkVersionTests = tasks.register<Test>("testOnJdk$jdkVersion") {
        javaLauncher.set(
            javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(jdkVersion))
            },
        )
        val jvmArgs = mutableListOf<String>()
        if (jdkVersion >= 16) {
            // https://docs.gradle.org/7.5/userguide/upgrading_version_7.html#removes_implicit_add_opens_for_test_workers
            jvmArgs += listOf(
                "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.util=ALL-UNNAMED",
            )
        }
        if (jdkVersion >= 18) {
            // https://openjdk.org/jeps/411
            jvmArgs += "-Djava.security.manager=allow"
        }
        setJvmArgs(jvmArgs)
    }
    tasks.named("check") {
        dependsOn(jdkVersionTests)
    }
}
