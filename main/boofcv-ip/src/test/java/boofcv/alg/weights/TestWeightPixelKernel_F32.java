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

package boofcv.alg.weights;

import boofcv.factory.filter.kernel.FactoryKernel;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
class TestWeightPixelKernel_F32 extends BoofStandardJUnit {

	@Test
	void kernelLookup() {
		WeightPixelKernel_F32 alg = new WeightPixelKernel_F32() {
			@Override
			public void setRadius(int radiusX, int radiusY, boolean odd) {}

			@Override
			public boolean isOdd() {return true;}
		};

		alg.kernel = FactoryKernel.random2D_F32(5,2,-2,2,rand);

		int index = 0;
		for( int y = -2; y <= 2; y++ )
			for( int x = -2; x <= 2; x++ , index++) {
				float found = alg.weight(x, y);
				float expected = alg.kernel.get(x+2,y+2);

				assertEquals(expected,found,1e-4);
				assertEquals(expected,alg.weightIndex(index),1e-4);
			}
	}

}
