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

package boofcv.gui.feature;

import boofcv.struct.feature.ScalePoint;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;


/**
 * @author Peter Abeles
 */
public class VisualizeFeatures {

	public static void drawPoints( Graphics2D g2 ,
								   Color color ,
								   java.util.List<Point2D_I32> points ,
								   int radius) {

		g2.setStroke(new BasicStroke(2));

		int ro = radius+1;
		int wo = ro*2+1;
		int w = radius*2+1;
		for( Point2D_I32 p : points ) {
			g2.setColor(color);
			g2.fillOval(p.x-radius,p.y-radius,w,w);
			g2.setColor(Color.BLACK);
			g2.drawOval(p.x-ro,p.y-ro,wo,wo);
		}
	}

	public static void drawPoint( Graphics2D g2 , int x , int y , Color color ) {
		drawPoint(g2, x, y, 5, color);
	}

	public static void drawPoint( Graphics2D g2 , int x , int y ,int r,  Color color ) {
		drawPoint(g2, x, y, r, color, true);
	}

	public static void drawPoint( Graphics2D g2 , int x , int y ,int r,  Color color , boolean hasBorder) {
		int w = r*2+1;

		if( hasBorder ) {
			int r2 = r+2;
			int w2 = r2*2+1;

			g2.setColor(Color.BLACK);
			g2.fillOval(x - r2, y - r2, w2, w2);
		}

		g2.setColor(color);
		g2.fillOval(x - r, y - r, w, w);
	}

	public static void drawPoint( Graphics2D g2 , double x , double y ,double r,  Color color , boolean hasBorder) {
		Ellipse2D.Double c = new Ellipse2D.Double();
		double w = r*2;

		if( hasBorder ) {
			double r2 = r+2;
			double w2 = r2*2;

			g2.setColor(Color.BLACK);
			c.setFrame(x - r2, y - r2, w2, w2);
			g2.fill(c);
		}

		g2.setColor(color);
		c.setFrame(x - r, y - r, w, w);
		g2.fill(c);
	}

	public static void drawCross( Graphics2D g2 , int x , int y ,int r ) {
		g2.drawLine(x - r, y, x + r, y);
		g2.drawLine(x, y - r, x, y + r);
	}

	public static void drawCross( Graphics2D g2 , double x , double y ,double r ) {
		Line2D.Double l = new Line2D.Double();
		l.setLine(x-r,y,x+r,y);
		g2.draw(l);
		l.setLine(x, y - r, x, y + r);
		g2.draw(l);
	}

	public static void drawScalePoints( Graphics2D g2 , java.util.List<ScalePoint> points ,
										double scaleToRadius ) {

		g2.setStroke(new BasicStroke(3));

		for( ScalePoint p : points ) {
			if( p.white ) {
				g2.setColor(Color.BLUE);
			} else {
				g2.setColor(Color.RED);
			}
			int r = (int)(p.scale*scaleToRadius +0.5);
			int w = r*2+1;
			g2.drawOval((int)p.x-r,(int)p.y-r,w,w);
		}
	}

	public static void drawCircle( Graphics2D g2 , double x , double y ,double r ) {
		Ellipse2D.Double c = new Ellipse2D.Double();
		c.setFrame(x - r, y - r, 2 * r, 2 * r);
		g2.draw(c);
	}
}
