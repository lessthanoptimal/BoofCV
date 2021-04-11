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

import boofcv.alg.misc.PixelMath;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;
import georegression.struct.point.Point2D_F64;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

/**
 * Utilities for visualizing optical flow
 */
public class VisualizeOpticalFlow {
	Line2D.Double line = new Line2D.Double();

	public double maxVelocity = 0;
	public int red, green, blue;

	Stroke strokeLine = new BasicStroke(5);

	// Used for computing log scale flow colors
	double logBase = 0.0;
	double logScale = 25.0;
	double maxLog = Math.log(logScale + logBase);

	public static void colorizeDirection( ImageFlow flowImage, BufferedImage out ) {

		int[] tableSine = new int[360];
		int[] tableCosine = new int[360];

		for (int i = 0; i < 360; i++) {
			double angle = i*Math.PI/180.0;
			tableSine[i] = (int)(255*(Math.sin(angle) + 1)/2);
			tableCosine[i] = (int)(255*(Math.cos(angle) + 1)/2);
		}

		for (int y = 0; y < flowImage.height; y++) {
			for (int x = 0; x < flowImage.width; x++) {
				ImageFlow.D f = flowImage.unsafe_get(x, y);

				if (!f.isValid()) {
					out.setRGB(x, y, 0xFF);
				} else {
					double angle = Math.atan2(f.y, f.x);
					int degree = (int)(180 + angle*179.999/Math.PI);
					int r = tableSine[degree];
					int g = tableCosine[degree];

					out.setRGB(x, y, r << 16 | g << 8);
				}
			}
		}
	}

	public static void magnitudeAbs( ImageFlow flowImage, BufferedImage out ) {

		GrayF32 magnitude = new GrayF32(flowImage.width, flowImage.height);

		float max = 0;

		for (int y = 0; y < flowImage.height; y++) {
			for (int x = 0; x < flowImage.width; x++) {
				ImageFlow.D f = flowImage.unsafe_get(x, y);

				if (!f.isValid()) {
					out.setRGB(x, y, 0xFF);
				} else {
					float m = Math.max(Math.abs(f.x), Math.abs(f.y));
					if (m > max)
						max = m;
					magnitude.unsafe_set(x, y, m);
				}
			}
		}

		PixelMath.multiply(magnitude, 255/max, magnitude);

		ConvertBufferedImage.convertTo(magnitude, out);
	}

	public static void magnitudeAbs( ImageFlow flowImage, float maxValue, BufferedImage out ) {

		GrayF32 magnitude = new GrayF32(flowImage.width, flowImage.height);

		for (int y = 0; y < flowImage.height; y++) {
			for (int x = 0; x < flowImage.width; x++) {
				ImageFlow.D f = flowImage.unsafe_get(x, y);

				if (!f.isValid()) {
					out.setRGB(x, y, 0xFF);
				} else {
					float m = Math.max(Math.abs(f.x), Math.abs(f.y));
					magnitude.unsafe_set(x, y, m);
				}
			}
		}

		PixelMath.multiply(magnitude, 255/maxValue, magnitude);
		PixelMath.boundImage(magnitude, 0, 255);

		ConvertBufferedImage.convertTo(magnitude, out);
	}

	public static void colorized( ImageFlow flowImage, float maxValue, BufferedImage out ) {

		int[] tableSine = new int[360];
		int[] tableCosine = new int[360];

		for (int i = 0; i < 360; i++) {
			double angle = i*Math.PI/180.0;
			tableSine[i] = (int)(255*(Math.sin(angle) + 1)/2);
			tableCosine[i] = (int)(255*(Math.cos(angle) + 1)/2);
		}

		for (int y = 0; y < flowImage.height; y++) {
			for (int x = 0; x < flowImage.width; x++) {
				ImageFlow.D f = flowImage.unsafe_get(x, y);

				if (!f.isValid()) {
					out.setRGB(x, y, 0x55);
				} else {
					float m = Math.max(Math.abs(f.x), Math.abs(f.y))/maxValue;

					if (m > 1) m = 1;

					double angle = Math.atan2(f.y, f.x);
					int degree = (int)(180 + angle*179.999/Math.PI);
					int r = (int)(m*tableSine[degree]);
					int g = (int)(m*tableCosine[degree]);

					out.setRGB(x, y, r << 16 | g << 8);
				}
			}
		}
	}

	public void drawLine( double x1, double y1, double x2, double y2, Graphics2D g2 ) {
		g2.setColor(createColor());
		line.x1 = x1;
		line.y1 = y1;
		line.x2 = x2;
		line.y2 = y2;
		g2.setStroke(strokeLine);
		g2.draw(line);
	}

	public Color createColor() {
		return new Color(red, green, blue);
	}

	public void computeColor( Point2D_F64 p, Point2D_F64 prev, boolean log ) {
		if (prev == null) {
			red = blue = 0;
			green = 0xFF;
		} else {
			if (log)
				computeColorLog(p.x - prev.x, p.y - prev.y);
			else
				computeColor(p.x - prev.x, p.y - prev.y);
		}
	}

	public void computeColor( double dx, double dy ) {
		red = blue = green = 0;

		if (dx > 0) {
			red = Math.min(255, (int)(255*dx/maxVelocity));
		} else {
			green = Math.min(255, (int)(-255*dx/maxVelocity));
		}
		if (dy > 0) {
			blue = Math.min(255, (int)(255*dy/maxVelocity));
		} else {
			int v = Math.min(255, (int)(-255*dy/maxVelocity));
			red += v;
			green += v;
			if (red > 255) red = 255;
			if (green > 255) green = 255;
		}
	}

	public void computeColorLog( double dx, double dy ) {
		red = blue = green = 0;

		if (dx > 0) {
			red = Math.max(0, (int)(255*Math.log(logBase + logScale*dx/maxVelocity)/maxLog));
		} else {
			green = Math.max(0, (int)(255*Math.log(logBase - logScale*dx/maxVelocity)/maxLog));
		}
		if (dy > 0) {
			blue = Math.max(0, (int)(255*Math.log(logBase + logScale*dy/maxVelocity)/maxLog));
		} else {
			int v = Math.max(0, (int)(255*Math.log(logBase - logScale*dy/maxVelocity)/maxLog));
			red += v;
			green += v;
			if (red > 255) red = 255;
			if (green > 255) green = 255;
		}
	}
}
