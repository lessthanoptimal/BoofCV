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

package boofcv.geo.simulation.mono;

import boofcv.abst.feature.tracker.PointTrack;
import georegression.struct.point.Point2D_I32;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Visualizes the location of simulated features on the camera
 *
 * @author Peter Abeles
 */
public class MonoSimulationPanel extends JPanel {

	List<Point2D_I32> features = new ArrayList<Point2D_I32>();

	Stack<Point2D_I32> unused = new Stack<Point2D_I32>();
	
	public MonoSimulationPanel( int imageWidth , int imageHeight ) {
		setPreferredSize(new Dimension(imageWidth,imageHeight));
	}
	
	public synchronized void setFeatures( List<PointTrack> tracks ) {
		unused.addAll(features);
		features.clear();
		
		for( PointTrack t : tracks ) {
			Point2D_I32 p = unused.isEmpty() ? new Point2D_I32() : unused.pop();

			p.x = (int)t.x;
			p.y = (int)t.y;
			
			features.add(p);
		}
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
	
		g2.setColor(Color.BLACK);
		
		int r = 2;
		int w = r*2+1;
		
		for( Point2D_I32 p : features ) {
			g2.drawOval(p.x-r,p.y-r,w,w);
		}
	}
}
