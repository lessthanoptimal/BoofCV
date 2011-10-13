/*
 * Copyright (c) 2011, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://www.boofcv.org).
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

package boofcv.alg.feature.detect.line;


import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;
import georegression.struct.point.Point2D_F32;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws lines over an image. Used for displaying the output of line detection algorithms.
 *
 * @author Peter Abeles
 */
public class ImageLinePanel extends JPanel {

	BufferedImage background;
	List<LineSegment2D_F32> lines = new ArrayList<LineSegment2D_F32>();

	public synchronized void setBackground(BufferedImage background) {
		this.background = background;
	}

	public synchronized void setLines(List<LineParametric2D_F32> lines) {
		this.lines.clear();
		for( LineParametric2D_F32 p : lines ) {
			this.lines.add(convert(p,background.getWidth(),background.getHeight()));
		}
	}

	public synchronized void setLineSegments(List<LineSegment2D_F32> lines) {
		this.lines.clear();
		this.lines.addAll(lines);
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		if( background == null )
			return;

		Graphics2D g2 = (Graphics2D)g;
		int w = background.getWidth();
		int h = background.getHeight();

		double scaleX = getWidth()/(double)w;
		double scaleY = getHeight()/(double)h;
		double scale = Math.min(scaleX, scaleY);
		if( scale > 1 ) {
			scale = 1;
		}

		g2.drawImage(background,0,0,(int)(scale*w),(int)(scale*h),0,0,w,h,null);
		g2.setStroke(new BasicStroke(3));

		for( LineSegment2D_F32 s : lines ) {
			g2.setColor(Color.RED);
			g2.drawLine((int)(scale*s.a.x),(int)(scale*s.a.y),(int)(scale*s.b.x),(int)(scale*s.b.y));
			g2.setColor(Color.BLUE);
			g2.fillOval((int)(scale*s.a.x)-1,(int)(scale*s.a.y)-1,3,3);
			g2.fillOval((int)(scale*s.b.x)-1,(int)(scale*s.b.y)-1,3,3);
		}
	}

	/**
	 * Find the point in which the line intersects the image border and create a line segment at those points
	 */
	private static LineSegment2D_F32 convert( LineParametric2D_F32 l ,
											  int width , int height ) {
		double t0 = (0-l.p.x)/l.getSlopeX();
		double t1 = (0-l.p.y)/l.getSlopeY();
		double t2 = (width-l.p.x)/l.getSlopeX();
		double t3 = (height-l.p.y)/l.getSlopeY();

		Point2D_F32 a = computePoint(l, t0);
		Point2D_F32 b = computePoint(l, t1);
		Point2D_F32 c = computePoint(l, t2);
		Point2D_F32 d = computePoint(l, t3);

		List<Point2D_F32> inside = new ArrayList<Point2D_F32>();
		checkAddInside(width , height , a, inside);
		checkAddInside(width , height , b, inside);
		checkAddInside(width , height , c, inside);
		checkAddInside(width , height , d, inside);

		if( inside.size() != 2 ) {
			System.out.println("interesting");
		}
		return new LineSegment2D_F32(inside.get(0),inside.get(1));
	}

	static double foo = 1e-4;
	private static void checkAddInside( int width , int height, Point2D_F32 a, List<Point2D_F32> inside) {
		if( a.x >= -foo && a.x <= width+foo && a.y >= -foo && a.y <= height+foo ) {

			for( Point2D_F32 p : inside ) {
				if( p.distance(a) < foo )
					return;
			}
			inside.add(a);
		}
	}

	private static Point2D_F32 computePoint(LineParametric2D_F32 l, double t) {
		return new Point2D_F32((float)(t*l.slope.x+l.p.x) , (float)(t*l.slope.y + l.p.y));
	}
}
