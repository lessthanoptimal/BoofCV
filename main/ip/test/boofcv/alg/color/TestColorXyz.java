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

/**
 * @author Peter Abeles
 */
public class TestColorXyz {

	public static final double tol = 1e-4;

	Random rand = new Random(234);

	double rgb_F64[] = new double[3];
	double xyz_F64[] = new double[3];

	float rgb_F32[] = new float[3];
	float xyz_F32[] = new float[3];


	@Test
	public void compareRGB_to_SRGB_F32() {
		float found[] = new float[3];
		float expected[] = new float[3];

		ColorXyz.rgbToXyz(100,202,90,found);
		ColorXyz.srgbToXyz(100 / 255.0f, 202 / 255.0f, 90 / 255.0f, expected);

		for( int i = 0; i < 3; i++ ) {
			assertEquals(expected[i],found[i],1e-4);
		}
	}

	@Test
	public void compareRGB_to_SRGB_F64() {
		double found[] = new double[3];
		double expected[] = new double[3];

		ColorXyz.rgbToXyz(100,202,90,found);
		ColorXyz.srgbToXyz(100/255.0,202/255.0,90/255.0,expected);

		for( int i = 0; i < 3; i++ ) {
			assertEquals(expected[i],found[i],1e-8);
		}
	}

	@Test
	public void backAndForth_srgb_F64_and_F32() {

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
		ColorXyz.srgbToXyz(r, g, b, xyz_F64);
		ColorXyz.xyzToSrgb(xyz_F64[0], xyz_F64[1], xyz_F64[2], rgb_F64);
		check(rgb_F64,r,g,b);

		// Check F32
		float fr = (float)r, fg = (float)g, fb = (float)b;
		ColorXyz.srgbToXyz(fr, fg, fb, xyz_F32);
		ColorXyz.xyzToSrgb(xyz_F32[0], xyz_F32[1], xyz_F32[2], rgb_F32);
		check(rgb_F32,fr,fg,fb);
	}

	private static void check( double found[] , double a , double b , double c ) {
		double tol = TestColorXyz.tol * Math.max(Math.max(a,b),c);

		assertEquals(a,found[0],tol);
		assertEquals(b,found[1],tol);
		assertEquals(c, found[2], tol);
	}

	private static void check( float found[] , float a , float b , float c ) {
		double tol = TestColorXyz.tol * Math.max(Math.max(a,b),c);

		assertEquals(a,found[0],tol);
		assertEquals(b,found[1],tol);
		assertEquals(c, found[2], tol);
	}

	@Test
	public void rgbToXyz_F32() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorXyz.rgbToXyz_F32(input,output);

		float expected[] = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float R = input.getBand(0).get(x,y);
				float G = input.getBand(1).get(x,y);
				float B = input.getBand(2).get(x,y);

				ColorXyz.srgbToXyz(R / 255f, G / 255f, B / 255f, expected);

				float X = output.getBand(0).get(x,y);
				float Y = output.getBand(1).get(x,y);
				float Z = output.getBand(2).get(x,y);

				assertEquals(expected[0],X,1e-4f);
				assertEquals(expected[1],Y,1e-4f);
				assertEquals(expected[2],Z,1e-4f);
			}
		}
	}

	@Test
	public void rgbToXyz_U8() {
		Planar<GrayU8> input = new Planar<>(GrayU8.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorXyz.rgbToXyz_U8(input, output);

		float expected[] = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int R = input.getBand(0).get(x,y);
				int G = input.getBand(1).get(x,y);
				int B = input.getBand(2).get(x,y);

				ColorXyz.rgbToXyz(R,G,B,expected);

				float X = output.getBand(0).get(x,y);
				float Y = output.getBand(1).get(x,y);
				float Z = output.getBand(2).get(x,y);

				assertEquals(expected[0],X,1e-4f);
				assertEquals(expected[1],Y,1e-4f);
				assertEquals(expected[2],Z,1e-4f);
			}
		}
	}

}
