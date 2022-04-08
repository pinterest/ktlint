plugins {
    id("ktlint-kotlin-common") // replace it with 'org.jetbrains.kotlin.jvm'
    `java-library`
    `maven-publish`
}

group = "com.github.username"

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    dependsOn(tasks.classes)
    classifier = "sources"
    from(sourceSets.main.get().allSource)
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.javadoc)
    classifier = "javadoc"
    from(tasks.javadoc.get().destinationDir)
}

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
}

val ktlint: Configuration = configurations.create("ktlint")

dependencies {
    ktlint(projects.ktlint)

    compileOnly(projects.ktlintCore)

    testImplementation(projects.ktlintCore)
    testImplementation(projects.ktlintTest)
    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("ktlint") {
    dependsOn(tasks.classes)
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    mainClass.set("com.pinterest.ktlint.Main")
    // adding compiled classes to the classpath so that ktlint would validate project"s sources
    // using its own ruleset (in other words to dogfood)
    classpath = configurations["ktlint"] + sourceSets.main.get().output
    args("--debug", "src/**/*.kt")
}

tasks.check {
    dependsOn(tasks["ktlint"])
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                pom {
                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }
                }
            }
        }
    }
}
