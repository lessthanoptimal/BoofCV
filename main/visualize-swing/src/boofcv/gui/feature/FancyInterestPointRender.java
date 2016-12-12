/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
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

import java.awt.*;
import java.util.ArrayList;
import java.util.List;


/**
 * Renders image interest points in a thread safe manor.
 *
 * @author Peter Abeles
 */
public class FancyInterestPointRender {

	List<Point> points = new ArrayList<>();
	List<Line> lines = new ArrayList<>();
	List<Circle> circles = new ArrayList<>();
	List<VString> strings = new ArrayList<>();

	public synchronized void draw( Graphics2D g2 ) {
		for( Point p : points ) {
			g2.setColor(p.color);
			int w = p.radius*2+1;
			g2.fillOval(p.x-p.radius,p.y-p.radius,w,w);
			w+=2;
			int r = p.radius+1;
			g2.setColor(Color.BLACK);
			g2.drawOval(p.x-r,p.y-r,w,w);
		}

		for( Circle c : circles ) {
			g2.setColor(c.color);
			int w = c.radius*2+1;
			g2.drawOval(c.x-c.radius,c.y-c.radius,w,w);
			g2.fillOval(c.x-1,c.y-1,3,3);
			if( !Double.isNaN(c.direction)) {
				g2.setColor(Color.BLUE);
				int dx = (int)(Math.cos(c.direction)*c.radius);
				int dy = (int)(Math.sin(c.direction)*c.radius);
				g2.drawLine(c.x,c.y,c.x+dx,c.y+dy);
			}
		}

		for( Line l : lines ) {
			g2.setColor(l.color);
			g2.drawLine(l.x0,l.y0,l.x1,l.y1);
		}

		for( VString l : strings ) {
			g2.setColor(l.color);
			g2.drawString(l.value,l.x,l.y);
		}
	}

	public synchronized void reset() {
		points.clear();
		lines.clear();
		circles.clear();
		strings.clear();
	}

	public synchronized void addString( int x , int y , String value , Color color) {
		VString s = new VString();
		s.x = x;
		s.y = y;
		s.value = value;
		s.color = color;
		strings.add(s);
	}

	public synchronized void addPoint( int x , int y ) {
		Point p = new Point();
		p.x = x;
		p.y = y;
		points.add(p);
	}

	public synchronized void addPoint(  int x , int y , int radius , Color color ) {
		Point p = new Point();
		p.x = x;
		p.y = y;
		p.radius = radius;
		p.color = color;
		points.add(p);
	}

	public synchronized void addCircle( int x , int y , int radius ) {
		Circle p = new Circle();
		p.x = x;
		p.y = y;
		p.radius = radius;
		circles.add(p);
	}

	public synchronized void addLine( int x0 , int y0 , int x1 , int y1 ) {
		Line p = new Line();
		p.x0 = x0;
		p.y0 = y0;
		p.x1 = x1;
		p.y1 = y1;
		lines.add(p);
	}

	public synchronized void addCircle( int x , int y , int radius , Color color ) {
		Circle p = new Circle();
		p.x = x;
		p.y = y;
		p.radius = radius;
		p.color = color;
		circles.add(p);
	}

	public synchronized void addCircle( int x , int y , int radius , Color color , double direction ) {
		Circle p = new Circle();
		p.x = x;
		p.y = y;
		p.radius = radius;
		p.color = color;
		p.direction = direction;
		circles.add(p);
	}

	public static class VString
	{
		int x,y;
		String value;
		Color color = Color.RED;
	}

	public static class Line
	{
		int x0,y0;
		int x1,y1;
		Color color = Color.BLUE;
	}

	public static class Point
	{
		int x,y;
		int radius=1;
		Color color = Color.RED;
	}

	public static class Circle
	{
		int x,y;                   
		int radius;
		double direction = Double.NaN;
		Color color = Color.RED;
	}
}
