
# Introduction

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

applet/        | Contains source code for Java applets which demonstrate BoofCV's capabilities.
data/          | Directory containing optional data used by applets and examples.
evaluation/    | Code that is used to debug and evaluate BoofCV's performance.
examples/      | Set of example code designed to be easy to read and understand.
integration/   | Contains code which allows BoofCV to be easily integrated with 3rd party libraries.  Primary for video input/output.
lib/           | Set of 3rd party libraries that BoofCV is dependent on.
main/          | Contains the source code for BoofCV

# Building from Source

BoofCV is a java library and can be compiled on any platform with Java installed. Gradle is now the preferred way to build BoofCV.  There are still Ant and Maven build scripts laying around but those will be removed in the near future and their use is not offically supported any more.

BEFORE trying to compile BoofCV make sure you have the following installed and that the paths are setup correctly:

- [ Java Developers Kit (JDK) version 1.6 or later    ]
  ( http://www.oracle.com/technetwork/java/index.html )
- [ Gradle                                            ]
  ( http://www.gradle.org/                            )

BoofCV is very easy to build on just about any system with Gradle and Java support.  Gradle will download all of the 
dependencies for you.  Well that's not totally true, there are a couple of optional packages which require manual 
downloading since they lack jars on Maven central. More on that later.

## Compilation Error

A stable build should always compile out of the box with no problem.  Before you complain about a 
problem on a stable build make sure you are absolutely certain that you're doing everything right.  If after a few 
attempts you still can't figure out post a message.  Maybe these instructions are lacking in clarity.

If you checked out the code from Github then you don't have a stable build and like to live dangerously.  There is a 
chance the code won't compile or one of the libraries it depends on has changed.  If you get a compilation error feel 
free to post a polite message with a copy of the error asking for someone to fix it.

## Command Line

In Linux you just need to switch to the boofcv directory and type "gradle compileJava".  Take a look at the examples 
directory and follow the instructions there for how to run those using the command line.

## IntelliJ

You will need to import the Gradle project.  This has been verified to work in IntelliJ IDEA 13.1.x.

1. File->Import Project
2. Select boofcv/build.gradle

## Eclipse

Should be easy... someone needs to fill this out

## Integration Modules

Most of the modules in the integration package should automatically with everything else.  Some require you to 
manually download and place files in certain locations.  Until you do so Gradle will ignore those modules.
Specific instructions are contained in the readme file in each of the module directories.

# Dependencies

BoofCV depends on a few other packages listed below.

- [ EJML          ]  ( http://code.google.com/p/efficient-java-matrix-library )
- [ GeoRegression ]  ( http://georegression.org                               )
- [ DDogleg       ]  ( http://ddogleg.org                                     )

The following are required for running unit tests and micro-benchmarks:

- [ JUnit   ]       ( http://junit.sourceforge.net/                           )
- [ Caliper ]       ( http://code.google.com/p/caliper/                       )

# Contact

For questions or comments about BoofCV please use the message board.  Only post a bug report after doing some due diligence to make sure it is really a bug and that it has not already been reported.

[Message Board](http://groups.google.com/group/boofcv)