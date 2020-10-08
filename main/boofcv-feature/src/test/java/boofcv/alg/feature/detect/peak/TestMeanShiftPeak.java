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

package boofcv.alg.feature.detect.peak;


import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.struct.border.BorderType;
import boofcv.struct.image.GrayF32;
import boofcv.testing.BoofStandardJUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMeanShiftPeak extends BoofStandardJUnit {

	@Test
	void even_odd() {
		even_odd(true);
		even_odd(false);
	}
	void even_odd( boolean odd ) {
		WeightPixelGaussian_F32 weight = new WeightPixelGaussian_F32();

		MeanShiftPeak<GrayF32> alg = new MeanShiftPeak<>(10,1e-4f,weight,odd,GrayF32.class,BorderType.EXTENDED);
		alg.setRadius(2);
		assertEquals(odd,alg.isOdd());
		assertEquals(odd?5:4,alg.width);
		assertEquals(2,alg.radius);

		// verify that set radius correctly handles the originally specified polarity
		alg.setRadius(3);
		assertEquals(odd,alg.isOdd());
		assertEquals(odd?7:6,alg.width);
		assertEquals(3,alg.radius);
	}

	@Test
	void searchTest() {
		// this is intentionally blank since it is covered by the abstract test
	}

	public static class Helper extends MeanShiftPeak {
		public Helper(int maxIterations, float convergenceTol) {
			super(maxIterations, convergenceTol,new WeightPixelGaussian_F32(),true,GrayF32.class, BorderType.EXTENDED);
		}

		@Override
		public void search(float cx, float cy) {
		}
	}
}
