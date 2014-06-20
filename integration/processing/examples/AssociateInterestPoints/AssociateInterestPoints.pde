// Detects interest points between two images and matches them together.
// These matches are shown by drawing a line between the matching features in each images.

import boofcv.processing.*;
import boofcv.struct.image.*;
import boofcv.struct.feature.*;
import georegression.struct.point.*;
import java.util.*;

PImage input0;
PImage input1;

// feature locations
List<Point2D_F64> locations0,locations1;

// which features are matched together
List<AssociatedIndex> matches;

void setup() {

  input0 = loadImage("cave_01.jpg");
  input1 = loadImage("cave_02.jpg");

  // BoofCV supports several feature descriptors and detectors, but SIFT and SURF are the
  // easy to configure and well known so they are provided in the 'simple' interface.  The others
  // can still be used but will take a bit more effort and understanding.
  SimpleDetectDescribePoint ddp = Boof.detectSurf(true, ImageDataType.F32);
  //SimpleDetectDescribePoint ddp = Boof.detectSift(ImageDataType.F32);

  // Only greedy association is provided in the simple interface.  Other options are available
  // in BoofCV, such as random forest.
  SimpleAssociateDescription assoc = Boof.associateGreedy(ddp, true);

  // Find the features
  ddp.process(input0);
  locations0 = ddp.getLocations();
  List<TupleDesc> descs0 = ddp.getDescriptions();
  
  ddp.process(input1);
  locations1 = ddp.getLocations();
  List<TupleDesc> descs1 = ddp.getDescriptions();
  
  // associate them
  assoc.associate(descs0,descs1);
  matches = assoc.getMatches();

  size(input0.width*2, input0.height);
}

void draw() {
  int w = input0.width;
  image(input0, 0, 0);
  image(input1, w, 0);

  randomSeed(123);
  strokeWeight(2);
  int count = 0;
  for( AssociatedIndex i : matches ) {
    // only display every 7th feature to make it less cluttered
    if( count++ % 7 != 0 )
      continue;
    
    // assign a random color to make individual feature pairs easier to identify
    stroke(random(256), random(256), random(256));

    // draw a line showing the matching features in each image
    Point2D_F64 p0 = locations0.get(i.src); 
    Point2D_F64 p1 = locations1.get(i.dst); 
    
    line((float)p0.x, (float)p0.y, (float)(w+p1.x), (float)p1.y);
  }
}
