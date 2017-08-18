[![Build Status](https://travis-ci.org/lessthanoptimal/BoofCV.svg?branch=master)](https://travis-ci.org/lessthanoptimal/BoofCV)
[![Join the chat at https://gitter.im/lessthanoptimal/BoofCV](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/lessthanoptimal/BoofCV?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Maven Central](https://img.shields.io/maven-central/v/org.boofcv/boofcv-core.svg)](https://maven-badges.herokuapp.com/maven-central/org.boofcv/boofcv-core)

------------------------------------------------------
# Table of Contents

* [Introduction](#introduction)
  * [Cloning Repository](#cloning-git-repository)
  * [Quick Start](#quick-start-examples-and-demonstrations)
  * [Gradle and Maven](#adding-to-gradle-and-maven-projects)
* [Building from Source](#building-from-source)
* [Dependencies](#dependencies)
* [Help/Contact](#contact)

------------------------------------------------------
## Introduction

BoofCV is an open source real-time computer vision library written entirely in Java and released under the Apache License 2.0.  Functionality includes low-level image processing, camera calibration, feature detection/tracking, structure-from-motion, classification, and recognition.

- [ Project Webpage ]( http://boofcv.org                                  )
- [ Message Board   ]( https://groups.google.com/group/boofcv             )
- [ Bug Reports     ]( https://github.com/lessthanoptimal/BoofCV/issues   )
- [ Repository      ]( https://github.com/lessthanoptimal/BoofCV          )

## Cloning GIT Repository

The bleeding edge source code can be obtained by cloning the git repository.

```
git clone -b SNAPSHOT --recursive https://github.com/lessthanoptimal/BoofCV.git boofcv
```

Is the data directory empty?  That's because you didn't follow instructions and skipped --recursive.  Fix that by doing the following.
```
cd boofcv
git submodule update --init --recursive
```

## Quick Start Examples and Demonstrations

Know what you're doing and you just want to see something running?  Then run the commands below!  Each jar will open a window, then to run an application just double click on its name.

```bash
cd boofcv
./gradlew examples
java -jar examples/examples.jar
./gradlew demonstrations
java -jar demonstrations/demonstrations.jar
```

All the code for what you see is in boofcv/examples and boofcv/demonstrations.  Example code is designed to be easy to understand so look there first.

## Maven Central Repository

BoofCV is on [Maven Central](http://search.maven.org/) and can be easily added to your Maven, Gradle, ...etc projects.  It's divided up into many modules.  The easiest way to include the critical modules is to have your project dependent on 'core'.

For Maven projects:
```
<dependency>
  <groupId>org.boofcv</groupId>
  <artifactId>core</artifactId>
  <version>0.27</version>
</dependency>
```

There are also several integration modules which help BoofCV interact with external projects.  A list of those is included below:

|     Name             |                 Description
|----------------------|-------------------------------------------------------------------------------------
| boofcv-all           | Absolutely everything
| boofcv-android       | Useful functions for working inside of Android devices.
| boofcv-javacv        | [JavaCV](https://github.com/bytedeco/javacv) is a wrapper around OpenCV mainly for file IO.
| boofcv-ffmpeg        | [javacpp-presets](https://github.com/bytedeco/javacpp-presets) their ffmepg wrapper is used for reading video files.
| boofcv-jcodec        | [JCodec](http://jcodec.org/) is a pure Java video reader/writer.
| boofcv-openkinect    | Used the [Kinect](http://openkinect.org) RGB-D sensor with BoofCV.
| boofcv-swing         | Visualization using Java Swing
| boofcv-WebcamCapture | A few functions that make [WebcamCapture](http://webcam-capture.sarxos.pl/) even easier to use.

## Directories

| Directory       | Description
|-----------------|-------------------------------------------------------------------------------------
| applications/   | Helpful applications
| data/           | Directory containing optional data used by applets and examples.
| demonstrations/ | Demonstration code which typically lets experiment by changing parameters in real-time
| examples/       | Set of example code designed to be easy to read and understand.
| integration/    | Contains code which allows BoofCV to be easily integrated with 3rd party libraries.  Primary for video input/output.
| main/           | Contains the source code for BoofCV

------------------------------------
# Building from Source

Building and installing BoofCV into your local Maven repository is easy[1] using the [gradlew](https://docs.gradle.org/current/userguide/gradle_wrapper.html) script:
```bash
cd boofcv
./gradlew install
```
If you wish to have jars instead, the following commands are provided.
```bash
./gradlew oneJarBin               # Builds a single jar with all of BoofCV in it
./gradlew createLibraryDirectory  # Puts all jars and dependencies into boofcv/library
./gradlew alljavadoc              # Combines all JavaDoc from all sub-porjects into a single set
```


[1] A couple of the integration submodules have a custom build process that can't be performed by Gradle.  The script is smart enough to ignore modules and tell you that it is doing so if you haven't configured it yet.

## IntelliJ

IntelliJ is the recommended IDE for use with BoofCV.  With IntelliJ you can directly import the Gradle project.  

1. File->Project From Existing Sources
2. Select your local "boofcv" directory
3. Confirm that you wish to import the Gradle project

## Eclipse

The easiest way to import the project is to use Gradle to generate an Eclipse project.

```bash
cd boofcv
./gradlew eclipse
```
Then in Eclipse; 1) "import existing projects", 2) Select your BoofCV directory, 3) Click Finish.  You can also install a Gradle plugin to Eclipse and import the project directory.  That's left as an exercise for the reader.

-----------------------------------------------------------
# Dependencies

Core BoofCV modules depends on the following libraries

- [ args4f        ]( http://args4j.kohsuke.org/)
- [ EJML          ]( http://code.google.com/p/efficient-java-matrix-library )
- [ GeoRegression ]( http://georegression.org )
- [ DDogleg       ]( http://ddogleg.org)
- [ DeepBoof      ]( https://github.com/lessthanoptimal/DeepBoof)

The following is required for unit tests

- [ JUnit   ]( http://junit.sourceforge.net/)

Code from the following libraries has been integrated into BoofCV

- [General Purpose FFT by Takuya Ooura](http://www.kurims.kyoto-u.ac.jp/~ooura/fft.html)
  * Java port by Piotr Wendykier with modifications by Peter Abeles to recycle memory.
  
The optional sub-projects in integration also have several dependencies. See those sub-projects for a list of their dependencies.

------------------------------------
# Contact

For questions or comments about BoofCV please use the message board.  Only post a bug report after doing some due diligence to make sure it is really a bug and that it has not already been reported.

[Message Board](http://groups.google.com/group/boofcv)
