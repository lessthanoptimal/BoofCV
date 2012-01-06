Running these examples requires that JDK 1.6 or later has been installed.  The examples can either be run through the provided ant script or by using your favorite IDE.  Instructions for running ant scripts in Linux is provided below.


---- Ant Instructions: Linux/Unix ----

First make sure that the JDK and Ant are correctly installed.

Ant:   http://ant.apache.org/
JDK:   http://www.oracle.com/technetwork/java/index.html

The procedure to compile the example code in linux goes something like this:

------------- BEGIN PASTE ----------------------
cd boofcv/main/
ant
cd ../examples
ant
-------------- END PASTE -----------------------

After the last command it should have output something like:

------------- BEGIN PASTE ----------------------
Buildfile: boofcv/examples/build.xml

clean:
   [delete] Deleting directory boofcv/examples/build

compile:
    [mkdir] Created dir: boofcv/examples/build/classes
    [javac] Compiling 12 source files to boofcv/examples/build/classes
    [javac] Note: Some input files use unchecked or unsafe operations.
    [javac] Note: Recompile with -Xlint:unchecked for details.

jar:
      [jar] Building jar: boofcv/examples/build/boofcv_examples.jar

main:

BUILD SUCCESSFUL
Total time: 2 seconds
-------------- END PASTE -----------------------

The same ant script can be used to run any of example from the command line easily.  See below for examples of how to correctly invoke the script:

ant -Dwhich=boofcv.examples.ExampleBinaryImage run
ant -Dwhich=boofcv.examples.ExampleInterestPoint run
ant -Dwhich=boofcv.examples.ExamplePointFeatureTracker run

If you wish to modify an example just change the code, run ant again, then invoke the example as shown above.


