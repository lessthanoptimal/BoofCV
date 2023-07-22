/*
 * Copyright (c) 2023, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayS32;
import boofcv.struct.image.GrayU8;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestImplSsdCorner_S16 extends BoofStandardJUnit {

	int width = 40;
	int height = 50;
	
	int radius=4;

	GrayU8 input = new GrayU8(width,height);

	GrayS16 derivX = new GrayS16(width,height);
	GrayS16 derivY = new GrayS16(width,height);

	GrayS32 derivXX = new GrayS32(width,height);
	GrayS32 derivXY = new GrayS32(width,height);
	GrayS32 derivYY = new GrayS32(width,height);

	/**
	 * Manually compute intensity values and see if they are the same
	 */
	@SuppressWarnings("unchecked")
	@Test
	void compareToManual() {
		GImageMiscOps.fillUniform(input, rand, 0, 100);

		GradientSobel.process(input, derivX, derivY, GImageDerivativeOps.borderDerivative_I32());

		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				int x = derivX.get(j, i);
				int y = derivY.get(j, i);

				derivXX.set(j, i, x * x);
				derivXY.set(j, i, x * y);
				derivYY.set(j, i, y * y);
			}
		}

		GrayF32 output = new GrayF32(width, height);

		ImplSsdCorner_S16 alg = new ImplSsdCorner_S16(radius, new MockSum());
		alg.process(derivX, derivY, output);

		for (int y = radius; y < height - radius; y++) {
			for (int x = radius; x < width - radius; x++) {
				float xx = sum(x, y, derivXX);
				float xy = sum(x, y, derivXY);
				float yy = sum(x, y, derivYY);

				assertEquals(xx + xy + yy, output.get(x, y), 1);
			}
		}
	}
	
	public int sum( int x , int y , GrayS32 img ) {
		int ret = 0;
		
		for( int i = -radius; i <= radius; i++ ) {
			int hsum = 0;
			for( int j = -radius; j <= radius; j++ ) {
				hsum += img.get(j+x,i+y);
			}
			ret += hsum;
		}
		
		return ret;
	}

	private class MockSum implements ImplSsdCornerBase.CornerIntensity_S32
	{
		@Override
		public float compute(int totalXX, int totalXY, int totalYY) {
			return totalXX + totalXY + totalYY;
		}
	}
}
