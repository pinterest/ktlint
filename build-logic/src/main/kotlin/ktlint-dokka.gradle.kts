plugins {
    `java-library`
    id("org.jetbrains.dokka")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.dokkaJavadoc {
    notCompatibleWithConfigurationCache("https://github.com/Kotlin/dokka/issues/1217")
    outputDirectory = layout.buildDirectory.dir("javadoc")
}

tasks.named<Jar>("javadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    archiveClassifier = "javadoc"
    from(tasks.dokkaJavadoc)
}
