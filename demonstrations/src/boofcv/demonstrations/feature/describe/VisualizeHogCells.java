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

package boofcv.demonstrations.feature.describe;

import boofcv.alg.feature.dense.DescribeDenseHogAlg;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

/**
 * Renders cells in HOG
 *
 * @author Peter Abeles
 */
public class VisualizeHogCells {

	DescribeDenseHogAlg<?,?> hog;
	Color colors[] = new Color[256];
	float cos[],sin[];

	boolean localMax = false;

	public VisualizeHogCells(DescribeDenseHogAlg<?,?> hog , boolean logIntensity ) {

		this.hog = hog;

		if( logIntensity ) {
			double k = 255.0 / Math.log(255);
			for (int i = 0; i < colors.length; i++) {
				int v = (int) (k * Math.log(i + 1));
				colors[i] = new Color(v, v, v);
			}
		} else {
			for (int i = 0; i < colors.length; i++) {
				colors[i] = new Color(i, i, i);
			}
		}

		int numAngles = hog.getOrientationBins();
		cos = new float[numAngles];
		sin = new float[numAngles];

		for (int i = 0; i < numAngles; i++) {
			double theta = Math.PI*(i+0.5)/numAngles;

			cos[i] = (float)Math.cos(theta);
			sin[i] = (float)Math.sin(theta);
		}
	}

	public BufferedImage createOutputBuffered( BufferedImage input ) {
		int cell = hog.getWidthCell();
		int rows = hog.getCellRows();
		int cols = hog.getCellCols();
		int width = cell*cols;
		int height = cell*rows;

		if( input == null || input.getWidth() != width || input.getHeight() != height )
			return new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
		else
			return input;
	}

	public void render( Graphics2D g2 ) {

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(1));

		if( localMax ) {
			local(g2);
		} else {
			global(g2);
		}
	}

	private void local(Graphics2D g2) {
		int cell = hog.getWidthCell();
		int rows = hog.getCellRows();
		int cols = hog.getCellCols();
		int width = cell*cols;
		int height = cell*rows;

		int numAngles = hog.getOrientationBins();

		float r = cell/2;

		Line2D.Float line = new Line2D.Float();

		for (int y = 0; y < height; y += cell) {
			float c_y = y + r;

			for (int x = 0; x < width; x += cell) {
				DescribeDenseHogAlg.Cell c = hog.getCell(y / cell, x / cell);

				float c_x = x + r;

				float maxValue = 0;
				for (int i = 0; i < c.histogram.length; i++) {
					maxValue = Math.max(maxValue,c.histogram[i]);
				}

				for (int i = 0; i < numAngles; i++) {
					int a = (int) (255.0f * c.histogram[i] / maxValue + 0.5f);

					g2.setColor(colors[a]);

					float x0 = c_x - r * cos[i] + 0.5f;
					float x1 = c_x + r * cos[i] + 0.5f;
					float y0 = c_y - r * sin[i] + 0.5f;
					float y1 = c_y + r * sin[i] + 0.5f;

					line.setLine(x0, y0, x1, y1);
					g2.draw(line);
				}
			}
		}
	}

	private void global(Graphics2D g2) {
		int cell = hog.getWidthCell();
		int rows = hog.getCellRows();
		int cols = hog.getCellCols();
		int width = cell*cols;
		int height = cell*rows;

		int numAngles = hog.getOrientationBins();

		float r = cell/2;

		float maxValue = 0;
		for (int y = 0; y < rows; y++) {
			for (int x = 0; x < cols; x++) {
				DescribeDenseHogAlg.Cell c = hog.getCell(y, x);
				for (int i = 0; i < numAngles; i++) {
					maxValue = Math.max(maxValue, c.histogram[i]);
				}
			}
		}

		System.out.println("max value "+maxValue);

		Line2D.Float line = new Line2D.Float();

		for (int y = 0; y < height; y += cell) {
			float c_y = y + r;

			for (int x = 0; x < width; x += cell) {
				DescribeDenseHogAlg.Cell c = hog.getCell(y / cell, x / cell);

				float c_x = x + r;

				for (int i = 0; i < numAngles; i++) {
					int a = (int) (255.0f * c.histogram[i] / maxValue + 0.5f);

					g2.setColor(colors[a]);

					float x0 = c_x - r * cos[i] + 0.5f;
					float x1 = c_x + r * cos[i] + 0.5f;
					float y0 = c_y - r * sin[i] + 0.5f;
					float y1 = c_y + r * sin[i] + 0.5f;

					line.setLine(x0, y0, x1, y1);
					g2.draw(line);
				}
			}
		}
	}

	public boolean isLocalMax() {
		return localMax;
	}

	public void setLocalMax(boolean localMax) {
		this.localMax = localMax;
	}
}
