/*
 * Copyright (c) 2020, Peter Abeles. All Rights Reserved.
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
import boofcv.testing.BoofStandardJUnit;
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestColorXyz extends BoofStandardJUnit {

	private final double[] rgb_F64 = new double[3];
	private final double[] xyz_F64 = new double[3];

	private final float[] rgb_F32 = new float[3];
	private final float[] xyz_F32 = new float[3];

	@Test
	void RGB_to_XYZ_to_RGB_F32_I32() {
		float[] tmp = new float[3];
		ColorXyz.rgbToXyz(100,202,90,tmp);
		int[] rgb = new int[3];
		ColorXyz.xyzToRgb(tmp[0],tmp[1],tmp[2],tmp,rgb);

		assertEquals(100,rgb[0]);
		assertEquals(202,rgb[1]);
		assertEquals(90,rgb[2]);
	}

	@Test
	void RGB_to_XYZ_to_RGB_F64_I32() {
		double[] tmp = new double[3];
		ColorXyz.rgbToXyz(100,202,90,tmp);
		int[] rgb = new int[3];
		ColorXyz.xyzToRgb(tmp[0],tmp[1],tmp[2],tmp,rgb);

		assertEquals(100,rgb[0]);
		assertEquals(202,rgb[1]);
		assertEquals(90,rgb[2]);
	}

	@Test
	void RGB_to_XYZ_to_RGB_F64_F32() {
		float[] tmp = new float[3];
		ColorXyz.rgbToXyz(100,202,90,tmp);
		float[] rgb = new float[3];
		ColorXyz.xyzToRgb(tmp[0],tmp[1],tmp[2],tmp,rgb);

		assertEquals(100,rgb[0], UtilEjml.TEST_F32);
		assertEquals(202,rgb[1], UtilEjml.TEST_F32);
		assertEquals(90 ,rgb[2], UtilEjml.TEST_F32);
	}

	@Test
	void backAndForth_linearRGB_F64_and_F32() {

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
		ColorXyz.linearRgbToXyz(r, g, b, xyz_F64);
		ColorXyz.xyzToLinearRgb(xyz_F64[0], xyz_F64[1], xyz_F64[2], rgb_F64);
		check(rgb_F64,r,g,b);

		// Check F32
		float fr = (float)r, fg = (float)g, fb = (float)b;
		ColorXyz.linearRgbToXyz(fr, fg, fb, xyz_F32);
		ColorXyz.xyzToLinearRgb(xyz_F32[0], xyz_F32[1], xyz_F32[2], rgb_F32);
		check(rgb_F32,fr,fg,fb);
	}

	private static void check(double[] found, double a , double b , double c ) {
		assertEquals(a,found[0],UtilEjml.TEST_F32);
		assertEquals(b,found[1],UtilEjml.TEST_F32);
		assertEquals(c,found[2],UtilEjml.TEST_F32);
	}

	private static void check(float[] found, float a , float b , float c ) {
		assertEquals(a,found[0],UtilEjml.TEST_F32);
		assertEquals(b,found[1],UtilEjml.TEST_F32);
		assertEquals(c,found[2],UtilEjml.TEST_F32);
	}

	@Test
	void rgbToXyz_F32() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorXyz.rgbToXyz(input,output);

		float[] expected = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float R = input.getBand(0).get(x,y);
				float G = input.getBand(1).get(x,y);
				float B = input.getBand(2).get(x,y);

				ColorXyz.rgbToXyz(R, G, B, expected);

				float X = output.getBand(0).get(x,y);
				float Y = output.getBand(1).get(x,y);
				float Z = output.getBand(2).get(x,y);

				assertEquals(expected[0],X,UtilEjml.TEST_F32);
				assertEquals(expected[1],Y,UtilEjml.TEST_F32);
				assertEquals(expected[2],Z,UtilEjml.TEST_F32);
			}
		}
	}

	@Test
	void rgbToXyz_U8() {
		Planar<GrayU8> input = new Planar<>(GrayU8.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorXyz.rgbToXyz(input, output);

		float[] expected = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				int R = input.getBand(0).get(x,y);
				int G = input.getBand(1).get(x,y);
				int B = input.getBand(2).get(x,y);

				ColorXyz.rgbToXyz(R,G,B,expected);

				float X = output.getBand(0).get(x,y);
				float Y = output.getBand(1).get(x,y);
				float Z = output.getBand(2).get(x,y);

				assertEquals(expected[0],X,UtilEjml.TEST_F32);
				assertEquals(expected[1],Y,UtilEjml.TEST_F32);
				assertEquals(expected[2],Z,UtilEjml.TEST_F32);
			}
		}
	}

	@Test
	void xyzToRgb_F32() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 1.0);

		ColorXyz.xyzToRgb(input,output);

		float[] srgb = new float[3];
		float[] expected = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float X = input.getBand(0).get(x,y);
				float Y = input.getBand(1).get(x,y);
				float Z = input.getBand(2).get(x,y);

				ColorXyz.xyzToRgb(X,Y,Z, srgb,expected);

				float R = output.getBand(0).get(x,y);
				float G = output.getBand(1).get(x,y);
				float B = output.getBand(2).get(x,y);

				assertEquals(expected[0],R,UtilEjml.TEST_F32);
				assertEquals(expected[1],G,UtilEjml.TEST_F32);
				assertEquals(expected[2],B,UtilEjml.TEST_F32);
			}
		}
	}

	@Test
	void xyzToRgb_U8() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayU8> output = new Planar<>(GrayU8.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 1.0);

		ColorXyz.xyzToRgb(input,output);

		float[] srgb = new float[3];
		int[] expected = new int[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float X = input.getBand(0).get(x,y);
				float Y = input.getBand(1).get(x,y);
				float Z = input.getBand(2).get(x,y);

				ColorXyz.xyzToRgb(X,Y,Z, srgb,expected);

				int R = output.getBand(0).get(x,y);
				int G = output.getBand(1).get(x,y);
				int B = output.getBand(2).get(x,y);

				assertEquals(expected[0],R);
				assertEquals(expected[1],G);
				assertEquals(expected[2],B);
			}
		}
	}

}
