
Introduction
====================================

BoofCV is an open source computer vision library written entirely in Java by Peter Abeles.  It is released under the Apache License 2.0.  Source code, examples, and other utilties are included in this package.  This document contains only a brief summary of the directory structure and how to build the source code.  For more detailed and update information please visible the web pages below.

- [ Project Webpage ] ( http://boofcv.org                                  )
- [ Message Board   ] ( https://groups.google.com/group/boofcv             )
- [ Bug Reports     ] ( https://github.com/lessthanoptimal/BoofCV/issues   )
- [ Repository      ] ( https://github.com/lessthanoptimal/BoofCV          )

## Where to Download

You can download complete jars, use Maven, or checkout from Github.  See the website for instructions.

http://boofcv.org/index.php?title=Download:BoofCV

## New to Java?

If you are new to Java, then using BoofCV will be a challenge.  With just a little bit of knowledge it is possible to build and run examples using the instructions below.  Integrating BoofCV into your own project is another issue.  If you don't know what a jar file is or how to import classes, it is highly recommend that you learn the basics first before attempting to use BoofCV.

## Directories

<pre>
applet/        | Contains source code for Java applets which demonstrate BoofCV's capabilities.
data/          | Directory containing optional data used by applets and examples.
evaluation/    | Code that is used to debug and evaluate BoofCV's performance.
examples/      | Set of example code designed to be easy to read and understand.
integration/   | Contains code which allows BoofCV to be easily integrated with 3rd party libraries.  Primary for video input/output.
lib/           | Set of 3rd party libraries that BoofCV is dependent on.
main/          | Contains the source code for BoofCV
</pre>

Building from Source
====================================

BoofCV is a java library and can be compiled on any platform with Java installed. Gradle is now the preferred way to build BoofCV.  There are still Ant build scripts laying around but those will be removed in the near future and their use is not officially supported any more.

BEFORE trying to compile BoofCV make sure you have the following installed and that the paths are setup correctly:

- [ Java Developers Kit (JDK) version 1.6 or later    ]
  ( http://www.oracle.com/technetwork/java/index.html )
- [ Gradle                                            ]
  ( http://www.gradle.org/                            )

BoofCV is very easy to build on just about any system with Gradle and Java support.  Gradle will download all of the 
dependencies for you.  Well that's not totally true, there are a couple of optional packages which require manual 
downloading since they lack jars on Maven central. More on that later.

*NOTE* As an alternative to installing Gradle directly there are shell scripts "boofcv/gradlew" and "boofcv/gradlew.bat".  They will download gradle and execute the commands the same as invoking "gradle" would.

Below are a few useful custom Gradle scripts that can be invoked:

* _createLibraryDirectory_ : Will gather all the BoofCV jars (main and integration) and jars which boofcv/main depend on and place them in the boofcv/library directory.
* _alljavadoc_ : Combines JavaDoc from all the sub-projects into one set.
* _exampleRun_ : Used to run an example from boofcv/examples, e.g. "gradle exampleRun -Pwhich=boofcv.examples.imageprocessing.ExampleBinaryOps"
* _webcamRun_ : Used to run an example from integration/WebcamCapture, e.g. "gradle webcamRun -Pwhich=boofcv.examples.ExampleTrackingKlt"

_createLibraryDirectory_ unless all dependencies are meet, not all projects in 'boofcv/integration' will produce jars.  See the "Integration Modules" section below for the details.

## Compilation Error

A stable build should always compile out of the box with no problem.  All of the examples should run without any problems,
as long as you don't modify anything, even slightly.  Before you complain about a problem on a stable build make 
sure you are absolutely certain that you're doing everything right.  If after a few 
attempts you still can't figure out post a message.  Maybe these instructions are lacking in clarity.

If you checked out the code from Github then you don't have a stable build and like to live dangerously.  There is a 
chance the code won't compile or one of the libraries it depends on has changed.  If you get a compilation error feel 
free to post a polite message with a copy of the error asking for someone to fix it.

## IntelliJ

You will need to import the Gradle project into IntelliJ.  Verified to work in IntelliJ IDEA 13.1.x.

### Install IntelliJ Gradle Plug-In
1. File -> Settings -> Plugins
2. Search for Gradle. Install if it isn't already

### Opening the Project
1. File->Import Project
2. Select boofcv/build.gradle

## Eclipse

Eclipse has a Gradle plugin available which allow it to open a Gradle project directly.  The following was
 tested with Kepler.

### Install Eclipse Gradle Plug-In
1. Help -> Eclipse Marketplace
2. Search for "Gradle IDE Pack" and install
3. Restart Eclipse

### Configure Gradle
1. Window -> Preference -> Gradle EnIDE
2. Specify Gradle Home.  On my system this is /opt/gradle/gradle-1.12
3. Click OK

### Opening the Project
1. File -> Import -> SelectGradle
2. Click "Browse" button and browse to boofcv/ directory
3. Click "Build Model" button
4. Click "Select All" to import all sub-projects 
5. Click finish

## Integration Modules

Most of the modules in the integration package should automatically with everything else.  Some require you to 
manually download and place files in certain locations.  Until you do so Gradle will ignore those modules.
Specific instructions are contained in the readme file in each of the module directories.

Dependencies
====================================

The main BoofCV modules depends on the following libraries:

- [ EJML          ]  ( http://code.google.com/p/efficient-java-matrix-library )
- [ GeoRegression ]  ( http://georegression.org                               )
- [ DDogleg       ]  ( http://ddogleg.org                                     )

The following are required for running unit tests

- [ JUnit   ]       ( http://junit.sourceforge.net/                           )

The optional sub-projects in integration also have several dependencies. See those sub-projects for a list of their dependencies.

Contact
====================================

For questions or comments about BoofCV please use the message board.  Only post a bug report after doing some due
diligence to make sure it is really a bug and that it has not already been reported.

[Message Board](http://groups.google.com/group/boofcv)