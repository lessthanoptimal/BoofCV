/*
 * Copyright (c) 2011-2017, Peter Abeles. All Rights Reserved.
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

import boofcv.alg.filter.binary.ThresholdBlockMinMax;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayU8;

/**
 * @author Peter Abeles
 */
public class TestThresholdBlockMinMax_U8
		extends GenericThresholdBlockMinMaxChecks<GrayU8>
{

	public TestThresholdBlockMinMax_U8() {
		super(GrayU8.class);
	}

	@Override
	public ThresholdBlockMinMax<GrayU8, ?>
	createAlg(double textureThreshold, int requestedBlockWidth, double scale, boolean down) {
		return new ThresholdBlockMinMax_U8(textureThreshold, ConfigLength.fixed(requestedBlockWidth),
				scale,down,true);
	}
}