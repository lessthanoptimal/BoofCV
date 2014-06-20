// Segments the image into superpixels.  These can be used a pre-processing step in
// recognition or as an intelligent image filter.  When you click on the image it will
// switch between a view of the randomly colorized segments and the input image

import boofcv.processing.*;
import boofcv.struct.image.*;
import boofcv.factory.segmentation.*;

PImage input,visualized;

void setup() {

  input = loadImage("berkeley_horses.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleColor bcolor = Boof.colorMS(input,ImageDataType.F32);

  // Declare the image segmentation algorithm
  SimpleImageSegmentation segmentator = Boof.segmentSlic(new ConfigSlic(400),bcolor.getImageType());
//  SimpleImageSegmentation segmentator = Boof.segmentFH04(null,bcolor.getImageType());
//  SimpleImageSegmentation segmentator = Boof.segmentMeanShift(null,bcolor.getImageType());
//  SimpleImageSegmentation segmentator = Boof.segmentWatershed(null,bcolor.getImageType());

  // Segment the image
  segmentator.segment(bcolor);

  // visualize the results
  visualized = segmentator.getOutput().visualize();

  size(input.width, input.height);
}

void draw() {
  background(0);
  if( mousePressed ) {
    image(input, 0, 0);
  } else {
    image(visualized, 0, 0);
  }
}