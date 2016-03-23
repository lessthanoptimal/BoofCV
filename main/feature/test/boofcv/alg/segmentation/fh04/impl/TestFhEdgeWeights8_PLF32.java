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

package boofcv.alg.segmentation.fh04.impl;

import boofcv.alg.segmentation.fh04.FhEdgeWeights;
import boofcv.struct.ConnectRule;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;

/**
 * @author Peter Abeles
 */
public class TestFhEdgeWeights8_PLF32 extends GenericFhEdgeWeightsChecks<Planar<GrayF32>>{

	public TestFhEdgeWeights8_PLF32() {
		super(ImageType.pl(3, GrayF32.class), ConnectRule.EIGHT);
	}

	@Override
	public FhEdgeWeights<Planar<GrayF32>> createAlg() {
		return new FhEdgeWeights8_PLF32(3);
	}

	@Override
	public float weight(Planar<GrayF32> input, int indexA, int indexB) {

		float total = 0;
		for( int i = 0; i < 3; i++ ) {
			float a = input.getBand(i).data[indexA];
			float b = input.getBand(i).data[indexB];

			total += (a-b)*(a-b);
		}

		return (float)Math.sqrt(total);
	}
}
