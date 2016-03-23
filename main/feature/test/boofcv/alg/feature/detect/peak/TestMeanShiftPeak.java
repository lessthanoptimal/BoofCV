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

package boofcv.alg.feature.detect.peak;


import boofcv.alg.weights.WeightPixelGaussian_F32;
import boofcv.struct.image.GrayF32;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestMeanShiftPeak {

	GrayF32 image = new GrayF32(30,40);

	@Test
	public void setRegion_inside() {

		Helper helper = new Helper(10,0.01f);
		helper.setRadius(5);

		helper.setImage(image);
		helper.setRegion(6.2f,7.5f);

		// in the middle, there should be no clipping
		assertEquals(1.2f,helper.x0,1e-4);
		assertEquals(2.5f,helper.y0,1e-4);

		// outside top left border
		helper.setRegion(4.5f,3);
		assertEquals(0,helper.x0,1e-4);
		assertEquals(0,helper.y0,1e-4);

		// outside bottom right border
		helper.setRegion(26.5f,39f);
		assertEquals(30-11,helper.x0,1e-4);
		assertEquals(40-11,helper.y0,1e-4);

	}

	@Test
	public void searchTest() {
		// this is intentionally blank since it is covered by the abstract test
	}

	public static class Helper extends MeanShiftPeak {
		public Helper(int maxIterations, float convergenceTol) {
			super(maxIterations, convergenceTol,new WeightPixelGaussian_F32(),GrayF32.class);
		}

		@Override
		public void search(float cx, float cy) {
		}
	}
}
