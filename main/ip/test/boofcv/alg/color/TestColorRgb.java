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

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Peter Abeles
 */
public class TestColorRgb {
	Random rand = new Random(0);

	@Test
	public void scalar_U8() {
		assertEquals(0,ColorRgb.rgbToGray_Weighted(0,0,0));
		assertEquals(255,ColorRgb.rgbToGray_Weighted(255,255,255));

		for (int i = 0; i < 20; i++) {
			int r = rand.nextInt(256);
			int g = rand.nextInt(256);
			int b = rand.nextInt(256);

			int found = ColorRgb.rgbToGray_Weighted(r,g,b);
			int expected = (int)computeExpected(r,g,b);
			assertEquals(expected,found);
		}
	}

	@Test
	public void scalar_F32() {
		assertEquals(0f,ColorRgb.rgbToGray_Weighted(0f,0f,0f),1e-4f);
		assertEquals(255f,ColorRgb.rgbToGray_Weighted(255f,255f,255f),1e-4f);

		for (int i = 0; i < 20; i++) {
			float r = rand.nextFloat()*255f;
			float g = rand.nextFloat()*255f;
			float b = rand.nextFloat()*255f;

			float found = ColorRgb.rgbToGray_Weighted(r,g,b);
			float expected = (float)computeExpected(r,g,b);
			assertEquals(expected,found,1e-4f);
		};
	}

	@Test
	public void scalar_F64() {
		assertEquals(0.0,ColorRgb.rgbToGray_Weighted(0.0,0.0,0.0),1e-8);
		assertEquals(255.0,ColorRgb.rgbToGray_Weighted(255.0,255.0,255.0),1e-8);

		for (int i = 0; i < 20; i++) {
			double r = rand.nextDouble()*255;
			double g = rand.nextDouble()*255;
			double b = rand.nextDouble()*255;

			double found = ColorRgb.rgbToGray_Weighted(r,g,b);
			double expected = computeExpected(r,g,b);
			assertEquals(expected,found,1e-8);
		};
	}

	@Test
	public void planar_U8() {
		fail("implement");
	}

	@Test
	public void planar_F32() {
		fail("implement");
	}

	@Test
	public void planar_F64() {
		fail("implement");
	}

	@Test
	public void interleaved_U8() {
		fail("implement");
	}

	@Test
	public void interleaved_F32() {
		fail("implement");
	}

	@Test
	public void interleaved_F64() {
		fail("implement");
	}

	public double computeExpected( double r , double g , double b ) {
		return 0.299*r + 0.587*g + 0.114*b;
	}
}
