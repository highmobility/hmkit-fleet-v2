# Public release

Release is via some manual steps and a final Github action step.
We don't do automatic releases on every push in order to reduce release count if there are no real changes.

## Steps
- update CHANGELOG.md
- merge a pull request to main and checkout main locally
- update version and create a tag. use either one of these
    - **with a gradle task**
        - call `./gradlew :hmkit-fleet:release -Prelease.useAutomaticVersion=true` to increment minor version and push
          new tag to remote
        - omit `useAutomaticVersion=true` to set a specific version in gradle dialog
    - **manually**
        - update version in `$projectRoot/gradle.properties` and push tag manually
- create a release in GitHub from this tag. Use `Generate release notes`
    - Action starts that pushes the package to MavenCentral.
    - You can check OSSRH whether release was successful or not.

## Make a test release locally to staging

- comment out line `useInMemoryPgpKeys(signingKey, signingPassword)` in deploy-ossrh.gradle
- Update version in `$projectRoot/gradle.properties` and call `./gradlew -Prelease :hmkit-fleet:publishToSonatype`.
- Don't merge test version names to main