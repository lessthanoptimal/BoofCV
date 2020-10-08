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

package boofcv.alg.feature.detect.intensity.impl;

import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.filter.derivative.GradientSobel;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestImplSsdCorner_F32 extends BoofStandardJUnit {

	int width = 40;
	int height = 50;
	
	int radius=4;

	GrayF32 input = new GrayF32(width,height);

	GrayF32 derivX = new GrayF32(width,height);
	GrayF32 derivY = new GrayF32(width,height);

	GrayF32 derivXX = new GrayF32(width,height);
	GrayF32 derivXY = new GrayF32(width,height);
	GrayF32 derivYY = new GrayF32(width,height);

	/**
	 * Manually compute intensity values and see if they are the same
	 */
	@Test
	void compareToManual() {
		GImageMiscOps.fillUniform(input, rand, 0, 100);

		GradientSobel.process(input,derivX,derivY, GImageDerivativeOps.borderDerivative_F32());

		for( int i = 0; i < height; i++ ) {
			for( int j = 0; j < width; j++ ) {
				float x = derivX.get(j,i);
				float y = derivY.get(j,i);

				derivXX.set(j,i,x*x);
				derivXY.set(j,i,x*y);
				derivYY.set(j,i,y*y);
			}
		}

		GrayF32 output = new GrayF32(width,height);

		ImplSsdCorner_F32 alg = new ImplSsdCorner_F32(radius,new MockSum());
		alg.process(derivX,derivY, output);

		for( int y = radius; y < height-radius; y++ ) {
			for (int x = radius; x < width-radius; x++) {
				float xx = sum(x,y,derivXX);
				float xy = sum(x,y,derivXY);
				float yy = sum(x,y,derivYY);

				assertEquals(xx+xy+yy, output.get(x,y), 1);
			}
		}

	}
	
	public float sum( int x , int y , GrayF32 img ) {
		float ret = 0;
		
		for( int i = -radius; i <= radius; i++ ) {
			float hsum = 0;
			for( int j = -radius; j <= radius; j++ ) {
				hsum += img.get(j+x,i+y);
			}
			ret += hsum;
		}
		
		return ret;
	}

	private class MockSum implements ImplSsdCornerBase.CornerIntensity_F32
	{
		@Override
		public float compute(float totalXX, float totalXY, float totalYY) {
			return totalXX + totalXY + totalYY;
		}
	}
}
