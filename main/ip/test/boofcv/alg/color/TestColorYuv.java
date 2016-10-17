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

package boofcv.alg.color;

import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Abeles
 */
public class TestColorYuv {
	public static final double tol = 0.01;

	Random rand = new Random(234);

	double yuv_F64[] = new double[3];
	double rgb_F64[] = new double[3];

	float yuv_F32[] = new float[3];
	float rgb_F32[] = new float[3];

	@Test
	public void backAndForth_F64_and_F32() {

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
		// check F64
		ColorYuv.rgbToYuv(r,g,b, yuv_F64);
		ColorYuv.yuvToRgb(yuv_F64[0], yuv_F64[1], yuv_F64[2], rgb_F64);
		check(rgb_F64,r,g,b);

		// Check F32
		float fr = (float)r, fg = (float)g, fb = (float)b;
		ColorYuv.rgbToYuv(fr,fg,fb, yuv_F32);
		ColorYuv.yuvToRgb(yuv_F32[0], yuv_F32[1], yuv_F32[2], rgb_F32);
		check(rgb_F32,fr,fg,fb);
	}

	@Test
	public void backAndForth_U8() {
		for( int i = 0; i < 1000; i++ ) {
			byte yuv[] = new byte[3];
			byte rgb[] = new byte[3];

			int r = rand.nextInt(256);
			int g = rand.nextInt(256);
			int b = rand.nextInt(256);

			ColorYuv.rgbToYCbCr(r,g,b,yuv);
			ColorYuv.ycbcrToRgb(yuv[0] & 0xFF , yuv[1] & 0xFF , yuv[2] & 0xFF , rgb );

			assertTrue(Math.abs(r - (rgb[0] & 0xFF)) < 5);
			assertTrue( Math.abs( g - (rgb[1]&0xFF)) < 5 );
			assertTrue( Math.abs( b - (rgb[2]&0xFF)) < 5 );
		}
	}

	@Test
	public void yuvToRgb_F32_Planar() {
		Planar<GrayF32> yuv = new Planar<>(GrayF32.class,10,15,3);
		Planar<GrayF32> rgb = new Planar<>(GrayF32.class,10,15,3);
		Planar<GrayF32> found = new Planar<>(GrayF32.class,10,15,3);

		GImageMiscOps.fillUniform(yuv, rand, 0, 1);

		ColorYuv.yuvToRgb_F32(yuv, rgb);
		ColorYuv.rgbToYuv_F32(rgb, found);

		for( int y = 0; y < yuv.height; y++ ) {
			for( int x = 0; x < yuv.width; x++ ) {
				float Y = yuv.getBand(0).get(x,y);
				float U = yuv.getBand(1).get(x,y);
				float V = yuv.getBand(2).get(x,y);

				assertEquals(Y,found.getBand(0).get(x,y),tol);
				assertEquals(U,found.getBand(1).get(x,y),tol);
				assertEquals(V,found.getBand(2).get(x,y),tol);
			}
		}
	}
	@Test
	public void ycbcrToRgb_U8_Planar() {
		Planar<GrayU8> yuv = new Planar<>(GrayU8.class,10,15,3);
		Planar<GrayU8> rgb = new Planar<>(GrayU8.class,10,15,3);

		GImageMiscOps.fillUniform(yuv, rand, 0, 255);

		ColorYuv.ycbcrToRgb_U8(yuv, rgb);

		byte []expected = new byte[3];

		for( int y = 0; y < yuv.height; y++ ) {
			for( int x = 0; x < yuv.width; x++ ) {
				int r = rgb.getBand(0).get(x,y);
				int g = rgb.getBand(1).get(x,y);
				int b = rgb.getBand(2).get(x,y);

				int Y = yuv.getBand(0).get(x,y);
				int U = yuv.getBand(1).get(x,y);
				int V = yuv.getBand(2).get(x,y);

				ColorYuv.ycbcrToRgb(Y,U,V,expected);

				assertEquals(expected[0]&0xFF,r);
				assertEquals(expected[1]&0xFF,g);
				assertEquals(expected[2]&0xFF,b);
			}
		}
	}


	private static void check( double found[] , double a , double b , double c ) {
		double tol = TestColorYuv.tol * Math.max(Math.max(a,b),c);

		assertEquals(a,found[0],tol);
		assertEquals(b,found[1],tol);
		assertEquals(c, found[2], tol);
	}

	private static void check( float found[] , float a , float b , float c ) {
		double tol = TestColorYuv.tol * Math.max(Math.max(a,b),c);

		assertEquals(a,found[0],tol);
		assertEquals(b,found[1],tol);
		assertEquals(c, found[2], tol);
	}
}
