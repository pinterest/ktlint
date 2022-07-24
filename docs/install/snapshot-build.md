## Access to the latest `master` snapshot

Whenever a commit is added to the `master` branch a snapshot build is automatically uploaded to [Sonatype's snapshots repository](https://oss.sonatype.org/content/repositories/snapshots/com/pinterest/ktlint/).
If you are eager to try upcoming changes (that might or might not be included in the next stable release) you can do
so by changing version of ktlint to `<latest-version>-SNAPSHOT` + adding a repo:

### Maven

```xml
...
<repository>
    <id>sonatype-snapshots</id>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
    <releases>
        <enabled>false</enabled>
    </releases>
</repository>
...
```

### Gradle

```groovy
repositories {
  maven {
    url "https://oss.sonatype.org/content/repositories/snapshots"
  }
}
```

### Kotlin development version snapshot

Additionally, project publishes snapshots build against latest kotlin development version. To use them, change version
of ktlint to `<latest-version>-kotlin-dev-SNAPSHOT`.
