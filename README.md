# HMKit Fleet

HMKit Fleet is a Kotlin library that combines different API-s: OAuth, Service Account API and
Telematics API to help car companies manage their fleet.

# Table of contents

* [Requirements](#requirements)
* [Getting Started](#getting-started)
* [Architecture](#architecture)
* [Contributing](#contributing)
* [Release](#release)
* [Licence](#Licence)

### Requirements

* Linux or OSX (x86)

Contact High-Mobility for binaries for other systems.

### Getting Started

Get started with HMKit Fleet 📘[browse the documentation](TODO:link to tutorial)

### Architecture

**General**: HMKit Fleet is a Kotlin library that combines 3 different API-s into a single fleet
owner package.

* hmkit-fleet: Uses [OkHttp](https://github.com/square/okhttp) to communicate with High-Mobility and [HMKit crypto](https://github.com/highmobility/hmkit-crypto-java)
to encrypt messages.

### Contributing

Before starting please read our contribution rules 📘[Contributing](CONTRIBUTE.md)

### Setup

* `git submodule update --init --recursive`
* import the Gradle project
* run the tests" `./gradlew test`

### Release

**Pre checks**

* Run the unit tests: `./gradlew test`

**Release**

* update ext.ver values in build.gradle or use -Pversion property
* set ext.depLocation to 1 or use -PdepLocation=1 property
* Call `./gradlew publish` to release all of the packages to dev repo.
* Call `./gradlew publish -Prepo=gradle-release-local` to specify the repo.
* If releasing to bintray, also call `./gradlew bintrayUpload`.

If pushing the same version number, the package will be overwritten in dev, rejected in release.

### Licence
This repository is using MIT licence. See more in 📘[LICENCE](LICENCE.md)