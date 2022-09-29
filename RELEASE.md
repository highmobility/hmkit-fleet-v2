* update ext.ver values in *all* build.gradle or use -Pversion property. search for `hmkit-fleet`
* publish to sonatype staging  `./gradlew publishReleasePublicationToMavenCentralRepository`
* release to mavencentral manually in `https://oss.sonatype.org/#stagingRepositories`
  * delete utils and crypto from the Content tab there if they were not updated. Otherwise, the release will fail
  * press Close
  * refresh until Activity processes are finished
  * press Release
  * Release is available in 15 minutes. However, it is not searchable in the website for a little while. 