
========== Introduction =====================

BoofCV is an open source computer vision library written entirely in Java by Peter Abeles.  It is released under the Apache License 2.0.  Source code, examples, and other utilties are included in this package.  This document contains only a brief summary of the directory structure and how to build the source code.  For more detailed and update information please visible the webpage below.

Project Webpage: http://boofcv.org
Message Board:   https://groups.google.com/group/boofcv
Bug Reports:     https://github.com/lessthanoptimal/BoofCV/issues
Repository:      https://github.com/lessthanoptimal/BoofCV

========== New to Java? =====================

If you are new to Java running and using BoofCV can be a challenge.  With a little bit of knowledge it is possible to build and run examples using the instructions below.  Integrating BoofCV into your own project is another issue.  If you don't know what a jar file is or how to import classes I highly recommend you learn the basics first before attempting to use BoofCV.

========== Directories ======================

applet/        Contains source code for Java applets which demonstrate BoofCV's capabilities.
data/          Directory containing optional data used by applets and examples.
evaluation/    Code that is used to debug and evaluate BoofCV's performance.
examples/      Set of example code designed to be easy to read and understand.
integration/   Contains code which allows BoofCV to be easily integrated with 3rd party libraries.  Primary for video input/output.
lib/           Set of 3rd party libraries that BoofCV is dependent on.
main/          Contains the source code for BoofCV

========== Building =========================

BoofCV is a java library and can be compiled on any platform with java installed.  Out of convinience ant scripts have been provided.  These ant scripts can be used to build and run example code.

BEFORE trying to compile BoofCV make sure you have the following installed and that the paths are setup correctly:

- Java Developers Kit (JDK) version 1.6 or later
      http://www.oracle.com/technetwork/java/index.html
- Ant  (If you wish to use the Ant build scripts)
      http://ant.apache.org/

In Linux BoofCV can be compiled using the following command line squence:

---- BEGIN TEXT-----
cd main
ant
----- END TEXT -----

Yes, with two commands you can compile BoofCV.  Inside the lib/ directory there should now be BoofCV.jar.  It is also possible to create JavaDoc by typeing "ant javadoc".  Next if you wish to compile and run the example code, move into the example/ directory and following the readme.txt instructions you find there.

BoofCV is in general very easy to build on just about any system.  Because it is Java you don't have any of the annoying system dependent or compilor issues that are the source of endless headaches.

========= Developing With BoofCV =============

BoofCV has been written to be used with an integrated development evironment (IDE) and access to the source code.  IntelliJ and Eclipse are two excellent IDEs for Java.  With IntelliJ being my personally preference.  After you learn the basics of BoofCV it is possible to highly customize the code and discard abstractions that make programming easier, but less efficient.  The best way to write highly optimized code is to start by looking at the factory containing the code you wish to use, then tracing through the algorithms it invokes.  The code is designed to be readable and documentation actually improves inside of low level algorithms. 

========= Contact ===========================

For questions or comments about BoofCV please use the message board.  Only post a bug report after doing some due diligence to make sure it is really a bug and that it has not already been reported.