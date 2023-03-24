plugins {
    `java-library`
    id("org.jetbrains.dokka")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.dokkaJavadoc.configure {
    notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/1217")
    outputDirectory.set(buildDir.resolve("javadoc"))
}

tasks.named<Jar>("javadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}
