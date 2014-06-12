For gradle to process this directory you must tell it where your Android SDK is.  Do this by setting the "ANDROID_HOME"
environmental variable to be this directory.  Gradle will then search the list of platforms available and pick the
latest one to compile against.

Example in ~/.profile on Linux:
------------- BEGIN -----------------
export ANDROID_HOME=/opt/android-sdk-linux/
------------- END -----------------

Make sure you set /opt/android-sdk-linux/ to be the path to your Android SDK!!
