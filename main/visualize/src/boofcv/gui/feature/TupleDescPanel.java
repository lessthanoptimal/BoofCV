/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

import boofcv.struct.feature.TupleDesc;

import javax.swing.*;
import java.awt.*;


/**
 * Visualizes the a {@link boofcv.struct.feature.TupleDesc_F64}.
 *
 * @author Peter Abeles
 */
public class TupleDescPanel extends JPanel {

	TupleDesc desc;

	public TupleDesc getDesc() {
		return desc;
	}

	public void setDescription(TupleDesc desc) {
		this.desc = desc;
	}

	@Override
	public synchronized void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;

		TupleDesc desc = this.desc;
		if( desc == null ) {
			g2.setColor(Color.WHITE);
			g2.fillRect(0,0,getWidth(),getHeight());
		} else {

			int h = getHeight();
			int w = getWidth();

			int m = h/2;

			int []x = new int[ desc.size() ];
			int []y = new int[ desc.size() ];

			// find the maximum magnitude of any of the elements
			double max = 0;
			for( int i = 0; i < desc.size(); i++ ) {
				double d = desc.getDouble(i);
				if( max < Math.abs(d)) {
					max = Math.abs(d);
				}
			}

			// draw a normalized histogram plot
			double stepX = 1.0/desc.size();

			for( int i = 0; i < desc.size(); i++ ) {
				x[i] = (int)(w*i*stepX);
				y[i] = (int)((m*desc.getDouble(i)/max)+m);
			}

			g2.setColor(Color.GRAY);
			g2.drawLine(0,m,w,m);

			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.RED);
			g2.drawPolyline(x,y,x.length);

			// print out the magnitude
			g2.setColor(Color.BLACK);
			String s = String.format("%4.1e",max);
			g2.drawString(s,0,20);

			g2.setColor(Color.BLUE);
			for( int i = 0; i < desc.size(); i++ ) {
				int r = 1;
				w = r*2+1;
				g2.fillOval(x[i]-r,y[i]-r,w,w);
			}
		}
	}
}
