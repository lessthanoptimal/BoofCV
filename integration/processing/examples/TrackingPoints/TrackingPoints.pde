// Launches the webcam and detects corner features in the image which are then tracked using KLT.
// KLT has many uses and can be used to track objects in the view or estimate the 3D structure
// of a scene.  In this case it just looks cool.

import processing.video.*;
import boofcv.processing.*;
import boofcv.struct.image.*;
import georegression.struct.point.*;
import boofcv.abst.feature.detect.interest.*;

Capture cam;
SimpleTrackerPoints tracker;
int target = 100;

void setup() {
  // Open up the camera so that it has a video feed to process
  initializeCamera(320,240);
  size(cam.width, cam.height);

  ConfigGeneralDetector confDetector = new ConfigGeneralDetector();
  confDetector.radius = 4;
  confDetector.threshold = 2;
  tracker = Boof.trackerKlt(null,confDetector,ImageDataType.F32);
}


void draw() {
  if (cam.available() == true) {
    cam.read();
    // track features in the camera
    tracker.process(cam);

    // spawn new tracks if there are too few active
    if( tracker.totalTracks() < target ) {
      tracker.spawnTracks();
      target = (int)(tracker.totalTracks()*0.4);
      target = target < 30 ? 30 : target;
    }
  }
  image(cam, 0, 0);

  // Draw a circle around each point being tracked
  stroke(0xFF,0,0);
  int N = tracker.totalTracks();
  for( int i = 0; i < N; i++ ) {
     Point2D_F64 p = tracker.getLocation(i);
     ellipse((float)p.x,(float)p.y,5,5);
  }
}

void initializeCamera( int desiredWidth , int desiredHeight ) {
  String[] cameras = Capture.list();

  if (cameras.length == 0) {
    println("There are no cameras available for capture.");
    exit();
  } else {
    cam = new Capture(this, desiredWidth,desiredHeight);
    cam.start();
  }
}
