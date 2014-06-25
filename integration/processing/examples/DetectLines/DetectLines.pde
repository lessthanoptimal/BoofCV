// Detects lines inside an image using different variables of a Hough detector.
// Each time you click the image it switches to a different type.

import boofcv.processing.*;
import boofcv.struct.image.*;
import boofcv.alg.feature.detect.line.*;
import boofcv.factory.feature.detect.line.*;
import georegression.struct.line.*;
import java.util.*;

PImage input;
List<LineParametric2D_F32> linesPolar;
List<LineParametric2D_F32> linesFoot;
List<LineParametric2D_F32> linesFootSub;

void setup() {

  input = loadImage("simple_objects.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleGray gray = Boof.gray(input,ImageDataType.F32);

  // Find lines in the image using different techniques
  int maxLines = 10;
  linesPolar = gray.linesHoughPolar(new ConfigHoughPolar(50,maxLines));
  linesFoot = gray.linesHoughFoot(new ConfigHoughFoot(maxLines));
  linesFootSub = gray.linesHoughFootSub(new ConfigHoughFootSubimage(maxLines));

  size(input.width, input.height);
}

int which = 0;
void draw() {
  image(input, 0, 0);

  if( which == 0 ) {
    drawLines(linesPolar,"Hough Polar");  
  } else if( which == 1 ) {
    drawLines(linesFoot,"Hough Foot");  
  } else if( which == 2 ) {
    drawLines(linesFootSub,"Hough Foot Subimage");  
  }
}

void drawLines( List<LineParametric2D_F32> lines, String name ) {
  noFill();
  strokeWeight(3);
  stroke(0xFF,0,0);

  for( LineParametric2D_F32 p : lines ) {
    LineSegment2D_F32 ls = LineImageOps.convert(p,width,height);
    line((float)ls.a.x, (float)ls.a.y, (float)ls.b.x, (float)ls.b.y); 
  }

  textFont(createFont("Arial", 24, true));
  textAlign(CENTER);
  fill(0, 0xFF, 0);
  text(name, width/2, height/12);
}

void mouseClicked() {
    which = (which + 1 ) % 3;  
}