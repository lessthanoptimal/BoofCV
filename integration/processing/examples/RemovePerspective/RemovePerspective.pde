// Given 4 corners of a shape which is known to be rectangular, remove the perspective distortion from it

import boofcv.processing.*;
import boofcv.struct.image.*;

PImage input;
PImage undistorted;

void setup() {

  input = loadImage("goals_and_stuff.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleColor image = Boof.colorMS(input,ImageDataType.F32);

  // Specify the size of the undistorted image and the corner
  // points of the rectangle in the original image.
  int outWidth = 400;
  int outHeight = 500;
  undistorted = image.removePerspective(outWidth,outHeight,
    267,182,  542,68,  519,736,  276,570).convert();

  size(input.width + undistorted.width, input.height);
}

void draw() {
  image(input, 0, 0);
  image(undistorted, input.width, 0);
}