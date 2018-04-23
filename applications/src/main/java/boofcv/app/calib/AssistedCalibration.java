/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.app.calib;

import boofcv.abst.fiducial.calib.CalibrationDetectorChessboard;
import boofcv.abst.fiducial.calib.CalibrationDetectorCircleHexagonalGrid;
import boofcv.abst.fiducial.calib.CalibrationDetectorCircleRegularGrid;
import boofcv.abst.fiducial.calib.CalibrationDetectorSquareGrid;
import boofcv.abst.geo.calibration.DetectorFiducialCalibration;
import boofcv.alg.geo.calibration.CalibrationObservation;
import boofcv.io.UtilIO;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.geo.PointIndex2D_F64;
import boofcv.struct.image.GrayF32;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>GUI which guides a person along to help ensure sufficient geometry variation and that they cover the entire screen,
 * including the edges, sufficiently.  The calibration process is broken up into three steps.</p>
 * <ol>
 * <li>Scale determination</li>
 * <li>Remove the dot<li>
 * <li>Fill the image<li>
 * </ol>
 *
 * <p>1) Scale Determination.  In this step user holds up the target approximately perpendicular to the camera and
 * it determines its visual size.  This information is then used to select the outer boundary that it's going
 * to attempt to fill.  That boundary is filled with a blue rectangle.  The canonical target is also found.  This
 * target is used to estimation how blurred the target is later on.
 * </p>
 *
 * <p>2) Remove The Dots.  Here the user is directed to move dots from the border of the image.  This is done to ensure
 * that the edges are sufficiently sampled to produce a good estimate of lens distortion.  Circles are drawn to
 * each location the fiducial should be moved to and are refered to as magnets.</p>
 *
 * <p>3) Fill The Image.  The location of previously sampled location is tinted blue and here the user can sample
 * more locations in an attempt to cover the image with blue.  A percentage of the image is not enforced and the
 * user can click finish at any point, as long as the geometric requirement is meet.</p>
 *
 * <p>Motion blue or other sources of distortion are minimized by requiring the user keep the fiducial still before
 * a picture is taken.  Then it selects the most in focus image from the period of time it has being held still. The
 * user is shown visually how blurred the image with a "preview" and a meter showing the blur error magnitude.</p>
 *
 * <p>For the initial linear estimate to work the fiducial needs to be seen at different orientations.  Feedback is
 * provided to the user through progress bar.  The percent filled is set by solving the linear system at looking at
 * the second smallest singular value.  One its hits the minimum number at least once the requirement has been meet.</p>
 *
 * @author Peter Abeles
 */
public class AssistedCalibration {

	public static final String OUTPUT_DIRECTORY = "calibration_data";
	public static final String IMAGE_DIRECTORY = "images";

	// determines how straight the target needs to be when selecting the canonical size
	double CENTER_SKEW = 0.93;
	// how long in seconds does the fiducial need to be held still
	double STILL_THRESHOLD = 2.0;
	// The still time is displayed after this number of seconds
	double DISPLAY_TIME = 0.5;


	// visual size of a magnet in pixels
	int MAGNET_RADIUS;

	// how far from the image border should the rectangle be drawn.  This limited by how close to the border
	// the fiducial is allowed to go
	int padding;

	// input image
	GrayF32 input;

	// detects the calibration target
	DetectorFiducialCalibration detector;

	int imageSize;
	int imageWidth,imageHeight;
	double canonicalWidth;

	CalibrationView view;

	List<Point2D_F64> sidesCollision = new ArrayList<>();
	List<Point2D_F64> focusQuad = new ArrayList<>();

	Graphics2D g2;
	Ellipse2D.Double ellipse = new Ellipse2D.Double();

	DetectUserActions actions = new DetectUserActions();
	AssistedCalibrationGui gui;

	// used to compute the geometric quality of collected fiducials
	ComputeGeometryScore quality;

	// state that the algorithm is in
	State state = State.DETERMINE_SIZE;

	// List of active magnets that need to be collected
	List<Magnet> magnets = new ArrayList<>();
	int totalMagnets;

	// Selects which image to save
	ImageSelectorAndSaver saver;

	// number of debug images saved
	int totalDebugSave = 0;

	// if true then there was sufficient geometric diversity at least once
	boolean geometryTrigger = false;

	/**
	 * Constructor with configuration
	 *
	 * @param detector Target detector
	 * @param quality Used to compute geometry score
	 * @param gui Visualization
	 * @param outputDirectory Root directory for where all output will be stored
	 * @param imageDirectory  Where images will be stored.  Relative to outputDirectory
	 */
	public AssistedCalibration(DetectorFiducialCalibration detector,
							   ComputeGeometryScore quality,
							   AssistedCalibrationGui gui,
							   String outputDirectory , String imageDirectory ) {
		File outputDir = new File(outputDirectory);
		if( outputDir.exists() ) {
			System.out.println("Deleting output directory "+outputDirectory);
			UtilIO.deleteRecursive(outputDir);
		}

		this.detector = detector;
		this.gui = gui;
		this.quality = quality;
		this.saver = new ImageSelectorAndSaver(new File(outputDir,imageDirectory).getPath());

		if( detector instanceof CalibrationDetectorChessboard) {
			view = new CalibrationView.Chessboard();
		} else if( detector instanceof CalibrationDetectorSquareGrid) {
			view = new CalibrationView.SquareGrid();
		} else if( detector instanceof CalibrationDetectorCircleHexagonalGrid) {
			view = new CalibrationView.CircleHexagonalGrid();
		} else if( detector instanceof CalibrationDetectorCircleRegularGrid) {
			view = new CalibrationView.CircleRegularGrid();
		} else {
			throw new RuntimeException("Unknown calibration detector type: "+detector.getClass().getSimpleName());
		}
		view.initialize(detector);
	}

	public AssistedCalibration(DetectorFiducialCalibration detector, ComputeGeometryScore quality, AssistedCalibrationGui gui) {
		this(detector,quality,gui,OUTPUT_DIRECTORY,IMAGE_DIRECTORY);
	}

	public void init( int imageWidth , int imageHeight ) {
		actions.setImageSize(imageWidth,imageHeight);

		MAGNET_RADIUS = Math.max(5,(int)(Math.min(imageWidth,imageHeight)/30.0));
	}

	public void process(GrayF32 gray , BufferedImage image ) {

		this.input = gray;
		imageWidth = gray.width;
		imageHeight = gray.height;
		imageSize = Math.min(gray.width, gray.height);

		g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		boolean success = detector.process(gray);

		actions.update(success,detector.getDetectedPoints());

		if( gui.getInfoPanel().forceSaveImage ) {
			String name = String.format("debug_save_%03d.png",totalDebugSave++);
			System.out.println("Saved image forced. "+name);
			UtilImageIO.saveImage(image,name);
			gui.getInfoPanel().resetForceSave();
		}

		switch( state ) {
			case DETERMINE_SIZE:
				handleDetermineSize(success);
				break;

			case REMOVE_DOTS:
				handleClearDots(success);
				break;

			case FILL_SCREEN:
				handleFillScreen(success);
		}

		gui.setImage(image);
		gui.repaint();
	}

	private void handleDetermineSize( boolean detected ) {
		String message = "Determine Scale: Hold target in view and center";

		if( detected ) {
			double stationaryTime = actions.getStationaryTime();

			CalibrationObservation points = detector.getDetectedPoints();
			view.getQuadFocus(points, focusQuad);

			double top = focusQuad.get(0).distance(focusQuad.get(1));
			double right = focusQuad.get(1).distance(focusQuad.get(2));
			double bottom = focusQuad.get(2).distance(focusQuad.get(3));
			double left = focusQuad.get(3).distance(focusQuad.get(0));

			double ratioHorizontal = Math.min(top,bottom)/Math.max(top,bottom);
			double ratioVertical = Math.min(left,right)/Math.max(left,right);

			boolean skewed = false;
			if( ratioHorizontal <= CENTER_SKEW || ratioVertical <= CENTER_SKEW) {
				actions.resetStationary();
				skewed = true;
				ratioHorizontal *= 100.0;
				ratioVertical *= 100.0;

				message = String.format("Straighten out.  H %3d   V %3d", (int) ratioHorizontal, (int) ratioVertical);
			} else {
				if (stationaryTime > STILL_THRESHOLD) {
					actions.resetStationary();
					saver.setTemplate(input, focusQuad);
					gui.getInfoPanel().updateTemplate(saver.getTemplate());
					actions.resetStationary();
					state = State.REMOVE_DOTS;
					canonicalWidth = Math.max(top,bottom);
					padding = (int)(view.getBufferWidth(canonicalWidth)*1.1);
					selectMagnetLocations();
				}
				if (stationaryTime > DISPLAY_TIME) {
					message = String.format("Hold still:  %6.1f", stationaryTime);
				}
			}

			int r = 6;
			int w = 2 * r + 1;
			if( skewed ) {
				g2.setColor(Color.BLUE);
				for (int i = 0; i < points.size(); i++) {
					Point2D_F64 p = points.points.get(i);

					ellipse.setFrame(p.x - r, p.y - r, w, w);
					g2.draw(ellipse);
				}
			} else {
				renderCalibrationPoints(stationaryTime,points.points);
			}
		}

		gui.setMessage(message);
	}

	private void selectMagnetLocations() {
		magnets.add( new Magnet(imageWidth/2,padding,false) );
		magnets.add( new Magnet(imageWidth/2,imageHeight-padding,false) );
		magnets.add( new Magnet(padding,imageHeight/2,false) );
		magnets.add( new Magnet(imageWidth-padding,imageHeight/2,false) );

		magnets.add( new Magnet(padding,padding,true) );
		magnets.add( new Magnet(padding,imageHeight-padding,true) );
		magnets.add( new Magnet(imageWidth-padding,imageHeight-padding,true) );
		magnets.add( new Magnet(imageWidth-padding,padding,true) );

		totalMagnets = magnets.size();
	}

	boolean pictureTaken = false;
	FastQueue<Polygon2D_F64> regions = new FastQueue<Polygon2D_F64>(Polygon2D_F64.class,true) {
		@Override
		protected Polygon2D_F64 createInstance() {
			return new Polygon2D_F64(4);
		}
	};

	private void handleClearDots( boolean detected ) {
		String message = "Clear the dots!";

		if( detected ) {

			gui.getInfoPanel().updateView(saver.getCurrentView());
			gui.getInfoPanel().updateFocusScore(saver.getFocusScore());

			double stationaryTime = actions.getStationaryTime();
			CalibrationObservation points = detector.getDetectedPoints();
			view.getSidesCollision(points, sidesCollision);
			view.getQuadFocus(points, focusQuad);

			boolean closeToMagnet = false;
			for (int i = 0; i < magnets.size(); i++) {
				closeToMagnet |= magnets.get(i).handleDetection();
			}

			boolean resetImageSelector = true;
			if( pictureTaken ) {
				if( stationaryTime >= STILL_THRESHOLD ) {
					message = "Move somewhere else";
				} else {
					pictureTaken = false;
				}
			} else if( stationaryTime >= STILL_THRESHOLD ) {
				if( checkMagnetCapturePicture() ) {
					saver.save();
					pictureTaken = true;
					message = "Move somewhere else";
					captureFiducialPoints();

					gui.getInfoPanel().updateEdgeFill(1.0 - (magnets.size() / (double) totalMagnets));

					if( magnets.isEmpty() ) {
						state = State.FILL_SCREEN;
					}
				}
			} else if( stationaryTime > DISPLAY_TIME ) {
				if( closeToMagnet ) {
					message = String.format("Hold still:  %6.1f", stationaryTime);
					resetImageSelector = false;
				} else {
					message = "Move closer to a dot";
				}
			}

			// save the images if the fiducial is being held still prior to capture
			if( resetImageSelector ) {
				saver.clearHistory();
				saver.updateScore(input, focusQuad);
			} else {
				saver.process( input, focusQuad);
			}

			drawPadding();
			renderMagnets();
			renderArrows();
			renderCalibrationPoints(stationaryTime, points.points);
		} else {
			drawPadding();
			saver.clearHistory();
			for (int i = 0; i < magnets.size(); i++) {
				magnets.get(i).handleNoDetection();
			}
			renderMagnets();
		}

		renderFillPolygons();

		gui.setMessage(message);
	}

	private void handleFillScreen( boolean detected ) {
		String message = "Tint the screen!";
		drawPadding();

		if( detected ) {
			gui.getInfoPanel().updateView(saver.getCurrentView());
			gui.getInfoPanel().updateFocusScore(saver.getFocusScore());

			double stationaryTime = actions.getStationaryTime();
			CalibrationObservation points = detector.getDetectedPoints();
			view.getSidesCollision(points, sidesCollision);
			view.getQuadFocus(points, focusQuad);

			saver.process(input, focusQuad);

			boolean resetImageSelector = true;
			if( pictureTaken ) {
				if( stationaryTime >= STILL_THRESHOLD ) {
					message = "Move somewhere else";
				} else {
					pictureTaken = false;
				}
			} else if( stationaryTime >= STILL_THRESHOLD ) {
				saver.save();
				pictureTaken = true;
				message = "Move somewhere else";
				captureFiducialPoints();
			} else if( stationaryTime > DISPLAY_TIME ) {
				resetImageSelector = false;
				message = String.format("Hold still:  %6.1f", stationaryTime);
			}

			// save the images if the fiducial is being held still prior to capture
			if( resetImageSelector ) {
				saver.clearHistory();
			} else {
				saver.process( input, focusQuad);
			}

			renderCalibrationPoints(stationaryTime, points.points);
		}
		renderFillPolygons();

		gui.setMessage(message);
	}

	private void renderCalibrationPoints(double stationaryTime, List<PointIndex2D_F64> points) {
		int shade = Math.min(255, (int) (255.0 * (stationaryTime / STILL_THRESHOLD)));
		if( pictureTaken ) {
			g2.setColor(new Color(0, shade, 0));
		} else {
			g2.setColor(new Color(shade, 0, Math.max(0,255-shade*2)));
		}

		int r = 6;
		int w = 2 * r;
		for (int i = 0; i < points.size(); i++) {
			PointIndex2D_F64 p = points.get(i);
			ellipse.setFrame(p.x - r, p.y - r, w, w);
			g2.fill(ellipse);
		}
	}

	private void renderFillPolygons() {
		if( regions.size() == 0)
			return;

		g2.setColor(new Color(0, 255, 255, 50));
		int N = regions.get(0).size();
		int polyX[] = new int[N];
		int polyY[] = new int[N];

		for (int i = 0; i < regions.size(); i++) {
			Polygon2D_F64 poly = regions.get(i);
			for (int j = 0; j < poly.size(); j++) {
				Point2D_F64 p = poly.get(j);
				polyX[j] = (int)(p.x+0.5);
				polyY[j] = (int)(p.y+0.5);
			}
			g2.fillPolygon(polyX,polyY,poly.size());
		}
	}

	private void renderMagnets() {
		for (int i = 0; i < magnets.size(); i++) {
			magnets.get(i).render();
		}
	}

	private void renderArrows() {
		for (int i = 0; i < magnets.size(); i++) {
			magnets.get(i).drawArrows();
		}
	}

	/**
	 * Record the area covered in the image by the fiducial, update the quality calculation, and see if it should
	 * enable the save button.
	 */
	private void captureFiducialPoints() {
		Polygon2D_F64 p = regions.grow();
		p.vertexes.resize(sidesCollision.size());
		for (int i = 0; i < sidesCollision.size(); i++) {
			p.get(i).set( sidesCollision.get(i) );
		}
		quality.addObservations(detector.getDetectedPoints());
		gui.getInfoPanel().updateGeometry(quality.getScore());

		// once the user has sufficient geometric variation enable save
		geometryTrigger |= quality.getScore() >= 1.0;
		if( geometryTrigger && magnets.isEmpty() ) {
			gui.getInfoPanel().enabledFinishedButton();
		}
	}

	/**
	 * Checks to see if its near one of the magnets in the image broder
	 */
	private boolean checkMagnetCapturePicture() {
		boolean captured = false;
		Iterator<Magnet> iter = magnets.iterator();
		while( iter.hasNext() ) {
			Magnet i = iter.next();
			if( i.handlePictureTaken() ) {
				iter.remove();
				captured = true;
			}
		}
		return captured;
	}

	private void drawPadding() {
		if( padding <= 0 )
			return;

		g2.setColor(Color.BLUE);
		g2.drawLine(padding,padding,imageWidth-padding,padding);
		g2.drawLine(imageWidth-padding,padding,imageWidth-padding,imageHeight-padding);
		g2.drawLine(imageWidth-padding,imageHeight-padding,padding,imageHeight-padding);
		g2.drawLine(padding,imageHeight-padding,padding,padding);

	}

	private void drawArrow( double x0 , double y0 , double pointX , double pointY ) {
		int pointsX[] = new int[7];
		int pointsY[] = new int[7];

		double tanX = -pointY;
		double tanY =  pointX;

		double w = 0.07;

		pointsX[0] = (int)(x0 +   0*pointX - w*tanX   + 0.5);
		pointsX[1] = (int)(x0 +   0*pointX + w*tanX   + 0.5);
		pointsX[2] = (int)(x0 + 0.7*pointX + w*tanX   + 0.5);
		pointsX[3] = (int)(x0 + 0.7*pointX + 3*w*tanX + 0.5);
		pointsX[4] = (int)(x0 + 1.0*pointX + 0*tanX   + 0.5);
		pointsX[5] = (int)(x0 + 0.7*pointX - 3*w*tanX + 0.5);
		pointsX[6] = (int)(x0 + 0.7*pointX - w*tanX   + 0.5);

		pointsY[0] = (int)(y0 +   0*pointY - w*tanY   + 0.5);
		pointsY[1] = (int)(y0 +   0*pointY + w*tanY   + 0.5);
		pointsY[2] = (int)(y0 + 0.7*pointY + w*tanY   + 0.5);
		pointsY[3] = (int)(y0 + 0.7*pointY + 3*w*tanY + 0.5);
		pointsY[4] = (int)(y0 + 1.0*pointY + 0*tanY   + 0.5);
		pointsY[5] = (int)(y0 + 0.7*pointY - 3*w*tanY + 0.5);
		pointsY[6] = (int)(y0 + 0.7*pointY - w*tanY   + 0.5);

		g2.setColor(Color.BLUE);
		g2.fill(new Polygon(pointsX,pointsY,pointsX.length));

	}

	private double findClosestCenter( Point2D_F64 target , Point2D_F64 result ) {

		Point2D_F64 center = new Point2D_F64();

		double bestDistanceSq = Double.MAX_VALUE;

		for (int i = 0, j = sidesCollision.size()-1; i < sidesCollision.size(); j=i,i++) {
			UtilPoint2D_F64.mean(sidesCollision.get(i), sidesCollision.get(j),center);

			double d = center.distance2(target);

			if( d < bestDistanceSq ) {
				bestDistanceSq = d;
				result.set(center);
			}
		}

		return Math.sqrt(bestDistanceSq);
	}

	private double findClosestCorner( Point2D_F64 target , Point2D_F64 result ) {


		double bestDistanceSq = Double.MAX_VALUE;

		for (int i = 0; i < sidesCollision.size(); i++) {
			double d = sidesCollision.get(i).distance2(target);

			if( d < bestDistanceSq ) {
				bestDistanceSq = d;
				result.set(sidesCollision.get(i));
			}
		}

		return Math.sqrt(bestDistanceSq);
	}

	private class Magnet
	{
		Point2D_F64 location = new Point2D_F64();

		Point2D_F64 closest = new Point2D_F64();

		boolean corner;

		boolean close = false;

		public Magnet( double x , double y , boolean corner ) {
			location.set(x,y);
			this.corner = corner;
		}

		public boolean handlePictureTaken() {
			return findClosest() <= MAGNET_RADIUS*2;
		}

		public boolean handleDetection() {
			close = findClosest() <= MAGNET_RADIUS*2;
			return close;
		}

		public void drawArrows() {
			double distance = findClosest();

			if( distance <= MAGNET_RADIUS*10 ) {
				double pointingX = location.x - closest.x;
				double pointingY = location.y - closest.y;

				drawArrow(closest.x,closest.y,pointingX,pointingY);
			}
		}

		public void handleNoDetection() {
			close = false;
		}

		private double findClosest() {
			double distance;
			if( corner ) {
				distance = findClosestCorner(location, closest);
			} else {
				distance = findClosestCenter(location, closest);
			}
			return distance;
		}

		public void render() {
			if( close ) {
				g2.setColor(Color.RED );
			} else {
				g2.setColor(Color.yellow);
			}
			int w = MAGNET_RADIUS*2+1;
			int x = (int)(location.x+0.5);
			int y = (int)(location.y+0.5);
			g2.fillOval(x-MAGNET_RADIUS,y-MAGNET_RADIUS,w,w);

			int strokeWidth = Math.max(2,MAGNET_RADIUS/8);
			w += strokeWidth;
			int r = strokeWidth/2;
			g2.setStroke(new BasicStroke(strokeWidth));
			g2.setColor(Color.BLACK);
			g2.drawOval(x - MAGNET_RADIUS - r, y - MAGNET_RADIUS-r,w,w);
		}
	}

	public boolean isFinished() {
		return gui.getInfoPanel().isFinished();
	}

	enum State {
		DETERMINE_SIZE,
		REMOVE_DOTS,
		FILL_SCREEN
	}

}
