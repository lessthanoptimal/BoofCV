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

package boofcv.gui.d3;

import boofcv.struct.FastQueue;

import javax.swing.*;
import java.awt.*;

/**
 * @author Peter Abeles
 */
public class PointCloudSideView extends JPanel {
	FastQueue<ColorPoint3D> points = new FastQueue<ColorPoint3D>(100,ColorPoint3D.class,true);

	double scale;

	public synchronized void reset() {
		points.reset();
	}

	public synchronized void addPoint( double x , double y , double z , int rgb ) {
		ColorPoint3D p = points.pop();
		p.set(x,y,z);
		p.rgb = rgb;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paint(g);

		Graphics2D g2 = (Graphics2D)g;

		int r = 2;
		int w = r*2+1;

		for( int i = 0; i < points.size; i++ ) {
			ColorPoint3D p = points.get(i);

			int x = (int)p.x;
			int y = (int)p.z;

			g2.setColor(new Color(p.rgb));
			g2.fillOval(x-r,y-r,w,w);
		}
	}

}
