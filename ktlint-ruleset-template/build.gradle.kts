plugins {
    id("ktlint-kotlin-common")
    `java-library`
    `maven-publish`
}

group = "com.github.username"

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn(tasks.classes)
    archiveClassifier.set("sources")
    from(sourceSets.main.map { it.allSource })
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.javadoc)
    archiveClassifier.set("javadoc")
    from(tasks.javadoc.map { it.destinationDir!! })
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
    classpath(ktlint, sourceSets.main.map { it.output })
    args("--debug", "src/**/*.kt")
}.let {
    tasks.check.configure {
        dependsOn(it)
    }
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
