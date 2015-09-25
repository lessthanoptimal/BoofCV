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
import georegression.geometry.UtilPolygons2D_F64;
import georegression.metric.Area2D_F64;
import georegression.metric.Intersection2D_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_F64;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * @author Peter Abeles
 */
// TODO Option to flip image?
	// TODO Check to see if sufficient area is inside desired rectangle
	// TODO Check to see if slant is sufficient and in the correct direction
	// TODO check blur factor
public class AssistedCalibration {

	double shrink = 0.85;

	double SIZE_MATCH_THRESHOLD = 0.85;

	double paddingWidth = 1.0/3.0; // TODO get from CalibrationView
	double paddingHeight = 1.0/5.0;

	PlanarCalibrationDetector detector;

	int imageSize;

	CalibrationView view;

	Quadrilateral_F64 desired = new Quadrilateral_F64();
	Polygon2D_F64 foundHull = new Polygon2D_F64();

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

	int targetSide;

	public AssistedCalibration(PlanarCalibrationDetector detector) {
		this.detector = detector;

		view = new CalibrationView() {
			@Override
			public void getBlurFactor(ImageFloat32 gray) {}

			@Override
			public void getBounds(List<Point2D_F64> detections, Quadrilateral_F64 quad) {

			}

			@Override
			public double getWidthHeightRatio() {
				return 3.0/5.0;
			}
		};
	}

	public void process( ImageFloat32 gray , BufferedImage image ) {

		imageSize = Math.min(gray.width,gray.height);

		g2 = image.createGraphics();

		boolean success = detector.process(gray);

		configureStep(step);
		configureDesired(gray.width, gray.height);

		drawLine(desired.get(0), desired.get(1));
		drawLine(desired.get(1), desired.get(2));
		drawLine(desired.get(2), desired.get(3));
		drawLine(desired.get(3), desired.get(0));

		int w = 11;
		int r = w/2;

		if( success ) {
			List<Point2D_F64> detected = detector.getDetectedPoints();

			int numInside = 0;
			for (int i = 0; i < detected.size(); i++) {
				Point2D_F64 p = detected.get(i);
				ellipse.setFrame(p.x - r-1, p.y - r-1, w, w);

				if (Intersection2D_F64.contains(desired, p)) {
					numInside++;
					g2.setColor(Color.red);
					g2.fill(ellipse);
				} else {
					g2.setColor(Color.blue);
					g2.draw(ellipse);
				}
			}

			if( numInside == detected.size() ) {
				UtilPolygons2D_F64.convexHull(detected,foundHull);
				double foundArea = Area2D_F64.polygonConvex(foundHull);
				double desiredArea = Area2D_F64.quadrilateral(desired);

				Polygon2D_F64 hack = new Polygon2D_F64(4);
				UtilPolygons2D_F64.convert(desired,hack);
//				System.out.println("areas "+ Area2D_F64.polygonConvex(hack)+"  "+desiredArea);

				// Use square root since it more closely follows what a person visually sees
				double ratio = Math.sqrt(foundArea/desiredArea);
				System.out.println("area ratio "+ratio);

				if( ratio >= SIZE_MATCH_THRESHOLD)
					step++;
			}

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

	private void drawGuideArrows( Graphics2D g2 , List<Point2D_F64> detections ) {
		if( targetSide < 0 )
			return;

		double length = 0.05*imageSize;

		double dx,dy;

		switch( targetSide ) {
			case 0: dx =  0; dy =  1; break;
			case 1: dx =  1; dy =  0; break;
			case 2: dx =  0; dy = -1; break;
			case 3: dx = -1; dy =  0; break;
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

		double w = 0.1;

		pointsX[0] = (int)(x0 +   0*pointX - w*tanX   + 0.5);
		pointsX[1] = (int)(x0 +   0*pointX + w*tanX   + 0.5);
		pointsX[2] = (int)(x0 +   1*pointX + w*tanX   + 0.5);
		pointsX[3] = (int)(x0 +   1*pointX + 3*w*tanX + 0.5);
		pointsX[4] = (int)(x0 + 1.5*pointX + 0*tanX   + 0.5);
		pointsX[5] = (int)(x0 +   1*pointX - 3*w*tanX + 0.5);
		pointsX[6] = (int)(x0 +   1*pointX - w*tanX   + 0.5);

		pointsY[0] = (int)(y0 +   0*pointY - w*tanY   + 0.5);
		pointsY[1] = (int)(y0 +   0*pointY + w*tanY   + 0.5);
		pointsY[2] = (int)(y0 +   1*pointY + w*tanY   + 0.5);
		pointsY[3] = (int)(y0 +   1*pointY + 3*w*tanY + 0.5);
		pointsY[4] = (int)(y0 + 1.5*pointY + 0*tanY   + 0.5);
		pointsY[5] = (int)(y0 +   1*pointY - 3*w*tanY + 0.5);
		pointsY[6] = (int)(y0 +   1*pointY - w*tanY   + 0.5);

		g2.setColor(Color.BLUE);
		g2.fill(new Polygon(pointsX,pointsY,pointsX.length));

	}


	public void configureDesired( int imageWidth , int imageHeight  ) {

		int centerX = imageWidth/2;
		int centerY = imageHeight/2;

		int maxSide = (int)(Math.min(imageWidth,imageHeight)*0.7);

		desired.a.x = centerX-maxSide*ratioTop/2.0;
		desired.b.x = centerX+maxSide*ratioTop/2.0;
		desired.d.x = centerX-maxSide*ratioBottom/2.0;
		desired.c.x = centerX+maxSide*ratioBottom/2.0;

		desired.a.y = centerY-maxSide*ratioLeft/2.0;
		desired.d.y = centerY+maxSide*ratioLeft/2.0;
		desired.b.y = centerY-maxSide*ratioRight/2.0;
		desired.c.y = centerY+maxSide*ratioRight/2.0;

		Rectangle2D_F64 bounding = new Rectangle2D_F64();

		UtilPolygons2D_F64.bounding(desired,bounding);

		double shiftX=0,shiftY = 0;

		double bufferX = Math.max(maxSide*ratioTop,maxSide*ratioBottom)*paddingWidth;
		double bufferY = Math.max(maxSide*ratioLeft,maxSide*ratioRight)*paddingHeight;

		if( shiftVertical > 0 ) {
			shiftY = -Math.max(0,bounding.p0.y-bufferY);
		} else if( shiftVertical < 0 ) {
			shiftY = Math.max(0,imageHeight - bounding.p1.y-bufferY);
		}

		if( shiftHorizontal > 0 ) {
			shiftX = -Math.max(0,bounding.p0.x-bufferX);
		} else if( shiftHorizontal < 0 ) {
			shiftX = Math.max(0,imageWidth - bounding.p1.x-bufferX);
		}

		for (int i = 0; i < 4; i++) {
			desired.get(i).x += shiftX;
			desired.get(i).y += shiftY;
		}
	}

	private void configureStep( int step ) {
		double fiducialRatio = view.getWidthHeightRatio();

		if( fiducialRatio > 1.0 ) {
			widthRatio = 1.0;
			heightRatio = 1.0/fiducialRatio;
		} else {
			widthRatio = fiducialRatio;
			heightRatio = 1.0;
		}

		ratioTop = heightRatio;
		ratioBottom = heightRatio;
		ratioLeft = widthRatio;
		ratioRight = widthRatio;

		shiftVertical = 0;
		shiftHorizontal = 0;

		switch( step ) {
			case 0: ratioBottom *= shrink; shiftVertical=-1; targetSide=0;break; // bottom
			case 1: ratioRight *= shrink; shiftHorizontal=-1;targetSide=1;break; // right
			case 2: ratioTop *= shrink; shiftVertical=1;targetSide=2;break; // top
			case 3: ratioLeft *= shrink;shiftHorizontal=1; targetSide=3;break; // left
			case 4: ratioBottom *= shrink;targetSide=-1; break; // center
		}

	}

}
