# Public release

Release is via a merged pull request to main and then creating a release in GitHub UI.

## Steps
- update CHANGELOG.md
  - next version is with minor +1, or the manually updated version
- merge a pull request to main. This creates a new tag with minor +=1.
  - optionally set a new major/minor version in `gradle.properties` before merging. Then the tag is created according to this manual version. Otherwise, patch number is incremented automatically.
- create a release from this tag in GitHub. Use `Generate release notes`
    - Action starts that pushes the package to MavenCentral.
    - You can check OSSRH whether release was successful or not.

## Make a test release locally to staging

- comment out line `useInMemoryPgpKeys(signingKey, signingPassword)` in deploy-ossrh.gradle
- Update version in `$projectRoot/gradle.properties` and call `./gradlew -Prelease :hmkit-fleet:publishToSonatype`.
- Don't merge test version names to main