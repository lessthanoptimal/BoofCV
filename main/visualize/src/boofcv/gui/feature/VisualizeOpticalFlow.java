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

import boofcv.alg.misc.PixelMath;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.flow.ImageFlow;
import boofcv.struct.image.GrayF32;

import java.awt.image.BufferedImage;

/**
 * @author Peter Abeles
 */
public class VisualizeOpticalFlow {

	public static void colorizeDirection(ImageFlow flowImage, BufferedImage out) {

		int tableSine[] = new int[360];
		int tableCosine[] = new int[360];

		for( int i = 0; i < 360; i++ ) {
			double angle = i*Math.PI/180.0;
			tableSine[i] = (int)(255*(Math.sin(angle)+1)/2);
			tableCosine[i] = (int)(255*(Math.cos(angle)+1)/2);
		}

		for( int y = 0; y < flowImage.height; y++ ) {
			for( int x = 0; x < flowImage.width; x++ ) {
				ImageFlow.D f = flowImage.unsafe_get(x,y);

				if( !f.isValid() ) {
					out.setRGB(x,y,0xFF);
				} else {
					double angle = Math.atan2(f.y,f.x);
					int degree = (int)(180+angle*179.999/Math.PI);
					int r = tableSine[degree];
					int g = tableCosine[degree];

					out.setRGB(x,y,r<<16 | g<<8);
				}
			}
		}
	}

	public static void magnitudeAbs(ImageFlow flowImage, BufferedImage out) {

		GrayF32 magnitude = new GrayF32(flowImage.width,flowImage.height);

		float max = 0;

		for( int y = 0; y < flowImage.height; y++ ) {
			for( int x = 0; x < flowImage.width; x++ ) {
				ImageFlow.D f = flowImage.unsafe_get(x,y);

				if( !f.isValid() ) {
					out.setRGB(x,y,0xFF);
				} else {
					float m = Math.max(Math.abs(f.x),Math.abs(f.y));
					if( m > max )
						max = m;
					magnitude.unsafe_set(x, y, m);
				}
			}
		}

		PixelMath.multiply(magnitude, 255 / max, magnitude);

		ConvertBufferedImage.convertTo(magnitude,out);
	}

	public static void magnitudeAbs(ImageFlow flowImage, float maxValue,  BufferedImage out) {

		GrayF32 magnitude = new GrayF32(flowImage.width,flowImage.height);


		for( int y = 0; y < flowImage.height; y++ ) {
			for( int x = 0; x < flowImage.width; x++ ) {
				ImageFlow.D f = flowImage.unsafe_get(x,y);

				if( !f.isValid() ) {
					out.setRGB(x,y,0xFF);
				} else {
					float m = Math.max(Math.abs(f.x),Math.abs(f.y));
					magnitude.unsafe_set(x, y, m);
				}
			}
		}

		PixelMath.multiply(magnitude, 255 / maxValue, magnitude);
		PixelMath.boundImage(magnitude,0,255);

		ConvertBufferedImage.convertTo(magnitude,out);
	}

	public static void colorized(ImageFlow flowImage, float maxValue,  BufferedImage out) {

		int tableSine[] = new int[360];
		int tableCosine[] = new int[360];

		for( int i = 0; i < 360; i++ ) {
			double angle = i*Math.PI/180.0;
			tableSine[i] = (int)(255*(Math.sin(angle)+1)/2);
			tableCosine[i] = (int)(255*(Math.cos(angle)+1)/2);
		}

		for( int y = 0; y < flowImage.height; y++ ) {
			for( int x = 0; x < flowImage.width; x++ ) {
				ImageFlow.D f = flowImage.unsafe_get(x,y);

				if( !f.isValid() ) {
					out.setRGB(x,y,0x55);
				} else {
					float m = Math.max(Math.abs(f.x),Math.abs(f.y))/maxValue;

					if( m > 1 )m = 1;

					double angle = Math.atan2(f.y,f.x);
					int degree = (int)(180+angle*179.999/Math.PI);
					int r = (int)(m*tableSine[degree]);
					int g = (int)(m*tableCosine[degree]);

					out.setRGB(x, y, r << 16 | g << 8);
				}
			}
		}
	}
}
