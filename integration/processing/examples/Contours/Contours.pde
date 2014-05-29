import boofcv.processing.*;

PImage img;

void setup() {

  img = loadImage("particles01.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleGray bimg = Boof.convF32(img);

  // Threshold the image using its mean value
  double threshold = bimg.mean();

  // Create a binary image, erode it, find the contours, then visualize them
  // External contours are red and internal are green
  img = bimg.threshold(threshold,true).erode8(1).contour().getContours().visualize();

  size(img.width, img.height);
}

void draw() {
  image(img, 0, 0);
}