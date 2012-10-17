/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.sfm;

import boofcv.struct.FastQueue;
import georegression.struct.point.Point3D_F64;
import georegression.struct.point.Vector3D_F64;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author Peter Abeles
 */
public class DisplayMonoPath extends JPanel {

	FastQueue<Point3D_F64> points = new FastQueue<Point3D_F64>(100,Point3D_F64.class,true);
	
	double max;
	
	public synchronized void setPoints( List<Point3D_F64> list ) {
		points.reset();
		
		max = 0;
		
		for( Point3D_F64 p : list ) {
			if( Math.abs(p.x) > max ) {
				max = Math.abs(p.x);
			}
			if( Math.abs(p.y) > max ) {
				max = Math.abs(p.y);
			}
			if( Math.abs(p.z) > max ) {
				max = Math.abs(p.z);
			}
			Point3D_F64 q = points.grow();
			q.set(p);
		}
	}

	public void addLocation(Vector3D_F64 p) {
		if( Math.abs(p.x) > max ) {
			max = Math.abs(p.x);
		}
		if( Math.abs(p.y) > max ) {
			max = Math.abs(p.y);
		}
		if( Math.abs(p.z) > max ) {
			max = Math.abs(p.z);
		}
		Point3D_F64 q = points.grow();
		q.set(p.x,p.y,p.z);
	}

	@Override
	protected synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
	
		Graphics2D g2 = (Graphics2D)g;
		
		int w = Math.min(getWidth()/2,getHeight()/2);

		g2.setColor(Color.BLUE);
		for( int i = 1; i < points.size(); i++ ) {
			Point3D_F64 a = points.get(i-1);
			Point3D_F64 b = points.get(i);
			
			int x1 = (int)((a.x/max)*w)+w;
			int x2 = (int)((b.x/max)*w)+w;
			int y1 = (int)((a.y/max)*w)+w;
			int y2 = (int)((b.y/max)*w)+w;

			g2.drawLine(x1,y1,x2,y2);
		}

		g2.setColor(Color.BLACK);
		for( int i = 1; i < points.size(); i++ ) {
			Point3D_F64 a = points.get(i-1);
			Point3D_F64 b = points.get(i);

			int x1 = (int)((a.x/max)*w)+w;
			int x2 = (int)((b.x/max)*w)+w;
			int z1 = (int)((a.z/max)*w)+w;
			int z2 = (int)((b.z/max)*w)+w;

			g2.drawLine(x1,z1,x2,z2);
		}
	}
}
