This example demonstrates use of Camera2 API with BoofCV's integration library to simplify 
capturing images and displaying the results. Much of the tedious handling of the Android
life cycle, image formats, and other book keeping has been taken care of for a more streamlined 
development of computer vision applications.

Support for Camera 1 API has been deprecated and will eventually be removed from boofCV.  
For Camera 1 API it's recommended that you use BoofCV v0.29 or earlier.

# Performance

If you run the application in Debug mode it will run slow. To get it to run at normal
speed you need to change the build variant to "Release" or "Fast". Fast is a mode
which was specified by this example in app/build.gradle so that you didn't need
to build a release jar to have to run at a reasonable speed.

To change the mode, select "Build Variants" in Android Studio on the left. In the
panel that is expanded out select "fast" under Build Variants.