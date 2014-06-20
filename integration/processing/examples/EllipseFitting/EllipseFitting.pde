// Converts the image into a binary image, finds the contours of each blob, and then fits
// an ellipse to each external contour.

import boofcv.processing.*;
import boofcv.struct.image.*;
import georegression.struct.shapes.*;
import java.util.*;

PImage input;
List<EllipseRotated_F64> external;
List<EllipseRotated_F64> internal;

void setup() {

  input = loadImage("shapes02.png");

  // Convert the image into a simplified BoofCV data type
  SimpleGray gray = Boof.gray(input,ImageDataType.F32);

  // Threshold the image using its mean value
  double threshold = gray.mean();

  // Find the set of contours
  SimpleContourList contours = gray.threshold(threshold, true).erode8(1).contour().getContours();

  // Fit ellipses to internal and external contours
  external = contours.fitEllipses(true);
  internal = contours.fitEllipses(false);

  size(input.width, input.height);
}

void draw() {
  image(input, 0, 0);

  // draw the internal and external ellipses different colors
  if ( !mousePressed ) {
    drawEllipses(external, 0xFFFF0000);
    drawEllipses(internal, 0xFF00FF00);
  }
}

void drawEllipses( List<EllipseRotated_F64> ellipses, int colorRGB ) {
  // Configure the line's appearance
  noFill();
  strokeWeight(3);
  stroke((colorRGB>>16) & 0xFF, (colorRGB>>8) & 0xFF, colorRGB & 0xFF);

  for ( EllipseRotated_F64 e : ellipses ) {
    pushMatrix();
    translate((float)e.center.x, (float)e.center.y);
    rotate((float)e.phi);  
    ellipse(0, 0, (float)e.a*2, (float)e.b*2); 
    popMatrix();
  }
}