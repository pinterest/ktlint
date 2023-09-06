plugins {
    `java-platform`
    id("ktlint-publication")
}

publishing.publications.named<MavenPublication>("maven") {
    from(components["javaPlatform"])
}

val internalNonPublishableProjects: Set<String> by rootProject.extra
val excludeList = internalNonPublishableProjects + "ktlint-test-ruleset-provider-v2-deprecated"

dependencies {
    logger.lifecycle("Creating dependencies for ktlint-bom")
    constraints {
        project.rootProject.subprojects.forEach { subproject ->
            if (subproject.name in excludeList) {
                logger.lifecycle("  - Ignore dependency '${subproject.name}' and do not add to ktlint-bom")
            } else {
                logger.lifecycle("  + Add api dependency on '${subproject.name}' to ktlint-bom")
                api(subproject)
            }
        }
    }
    logger.lifecycle("Finished creating dependencies for ktlint-bom")
}
