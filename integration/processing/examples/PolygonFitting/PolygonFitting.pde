// Converts the image into a binary image, finds the contours of each blob, and then fits
// a polygon to each external contour.

import boofcv.processing.*;
import boofcv.struct.image.*;
import georegression.struct.point.*;
import java.util.*;

PImage input;
List<List<Point2D_I32>> polygons;

void setup() {

  input = loadImage("simple_objects.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleGray gray = Boof.gray(input,ImageDataType.F32);

  // Find the initial set of contours automatically select a threshold
  // using the Otsu method
  SimpleContourList contours = gray.thresholdOtsu(false).erode8(1).contour().getContours();

  // filter contours which are too small
  List<SimpleContour> list = contours.getList();
  List<SimpleContour> prunedList = new ArrayList<SimpleContour>();

  for ( SimpleContour c : list ) {
    if ( c.getContour().external.size() >= 200 ) {
      prunedList.add( c );
    }
  }

  // create a new contour list
  contours = new SimpleContourList(prunedList, input.width, input.height);

  // Fit polygons to external contours
  polygons = contours.fitPolygons(true, 3, 0.1);

  size(input.width, input.height);
}

void draw() {
  // Toggle between the background image and a solid color for clarity
  if( mousePressed ) {
    background(0);
  } else {
    image(input, 0, 0);
  }

  // Configure the line's appearance
  noFill();
  strokeWeight(3);
  stroke(255, 0, 0);

  // Draw each polygon
  for ( List<Point2D_I32> poly : polygons ) {
    beginShape();
    for ( Point2D_I32 p : poly) {
      vertex( p.x, p.y );
    }
    // close the loop
    Point2D_I32 p = poly.get(0);
    vertex( p.x, p.y );
    endShape();
  }
}