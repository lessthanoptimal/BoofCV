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

package boofcv.gui.feature;

import boofcv.alg.feature.detect.line.LineImageOps;
import boofcv.gui.BoofSwingUtil;
import boofcv.gui.image.ImageZoomPanel;
import georegression.metric.Distance2D_F32;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws lines over an image. Used for displaying the output of line detection algorithms.
 *
 * @author Peter Abeles
 */
public class ImageLinePanelZoom extends ImageZoomPanel {

	public List<LineSegment2D_F32> lines = new ArrayList<>();

	protected Line2D.Double line = new Line2D.Double();

	protected int selectedLine = -1;

	public synchronized void setLines( List<LineParametric2D_F32> lines, int width, int height ) {
		this.lines.clear();
		for (LineParametric2D_F32 p : lines) {
			LineSegment2D_F32 l = LineImageOps.convert(p, width, height);
			if (l == null)
				continue;
//				throw new RuntimeException("null line?!");
			this.lines.add(l);
		}
		selectedLine = -1;
	}

	public synchronized void setLineSegments( List<LineSegment2D_F32> lines ) {
		this.lines.clear();
		this.lines.addAll(lines);
		selectedLine = -1;
	}

	public synchronized int findLine( double x, double y, float tolerance ) {
		int bestLine = -1;
		float bestDistance = tolerance;

		for (int i = 0; i < lines.size(); i++) {
			float d = Distance2D_F32.distance(lines.get(i), (float)x, (float)y);
			if (d < bestDistance) {
				bestDistance = d;
				bestLine = i;
			}
		}

		return bestLine;
	}

	public void setSelected( int selected ) {
		this.selectedLine = selected;
	}

	public int getSelected() {
		return selectedLine;
	}

	@Override
	protected synchronized void paintInPanel( AffineTransform tran, Graphics2D g2 ) {
		BoofSwingUtil.antialiasing(g2);
		g2.setStroke(new BasicStroke(3));

		for (int i = 0; i < lines.size(); i++) {
			LineSegment2D_F32 s = lines.get(i);
			line.x1 = scale*s.a.x;
			line.y1 = scale*s.a.y;
			line.x2 = scale*s.b.x;
			line.y2 = scale*s.b.y;

			if (i == selectedLine) {
				g2.setColor(Color.GREEN);
			} else {
				g2.setColor(Color.RED);
			}
			g2.draw(line);
			g2.setColor(Color.BLUE);
			g2.fillOval((int)line.x1 - 1, (int)line.y1 - 1, 3, 3);
			g2.fillOval((int)line.x2 - 1, (int)line.y2 - 1, 3, 3);
		}
	}
}
