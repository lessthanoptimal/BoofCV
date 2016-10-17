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
public class TestColorLab {

	Random rand = new Random(234);

	@Test
	public void rgbToLab_U8() {
		Planar<GrayU8> input = new Planar<>(GrayU8.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorLab.rgbToLab_U8(input, output);

		float expected[] = new float[3];

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
	public void rgbToLab_F32() {
		Planar<GrayF32> input = new Planar<>(GrayF32.class,20,25,3);
		Planar<GrayF32> output = new Planar<>(GrayF32.class,20,25,3);
		GImageMiscOps.fillUniform(input, rand, 0, 255);

		ColorLab.rgbToLab_F32(input,output);

		float expected[] = new float[3];

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

}
