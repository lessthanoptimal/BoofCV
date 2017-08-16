For Gradle to recognize this sub-project will need to checkout libfreenect https://github.com/OpenKinect/libfreenect

cd integration/boofcv-openkinect
git clone https://github.com/OpenKinect/libfreenect.git libfreenect

To build this sub-project's jar type "gradle createLibraryDirectory" and look in boofcv/libraries for the compiled jar.