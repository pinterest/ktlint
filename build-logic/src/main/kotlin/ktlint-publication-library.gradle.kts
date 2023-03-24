plugins {
    id("ktlint-kotlin-common")
    id("ktlint-dokka")
    id("ktlint-publication")
}

publishing.publications.named<MavenPublication>("maven") {
    from(components["java"])
}
