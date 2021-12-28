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
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import georegression.struct.line.LineParametric2D_F32;
import georegression.struct.line.LineSegment2D_F32;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Draws lines over an image. Used for displaying the output of line detection algorithms.
 *
 * @author Peter Abeles
 */
public class ImageLinePanel extends ImagePanel {

	public final List<LineSegment2D_F32> lines = new ArrayList<>();

	Line2D.Double line = new Line2D.Double();

	public ImageLinePanel() {
		setScaling(ScaleOptions.DOWN);
	}

	public synchronized void setLines( List<LineParametric2D_F32> lines ) {
		Objects.requireNonNull(img);
		this.lines.clear();
		for (LineParametric2D_F32 p : lines) {
			LineSegment2D_F32 s = LineImageOps.convert(p, img.getWidth(), img.getHeight());
			if (s != null)
				this.lines.add(s);
		}
	}

	public synchronized void setLineSegments( List<LineSegment2D_F32> lines ) {
		this.lines.clear();
		this.lines.addAll(lines);
	}

	@Override
	public synchronized void paintComponent( Graphics g ) {
		super.paintComponent(g);

		if (img == null)
			return;

		Graphics2D g2 = BoofSwingUtil.antialiasing(g);
		g2.setStroke(new BasicStroke(3));

		for (LineSegment2D_F32 s : lines) {
			line.x1 = scale*s.a.x + offsetX;
			line.y1 = scale*s.a.y + offsetY;
			line.x2 = scale*s.b.x + offsetX;
			line.y2 = scale*s.b.y + offsetY;

			g2.setColor(Color.RED);
			g2.draw(line);
			g2.setColor(Color.BLUE);
			g2.fillOval((int)line.x1 - 1, (int)line.y1 - 1, 3, 3);
			g2.fillOval((int)line.x2 - 1, (int)line.y2 - 1, 3, 3);
		}
	}
}
