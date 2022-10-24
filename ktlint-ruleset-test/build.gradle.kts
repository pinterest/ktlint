plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
}

dependencies {
    api(projects.ktlintCore)
}
