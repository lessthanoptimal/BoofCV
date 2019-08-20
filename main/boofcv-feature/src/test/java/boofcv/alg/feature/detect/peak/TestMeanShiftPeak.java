/*
 * Copyright (c) 2011-2019, Peter Abeles. All Rights Reserved.
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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Peter Abeles
 */
public class TestMeanShiftPeak {

	GrayF32 image = new GrayF32(30,40);

	@Test
	void even_odd() {
		fail("Implement");
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
