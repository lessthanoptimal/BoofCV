/*
 * Copyright (c) 2011-2020, Peter Abeles. All Rights Reserved.
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
import org.ejml.UtilEjml;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Peter Abeles
 */
class TestColorLab {

	Random rand = new Random(234);

	@Test
	void rgb_to_lab_to_rgb__F64_F32() {
		float[] tmp = new float[3];
		float[] found = new float[3];

		ColorLab.rgbToLab(100.0,200.0,70.0,tmp);
		ColorLab.labToRgb(tmp[0],tmp[1],tmp[2],tmp,found);

		assertEquals(100, found[0], UtilEjml.TEST_F32);
		assertEquals(200, found[1], UtilEjml.TEST_F32);
		assertEquals( 70, found[2], UtilEjml.TEST_F32);
	}


	@Test
	void rgb_to_lab_to_rgb__F32_F32() {
		float[] tmp = new float[3];
		float[] found = new float[3];

		ColorLab.rgbToLab(100,200,70,tmp);
		ColorLab.labToRgb(tmp[0],tmp[1],tmp[2],tmp,found);

		assertEquals(100, found[0], UtilEjml.TEST_F32);
		assertEquals(200, found[1], UtilEjml.TEST_F32);
		assertEquals( 70, found[2], UtilEjml.TEST_F32);
	}

	@Test
	void rgb_to_lab_to_rgb__I32_F32() {
		float[] tmp = new float[3];
		int[] found = new int[3];

		ColorLab.rgbToLab(100,200,70,tmp);
		ColorLab.labToRgb(tmp[0],tmp[1],tmp[2],tmp,found);

		assertEquals(100, found[0]);
		assertEquals(200, found[1]);
		assertEquals( 70, found[2]);
	}

	@Test
	void rgb_to_lab_to_rgb__I32_F64() {
		double[] tmp = new double[3];
		int[] found = new int[3];

		ColorLab.rgbToLab(100,200,70,tmp);
		ColorLab.labToRgb(tmp[0],tmp[1],tmp[2],tmp,found);

		assertEquals(100, found[0]);
		assertEquals(200, found[1]);
		assertEquals( 70, found[2]);
	}


	@Test
	void xyz_to_lab_to_xyz_F32() {
		float[] found = new float[3];

		ColorLab.xyzToLab(0.5f,0.25f,0.77f,found);
		ColorLab.labToXyz(found[0],found[1],found[2],found);

		assertEquals(0.5f, found[0], UtilEjml.TEST_F32);
		assertEquals(0.25f, found[1], UtilEjml.TEST_F32);
		assertEquals(0.77f, found[2], UtilEjml.TEST_F32);
	}

	@Test
	void xyz_to_lab_to_xyz_F64() {
		double[] found = new double[3];

		ColorLab.xyzToLab(0.5,0.25,0.77,found);
		ColorLab.labToXyz(found[0],found[1],found[2],found);

		assertEquals(0.5, found[0], UtilEjml.TEST_F32);
		assertEquals(0.25, found[1], UtilEjml.TEST_F32);
		assertEquals(0.77, found[2], UtilEjml.TEST_F32);
	}

	@Test
	void rgbToLab_U8() {
		Planar<GrayU8> input = new Planar<>(GrayU8.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorLab.rgbToLab(input, output);

		float[] expected = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float R = input.getBand(0).get(x,y);
				float G = input.getBand(1).get(x,y);
				float B = input.getBand(2).get(x,y);

				ColorLab.srgbToLab(R/255f,G/255f,B/255f,expected);

				float L = output.getBand(0).get(x,y);
				float A = output.getBand(1).get(x,y);
				float B_ = output.getBand(2).get(x,y);

				assertTrue( L >= 0f && L <= 100.0f);

				assertEquals(expected[0],L,1e-4f);
				assertEquals(expected[1],A,1e-4f);
				assertEquals(expected[2],B_,1e-4f);
			}
		}
	}

	@Test
	void rgbToLab_F32() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorLab.rgbToLab(input,output);

		float[] expected = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float R = input.getBand(0).get(x,y);
				float G = input.getBand(1).get(x,y);
				float B = input.getBand(2).get(x,y);

				ColorLab.srgbToLab(R / 255f, G / 255f, B / 255f, expected);

				float L = output.getBand(0).get(x,y);
				float A = output.getBand(1).get(x,y);
				float B_ = output.getBand(2).get(x,y);

				assertTrue( L >= 0f && L <= 100.0f);

				assertEquals(expected[0],L,1e-4f);
				assertEquals(expected[1],A,1e-4f);
				assertEquals(expected[2],B_,1e-4f);
			}
		}
	}

	@Test
	void labToRgb_U8() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayU8> output = new Planar<>(GrayU8.class,20,12,3);
		GImageMiscOps.fillUniform(input, rand, 0, 1.0);

		ColorLab.labToRgb(input,output);

		float[] srgb = new float[3];
		int[] expected = new int[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float L = input.getBand(0).get(x,y);
				float A = input.getBand(1).get(x,y);
				float B_ = input.getBand(2).get(x,y);

				ColorLab.labToRgb(L, A, B_, srgb, expected);

				int R = output.getBand(0).get(x,y);
				int G = output.getBand(1).get(x,y);
				int B = output.getBand(2).get(x,y);

				assertEquals(expected[0],R);
				assertEquals(expected[1],G);
				assertEquals(expected[2],B);
			}
		}
	}

	@Test
	void labToRgb_F32() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,12,3);
		GImageMiscOps.fillUniform(input, rand, 0, 1.0);

		ColorLab.labToRgb(input,output);

		float[] srgb = new float[3];
		float[] expected = new float[3];

		for (int y = 0; y < input.height; y++) {
			for (int x = 0; x < input.width; x++) {
				float L = input.getBand(0).get(x,y);
				float A = input.getBand(1).get(x,y);
				float B_ = input.getBand(2).get(x,y);

				ColorLab.labToRgb(L, A, B_, srgb, expected);

				float R = output.getBand(0).get(x,y);
				float G = output.getBand(1).get(x,y);
				float B = output.getBand(2).get(x,y);

				assertEquals(expected[0],R,1e-4f);
				assertEquals(expected[1],G,1e-4f);
				assertEquals(expected[2],B,1e-4f);
			}
		}
	}

}
