// Detects interest points in an image.  Then draws the size and location of the found interest point

import boofcv.processing.*;
import boofcv.struct.image.*;
import georegression.struct.point.*;
import java.util.*;

PImage input;
// BoofCV supports several feature descriptors and detectors, but SIFT and SURF are the
// easest and most well known so they are provided in the 'simple' interface.  The others
// can still be used but will take a bit more effort and understanding.
SimpleDetectDescribePoint ddp = Boof.detectSurf(true,ImageDataType.F32);
//SimpleDetectDescribePoint ddp = Boof.detectSift(ImageDataType.F32);

void setup() {

  input = loadImage("kayak_02.jpg");

  // Detect and describe the image features. 
  // The feature location and size is visualized later on
  ddp.process(input);

  size(input.width, input.height);
}

void draw() {
  image(input, 0, 0);

  // Visualize the found features
  List<Point2D_F64> location = ddp.getLocations();

  noFill();
  strokeWeight(1);
  stroke(0xFF,0,0);


  for( int i = 0; i < location.size(); i++ ) {
    // Scale isn't provided in the simplified interface because its not required for 
    // standard assocation algorithms
    double scale = ddp.getDetectDescribe().getScale(i);    
    // Get the feature's location
    Point2D_F64 p = location.get(i);

    ellipse((float)p.x, (float)p.y, (float)(2.5*scale), (float)(2.5*scale)); 
  }
}
