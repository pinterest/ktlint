plugins {
    id("ktlint-kotlin-common")
//    id("ktlint-publication") No need to publish?
}

dependencies {
    api(projects.ktlintCore)
}
