/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.segmentation.ms;

import boofcv.alg.weights.WeightDistance_F32;
import boofcv.alg.weights.WeightPixelUniform_F32;
import boofcv.alg.weights.WeightPixel_F32;
import boofcv.struct.image.ImageBase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Abeles
 */
public class TestSegmentMeanShiftSearch {

	@Test
	public void constructor() {
		WeightPixel_F32 weightSpacial = new WeightPixelUniform_F32(2,3);

		SegmentMeanShiftSearch alg = new Dummy(1,2,weightSpacial,null);

		assertEquals(5,alg.widthX);
		assertEquals(7,alg.widthY);
	}

	@Test
	public void distanceSq() {
		float a[] = new float[]{2,3,4};
		float b[] = new float[]{4,3,2};

		float found = SegmentMeanShiftSearch.distanceSq(a,b);

		assertEquals(8,found,1e-4);
	}

	public static class Dummy extends SegmentMeanShiftSearch {

		public Dummy(int maxIterations, float convergenceTol, WeightPixel_F32 weightSpacial, WeightDistance_F32 weightColor) {
			super(maxIterations, convergenceTol, weightSpacial, weightColor,false);
		}

		@Override
		public void process(ImageBase image) {}
	}

}
