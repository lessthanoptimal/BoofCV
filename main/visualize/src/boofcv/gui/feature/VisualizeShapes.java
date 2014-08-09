/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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
import georegression.struct.shapes.Quadrilateral_F64;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
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
		g2.drawLine((int)(p0.x+0.5),(int)(p0.y+0.5),(int)(p1.x+0.5),(int)(p1.y+0.5));
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
}
