/*
 * Copyright (c) 2011-2018, Peter Abeles. All Rights Reserved.
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

package boofcv.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;

/**
 * Draws a simple XY plot
 */
public class PlotXyPanel extends JPanel {

	public double valuesY[]=new double[0];

	public double minY,maxY;

	public double scaleX=1.0;
	public double offsetX=0;

	protected Line2D.Double line = new Line2D.Double();

	public PlotXyPanel( double valuesY[] , double minY , double maxY )
	{
		this.valuesY = valuesY.clone();
		this.minY = minY;
		this.maxY = maxY;
	}

	public PlotXyPanel() {

	}

	{
		setBackground(Color.WHITE);
	}


	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke(1));

		double h = getHeight()-1;
		double rangeY = maxY-minY;

		double mh = h*0.1;

		// Draw vertical line at data point to make it's location easier to see
		g2.setColor(Color.LIGHT_GRAY);
		for (int i = 0; i < valuesY.length; i++) {
			double y = valuesY[i];

			double pixelY = h*(1.0-(y-minY)/rangeY);
			double pixelX = scaleX*(i-offsetX);

			line.setLine(pixelX,pixelY-mh,pixelX,pixelY+mh);
			g2.draw(line);
		}

		// Draw a line connecting points in the plot
		g2.setColor(Color.BLACK);
		for (int i = 1; i < valuesY.length; i++) {
			double y0 = valuesY[i-1];
			double y1 = valuesY[i];

			double pixelY0 = h*(1.0-(y0-minY)/rangeY);
			double pixelY1 = h*(1.0-(y1-minY)/rangeY);

			double x0 = scaleX*(i-1-offsetX);
			double x1 = scaleX*(i-offsetX);

			line.setLine(x0,pixelY0,x1,pixelY1);
			g2.draw(line);
		}
	}

	public void setScaleX(double scale) {
		this.scaleX = scale;
	}
}
