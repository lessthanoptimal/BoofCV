import boofcv.processing.*;
import georegression.struct.point.*;
import java.util.*;

PImage img;
List<List<Point2D_I32>> polygons;

void setup() {

  img = loadImage("simple_objects.jpg");

  // Convert the image into a simplified BoofCV data type
  SimpleGray bimg = Boof.convF32(img);

  // Threshold the image using its mean value
  double threshold = bimg.mean();

  // Find the initial set of contours
  SimpleContourList contours = bimg.threshold(threshold, false).erode8(1).contour().getContours();

  // filter contours which are too small
  List<SimpleContour> list = contours.getList();
  List<SimpleContour> prunedList = new ArrayList<SimpleContour>();

  for ( SimpleContour c : list ) {
    if ( c.getContour().external.size() >= 200 ) {
      prunedList.add( c );
    }
  }

  // create a new contour list
  contours = new SimpleContourList(prunedList, img.width, img.height);

  // Fit polygons to external contours
  polygons = contours.fitPolygons(true, 3, 0.1);

  size(img.width, img.height);
}

void draw() {
  // Toggle between the background image and a solid color for clarity
  if( mousePressed ) {
    fill(0);
    noStroke();
    rect(0, 0, width, height);
  } else {
    image(img, 0, 0);
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