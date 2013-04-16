The following jars go into /lib

jna-3.5.2.jar
platform-3.5.2.jar

which can be download from https://github.com/twall/jna

You will also need to checkout libfreenect

Compiling and running example code with ant:

ant
ant examples
ant -Dwhich=boofcv.example.OverlayRgbDepthStreamsApp run

