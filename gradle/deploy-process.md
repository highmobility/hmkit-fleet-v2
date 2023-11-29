# Public release

Release is done via a merged pull request to main/v0/v1 and then creating a release in GitHub UI.

## Steps for v2
- update CHANGELOG.md
  - next version is with minor +1, or the manually updated version
- merge a pull request to main. This creates a new tag with minor +=1.
  - optionally set a new major/minor version in `gradle.properties` before merging. Then the tag is created according to this manual version. Otherwise, patch number is incremented automatically.
- create a release from this tag in GitHub. Use `Generate release notes`
    - Action starts that pushes the package to MavenCentral.
    - You can check OSSRH whether release was successful or not.

## Steps for v0 and v1

- update CHANGELOG.md
- update the version in `gradle.properties`. Needs to be done manually because the new tag task works for main branch only. 
- merge the PR to the `v0`/`v1` branch
- Create release tag manually
- Create a release from this tag 

❗Release the v0 and v1 first if releasing all versions. This way the v2 is the latest in the Releases changelog.

## Make a test release locally to staging

- comment out line `useInMemoryPgpKeys(signingKey, signingPassword)` in deploy-ossrh.gradle
- Update version in `$projectRoot/gradle.properties` and call `./gradlew -Prelease :hmkit-fleet:publishToSonatype`.
- Don't merge test version names to main