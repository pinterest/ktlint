plugins {
    `java-platform`
}

val excludeList = listOf(
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
