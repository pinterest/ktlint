plugins {
    `java-platform`
    id("ktlint-publication")
}

publishing.publications.named<MavenPublication>("maven") {
    from(components["javaPlatform"])
}

val excludeList =
    listOf(
        "ktlint-bom",
        "ktlint-ruleset-template",
        "ktlint-test-ruleset-provider-v2-deprecated",
    )

dependencies {
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
            if (subproject.name !in excludeList) {
                api(subproject)
            }
        }
    }
}
