// Finds the image gradient for the input image and visualizes it

import boofcv.processing.*;
import boofcv.struct.image.*;

PImage img0,img1,img2,img3;

void setup() {

  PImage input = loadImage("simple_objects.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleGray gray = Boof.gray(input,ImageDataType.F32);

  SimpleGradient gradient = gray.gradientSobel();

  // show the input image
  img0 = input;
  // gradient will visualize its data into a single image
  img1 = gradient.visualize();
  // visualize the data for the x-derivative
  img2 = gradient.dx().visualizeSign();
  // visualize the data for the y-derivative
  img3 = gradient.dy().visualizeSign();

  size(input.width, input.height);
}

void draw() {
  // Display the 4 images by shrinking them
  scale(0.5);
  image(img0, 0, 0);
  image(img1, img1.width, 0);
  image(img2, 0, img1.height);
  image(img3, img1.width, img1.height);
}