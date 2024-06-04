# How to make new project release

### Publishing a new release

Note: These steps should be done directly in the pinterest/ktlint repository, not in your personal fork.

1. Create a new branch (e.g. `0.50.0-prep`)
2. Manually run GitHub Action workflow https://github.com/pinterest/ktlint/actions/workflows/generate-changelog.yml to generate the new changelog based on the PR's since last release
   * Check whether each PR is listed in the correct category. If not, add the proper label to the PR and repeat. See https://github.com/pinterest/ktlint/blob/master/.github/workflows/generate-changelog.yml#L35 for which labels to use. 
   * Copy the generated changelog from build step `Echo generated changelog` to the `CHANGELOG.md` file
   * The generated changelog only contains the PR titles. For most changes this should be sufficient. For breaking API changes, it is better to add additional information. To indent this explanation correctly, append `  ` (two spaces) to the end of the previous line.
3. Update `VERSION_NAME` with the new release version in the project root `gradle.properties`. Be sure to remove `-SNAPSHOT`.
4. Update `CHANGELOG.md` to rename the `Unreleased` section to the new release name, following the `## [x.x.x] - YYYY-MM-DD` format.
5. Add the new release to the bottom of the `CHANGELOG.md` file.
6. Push your changes to the branch, and merge it to `master`.
7. Update your local `pinterest/ktlint` `master` branch; verify you see the `gradle.properties` and `CHANGELOG.md` changes locally.
8. Add a tag with the new release version, and push it directly to remote (e.g. `git tag 0.50.0 && git push origin 0.50.0`). This will kick off the [Release workflow](https://github.com/pinterest/ktlint/actions/workflows/publish-release-build.yml).Important: when you get an error like `HttpError: refusing to allow a Personal Access Token to create or update workflow '.github/workflows/automerge-triggers.yml' without 'workflow' scope`, the GitHub personal access token is most likely expired. When this happens on the bump of the Homebrew formula, the personal access token of @shashachu needs to be updated.
9. Close and release the repo on Sonatype. Only Pinterest employees can do this. Wait with steps below until artifacts are published on https://central.sonatype.com/search?q=com.pinterest.ktlint&sort=published
10. The `.announce` script has created the `<release>-update-refs` branch in the repo. Create a new pull request (https://github.com/pinterest/ktlint/compare) and merge it to master.
11. Merge of branch `<release>-update-refs` to master starts the `Publish release documentation` and `Publish snapshot documentation` workflows. Check that both workflows succeed (https://github.com/pinterest/ktlint/actions). Also check that the documentation has actually been published on https://pinterest.github.io/ktlint/latest.
12. Verify that documentation of new release is published correctly. Especially check whether version numbers in [documentation](https://pinterest.github.io/ktlint/latest/install/cli/) have been changed. After publication of `1.0.0` the documentation still referred to `0.50.0` (according to https://github.com/pinterest/ktlint/actions/runs/6085301212/job/16509057702#step:11:14 it tried updating `0.49.1` to `1.0.0` which most likely was caused by the already fixed issue that the changelog heading of the `0.50.0` did not comply to the expected format).
13. Verify that the published documentation does not contain broken links with [Broken Link Checker Tool](https://www.deadlinkchecker.com/website-dead-link-checker.asp).
14. Announce release on Ktlint Slack channel but wait with doing so until sonatype release is closed by Pinterest.
15. Update `gradle.properties` with the new `SNAPSHOT` version
