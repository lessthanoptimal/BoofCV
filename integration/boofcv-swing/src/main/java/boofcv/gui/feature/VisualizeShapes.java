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

package boofcv.gui.feature;

import georegression.struct.curve.EllipseRotated_F64;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.shapes.Polygon2D_F64;
import georegression.struct.shapes.Quadrilateral_F64;
import georegression.struct.shapes.Rectangle2D_I32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.List;

import static boofcv.gui.fiducial.VisualizeFiducial.drawLine;

/**
 * @author Peter Abeles
 */
public class VisualizeShapes {


	public static void draw( Quadrilateral_F64 quad , Graphics2D g2 ) {
		Line2D.Double line = new Line2D.Double();

		drawSubPixel(quad.a,quad.b,line,g2);
		drawSubPixel(quad.b,quad.c,line,g2);
		drawSubPixel(quad.c,quad.d,line,g2);
		drawSubPixel(quad.d,quad.a,line,g2);
	}

	public static void draw( Point2D_F64 p0 , Point2D_F64 p1 , Graphics2D g2 ) {
		g2.drawLine((int) (p0.x + 0.5), (int) (p0.y + 0.5), (int) (p1.x + 0.5), (int) (p1.y + 0.5));
	}

	public static void drawSubPixel( Point2D_F64 p0 , Point2D_F64 p1 , Line2D.Double line, Graphics2D g2 ) {
		line.setLine(p0.x,p0.y,p1.x,p1.y);
		g2.draw(line);
	}

	public static void drawArrowSubPixel(Quadrilateral_F64 quad, double strokeSize, double scale, Graphics2D g2) {
		Line2D.Double line = new Line2D.Double();

		Color colors[] = new Color[]{Color.RED,Color.ORANGE,Color.CYAN,Color.BLUE};

		Point2D_F64 start = new Point2D_F64();
		Point2D_F64 end = new Point2D_F64();
		for (int i = 0; i < 4; i++ ) {
			int j = (i+1)%4;

			Point2D_F64 a = quad.get(i);
			Point2D_F64 b = quad.get(j);

			double length = a.distance(b);
			double dx = (b.x-a.x)/length;
			double dy = (b.y-a.y)/length;
			start.x = a.x*scale;
			start.y = a.y*scale;

			end.x = (length*scale-strokeSize*2)*dx + start.x;
			end.y = (length*scale-strokeSize*2)*dy + start.y;

			g2.setColor(Color.BLACK);
			g2.setStroke(new BasicStroke((float)strokeSize*2.0f));
			drawArrowSubPixel(start, end, line, g2);
			g2.setColor(colors[i]);
			g2.setStroke(new BasicStroke((float)strokeSize));
			drawArrowSubPixel(start, end, line, g2);
		}
	}

	public static void drawArrowSubPixel( Point2D_F64 p0 , Point2D_F64 p1 , Line2D.Double line, Graphics2D g2 ) {
		drawSubPixel(p0,p1,line,g2);

		double x2 = p0.x+(p1.x-p0.x)*0.9;
		double y2 = p0.y+(p1.y-p0.y)*0.9;

		double tanX = (p1.y-y2);
		double tanY = (x2-p1.x);

		drawSubPixel(new Point2D_F64(x2 + tanX, y2 + tanY), p1, line, g2);
		drawSubPixel(new Point2D_F64(x2 - tanX, y2 - tanY), p1, line, g2);
	}


	public static void drawArrow( Quadrilateral_F64 quad , Graphics2D g2 ) {
		drawArrow(quad.a, quad.b, g2);
		drawArrow(quad.b, quad.c, g2);
		drawArrow(quad.c, quad.d, g2);
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

	public static void drawArrow( double x0 , double y0 , double x1 , double y1 , double length ,
								  Line2D.Double line , Graphics2D g2 ) {

		line.setLine(x0,y0,x1,y1); g2.draw(line);

		double x2,y2;

		if( length > 0 ) {
			double dx = x1-x0;
			double dy = y1-y0;
			double r = Math.sqrt(dx*dx + dy*dy);
			dx /= r;
			dy /= r;

			x2 = x1 - dx*length;
			y2 = y1 - dy*length;
		} else {
			x2 = x0 + ((x1 - x0) * 0.9);
			y2 = y0 + ((y1 - y0) * 0.9);
		}
		double tanX = (y1-y2);
		double tanY = (x2-x1);

		line.setLine(x2+tanX,y2+tanY,x1,y1); g2.draw(line);
		line.setLine(x2-tanX,y2-tanY,x1,y1); g2.draw(line);
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

	public static<T extends Point2D_I32> void drawPolygon( List<T> vertexes, boolean loop,double scale,  Graphics2D g2 ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Line2D.Double l = new Line2D.Double();

		for( int i = 0; i < vertexes.size()-1; i++ ) {
			Point2D_I32 p0 = vertexes.get(i);
			Point2D_I32 p1 = vertexes.get(i+1);
			drawLine(g2, l, scale*(p0.x+0.5),  scale*(p0.y+0.5),  scale*(p1.x+0.5),  scale*(p1.y+0.5));
		}
		if( loop && vertexes.size() > 0) {
			Point2D_I32 p0 = vertexes.get(0);
			Point2D_I32 p1 = vertexes.get(vertexes.size()-1);
			drawLine(g2, l, scale*(p0.x+0.5), scale*(p0.y+0.5), scale*(p1.x+0.5),scale*(p1.y+0.5));
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

	public static void drawPolygon( Polygon2D_F64 polygon, boolean loop, Graphics2D g2 , boolean interpolate ) {
		if( interpolate ) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			Path2D path = new Path2D.Double();

			for( int i = 0; i < polygon.size(); i++ ) {
				Point2D_F64 p = polygon.get(i);
				if( i == 0 )
					path.moveTo(p.x,p.y);
				else
					path.lineTo(p.x,p.y);

			}
			if( loop && polygon.size() > 0) {
				path.closePath();
			}

			g2.draw(path);

		} else {
			drawPolygon(polygon, loop, g2);
		}
	}

	public static void drawPolygon( Polygon2D_F64 polygon, boolean loop,double scale,  Graphics2D g2 ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Path2D path = new Path2D.Double();

		for( int i = 0; i < polygon.size(); i++ ) {
			Point2D_F64 p = polygon.get(i);
			if( i == 0 )
				path.moveTo(scale*p.x,scale*p.y);
			else
				path.lineTo(scale*p.x,scale*p.y);

		}
		if( loop && polygon.size() > 0) {
			path.closePath();
		}
		g2.draw(path);
	}

	public static void fillPolygon( Polygon2D_F64 polygon, double scale,  Graphics2D g2 ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Path2D path = new Path2D.Double();

		Point2D_F64 p = polygon.get(0);
		path.moveTo(p.x*scale,p.y*scale);

		for( int i = 1; i <= polygon.size(); i++ ) {
			p = polygon.get(i%polygon.size());
			path.lineTo(p.x*scale,p.y*scale);
		}
		g2.fill(path);
	}

	public static void drawPolygonCorners( Polygon2D_F64 polygon, int radius , Graphics2D g2 , boolean interpolate ) {
		if( interpolate ) {
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			for( int i = 0; i < polygon.size(); i++ ) {
				Point2D_F64 p = polygon.get(i);
				VisualizeFeatures.drawCircle(g2,p.x,p.y,radius);
			}

		} else {
			for( int i = 0; i < polygon.size(); i++ ) {
				Point2D_F64 p = polygon.get(i);
				VisualizeFeatures.drawCircle(g2,(int)(p.x+0.5),(int)(p.y+0.5),radius);
			}
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

	public static void drawEllipse( EllipseRotated_F64 ellipse , double scale , Graphics2D g2 ) {

		AffineTransform rotate = new AffineTransform();
		rotate.rotate(ellipse.phi);

		double w = scale*ellipse.a*2;
		double h = scale*ellipse.b*2;

		Shape shape = rotate.createTransformedShape(new Ellipse2D.Double(-w/2,-h/2,w,h));
		shape = AffineTransform.getTranslateInstance(scale*ellipse.center.x,scale*ellipse.center.y).createTransformedShape(shape);

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

	public static void drawRectangle( double x0, double y0, double x1, double y1, Line2D.Double line , Graphics2D g2 ) {
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		line.setLine(x0,y0,x1,y0);g2.draw(line);
		line.setLine(x1,y0,x1,y1);g2.draw(line);
		line.setLine(x1,y1,x0,y1);g2.draw(line);
		line.setLine(x0,y1,x0,y0);g2.draw(line);
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

	public static void drawQuad( Quadrilateral_F64 quad , Graphics2D g2 , boolean subpixel , Color color1 , Color color2) {
		g2.setColor(color1);
		if( subpixel ) {
			Line2D.Double line = new Line2D.Double();

			line.setLine(quad.a.x,quad.a.y,quad.b.x,quad.b.y);  g2.draw(line);
			line.setLine(quad.b.x, quad.b.y, quad.c.x, quad.c.y);  g2.draw(line);
			line.setLine(quad.c.x, quad.c.y, quad.d.x, quad.d.y);  g2.draw(line);
			g2.setColor(color2);
			line.setLine(quad.d.x,quad.d.y,quad.a.x,quad.a.y);  g2.draw(line);
		} else {
			g2.drawLine((int) (quad.a.x + 0.5), (int) (quad.a.y + 0.5), (int) (quad.b.x + 0.5), (int) (quad.b.y + 0.5));
			g2.drawLine((int) (quad.b.x + 0.5), (int) (quad.b.y + 0.5), (int) (quad.c.x + 0.5), (int) (quad.c.y + 0.5));
			g2.drawLine((int) (quad.c.x + 0.5), (int) (quad.c.y + 0.5), (int) (quad.d.x + 0.5), (int) (quad.d.y + 0.5));
			g2.setColor(color2);
			g2.drawLine((int) (quad.d.x + 0.5), (int) (quad.d.y + 0.5), (int) (quad.a.x + 0.5), (int) (quad.a.y + 0.5));
		}
	}

	public static void drawQuad( Quadrilateral_F64 quad , Graphics2D g2 , double scale,
								 boolean subpixel , Color color1 , Color color2) {
		g2.setColor(color1);
		if( subpixel ) {
			Line2D.Double line = new Line2D.Double();

			line.setLine(quad.a.x*scale,quad.a.y*scale,quad.b.x*scale,quad.b.y*scale);  g2.draw(line);
			line.setLine(quad.b.x*scale, quad.b.y*scale, quad.c.x*scale, quad.c.y*scale);  g2.draw(line);
			line.setLine(quad.c.x*scale, quad.c.y*scale, quad.d.x*scale, quad.d.y*scale);  g2.draw(line);
			g2.setColor(color2);
			line.setLine(quad.d.x*scale,quad.d.y*scale,quad.a.x*scale,quad.a.y*scale);  g2.draw(line);
		} else {
			g2.drawLine((int) (quad.a.x*scale + 0.5), (int) (quad.a.y*scale + 0.5), (int) (quad.b.x*scale + 0.5), (int) (quad.b.y*scale + 0.5));
			g2.drawLine((int) (quad.b.x*scale + 0.5), (int) (quad.b.y*scale + 0.5), (int) (quad.c.x*scale + 0.5), (int) (quad.c.y*scale + 0.5));
			g2.drawLine((int) (quad.c.x*scale + 0.5), (int) (quad.c.y*scale + 0.5), (int) (quad.d.x*scale + 0.5), (int) (quad.d.y*scale + 0.5));
			g2.setColor(color2);
			g2.drawLine((int) (quad.d.x*scale + 0.5), (int) (quad.d.y*scale + 0.5), (int) (quad.a.x*scale + 0.5), (int) (quad.a.y*scale + 0.5));
		}
	}
}
