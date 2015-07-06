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

package boofcv.gui.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.WorldToCameraToPixel;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * @author Peter Abeles
 */
public class VisualizeFiducial {

	private static final Font font = new Font("Serif", Font.BOLD, 24);


	/**
	 * Draws a flat cube to show where the square fiducial is on the image
	 *
	 */
	public static void drawNumbers( Se3_F64 targetToCamera , IntrinsicParameters intrinsic , int id , Graphics2D g2 )
	{
		// Computer the center of the fiducial in pixel coordinates
		Point2D_F64 p = new Point2D_F64();
		Point3D_F64 c = new Point3D_F64();
		WorldToCameraToPixel worldToPixel = PerspectiveOps.createWorldToPixel(intrinsic, targetToCamera);
		worldToPixel.transform(c,p);

		// Draw the ID number approximately in the center
		FontMetrics metrics = g2.getFontMetrics(font);
		String text = Integer.toString(id);
		Rectangle2D r = metrics.getStringBounds(text,null);
		g2.setColor(Color.ORANGE);
		g2.setFont(font);
		g2.drawString(text,(float)(p.x-r.getWidth()/2),(float)(p.y+r.getHeight()/2));
	}

	/**
	 * Draws a flat cube to show where the square fiducial is on the image
	 *
	 */
	public static void drawCube( Se3_F64 targetToCamera , IntrinsicParameters intrinsic , double width ,
								 Graphics2D g2 )
	{
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		double r = width/2.0;
		double h = r;

		Point3D_F64 corners[] = new Point3D_F64[8];
		corners[0] = new Point3D_F64(-r,-r,0);
		corners[1] = new Point3D_F64( r,-r,0);
		corners[2] = new Point3D_F64( r, r,0);
		corners[3] = new Point3D_F64(-r, r,0);
		corners[4] = new Point3D_F64(-r,-r,h);
		corners[5] = new Point3D_F64( r,-r,h);
		corners[6] = new Point3D_F64( r, r,h);
		corners[7] = new Point3D_F64(-r, r,h);

		Point2D_I32 pixel[] = new Point2D_I32[8];
		Point2D_F64 p = new Point2D_F64();
		WorldToCameraToPixel transform = PerspectiveOps.createWorldToPixel(intrinsic,targetToCamera);
		for (int i = 0; i < 8; i++) {
			Point3D_F64 c = corners[i];
			if( !transform.transform(c,p) )
				throw new RuntimeException("Crap");
			pixel[i] = new Point2D_I32((int)(p.x+0.5),(int)(p.y+0.5));
		}

		Line2D.Double l = new Line2D.Double();
		g2.setStroke(new BasicStroke(4));
		g2.setColor(Color.RED);

		drawLine(g2,l,pixel[0].x, pixel[0].y, pixel[1].x, pixel[1].y);
		drawLine(g2,l,pixel[1].x, pixel[1].y, pixel[2].x, pixel[2].y);
		drawLine(g2,l,pixel[2].x, pixel[2].y, pixel[3].x, pixel[3].y);
		drawLine(g2,l,pixel[3].x, pixel[3].y, pixel[0].x, pixel[0].y);

		g2.setColor(Color.BLACK);
		drawLine(g2,l,pixel[0].x,pixel[0].y,pixel[4].x,pixel[4].y);
		drawLine(g2,l,pixel[1].x,pixel[1].y,pixel[5].x,pixel[5].y);
		drawLine(g2,l,pixel[2].x,pixel[2].y,pixel[6].x,pixel[6].y);
		drawLine(g2,l,pixel[3].x,pixel[3].y,pixel[7].x,pixel[7].y);

		g2.setColor(new Color(0x00,0xFF,0x00,125));
		drawLine(g2,l,pixel[4].x,pixel[4].y,pixel[5].x,pixel[5].y);
		g2.setColor(new Color(0xC0,0x10,0xC0,125));
		drawLine(g2,l,pixel[5].x,pixel[5].y,pixel[6].x,pixel[6].y);
		g2.setColor(new Color(0x00,0xA0,0xC0,125));
		drawLine(g2,l,pixel[6].x,pixel[6].y,pixel[7].x,pixel[7].y);
		g2.setColor(Color.BLUE);
		drawLine(g2,l,pixel[7].x,pixel[7].y,pixel[4].x,pixel[4].y);
	}

	private static void drawLine( Graphics2D g2 , Line2D.Double line , double x0 , double y0 , double x1 , double y1 ) {
		line.setLine(x0,y0,x1,y1);
		g2.draw(line);
	}
}
