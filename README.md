This is the HMKitFleet v1 branch. See the [v0 branch](https://github.com/highmobility/hmkit-fleet/tree/v0) for the previous version.

# HMKit Fleet

HMKit Fleet is a Kotlin/Java library that combines different API-s: OAuth, Service Account API and
Telematics API to help car companies manage their fleet.

# Table of contents

* [Requirements](#requirements)
* [Getting Started](#getting-started)
* [Architecture](#architecture)
* [License](#License)
* [Contributing](#contributing)

### Requirements

- Target is Java 8
- Compiled with Kotlin and Java 17

### Getting Started

Get started with HMKit Fleet 📘[browse the documentation](https://docs.high-mobility.com/guides/getting-started/fleet/)

```
dependencies {
  implementation('com.high-mobility:hmkit-fleet:{version}')
}
```

### Architecture

**General**: HMKit Fleet is a Kotlin library that combines 3 different API-s into a single fleet
owner package.

* hmkit-fleet: Uses [OkHttp](https://github.com/square/okhttp) to communicate with High-Mobility
  and [HMKit crypto](https://github.com/highmobility/hmkit-crypto-java/tree/telematics)
  to encrypt messages.

### Setup

* `git submodule update --init --recursive`
* import the Gradle project
* run the tests `./gradlew test`

### Release/Deployment

check `./gradle/deploy-process.md`

### License

This repository is using the MIT license. See more in 📘[LICENSE](LICENSE)

### Contributing

Before starting, please read our contribution rules 📘[Contributing](CONTRIBUTING.md)
