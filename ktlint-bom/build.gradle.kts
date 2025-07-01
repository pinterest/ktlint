plugins {
    `java-platform`
    id("ktlint-publication")
}

publishing.publications.named<MavenPublication>("maven") {
    from(components["javaPlatform"])
}

dependencies {
    logger.info("Creating dependencies for ktlint-bom")
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
            subproject.plugins.withId("ktlint-publication") {
                // Exclude self project from BOM.
                if (subproject == project) return@withId
                logger.info("  + Add api dependency on '${subproject.name}' to ktlint-bom")
                api(subproject)
            }
        }
    }
    logger.info("Finished creating dependencies for ktlint-bom")
}
