Running these examples requires that JDK 1.6 or later has been installed.  The examples can either be run through the provided ant script or by using your favorite IDE.  Instructions for running ant scripts in Linux is provided below.

---------------------------------------------------------
---- DATA FILES ----
---------------------------------------------------------

Before you attempt to run any example make sure you have all the data files! If you are not sure if you have the data files already look in the 'boofcv/data' directory and if it is empty you need to download it.

Scenario A: You checkout the git repository.

The data is stored in a submodule which can be grabbed by typing the following:

git submodule init
git submodule update

Scenario B: You got the source code from somewhere else and there is no data directory:

Checkout the data files from github.  You can also just download them directly from github too:

git@github.com:lessthanoptimal/BoofCV-Data.git


---------------------------------------------------------
---- Ant Instructions: Linux/Unix ----
---------------------------------------------------------

First make sure that the JDK and Ant are correctly installed.

Ant:   http://ant.apache.org/
JDK:   http://www.oracle.com/technetwork/java/index.html

The procedure to compile the example code in Linux goes something like this:

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


