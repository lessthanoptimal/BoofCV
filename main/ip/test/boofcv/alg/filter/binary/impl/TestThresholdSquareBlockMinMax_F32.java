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

package boofcv.alg.filter.binary.impl;

import boofcv.alg.filter.binary.ThresholdSquareBlockMinMax;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class TestThresholdSquareBlockMinMax_F32
		extends GenericThresholdSquareBlockMinMaxChecks<GrayF32> {

	public TestThresholdSquareBlockMinMax_F32() {
		super(GrayF32.class);
	}

	@Override
	public ThresholdSquareBlockMinMax<GrayF32, ?>
	createAlg(double textureThreshold, int requestedBlockWidth, double scale, boolean down) {
		return new ThresholdSquareBlockMinMax_F32((float)textureThreshold,requestedBlockWidth,
				(float)scale,down);
	}
}