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
---- Gradle Instructions: Linux/Unix ----
---------------------------------------------------------

You can run these examples easily using gradle:

------------- BEGIN PASTE ----------------------
gradle exampleRun -Pwhich=boofcv.examples.imageprocessing.ExampleBinaryOps
-------------- END PASTE -----------------------

However, Gradle is a bit slow when all you want to do is run an example and you can do the following instead:

------------- BEGIN PASTE ----------------------
gradle exampleJar
java -cp build/libs/*.jar boofcv.examples.imageprocessing.ExampleBinaryOps
-------------- END PASTE -----------------------



