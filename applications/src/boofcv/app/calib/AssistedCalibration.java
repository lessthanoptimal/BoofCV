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
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import org.ddogleg.struct.FastQueue;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
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
	double shrink = 0.85;
	double STILL_THRESHOLD = 2.0;
	double DISPLAY_TIME = 0.5;

	double SIZE_MATCH_THRESHOLD = 0.85;

	int padding;

	PlanarCalibrationDetector detector;

	int imageSize;
	int imageWidth,imageHeight;
	double canonicalWidth;

	CalibrationView view;

	Quadrilateral_F64 desired = new Quadrilateral_F64();
	Polygon2D_F64 foundHull = new Polygon2D_F64();

	List<Point2D_F64> sides = new ArrayList<Point2D_F64>();

	double widthRatio,heightRatio;

	double ratioLeft;
	double ratioRight;
	double ratioTop;
	double ratioBottom;

	int shiftVertical,shiftHorizontal;

	int step = 0;

	Graphics2D g2;
	Line2D.Double line = new Line2D.Double();
	Ellipse2D.Double ellipse = new Ellipse2D.Double();

	Side targetSide;

	DetectUserActions actions = new DetectUserActions();
	AssistedCalibrationGui gui;

	ComputeGeometryScore quality;

	State state = State.DETERMINE_SIZE;

	public AssistedCalibration(PlanarCalibrationDetector detector, ComputeGeometryScore quality, AssistedCalibrationGui gui) {
		this.detector = detector;
		this.gui = gui;
		this.quality = quality;

		view = new CalibrationView.Chessboard();
		view.initialize(detector);
	}

	public void init( int imageWidth , int imageHeight ) {
		actions.setImageSize(imageWidth,imageHeight);
	}

	public void process( ImageFloat32 gray , BufferedImage image ) {

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

			default:
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
					actions.resetStationary();
					state = State.FILL_BOTTOM;
					canonicalWidth = Math.max(top,bottom);
					padding = view.getBufferWidth(canonicalWidth);
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

	boolean pictureTaken = false;
	FastQueue<Polygon2D_F64> regions = new FastQueue<Polygon2D_F64>(Polygon2D_F64.class,true) {
		@Override
		protected Polygon2D_F64 createInstance() {
			return new Polygon2D_F64(4);
		}
	};

	private void handleFillScreen( boolean detected ) {
		String message = "Tint the screen!";
		drawPadding();
		if( detected ) {
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
				pictureTaken = true;
				message = "Move somewhere else";
				captureFiducialPoints();
			} else if( stationaryTime > DISPLAY_TIME ) {
				message = String.format("Hold still:  %6.1f", stationaryTime);
			}

			int red = Math.min(255, (int) (150.0 * (stationaryTime / STILL_THRESHOLD) + 105.0));
			g2.setColor(new Color(red, 0, 0));

			int r = 6;
			int w = 2 * r + 1;
			for (int i = 0; i < points.size(); i++) {
				Point2D_F64 p = points.get(i);
				ellipse.setFrame(p.x - r, p.y - r, w, w);
				g2.fill(ellipse);
			}
		}
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

		gui.setMessage(message);
	}

	private void captureFiducialPoints() {
		Polygon2D_F64 p = regions.grow();
		for (int i = 0; i < 4; i++) {
			p.get(i).set( sides.get(i) );
		}
		quality.addObservations(detector.getDetectedPoints());
		gui.getInfoPanel().updateGeometry(quality.getScore());
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

	// TODO to buffer edge
	// TODO colorize based on disance
	// TODO if within tolerance show a green circle instead
	private void visualizeEdgeGuidance( List<Point2D_F64> detections ) {
		List<Point2D_F64> edges = new ArrayList<Point2D_F64>();

		view.getSides(detections,edges);

		g2.setColor(Color.CYAN);
		g2.setStroke(new BasicStroke(4));
		int best = -1;
		double bestScore = Double.MAX_VALUE;
		for (int i = 0; i < edges.size(); i += 2) {
			Point2D_F64 a = edges.get(i);
			Point2D_F64 b = edges.get(i+1);

			double score;

			switch( targetSide ) {
				case BOTTOM: score = 2*imageHeight-a.y - b.y; break;
				case RIGHT: score = 2*imageWidth-a.x - b.x; break;
				case TOP: score = a.y + b.y; break;
				case LEFT: score = a.x + b.x; break;
				default: throw new RuntimeException("Egads");
			}

			if( score < bestScore ) {
				bestScore = score;
				best = i;
			}
		}

		Point2D_F64 a = edges.get(best);
		Point2D_F64 b = edges.get(best+1);

		switch( targetSide ) {
			case BOTTOM:
				drawArrow(a.x,a.y,0,imageHeight-a.y);
				drawArrow(b.x,b.y,0,imageHeight-b.y);
				break;

			case RIGHT:
				drawArrow(a.x,a.y,imageWidth-a.x,0);
				drawArrow(b.x,b.y,imageWidth-b.x,0);
				break;

			case TOP:
				drawArrow(a.x,a.y,0, -a.y);
				drawArrow(b.x,b.y,0, -b.y);
				break;

			case LEFT:
				drawArrow(a.x,a.y,-a.x,0);
				drawArrow(b.x,b.y,-b.x,0);
				break;
		}
	}

	private void drawLine( Point2D_F64 a , Point2D_F64 b) {
		line.setLine(a.x,a.y, b.x,b.y);
		g2.draw(line);
	}

	private void drawLine( double x0 , double y0 , double x1 , double y1 ) {
		line.setLine(x0, y0, x1, y1);
		g2.draw(line);
	}

	private void drawGuideArrows( List<Point2D_F64> detections ) {
		if( targetSide == null )
			return;

		double length = 0.05*imageSize;

		double dx,dy;

		switch( targetSide ) {
			case BOTTOM: dx =  0; dy =  1; break;
			case RIGHT: dx =  1; dy =  0; break;
			case TOP: dx =  0; dy = -1; break;
			case LEFT: dx = -1; dy =  0; break;
			default: throw new RuntimeException(("BUG!"));
		}

		for (int i = 0; i < detections.size(); i++) {
			Point2D_F64 p = detections.get(i);

			drawArrow(p.x,p.y,length*dx,length*dy);
		}
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


	enum Side {
		LEFT,RIGHT,TOP,BOTTOM
	}

	enum State {
		DETERMINE_SIZE,
		FILL_BOTTOM,
		FILL_LEFT,
		FILL_TOP,
		FILL_RIGHT
	}

}
