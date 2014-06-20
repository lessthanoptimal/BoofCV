// Applies different image enhancement operators to the input image.
// These operators are designed to make features inside the image easier for a person to see.
// Each time you click the image it switches to a different one.

import boofcv.processing.*;
import boofcv.struct.image.*;

PImage input;
PImage histogram,histogramLocal,sharp4,sharp8;

void setup() {

  input = loadImage("dark.png");

  // Convert the image into a simplified BoofCV data type
  SimpleGray gray = Boof.gray(input,ImageDataType.U8);
  input = gray.convert();

  // Apply different image enhancement routines on the input image
  histogram = gray.histogramEqualize().convert();
  histogramLocal = gray.histogramEqualizeLocal(15).convert();
  sharp4 = gray.enhanceSharpen4().convert();
  sharp8 = gray.enhanceSharpen8().convert();

  size(input.width*2, input.height);
}

int which = 0;
void draw() {
  image(input, 0, 0);
  String s = null;

  // allow the user to switch between different routines by clicking on the image
  switch( which ) {
    case 0: image(histogram, input.width, 0); s = "histogram"; break;
    case 1: image(histogramLocal, input.width, 0); s = "histogram local";break;
    case 2: image(sharp4, input.width, 0); s = "sharpen 4"; break;
    case 3: image(sharp8, input.width, 0); s = "sharpen 8"; break;
  }
  
  textFont(createFont("Arial", 24, true));
  textAlign(CENTER);
  fill(0xFF, 0x4F, 0);
  text(s, 3*width/4, height/12);
}

void mouseClicked() {
    which = (which + 1 ) % 4;  
}