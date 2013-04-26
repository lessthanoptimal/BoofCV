The following jars go into /lib

jna-3.5.2.jar
platform-3.5.2.jar

which can be download from https://github.com/twall/jna

https://maven.java.net/content/repositories/releases/net/java/dev/jna/jna/3.5.2/jna-3.5.2.jar
https://maven.java.net/content/repositories/releases/net/java/dev/jna/platform/3.5.2/platform-3.5.2.jar

You will also need to checkout libfreenect:  https://github.com/OpenKinect/libfreenect

Compiling and running example code with ant:

ant
ant examples
ant -Dwhich=boofcv.example.OverlayRgbDepthStreamsApp run

