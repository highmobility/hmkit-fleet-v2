# HMKit Fleet

HMKit Fleet is a Kotlin library that combines different API-s: OAuth, Service Account API and
Telematics API to help car companies manage their fleet.

# Table of contents

* [Architecture](#architecture)
* [Requirements](#requirements)
* [Getting Started](#getting-started)
* [Contributing](#contributing)
* [Release](#release)
* [Licence](#Licence)

### Architecture

**General**: HMKit Fleet is a Kotlin library combines 3 different API-s into single fleet owner
package.

// TODO: describe modules 

### Requirements

* Linux environment. Contact High-Mobility if you require binaries for other systems.

### Getting Started

// TODO: link to tutorial

### Contributing

Before starting please read our contribution rules 📘[Contributing](CONTRIBUTE.md)

### Setup

* `git submodule update --init --recursive`
* Build the HMKit Core: `cd hmkit-oem/src/main/jni && make && cd -`
* import the Gradle project
* run the tests" `./gradlew test`

### Release

All of the HMKit Fleet packages can be released from this project. This includes hmkit-oem, hmkit-core-jni, 
hmkit-crypto, hmkit-utils.

**Pre checks**

* Run the unit tests: `./gradlew test`

**Release**

* update ext.ver values in build.gradle or use -Pversion property
* set ext.depLocation to 1 or use -PdepLocation=1 property
* Call `./gradlew publish` to release all the packages to dev repo.
* Call `./gradlew :hmkit-utils:publish` to release a specific package.
* Call `./gradlew :hmkit-utils:publish -Prepo=gradle-release-local` to specify the repo.
* If releasing to bintray, also call `./gradlew bintrayUpload`.

If pushing the same version number, the package will be overwritten in dev, rejected in release.

### Licence
This repository is using MIT licence. See more in 📘[LICENCE](LICENCE.md)