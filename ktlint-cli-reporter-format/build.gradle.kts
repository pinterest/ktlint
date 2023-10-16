import java.net.URI

plugins {
    id("ktlint-publication-library")
}

dependencies {
    implementation(projects.ktlintCliReporterCore)

    testImplementation(projects.ktlintTest)
}
