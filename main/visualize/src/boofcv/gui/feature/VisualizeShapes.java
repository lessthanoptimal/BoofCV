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

import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.EllipseRotated_F64;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_I32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class VisualizeShapes {


	public static void draw( Quadrilateral_F64 quad , Graphics2D g2 ) {
		draw(quad.a,quad.b,g2);
		draw(quad.b,quad.c,g2);
		draw(quad.c,quad.d,g2);
		draw(quad.d,quad.a,g2);
	}

	public static void draw( Point2D_F64 p0 , Point2D_F64 p1 , Graphics2D g2 ) {
		g2.drawLine((int) (p0.x + 0.5), (int) (p0.y + 0.5), (int) (p1.x + 0.5), (int) (p1.y + 0.5));
	}

	public static void drawArrow( Quadrilateral_F64 quad , Graphics2D g2 ) {
		drawArrow(quad.a, quad.b, g2);
		drawArrow(quad.b, quad.c,g2);
		drawArrow(quad.c, quad.d,g2);
		drawArrow(quad.d, quad.a, g2);
	}

	public static void drawArrow( Point2D_F64 p0 , Point2D_F64 p1 , Graphics2D g2 ) {
		int x0 = (int)(p0.x+0.5);
		int y0 = (int)(p0.y+0.5);
		int x1 = (int)(p1.x+0.5);
		int y1 = (int)(p1.y+0.5);

		g2.drawLine(x0,y0,x1,y1);

		int x2 = x0+(int)((x1-x0)*0.9);
		int y2 = y0+(int)((y1-y0)*0.9);

		int tanX = (y1-y2);
		int tanY = (x2-x1);

		g2.drawLine(x2 + tanX, y2 + tanY, x1, y1);
		g2.drawLine(x2 - tanX, y2 - tanY, x1, y1);
	}

	/**
	 * Draws a polygon
	 *
	 * @param vertexes List of vertices in the polygon
	 * @param loop true if the end points are connected, forming a loop
	 * @param g2 Graphics object it's drawn to
	 */
	public static<T extends Point2D_I32> void drawPolygon( List<T> vertexes , boolean loop, Graphics2D g2 ) {
		for( int i = 0; i < vertexes.size()-1; i++ ) {
			Point2D_I32 p0 = vertexes.get(i);
			Point2D_I32 p1 = vertexes.get(i+1);
			g2.drawLine(p0.x,p0.y,p1.x,p1.y);
		}
		if( loop && vertexes.size() > 0) {
			Point2D_I32 p0 = vertexes.get(0);
			Point2D_I32 p1 = vertexes.get(vertexes.size()-1);
			g2.drawLine(p0.x,p0.y,p1.x,p1.y);
		}
	}

	/**
	 * Draws a polygon
	 *
	 * @param polygon The polygon
	 * @param loop true if the end points are connected, forming a loop
	 * @param g2 Graphics object it's drawn to
	 */
	public static void drawPolygon( Polygon2D_F64 polygon, boolean loop, Graphics2D g2 ) {
		for( int i = 0; i < polygon.size()-1; i++ ) {
			Point2D_F64 p0 = polygon.get(i);
			Point2D_F64 p1 = polygon.get(i+1);
			g2.drawLine((int)(p0.x+0.5),(int)(p0.y+0.5),(int)(p1.x+0.5),(int)(p1.y+0.5));
		}
		if( loop && polygon.size() > 0) {
			Point2D_F64 p0 = polygon.get(0);
			Point2D_F64 p1 = polygon.get(polygon.size()-1);
			g2.drawLine((int)(p0.x+0.5),(int)(p0.y+0.5),(int)(p1.x+0.5),(int)(p1.y+0.5));
		}
	}

	/**
	 * Draws the rotated ellipse
	 * @param ellipse Description of the ellipse
	 * @param g2 Graphics object
	 */
	public static void drawEllipse( EllipseRotated_F64 ellipse , Graphics2D g2 ) {

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(ellipse.phi);

		double w = ellipse.a*2;
		double h = ellipse.b*2;


		Shape shape = rotate.createTransformedShape(new Ellipse2D.Double(-w/2,-h/2,w,h));
		shape = AffineTransform.getTranslateInstance(ellipse.center.x,ellipse.center.y).createTransformedShape(shape);

		g2.draw(shape);
	}

	/**
	 * Draws an axis aligned rectangle
	 * @param rect Rectangle
	 * @param g2 Graphics object
	 */
	public static void drawRectangle( Rectangle2D_I32 rect , Graphics2D g2 ) {
		g2.drawLine(rect.x0, rect.y0, rect.x1, rect.y0);
		g2.drawLine(rect.x1, rect.y0, rect.x1, rect.y1);
		g2.drawLine(rect.x0, rect.y1, rect.x1, rect.y1);
		g2.drawLine(rect.x0, rect.y1, rect.x0, rect.y0);
	}

	public static void drawQuad( Quadrilateral_F64 quad , Graphics2D g2 , boolean subpixel  ) {
		if( subpixel ) {
			Line2D.Double line = new Line2D.Double();

			line.setLine(quad.a.x,quad.a.y,quad.b.x,quad.b.y);  g2.draw(line);
			line.setLine(quad.b.x,quad.b.y,quad.c.x,quad.c.y);  g2.draw(line);
			line.setLine(quad.c.x,quad.c.y,quad.d.x,quad.d.y);  g2.draw(line);
			line.setLine(quad.d.x,quad.d.y,quad.a.x,quad.a.y);  g2.draw(line);
		} else {
			g2.drawLine((int) (quad.a.x + 0.5), (int) (quad.a.y + 0.5), (int) (quad.b.x + 0.5), (int) (quad.b.y + 0.5));
			g2.drawLine((int) (quad.b.x + 0.5), (int) (quad.b.y + 0.5), (int) (quad.c.x + 0.5), (int) (quad.c.y + 0.5));
			g2.drawLine((int) (quad.c.x + 0.5), (int) (quad.c.y + 0.5), (int) (quad.d.x + 0.5), (int) (quad.d.y + 0.5));
			g2.drawLine((int) (quad.d.x + 0.5), (int) (quad.d.y + 0.5), (int) (quad.a.x + 0.5), (int) (quad.a.y + 0.5));
		}
	}
}
