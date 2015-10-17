/*
 * Copyright (c) 2011-2015, Peter Abeles. All Rights Reserved.
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

import boofcv.abst.calib.PlanarCalibrationDetector;
import boofcv.struct.image.ImageFloat32;
import georegression.geometry.UtilPoint2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO Determine if the target is stationary enough

// TODO Option to flip image?
	// TODO Check to see if sufficient area is inside desired rectangle
	// TODO Check to see if slant is sufficient and in the correct direction
	// TODO check blur factor


	// TODO compute calibration parameters live?
public class AssistedCalibration {

	double CENTER_SKEW = 0.93;
	double STILL_THRESHOLD = 2.0;
	double DISPLAY_TIME = 0.5;


	int MAGNET_RADIUS;

	int padding;

	ImageFloat32 input;

	PlanarCalibrationDetector detector;

	int imageSize;
	int imageWidth,imageHeight;
	double canonicalWidth;

	CalibrationView view;

	List<Point2D_F64> sides = new ArrayList<Point2D_F64>();

	Graphics2D g2;
	Ellipse2D.Double ellipse = new Ellipse2D.Double();

	Side targetSide;

	DetectUserActions actions = new DetectUserActions();
	AssistedCalibrationGui gui;

	ComputeGeometryScore quality;

	State state = State.DETERMINE_SIZE;

	List<Magnet> magnets = new ArrayList<Magnet>();
	int totalMagnets;

	ImageSectorSaver saver = new ImageSectorSaver();

	public AssistedCalibration(PlanarCalibrationDetector detector, ComputeGeometryScore quality, AssistedCalibrationGui gui) {
		this.detector = detector;
		this.gui = gui;
		this.quality = quality;

		view = new CalibrationView.Chessboard();
		view.initialize(detector);
	}

	public void init( int imageWidth , int imageHeight ) {
		actions.setImageSize(imageWidth,imageHeight);

		MAGNET_RADIUS = Math.max(5,(int)(Math.min(imageWidth,imageHeight)/30.0));
	}

	public void process( ImageFloat32 gray , BufferedImage image ) {

		this.input = gray;
		imageWidth = gray.width;
		imageHeight = gray.height;
		imageSize = Math.min(gray.width, gray.height);

		g2 = image.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		boolean success = detector.process(gray);

		actions.update(success,detector.getDetectedPoints());

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
	}

	private void handleDetermineSize( boolean detected ) {
		String message = "Determine Scale: Hold target in view and center";

		if( detected ) {
			double stationaryTime = actions.getStationaryTime();

			List<Point2D_F64> points = detector.getDetectedPoints();
			view.getSides(points, sides);

			double top = sides.get(0).distance(sides.get(1));
			double right = sides.get(1).distance(sides.get(2));
			double bottom = sides.get(2).distance(sides.get(3));
			double left = sides.get(3).distance(sides.get(0));

			double ratioHorizontal = Math.min(top,bottom)/Math.max(top,bottom);
			double ratioVertical = Math.min(left,right)/Math.max(left,right);

			boolean skewed = false;
			if( ratioHorizontal <= CENTER_SKEW || ratioVertical <= CENTER_SKEW) {
				actions.resetStationary();
				skewed = true;
				ratioHorizontal *= 100.0;
				ratioVertical *= 100.0;

				message = String.format(
						"Straighten out.  H %3d   V %3d", (int) ratioHorizontal, (int) ratioVertical);
			} else {
				if (stationaryTime > STILL_THRESHOLD) {
					saver.setTemplate(input,sides);
					gui.getInfoPanel().updateTemplate(saver.getTemplate());
					actions.resetStationary();
					state = State.REMOVE_DOTS;
					canonicalWidth = Math.max(top,bottom);
					padding = view.getBufferWidth(canonicalWidth);
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
					Point2D_F64 p = points.get(i);

					ellipse.setFrame(p.x - r, p.y - r, w, w);
					g2.draw(ellipse);
				}
			} else {
				int red = Math.min(255, (int) (150.0 * (stationaryTime / STILL_THRESHOLD) + 105.0));
				g2.setColor(new Color(red, 0, 0));

				for (int i = 0; i < points.size(); i++) {
					Point2D_F64 p = points.get(i);
					ellipse.setFrame(p.x - r, p.y - r, w, w);
					g2.fill(ellipse);
				}
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
		drawPadding();

		if( detected ) {
			saver.process(input, sides);
			gui.getInfoPanel().updateView(saver.getCurrentView());
			gui.getInfoPanel().updateFocusScore(saver.getFocusScore());

			double stationaryTime = actions.getStationaryTime();
			List<Point2D_F64> points = detector.getDetectedPoints();
			view.getSides(points, sides);

			boolean closeToMagnet = false;
			for (int i = 0; i < magnets.size(); i++) {
				closeToMagnet |= magnets.get(i).handleDetection();
			}

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
				} else {
					message = "Move closer to a dot";
				}
			}

			renderMagnets();
			renderCalibrationPoints(stationaryTime, points);
		} else {
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
			saver.process(input, sides);
			gui.getInfoPanel().updateView(saver.getCurrentView());
			gui.getInfoPanel().updateFocusScore(saver.getFocusScore());

			double stationaryTime = actions.getStationaryTime();
			List<Point2D_F64> points = detector.getDetectedPoints();
			view.getSides(points, sides);

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
				message = String.format("Hold still:  %6.1f", stationaryTime);
			}

			renderCalibrationPoints(stationaryTime, points);
		}
		renderFillPolygons();

		gui.setMessage(message);
	}

	private void renderCalibrationPoints(double stationaryTime, List<Point2D_F64> points) {
		int shade = Math.min(255, (int) (150.0 * (stationaryTime / STILL_THRESHOLD) + 105.0));
		if( pictureTaken ) {
			g2.setColor(new Color(0, shade, 0));
		} else {
			g2.setColor(new Color(shade, 0, 0));
		}

		int r = 6;
		int w = 2 * r + 1;
		for (int i = 0; i < points.size(); i++) {
			Point2D_F64 p = points.get(i);
			ellipse.setFrame(p.x - r, p.y - r, w, w);
			g2.fill(ellipse);
		}
	}

	private void renderFillPolygons() {
		g2.setColor(new Color(0, 255, 255, 50));
		int polyX[] = new int[4];
		int polyY[] = new int[4];

		for (int i = 0; i < regions.size(); i++) {
			Polygon2D_F64 poly = regions.get(i);
			for (int j = 0; j < 4; j++) {
				Point2D_F64 p = poly.get(j);
				polyX[j] = (int)(p.x+0.5);
				polyY[j] = (int)(p.y+0.5);
			}
			g2.fillPolygon(polyX,polyY,4);
		}
	}

	private void renderMagnets() {
		for (int i = 0; i < magnets.size(); i++) {
			magnets.get(i).render();
		}
	}

	private void captureFiducialPoints() {
		Polygon2D_F64 p = regions.grow();
		for (int i = 0; i < 4; i++) {
			p.get(i).set( sides.get(i) );
		}
		quality.addObservations(detector.getDetectedPoints());
		gui.getInfoPanel().updateGeometry(quality.getScore());
	}

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

		for (int i = 0, j = sides.size()-1; i < sides.size(); j=i,i++) {
			UtilPoint2D_F64.mean(sides.get(i),sides.get(j),center);

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

		for (int i = 0; i < sides.size(); i++) {
			double d = sides.get(i).distance2(target);

			if( d < bestDistanceSq ) {
				bestDistanceSq = d;
				result.set(sides.get(i));
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
			double distance = findClosest();

			if( distance <= MAGNET_RADIUS*10 ) {
				double pointingX = location.x - closest.x;
				double pointingY = location.y - closest.y;

				drawArrow(closest.x,closest.y,pointingX,pointingY);
			}

			close = distance <= MAGNET_RADIUS*2;
			return close;
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



	enum Side {
		LEFT,RIGHT,TOP,BOTTOM
	}

	enum State {
		DETERMINE_SIZE,
		REMOVE_DOTS,
		FILL_SCREEN
	}

}
