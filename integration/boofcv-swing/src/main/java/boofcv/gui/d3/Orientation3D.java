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

package boofcv.gui.d3;

import boofcv.gui.BoofSwingUtil;
import georegression.metric.UtilAngle;
import georegression.struct.point.Vector3D_F64;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Graphically displays the orientation
 *
 * @author Peter Abeles
 */
public class Orientation3D extends JPanel {

	double yaw;
	double tilt;

	int a = 6;
	int b = 40;

	Line2D.Double line = new Line2D.Double();

	public Orientation3D() {
		setPreferredSize(new Dimension(a+b+a+25,a+b+a));
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());
	}

	public void setVector( Vector3D_F64 v ) {
		yaw = Math.atan2( v.x , v.z );
		tilt = UtilAngle.atanSafe(-v.y,Math.abs(v.z));
	}

	public void setTilt(double tilt) {
		this.tilt = tilt;
	}

	public void setYaw(double yaw) {
		this.yaw = yaw;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = BoofSwingUtil.antialiasing(g);

		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(5));
		g2.drawOval(a,a,b,b);
		drawMark(a+b/2,a,5,true,g2);
		drawMark(a+b/2,a+b,5,true,g2);
		drawMark(a,a+b/2,5,false,g2);
		drawMark(a+b,a+b/2,5,false,g2);

		drawNeedle(a+b/2,a+b/2,b/2+2,yaw,g2);

		drawTiltBar(a+b+20,g2);
	}

	private void drawTiltBar( int xc  , Graphics2D g2 ) {
		g2.setStroke(new BasicStroke(3));
		g2.setColor(Color.BLACK);
		g2.drawLine(xc,a,xc,a+b);
		// draw top, bottom, middle fiducials
		g2.drawLine(xc-3,a,xc+3,a);
		g2.drawLine(xc-3,a+b,xc+3,a+b);
		g2.drawLine(xc-3,a+b/2,xc+3,a+b/2);

		g2.setColor(Color.RED);
		g2.setStroke(new BasicStroke(5));
		int y = (int)(a+b/2-(b/2.0)*tilt/(Math.PI/2));
		g2.drawLine(xc-4,y,xc+4,y);
	}

	private void drawNeedle( double xc , double yc , int r , double yaw , Graphics2D g2  ) {
		g2.setColor(Color.RED);
		double lx = r*Math.cos(yaw);
		double ly = r*Math.sin(yaw);

		line.setLine(xc,yc,xc+lx,yc+ly);
		g2.draw(line);
	}

	private void drawMark( double xc , double yc , double r , boolean vertical , Graphics2D g2 ) {
		if( vertical ) {
			line.setLine(xc, yc - r, xc, yc + r);
		} else {
			line.setLine(xc - r, yc, xc + r, yc);
		}
		g2.draw(line);
	}
}
