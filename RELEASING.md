# How to make new project release

### Publishing new release

1. Update `VERSION_NAME` with new release version in project root `gradle.properties`. Be sure to remove `-SNAPSHOT`
2. Update `CHANGELOG.md` to rename the `Unreleased` section to the new release name, following the `## [x.x.x] - YYYY-MM-DD` format.
3. Add the new release to the bottom of the `CHANGELOG.md` file.
4. Commit `gradle.properties` and `CHANGELOG.md`.
5. Add a tag with the new release version, and push it to remote. This will kick off the Release workflow.
6. Close and release the repo on Sonatype.
7. Find the `<release>-update-refs` branch in the repo (created by the `.announce` script) and merge it.
8. Update `gradle.properties` with the new `SNAPSHOT` version, and add a new empty `Unreleased` section to the top of `CHANGELOG.md` and commit.
