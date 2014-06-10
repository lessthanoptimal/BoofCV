/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.processing;

import boofcv.alg.misc.ImageStatistics;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.ImageInteger;
import boofcv.struct.image.ImageSInt16;
import boofcv.struct.image.ImageSInt32;
import processing.core.PConstants;
import processing.core.PImage;

import java.util.Random;

/**
 * Functions for visualizing BoofCV data for Processing
 *
 * @author Peter Abeles
 */
public class VisualizeProcessing {

	/**
	 * Visualizes a labeled image.  Each label is assigned a random color
	 * @param image Labeled input image
	 * @return Rendered color output image
	 */
	public static PImage labeled( ImageSInt32 image ) {

		int numRegions = ImageStatistics.max(image)+1;

		int colors[] = new int[numRegions];

		Random rand = new Random(123);
		for( int i = 0; i < colors.length; i++ ) {
			colors[i] = rand.nextInt() | 0xFF000000;
		}
		colors[0]= 0xFF000000;

		return labeled(image, colors);
	}

	/**
	 * Renders positive values as red and negative values as green.  The intensity of the color is a linear
	 * function relative to the maximum intensity of the image.
	 *
	 * @param src Input image.
	 * @param maxAbsValue The maximum absolute value of the image.
	 * @return Visualized image.
	 */
	public static PImage colorizeSign(ImageFloat32 src, float maxAbsValue) {
		PImage out = new PImage(src.width, src.height, PConstants.RGB);

		int indexOut = 0;
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++, indexOut++ ) {
				float v = src.get(x, y);

				int rgb;
				if (v > 0) {
					rgb = (int) (255 * v / maxAbsValue) << 16;
				} else {
					rgb = (int) (-255 * v / maxAbsValue) << 8;
				}
				out.pixels[indexOut] = rgb;
			}
		}

		return out;
	}

	/**
	 * Renders positive values as red and negative values as green.  The intensity of the color is a linear
	 * function relative to the maximum intensity of the image.
	 *
	 * @param src Input image.
	 * @param maxAbsValue The maximum absolute value of the image.
	 * @return Visualized image.
	 */
	public static PImage colorizeSign(ImageInteger src, int maxAbsValue) {
		PImage out = new PImage(src.width, src.height, PConstants.RGB);

		int indexOut = 0;
		for (int y = 0; y < src.height; y++) {
			for (int x = 0; x < src.width; x++, indexOut++ ) {
				int v = src.get(x, y);

				int rgb;
				if (v > 0) {
					rgb = (int) (255 * v / maxAbsValue) << 16;
				} else {
					rgb = (int) (-255 * v / maxAbsValue) << 8;
				}
				out.pixels[indexOut] = rgb;
			}
		}

		return out;
	}

	/**
	 * Renders a labeled image with the provided lookup table for each region's RGB color.
	 *
	 * @param image Input labeled image
	 * @param colors Color lookup table
	 * @return Visualized image
	 */
	public static PImage labeled(ImageSInt32 image, int[] colors) {
		PImage out = new PImage(image.width, image.height, PConstants.RGB);

		int indexOut = 0;
		for (int y = 0; y < image.height; y++) {
			int indexImage = image.startIndex + image.stride*y;
			for (int x = 0; x < image.width; x++,indexImage++,indexOut++) {
				out.pixels[indexOut] = colors[image.data[indexImage]];
			}
		}

		return out;
	}

	/**
	 * Renders the image gradient into a single output image.  Each direction has a unique color and the
	 * intensity is dependent upon the edge's relative intensity
	 * @param dx Derivative x-axis
	 * @param dy Derivative y-axis
	 * @return Visualized image
	 */
	public static PImage gradient(ImageFloat32 dx, ImageFloat32 dy) {
		PImage out = new PImage(dx.width, dx.height, PConstants.RGB);

		float maxAbsValue = ImageStatistics.maxAbs(dx);
		maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(dy));
		if( maxAbsValue == 0 )
			return out;

		int indexOut = 0;
		for (int y = 0; y < dx.height; y++) {
			int indexX = dx.startIndex + dx.stride*y;
			int indexY = dy.startIndex + dy.stride*y;

			for (int x = 0; x < dy.width; x++,indexX++,indexY++,indexOut++) {

				float valueX = dx.data[ indexX ];
				float valueY = dy.data[ indexY ];

				int r=0,g=0,b=0;

				if( valueX > 0 ) {
					r = (int)(255*valueX/maxAbsValue);
				} else {
					g = (int)(-255*valueX/maxAbsValue);
				}
				if( valueY > 0 ) {
					b = (int)(255*valueY/maxAbsValue);
				} else {
					int v = (int)(-255*valueY/maxAbsValue);
					r += v;
					g += v;
					if( r > 255 ) r = 255;
					if( g > 255 ) g = 255;
				}

				out.pixels[indexOut] = 0xFF << 24 | r << 16 | g << 8 | b;
			}
		}

		return out;
	}

	/**
	 * Renders the image gradient into a single output image.  Each direction has a unique color and the
	 * intensity is dependent upon the edge's relative intensity
	 * @param dx Derivative x-axis
	 * @param dy Derivative y-axis
	 * @return Visualized image
	 */
	public static PImage gradient(ImageSInt16 dx, ImageSInt16 dy) {
		PImage out = new PImage(dx.width, dx.height, PConstants.RGB);

		int maxAbsValue = ImageStatistics.maxAbs(dx);
		maxAbsValue = Math.max(maxAbsValue, ImageStatistics.maxAbs(dy));
		if( maxAbsValue == 0 )
			return out;

		int indexOut = 0;
		for (int y = 0; y < dx.height; y++) {
			int indexX = dx.startIndex + dx.stride*y;
			int indexY = dy.startIndex + dy.stride*y;

			for (int x = 0; x < dy.width; x++,indexX++,indexY++,indexOut++) {

				int valueX = dx.data[ indexX ];
				int valueY = dy.data[ indexY ];

				int r=0,g=0,b=0;

				if( valueX > 0 ) {
					r = 255*valueX/maxAbsValue;
				} else {
					g = -255*valueX/maxAbsValue;
				}
				if( valueY > 0 ) {
					b = 255*valueY/maxAbsValue;
				} else {
					int v = -255*valueY/maxAbsValue;
					r += v;
					g += v;
					if( r > 255 ) r = 255;
					if( g > 255 ) g = 255;
				}

				out.pixels[indexOut] = 0xFF << 24 | r << 16 | g << 8 | b;
			}
		}

		return out;
	}

	public static PImage denseFlow(ImageFlow flowImage ) {

		float maxValue = 0;
		int N = flowImage.width*flowImage.height;
		for (int i = 0; i < N; i++) {
			ImageFlow.D f = flowImage.data[i];

			float v = Math.max(Math.abs(f.x),Math.abs(f.y));
			if( v > maxValue )
				maxValue = v;
		}
		return denseFlow(flowImage,maxValue);
	}

	public static PImage denseFlow(ImageFlow flowImage , float maxValue ) {

		PImage out = new PImage(flowImage.width,flowImage.height, PConstants.RGB);

		int tableSine[] = new int[360];
		int tableCosine[] = new int[360];

		for( int i = 0; i < 360; i++ ) {
			double angle = i*Math.PI/180.0;
			tableSine[i] = (int)(255*(Math.sin(angle)+1)/2);
			tableCosine[i] = (int)(255*(Math.cos(angle)+1)/2);
		}

		int N = flowImage.width*flowImage.height;

		for (int i = 0; i < N; i++) {
			ImageFlow.D f = flowImage.data[i];

			if( !f.isValid() ) {
				out.pixels[i] = 0xFF000055;
			} else {
				float m = Math.max(Math.abs(f.x),Math.abs(f.y))/maxValue;

				if( m > 1 )m = 1;

				double angle = Math.atan2(f.y,f.x);
				int degree = (int)(180+angle*179.999/Math.PI);
				int r = (int)(m*tableSine[degree]);
				int g = (int)(m*tableCosine[degree]);

				out.pixels[i] = 0xFF << 24 | r << 16 | g << 8;
			}
		}

		return out;
	}
}
