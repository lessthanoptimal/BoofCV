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


import boofcv.alg.feature.detect.line.LineImageOps;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;

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

	public BufferedImage background;
	public List<LineSegment2D_F32> lines = new ArrayList<>();

	public synchronized void setBackground(BufferedImage background) {
		this.background = background;
	}

	public synchronized void setLines(List<LineParametric2D_F32> lines) {
		this.lines.clear();
		for( LineParametric2D_F32 p : lines ) {
			this.lines.add(LineImageOps.convert(p, background.getWidth(), background.getHeight()));
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

}
