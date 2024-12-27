plugins {
    `java-library`
//    id("org.jetbrains.dokka")
    // Generates Javadoc documentation
    id("org.jetbrains.dokka-javadoc")
}

dokka {
    dokkaPublications.javadoc {
        outputDirectory.set(layout.buildDirectory.dir("javadoc"))
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}
