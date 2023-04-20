plugins {
    `java-platform`
    id("ktlint-publication")
}

publishing.publications.named<MavenPublication>("maven") {
    from(components["javaPlatform"])
}

val excludeList =
    listOf(
        "ktlint-api-consumer",
        "ktlint-bom",
        "ktlint-ruleset-template",
        "ktlint-test-ruleset-provider-v2-deprecated",
    )

dependencies {
    logger.lifecycle("Creating dependencies for ktlint-bom")
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
            if (subproject.name in excludeList) {
                logger.lifecycle("Ignore dependency on $subproject")
            } else {
                logger.lifecycle("Add api dependency on $subproject to ktlint-bom")
                api(subproject)
            }
        }
    }
    logger.lifecycle("Finished creating dependencies for ktlint-bom")
}
