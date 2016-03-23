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
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;

/**
* @author Peter Abeles
*/
public class TestFhEdgeWeights8_U8 extends GenericFhEdgeWeightsChecks<GrayU8> {

	public TestFhEdgeWeights8_U8() {
		super(ImageType.single(GrayU8.class), ConnectRule.EIGHT);
	}

	@Override
	public FhEdgeWeights<GrayU8> createAlg() {
		return new FhEdgeWeights8_U8();
	}

	@Override
	public float weight(GrayU8 input , int indexA, int indexB) {
		return Math.abs((input.data[indexA]&0xFF) - (input.data[indexB]&0xFF));
	}
}
