# How to make new project release

### Publishing a new release

Note: These steps should be done directly in the pinterest/ktlint repository, not in your personal fork.

1. Create a new branch (e.g. `0.50.0-prep`)
2. Update `VERSION_NAME` with the new release version in the project root `gradle.properties`. Be sure to remove `-SNAPSHOT`.
3. Update `CHANGELOG.md` to rename the `Unreleased` section to the new release name, following the `## [x.x.x] - YYYY-MM-DD` format.
4. Add the new release to the bottom of the `CHANGELOG.md` file.
5. Push your changes to the branch, and merge it to `master`.
6. Update your local `pinterest/ktlint` `master` branch; verify you see the `gradle.properties` and `CHANGELOG.md` changes locally.
7. Add a tag with the new release version, and push it directly to remote (e.g. `git tag 0.50.0 && git push origin 0.50.0`). This will kick off the [Release workflow](https://github.com/pinterest/ktlint/actions/workflows/release.yml).Important: when you get an error like `HttpError: refusing to allow a Personal Access Token to create or update workflow '.github/workflows/automerge-triggers.yml' without 'workflow' scope`, the Github personal access token is most likely expired. When this happens on the bump of the Homebrew formula, the personal access token of @shashachu needs to be updated.
8. Close and release the repo on Sonatype. (Only Pinterest employees can do this.)
9. The `.announce` script has created the `<release>-update-refs` branch in the repo. Create a new pull request (https://github.com/pinterest/ktlint/compare) and merge it to master.
10. Merge of branch `<release>-update-refs` to master starts the `Publish release documentation` and `Publish snapshot documentation` workflows. Check that both workflows succeed (https://github.com/pinterest/ktlint/actions). Also check that the documentation has actually been published on https://pinterest.github.io/ktlint/latest.
11. Update `gradle.properties` with the new `SNAPSHOT` version, and add the section below to the top of `CHANGELOG.md` and commit. (This can be done directly in the main repo or in your fork.)
```markdown
## Unreleased

### Added

### Removed

### Fixed

### Changed
```

## After release of documentation

[The documentation for KtLint](https://pinterest.github.io/ktlint/) should be checked for dead links.
Follow the instructions for building the documentation in `/documentation/readme.md`, and use a tool such as [Broken Link Checker Tool](https://www.deadlinkchecker.com/website-dead-link-checker.asp) to find broken links.
