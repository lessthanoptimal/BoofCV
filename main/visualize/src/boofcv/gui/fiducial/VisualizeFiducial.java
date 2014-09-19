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

package boofcv.gui.fiducial;

import boofcv.alg.geo.PerspectiveOps;
import boofcv.struct.calib.IntrinsicParameters;
import georegression.struct.point.Point2D_F64;
import georegression.struct.point.Point2D_I32;
import georegression.struct.point.Point3D_F64;
import georegression.struct.se.Se3_F64;
import georegression.transform.se.SePointOps_F64;

import java.awt.*;

/**
 * @author Peter Abeles
 */
public class VisualizeFiducial {

	/**
	 * Draws a flat cube to show where the square fiducial is on the image
	 *
	 */
	public static void drawCube( Se3_F64 targetToCamera , IntrinsicParameters intrinsic , double width ,
								 Graphics2D g2 )
	{
		double r = width/2.0;
		Point3D_F64 corners[] = new Point3D_F64[8];
		corners[0] = new Point3D_F64(-r,-r,0);
		corners[1] = new Point3D_F64( r,-r,0);
		corners[2] = new Point3D_F64( r, r,0);
		corners[3] = new Point3D_F64(-r, r,0);
		corners[4] = new Point3D_F64(-r,-r,r);
		corners[5] = new Point3D_F64( r,-r,r);
		corners[6] = new Point3D_F64( r, r,r);
		corners[7] = new Point3D_F64(-r, r,r);

		Point2D_I32 pixel[] = new Point2D_I32[8];
		Point2D_F64 p = new Point2D_F64();
		for (int i = 0; i < 8; i++) {
			Point3D_F64 c = corners[i];
			SePointOps_F64.transform(targetToCamera,c,c);
			PerspectiveOps.convertNormToPixel(intrinsic,c.x/c.z,c.y/c.z,p);
			pixel[i] = new Point2D_I32((int)(p.x+0.5),(int)(p.y+0.5));
		}

		g2.setStroke(new BasicStroke(4));
		g2.setColor(Color.RED);
		g2.drawLine(pixel[0].x,pixel[0].y,pixel[1].x,pixel[1].y);
		g2.drawLine(pixel[1].x,pixel[1].y,pixel[2].x,pixel[2].y);
		g2.drawLine(pixel[2].x,pixel[2].y,pixel[3].x,pixel[3].y);
		g2.drawLine(pixel[3].x,pixel[3].y,pixel[0].x,pixel[0].y);

		g2.setColor(Color.BLACK);
		g2.drawLine(pixel[0].x,pixel[0].y,pixel[4].x,pixel[4].y);
		g2.drawLine(pixel[1].x,pixel[1].y,pixel[5].x,pixel[5].y);
		g2.drawLine(pixel[2].x,pixel[2].y,pixel[6].x,pixel[6].y);
		g2.drawLine(pixel[3].x,pixel[3].y,pixel[7].x,pixel[7].y);

		g2.setColor(new Color(0x00,0xFF,0x00,125));
		g2.drawLine(pixel[4].x,pixel[4].y,pixel[5].x,pixel[5].y);
		g2.setColor(new Color(0xC0,0x10,0xC0,125));
		g2.drawLine(pixel[5].x,pixel[5].y,pixel[6].x,pixel[6].y);
		g2.setColor(new Color(0x00,0xA0,0xC0,125));
		g2.drawLine(pixel[6].x,pixel[6].y,pixel[7].x,pixel[7].y);
		g2.setColor(Color.BLUE);
		g2.drawLine(pixel[7].x,pixel[7].y,pixel[4].x,pixel[4].y);
	}
}
