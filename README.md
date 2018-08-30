# BasaltClient
[![Build Status](https://travis-ci.org/SamOphis/BasaltClient.svg?branch=master)](https://travis-ci.org/SamOphis/BasaltClient)
[![Download](https://api.bintray.com/packages/samophis/maven/BasaltClient/images/download.svg) ](https://bintray.com/samophis/maven/BasaltClient/_latestVersion)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A fast, reactive client implementation for [Basalt](https://github.com/SamOphis/Basalt) and [JDA](https://github.com/DV8FromTheWorld/JDA),
used as a reference and the default JDA client for other developers looking to make clients for Basalt.

Check out the [Wiki](https://github.com/SamOphis/BasaltClient/wiki) for future guides on how to use BasaltClient.
<br>Documentation in the source code will come later but, for now, isn't a major priority for this project.

# Adding BasaltClient
You can either grab a shaded .jar from the [Latest Release](https://github.com/SamOphis/BasaltClient/releases/latest)
or you can do the very-heavily-recommended approach of adding BasaltClient through Gradle, Maven or some other build tool via
the JCenter repository.

> Replace `VERSION` in the following samples with the latest version displayed at the top of this page.

Gradle:
```groovy
dependencies {
    compile group: 'com.github.samophis', name: 'BasaltClient', version: 'VERSION'
}
```

Maven:
```xml
<dependency>
    <groupId>com.github.samophis</groupId>
    <artifactId>BasaltClient</artifactId>
    <version>VERSION</version>
    <type>pom</type>
</dependency>
```

# Contributions
Contributions are very welcome, especially since BasaltClient is still in a development/experimental stage. Really, the
only requirements are that you:

* Base your changes off a development branch.
* Preserve the Apache 2.0 License in files you create/edit.
* Add documentation to the same standard as the rest of the project if you change functionality.
* Write your contributions in Kotlin or Java (although I'd really, *really* appreciate Kotlin more).
* Test your changes -- make sure they work well, have good performance, etc.

Thanks for contributing if you do! It really helps the state of this project.