/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.gui.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.gui.BoofSwingUtil;
import boofcv.struct.calib.CameraPinholeBrown;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Functions to help with visualizing fiducials.
 *
 * @author Peter Abeles
 */
public class VisualizeFiducial {

	private static final Font font = new Font("Serif", Font.BOLD, 24);
	private static final Color black130 = new Color(0, 0, 0, 130);

	/**
	 * Draws a flat cube to show where the square fiducial is on the image
	 */
	public static void drawLabelCenter( Se3_F64 targetToCamera, CameraPinholeBrown intrinsic, String label,
										Graphics2D g2, double scale ) {
		// Computer the center of the fiducial in pixel coordinates
		Point2D_F64 p = new Point2D_F64();
		Point3D_F64 c = new Point3D_F64();
		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic, targetToCamera);
		worldToPixel.transform(c, p);

		drawLabel(p, label, g2, scale);
	}

	public static void drawLabelCenter( Se3_F64 targetToCamera, CameraPinholeBrown intrinsic, String label,
										Graphics2D g2 ) {
		drawLabelCenter(targetToCamera, intrinsic, label, g2, 1);
	}

	public static void drawLabel( Point2D_F64 locationPixel, String label, Graphics2D g2 ) {
		drawLabel(locationPixel, label, g2, 1);
	}

	public static void drawLabel( Point2D_F64 locationPixel, String label, Graphics2D g2, double scale ) {
		drawLabel(locationPixel, label, font, Color.ORANGE, black130, g2, scale);
	}

	public static void drawLabel( Point2D_F64 locationPixel, String label,
								  Font font, Color cText, Color cBackground,
								  Graphics2D g2, double scale ) {
		// Draw the ID number approximately in the center
		FontMetrics metrics = g2.getFontMetrics(font);
		Rectangle2D r = metrics.getStringBounds(label, null);

		double extra = 5;
		double w = r.getWidth();
		double h = r.getHeight();
		double tx = -r.getCenterX();
		double ty = -r.getCenterY();

		int rx = (int)(scale*locationPixel.x + tx - extra - r.getX()/4 + 0.5);
		int ry = (int)(scale*locationPixel.y + ty - extra - h - r.getY()/4 + 0.5);

		g2.setColor(cBackground);
		g2.fillRoundRect(rx, ry, (int)(w + extra*2), (int)(h + extra*2), (int)(2*extra), (int)(2*extra));
		g2.setColor(cText);
		g2.setFont(font);
		g2.drawString(label, (float)(scale*locationPixel.x + tx), (float)(scale*locationPixel.y + ty));
	}

	public static void drawCube( Se3_F64 targetToCamera, CameraPinholeBrown intrinsic, double width,
								 int lineThickness, Graphics2D g2 ) {
		drawCube(targetToCamera, intrinsic, width, 0.5, lineThickness, g2, 1);
	}

	/**
	 * Draws a flat cube to show where the square fiducial is on the image
	 */
	public static void drawCube( Se3_F64 targetToCamera, CameraPinholeBrown intrinsic, double width,
								 double heightScale, int lineThickness, Graphics2D g2, double scale ) {
		BoofSwingUtil.antialiasing(g2);

		double r = width/2.0;
		double h = width*heightScale;

		Point3D_F64[] corners = new Point3D_F64[8];
		corners[0] = new Point3D_F64(-r, -r, 0);
		corners[1] = new Point3D_F64(r, -r, 0);
		corners[2] = new Point3D_F64(r, r, 0);
		corners[3] = new Point3D_F64(-r, r, 0);
		corners[4] = new Point3D_F64(-r, -r, h);
		corners[5] = new Point3D_F64(r, -r, h);
		corners[6] = new Point3D_F64(r, r, h);
		corners[7] = new Point3D_F64(-r, r, h);

		Point2D_F64[] pixel = new Point2D_F64[8];
		Point2D_F64 p = new Point2D_F64();
		WorldToCameraToPixel transform = PerspectiveOps.createWorldToPixel(intrinsic, targetToCamera);
		for (int i = 0; i < 8; i++) {
			if (!transform.transform(corners[i], p)) {
				System.err.println("Crap transform failed in drawCube!");
				return;
			}
			pixel[i] = p.copy();
		}

		Line2D.Double l = new Line2D.Double();
		g2.setStroke(new BasicStroke(lineThickness));
		g2.setColor(Color.RED);

		drawLine(g2, l, pixel[0].x, pixel[0].y, pixel[1].x, pixel[1].y, scale);
		drawLine(g2, l, pixel[1].x, pixel[1].y, pixel[2].x, pixel[2].y, scale);
		drawLine(g2, l, pixel[2].x, pixel[2].y, pixel[3].x, pixel[3].y, scale);
		drawLine(g2, l, pixel[3].x, pixel[3].y, pixel[0].x, pixel[0].y, scale);

		g2.setColor(Color.BLACK);
		drawLine(g2, l, pixel[0].x, pixel[0].y, pixel[4].x, pixel[4].y, scale);
		drawLine(g2, l, pixel[1].x, pixel[1].y, pixel[5].x, pixel[5].y, scale);
		drawLine(g2, l, pixel[2].x, pixel[2].y, pixel[6].x, pixel[6].y, scale);
		drawLine(g2, l, pixel[3].x, pixel[3].y, pixel[7].x, pixel[7].y, scale);

		g2.setColor(new Color(0x00, 0xFF, 0x00, 255));
		drawLine(g2, l, pixel[4].x, pixel[4].y, pixel[5].x, pixel[5].y, scale);
		g2.setColor(new Color(0xC0, 0x10, 0xC0, 255));
		drawLine(g2, l, pixel[5].x, pixel[5].y, pixel[6].x, pixel[6].y, scale);
		g2.setColor(new Color(0x00, 0xA0, 0xC0, 255));
		drawLine(g2, l, pixel[6].x, pixel[6].y, pixel[7].x, pixel[7].y, scale);
		g2.setColor(Color.BLUE);
		drawLine(g2, l, pixel[7].x, pixel[7].y, pixel[4].x, pixel[4].y, scale);
	}

	public static void drawLine( Graphics2D g2, Line2D.Double line, double x0, double y0, double x1, double y1 ) {
		line.setLine(x0, y0, x1, y1);
		g2.draw(line);
	}

	public static void drawLine( Graphics2D g2, Line2D.Double line,
								 double x0, double y0, double x1, double y1,
								 double scale ) {
		line.setLine(scale*x0, scale*y0, scale*x1, scale*y1);
		g2.draw(line);
	}

	/**
	 * Renders a translucent chessboard pattern
	 *
	 * @param g2 Graphics object it's drawn in
	 * @param fiducialToPixel Coverts a coordinate from fiducial into pixel
	 * @param numRows Number of rows in the calibration grid
	 * @param numCols Number of columns in the calibration grid
	 * @param squareWidth Width of each square
	 */
	public static void drawChessboard( Graphics2D g2, WorldToCameraToPixel fiducialToPixel,
									   int numRows, int numCols, double squareWidth ) {
		Point3D_F64 fidPt = new Point3D_F64();
		Point2D_F64 pixel0 = new Point2D_F64();
		Point2D_F64 pixel1 = new Point2D_F64();
		Point2D_F64 pixel2 = new Point2D_F64();
		Point2D_F64 pixel3 = new Point2D_F64();
		Line2D.Double l = new Line2D.Double();

		int[] polyX = new int[4];
		int[] polyY = new int[4];

		int alpha = 100;
		Color red = new Color(255, 0, 0, alpha);
		Color black = new Color(0, 0, 0, alpha);

		for (int row = 0; row < numRows; row++) {
			double y0 = -numRows*squareWidth/2 + row*squareWidth;
			for (int col = row%2; col < numCols; col += 2) {
				double x0 = -numCols*squareWidth/2 + col*squareWidth;

				fidPt.setTo(x0, y0, 0);
				fiducialToPixel.transform(fidPt, pixel0);
				fidPt.setTo(x0 + squareWidth, y0, 0);
				fiducialToPixel.transform(fidPt, pixel1);
				fidPt.setTo(x0 + squareWidth, y0 + squareWidth, 0);
				fiducialToPixel.transform(fidPt, pixel2);
				fidPt.setTo(x0, y0 + squareWidth, 0);
				fiducialToPixel.transform(fidPt, pixel3);

				polyX[0] = (int)(pixel0.x + 0.5);
				polyX[1] = (int)(pixel1.x + 0.5);
				polyX[2] = (int)(pixel2.x + 0.5);
				polyX[3] = (int)(pixel3.x + 0.5);

				polyY[0] = (int)(pixel0.y + 0.5);
				polyY[1] = (int)(pixel1.y + 0.5);
				polyY[2] = (int)(pixel2.y + 0.5);
				polyY[3] = (int)(pixel3.y + 0.5);

				g2.setColor(black);
				g2.fillPolygon(polyX, polyY, 4);

				g2.setColor(red);
				drawLine(g2, l, pixel0.x, pixel0.y, pixel1.x, pixel1.y);
				drawLine(g2, l, pixel1.x, pixel1.y, pixel2.x, pixel2.y);
				drawLine(g2, l, pixel2.x, pixel2.y, pixel3.x, pixel3.y);
				drawLine(g2, l, pixel3.x, pixel3.y, pixel0.x, pixel0.y);
			}
		}
	}
}
