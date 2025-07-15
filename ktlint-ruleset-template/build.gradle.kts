plugins {
    id("ktlint-kotlin-common")
    `java-library`
    `maven-publish`
}

group = "com.github.username"

java {
    withSourcesJar()
    withJavadocJar()
}

val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(projects.ktlintCli)

    implementation(projects.ktlintCliRulesetCore)
    implementation(projects.ktlintRuleEngineCore)

    testImplementation(projects.ktlintTest)
    testRuntimeOnly(libs.slf4j)

    testImplementation(libs.junit5.jupiter)
    // Since Gradle 8 the platform launcher needs explicitly be defined as runtime dependency to avoid classpath problems
    // https://docs.gradle.org/8.12/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
    testRuntimeOnly(libs.junit5.platform.launcher)
}

val ktlintCheck by tasks.registering(JavaExec::class) {
    dependsOn(tasks.classes)
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    mainClass = "com.pinterest.ktlint.Main"
    // Adding compiled classes of this ruleset to the classpath so that ktlint validates the ruleset using its own ruleset
    classpath(ktlint, sourceSets.main.map { it.output })
    args("--log-level=debug", "src/**/*.kt")
}

tasks.check {
    dependsOn(ktlintCheck)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components.named("java").get())
            pom {
                licenses {
                    license {
                        name = "The Apache Software License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "repo"
                    }
                }
            }
        }
    }
}
