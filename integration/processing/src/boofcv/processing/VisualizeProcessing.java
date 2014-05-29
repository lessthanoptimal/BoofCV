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
}
