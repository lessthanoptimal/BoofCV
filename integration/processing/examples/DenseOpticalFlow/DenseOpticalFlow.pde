// Compares two images from a sequence and finds the motion of each pixel from the first
// image to the second image

import boofcv.processing.*;
import boofcv.struct.image.*;

PImage input0;
PImage input1;
PImage outputFlow;

void setup() {

  // read in two image from a sequence
  input0 = loadImage("Urban2_07.png");
  input1 = loadImage("Urban2_08.png");

  // dense flow calculations can be computationally expensive.  
  // Shrink the images to make it run faster
  input0.resize(input0.width/2, input0.height/2);
  input1.resize(input1.width/2, input1.height/2);

  SimpleDenseOpticalFlow flow = Boof.flowHornSchunckPyramid(null, ImageDataType.F32);
  //  SimpleDenseOpticalFlow flow = Boof.flowBroxWarping(null,ImageDataType.F32);
  //  SimpleDenseOpticalFlow flow = Boof.flowKlt(null,6,ImageDataType.F32);
  //  SimpleDenseOpticalFlow flow = Boof.flowRegion(null,ImageDataType.F32);
  //  SimpleDenseOpticalFlow flow = Boof.flowHornSchunck(null,ImageDataType.F32);

  // process and visualize the results.  The optical flow data can be access via getFlow()
  flow.process(input0, input1);
  outputFlow = flow.visualizeFlow();

  size(input0.width*2, input0.height);
}

void draw() {
  
  // animate the input
  if ( (frameCount / 12) % 2 == 0 )
    image(input0, 0, 0);
  else
    image(input1, 0, 0);

  image(outputFlow, input0.width, 0);
}
