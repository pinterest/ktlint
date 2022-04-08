plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    implementation(projects.ktlintCore)
    implementation(libs.sarif4k)

    testImplementation(libs.junit5)
    testImplementation(libs.assertj)
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to project.property("VERSION_NAME"))
    }
}
