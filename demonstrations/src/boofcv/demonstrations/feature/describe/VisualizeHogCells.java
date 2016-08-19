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

import boofcv.abst.feature.dense.DescribeImageDense;
import boofcv.factory.feature.dense.ConfigDenseHoG;
import boofcv.struct.feature.TupleDesc_F64;
import georegression.struct.point.Point2D_I32;

import java.awt.*;
import java.awt.geom.Line2D;
import java.util.List;

/**
 * Renders cells in HOG
 *
 * @author Peter Abeles
 */
public class VisualizeHogCells {

	DescribeImageDense<?,TupleDesc_F64> hog;
	ConfigDenseHoG config;
	Color colors[];
	float cos[],sin[];

	boolean localMax = false;
	boolean showGrid = false;

	public VisualizeHogCells(DescribeImageDense<?,TupleDesc_F64> hog, ConfigDenseHoG config ) {
		setHoG(hog,config);
	}

	public synchronized void setHoG(DescribeImageDense<?,TupleDesc_F64> hog, ConfigDenseHoG config ) {
		this.hog = hog;
		this.config = config;
		int numAngles = config.orientationBins;
		cos = new float[numAngles];
		sin = new float[numAngles];

		for (int i = 0; i < numAngles; i++) {
			double theta = Math.PI*(i+0.5)/numAngles;

			cos[i] = (float)Math.cos(theta);
			sin[i] = (float)Math.sin(theta);
		}
	}

	public synchronized void render( Graphics2D g2 , int imageWidth , int imageHeight ) {

		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

		if( showGrid ) {
			int regionWidth = config.pixelsPerCell *config.cellsPerBlock;
			int regionHeight = config.pixelsPerCell *config.cellsPerBlock;

			int stepX = config.pixelsPerCell *config.stepBlock;
			int stepY = config.pixelsPerCell *config.stepBlock;

			g2.setColor(new Color(150, 150, 0));
			g2.setStroke(new BasicStroke(1));

			for (int x = 0; x < imageWidth; x += stepX ) {
				g2.drawLine(x,0,x,imageHeight);
				int xx = x + regionWidth;
				g2.drawLine(xx,0,xx,imageHeight);
			}

			for (int y = 0; y < imageHeight; y += stepY ) {
				g2.drawLine(0,y,imageWidth,y);
				int yy = y + regionHeight;
				g2.drawLine(0,yy,imageWidth,yy);
			}

		}

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setStroke(new BasicStroke(1));
		if( localMax ) {
			local(g2);
		} else {
			global(g2);
		}
	}

	private void local(Graphics2D g2) {

		float r = config.pixelsPerCell /2.0f;
		int numAngles = config.orientationBins;

		List<TupleDesc_F64> descriptions = hog.getDescriptions();
		List<Point2D_I32> locations = hog.getLocations();

		Line2D.Float line = new Line2D.Float();
		for (int i = 0; i < locations.size(); i++) {
			TupleDesc_F64 desc = descriptions.get(i);
			Point2D_I32 p = locations.get(i);

			for (int cellRow = 0; cellRow < config.cellsPerBlock; cellRow++) {
				int c_y = p.y;
				for (int cellCol = 0; cellCol < config.cellsPerBlock; cellCol++) {
					int c_x = p.x;

					// the descriptor is encoded in a row major format cell by cell
					int start = (cellRow*config.cellsPerBlock + cellCol)*numAngles;

					double maxValue = 0;
					for (int j = 0; j < numAngles; j++) {
						maxValue = Math.max(maxValue,desc.value[j+start]);
					}

					for (int j = 0; j < numAngles; j++) {
						int a = (int) (255.0f * desc.value[j+start] / maxValue + 0.5f);

						g2.setColor(colors[a]);

						float x0 = c_x - r * cos[j];
						float x1 = c_x + r * cos[j];
						float y0 = c_y - r * sin[j];
						float y1 = c_y + r * sin[j];

						line.setLine(x0, y0, x1, y1);
						g2.draw(line);
					}
				}
			}
		}
	}

	private void global(Graphics2D g2) {
		float r = config.pixelsPerCell /2.0f;
		int numAngles = config.orientationBins;

		List<TupleDesc_F64> descriptions = hog.getDescriptions();
		List<Point2D_I32> locations = hog.getLocations();

		double maxValue = 0;
		for (int i = 0; i < descriptions.size(); i++) {
			TupleDesc_F64 d = descriptions.get(i);
			for (int j = 0; j < d.size(); j++) {
				maxValue = Math.max(maxValue,d.value[j]);
			}
		}

		Line2D.Float line = new Line2D.Float();
		for (int i = 0; i < locations.size(); i++) {
			TupleDesc_F64 desc = descriptions.get(i);
			Point2D_I32 p = locations.get(i);

			for (int cellRow = 0; cellRow < config.cellsPerBlock; cellRow++) {
				int c_y = p.y;
				for (int cellCol = 0; cellCol < config.cellsPerBlock; cellCol++) {
					int c_x = p.x;

					// the descriptor is encoded in a row major format cell by cell
					int start = (cellRow*config.cellsPerBlock + cellCol)*numAngles;

					for (int j = 0; j < numAngles; j++) {
						int a = (int) (255.0f * desc.value[j+start] / maxValue + 0.5f);

						g2.setColor(colors[a]);

						float x0 = c_x - r * cos[j];
						float x1 = c_x + r * cos[j];
						float y0 = c_y - r * sin[j];
						float y1 = c_y + r * sin[j];

						line.setLine(x0, y0, x1, y1);
						g2.draw(line);
					}
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

	public boolean isShowGrid() {
		return showGrid;
	}

	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}

	public void setShowLog(boolean logIntensity) {
		Color colors[] = new Color[256];
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
		this.colors = colors;
	}
}
