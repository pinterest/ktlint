# How to make new project release

### Required configuration

Add following information to your `$HOME/.gradle/gradle.properties` file:
```properties
# You Github access token
servers.github.privKey=github_token
# Signing GPG key id
signing.keyId=12345678
# Signing GPG key password
signing.password=some_password
# Path to GPG file to sync artifcats
signing.secretKeyRingFile=~/.gnupg/secring.gpg
```

### Publishing new release

1. Update `VERSION_NAME` with new release version in project root `gradle.properties`
2. Fill in `CHANGELOG.md` with related to new version changes.
3. Run `./gradlew publishNewRelease` to build, upload new artifacts, update KtLint version number on [CLI documentation](docs/install/cli.md) and [integrations documentation](docs/install/integrations.md) and [https://ktlint.github.io](https://ktlint.github.io) site.
4. Update Github release notes with info from `CHANGELOG.md`
