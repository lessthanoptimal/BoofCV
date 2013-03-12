/*
 * Copyright (c) 2011-2013, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.color;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.ImageFloat32;
import boofcv.struct.image.MultiSpectral;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestColorYuv {
	public static final double tol = 0.01;

	Random rand = new Random(234);

	double yuv[] = new double[3];
	double rgb[] = new double[3];

	@Test
	public void backAndForth_F64() {

		check(0.5, 0.3, 0.2);
		check(0, 0, 0);
		check(1, 0, 0);
		check(0, 1, 0);
		check(0, 0, 1);
		check(0.5, 0.5, 0.5);
		check(0.25, 0.5, 0.75);
		check(0.8, 0.1, 0.75);

		for( int i = 0; i < 50; i++ ) {
			double r = rand.nextDouble();
			double g = rand.nextDouble();
			double b = rand.nextDouble();

			check(r,g,b);
		}

		// check other ranges
		for( int i = 0; i < 50; i++ ) {
			double r = rand.nextDouble()*255;
			double g = rand.nextDouble()*255;
			double b = rand.nextDouble()*255;

			check(r,g,b);
		}
	}

	private void check( double r , double g , double b ) {
		ColorYuv.rgbToYuv(r,g,b, yuv);
		ColorYuv.yuvToRgb(yuv[0], yuv[1], yuv[2],rgb);
		check(rgb,r,g,b);
	}

	@Test
	public void multispectral_F32() {
		MultiSpectral<ImageFloat32> rgb = new MultiSpectral<ImageFloat32>(ImageFloat32.class,10,15,3);
		MultiSpectral<ImageFloat32> hsv = new MultiSpectral<ImageFloat32>(ImageFloat32.class,10,15,3);
		MultiSpectral<ImageFloat32> found = new MultiSpectral<ImageFloat32>(ImageFloat32.class,10,15,3);

		GImageMiscOps.fillUniform(rgb, rand, 0, 1);

		ColorHsv.hsvToRgb_F32(rgb,hsv);
		ColorHsv.rgbToHsv_F32(hsv,found);

		for( int y = 0; y < rgb.height; y++ ) {
			for( int x = 0; x < rgb.width; x++ ) {
				double r = rgb.getBand(0).get(x,y);
				double g = rgb.getBand(1).get(x,y);
				double b = rgb.getBand(2).get(x,y);

				assertEquals(r,found.getBand(0).get(x,y),tol);
				assertEquals(g,found.getBand(1).get(x,y),tol);
				assertEquals(b,found.getBand(2).get(x,y),tol);
			}
		}
	}

	private static void check( double found[] , double a , double b , double c ) {
		assertEquals(a,found[0],tol);
		assertEquals(b,found[1],tol);
		assertEquals(c, found[2], tol);
	}
}
