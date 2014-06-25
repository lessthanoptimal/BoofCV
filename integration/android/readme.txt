For gradle to process this directory you must tell it where your Android SDK is.  Do this by setting the "ANDROID_HOME"
environmental variable to be the Android SDK directory.  Gradle will then search the list of platforms available and
pick the latest one to compile against.

Example in ~/.profile on Linux:
------------- BEGIN -----------------
export ANDROID_HOME=/path/to/android-sdk-linux/
------------- END -----------------

To build this sub-project's jar type "gradle createLibraryDirectory" and look in boofcv/libraries for the compiled jar.
