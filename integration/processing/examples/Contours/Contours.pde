// Converts the image from gray scale into binary and then finds the contour around each binary blob.
// Internal and external contours of each blob are drawn a different color.
import boofcv.processing.*;
import boofcv.struct.image.*;

PImage imgContour;
PImage imgBlobs;

void setup() {

  PImage input = loadImage("particles01.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleGray gray = Boof.gray(input,ImageDataType.F32);

  // Threshold the image using its mean value
  double threshold = gray.mean();

  // find blobs and contour of the particles
  ResultsBlob results = gray.threshold(threshold,true).erode8(1).contour();

  // Visualize the results
  imgContour = results.getContours().visualize();
  imgBlobs = results.getLabeledImage().visualize();

  size(input.width, input.height);
}

void draw() {
  background(0);
  if( mousePressed ) {
    image(imgBlobs, 0, 0);
  } else {
    image(imgContour, 0, 0);
  }
}