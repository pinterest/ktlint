plugins {
    id("ktlint-kotlin-common")
    id("ktlint-publication")
    id("ktlint-publication-dokka")
}

publishing.publications.named<MavenPublication>("maven") {
    from(components["java"])
}
