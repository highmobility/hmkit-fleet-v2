# Public release

Release is done via Github actions.

- update CHANGELOG.md
- merge a pull request
- update version and create a tag. use either of these
    - **with a gradle task**
        - call `./gradlew :hmkit-fleet:release -Prelease.useAutomaticVersion=true` to increment minor version and push
          new tag to remote
        - omit `useAutomaticVersion=true` to set a specific version in gradle dialog
    - **manually**
        - update version in `$projectRoot/gradle.properties` and push tag manually
- create a release in GitHub. Package is pushed to OSSRH staging automatically.
- close and release manually in OSSRH staging.

## Test release

- Update version in `$projectRoot/gradle.properties` and call `./gradlew -Prelease :hmkit-fleet:publishToSonatype`.
- Don't merge test version names to main. Public versions are managed via Github Actions.
- Check the package in staging URL in OSSRH.