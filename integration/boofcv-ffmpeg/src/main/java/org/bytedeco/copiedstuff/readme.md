# Vendored JavaCV frame grabber

Copied from [JavaCV](https://github.com/bytedeco/javacv) (Apache 2.0 / GPLv2 with Classpath
exception, © Samuel Audet) and trimmed to video only. Original copyright headers are kept at the
top of each file.

Vendoring avoids depending on `org.bytedeco:javacv`, which declares non-optional dependencies on
every preset it wraps (OpenCV, OpenBLAS, Tesseract, RealSense, Kinect, ...) that this module never
uses.

