/*
 * Copyright (c) 2021, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.filter.binary;

import boofcv.abst.filter.binary.InputToBinary;
import boofcv.alg.filter.binary.impl.GenericThresholdCommon;
import boofcv.struct.ConfigLength;
import boofcv.struct.image.GrayF32;

/**
 * @author Peter Abeles
 */
public class TestThresholdNick extends GenericThresholdCommon<GrayF32> {
	public TestThresholdNick() {
		super(GrayF32.class);
	}
	@Override public InputToBinary<GrayF32> createAlg( int requestedBlockWidth, double scale, boolean down ) {
		return new ThresholdNick(ConfigLength.fixed(requestedBlockWidth), -0.2f, down);
	}

	public void widthLargerThanImage() {
		// perfectly acceptable
	}
}
